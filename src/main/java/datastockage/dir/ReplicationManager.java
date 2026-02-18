package dir;

import common.FileMeta;
import common.SlaveInfo;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * Gère toute la logique de réplication avancée :
 *
 *  1. HealthChecker  — évalue le statut de chaque OSD toutes les 5s
 *  2. RepairEngine   — détecte les chunks sous-répliqués et les re-réplique
 *  3. Placement      — choisit les OSD cibles en évitant les doublons
 *
 * Tolérance aux pannes avec replicationFactor=3 :
 *   - 1 OSD DOWN  → chunk toujours lisible (2 copies restantes)
 *   - 2 OSD DOWN  → chunk toujours lisible (1 copie restante)
 *   - 3 OSD DOWN  → chunk perdu (cas extrême)
 *   + auto-réparation dès qu'un OSD revient UP
 */
public class ReplicationManager {

    // ── Configuration ────────────────────────────────────────────────────────
    public static final int REPLICATION_FACTOR = 3;   // copies par chunk

    // ── État partagé (injecté depuis DirServer) ───────────────────────────────
    private final Map<String, SlaveInfo>  osds;
    private final Map<String, OSDHealth>  health;
    private final Map<String, FileMeta>   files;
    private final MetaStore               metaStore;

    // ── File de réparation ───────────────────────────────────────────────────
    private final BlockingQueue<RepairTask> repairQueue = new LinkedBlockingQueue<>();

    // ── Stats ────────────────────────────────────────────────────────────────
    private volatile int totalRepairs    = 0;
    private volatile int failedRepairs   = 0;

    public ReplicationManager(Map<String, SlaveInfo> osds,
                              Map<String, OSDHealth>  health,
                              Map<String, FileMeta>   files,
                              MetaStore               metaStore) {
        this.osds      = osds;
        this.health    = health;
        this.files     = files;
        this.metaStore = metaStore;
    }

    // ════════════════════════════════════════════════════════════════════════
    //  DÉMARRAGE
    // ════════════════════════════════════════════════════════════════════════

    public void start() {
        startHealthChecker();
        startRepairEngine();
        System.out.println("[Replication] Manager started — factor=" + REPLICATION_FACTOR);
    }

    // ════════════════════════════════════════════════════════════════════════
    //  1. HEALTH CHECKER
    //     Évalue le statut de chaque OSD toutes les 5 secondes.
    //     Si un OSD passe DOWN → planifie la réparation de ses chunks.
    // ════════════════════════════════════════════════════════════════════════

    private void startHealthChecker() {
        Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "health-checker");
            t.setDaemon(true);
            return t;
        }).scheduleAtFixedRate(() -> {
            for (OSDHealth h : health.values()) {
                OSDHealth.Status before = h.status;
                h.evaluate();
                OSDHealth.Status after  = h.status;

                // Transition UP/SUSPECTED → DOWN : planifier réparation
                if (before != OSDHealth.Status.DOWN
                        && after == OSDHealth.Status.DOWN) {
                    System.out.println("[HealthChecker] OSD DOWN detected: " + h.osdId
                            + " → scheduling repair");
                    scheduleRepairForOSD(h.osdId);
                }

                // Transition DOWN → UP : log (la réparation est déjà planifiée)
                if (before == OSDHealth.Status.DOWN
                        && after == OSDHealth.Status.UP) {
                    System.out.println("[HealthChecker] OSD RECOVERED: " + h.osdId);
                }

                // Log seulement lors des transitions ou toutes les 30s pour DOWN
                boolean isTransition = before != after;
                boolean periodic     = after == OSDHealth.Status.DOWN
                        && h.silenceSeconds() % 30 == 0;
                if (isTransition || periodic) {
                    long sil = h.silenceSeconds();
                    String silStr = sil < 0 ? "never seen" : sil + "s";
                    System.out.println("[HealthChecker] " + h.osdId
                            + " → " + after
                            + " (silence " + silStr + ")");
                }
            }
        }, 5, 5, TimeUnit.SECONDS);
    }

    /**
     * Pour un OSD tombé, trouve tous les chunks qui y étaient stockés
     * et les ajoute à la file de réparation.
     */
    private void scheduleRepairForOSD(String deadOsdId) {
        for (FileMeta meta : files.values()) {
            for (String chunkId : meta.chunkIds) {
                List<String> locs = meta.chunkLocations.get(chunkId);
                if (locs != null && locs.contains(deadOsdId)) {
                    int alive = countAliveReplicas(locs);
                    if (alive < REPLICATION_FACTOR) {
                        RepairTask task = new RepairTask(
                                meta.fileName, chunkId,
                                alive, REPLICATION_FACTOR);
                        repairQueue.offer(task);
                        System.out.println("[HealthChecker] Queued repair: " + task);
                    }
                }
            }
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    //  2. REPAIR ENGINE
    //     Consomme la file de réparation en continu.
    //     Scan complet toutes les 30s pour détecter les cas manqués.
    // ════════════════════════════════════════════════════════════════════════

    private void startRepairEngine() {
        // Thread consommateur de la file
        Thread consumer = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    RepairTask task = repairQueue.poll(10, TimeUnit.SECONDS);
                    if (task != null) executeRepair(task);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }, "repair-engine");
        consumer.setDaemon(true);
        consumer.start();

        // Scan complet périodique (filet de sécurité)
        Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "repair-scanner");
            t.setDaemon(true);
            return t;
        }).scheduleAtFixedRate(this::fullRepairScan, 30, 30, TimeUnit.SECONDS);
    }

    /**
     * Scan tous les chunks et enfile ceux qui sont sous-répliqués.
     */
    void fullRepairScan() {
        int queued = 0;
        for (FileMeta meta : files.values()) {
            for (String chunkId : meta.chunkIds) {
                List<String> locs = meta.chunkLocations.get(chunkId);
                if (locs == null) continue;

                // Nettoyer les entrées d'OSD qui n'existent plus
                locs.removeIf(id -> !osds.containsKey(id));

                int alive = countAliveReplicas(locs);
                if (alive < REPLICATION_FACTOR) {
                    RepairTask task = new RepairTask(
                            meta.fileName, chunkId,
                            alive, REPLICATION_FACTOR);
                    repairQueue.offer(task);
                    queued++;
                }
            }
        }
        if (queued > 0)
            System.out.println("[RepairScanner] " + queued + " chunk(s) queued for repair");
    }

    /**
     * Exécute une tâche de réparation :
     * copie le chunk depuis un OSD vivant vers de nouveaux OSD.
     */
    private void executeRepair(RepairTask task) {
        FileMeta meta = files.get(task.fileName);
        if (meta == null) return;

        List<String> locs = meta.chunkLocations.get(task.chunkId);
        if (locs == null) return;

        // Trouver une source vivante
        SlaveInfo source = findAliveSource(locs);
        if (source == null) {
            System.out.println("[RepairEngine] ❌ No alive source for chunk: " + task.chunkId);
            failedRepairs++;
            return;
        }

        // Combien de copies manquent ?
        int needed = REPLICATION_FACTOR - countAliveReplicas(locs);
        if (needed <= 0) return; // déjà réparé entre-temps

        // Choisir les OSD cibles (pas déjà dans locs)
        List<SlaveInfo> targets = pickTargets(locs, needed);
        if (targets.isEmpty()) {
            System.out.println("[RepairEngine] ⚠️ Not enough alive OSD to repair: "
                    + task.chunkId + " (need " + needed + " more)");
            return;
        }

        // Lire le chunk depuis la source
        byte[] chunkData;
        try {
            chunkData = fetchChunk(source, task.chunkId);
        } catch (Exception e) {
            System.out.println("[RepairEngine] ❌ Failed to fetch chunk from "
                    + source.id + ": " + e.getMessage());
            failedRepairs++;
            return;
        }

        // Envoyer vers les nouvelles cibles
        boolean anySuccess = false;
        for (SlaveInfo target : targets) {
            if (sendChunk(target, task.chunkId, chunkData)) {
                synchronized (locs) {
                    if (!locs.contains(target.id)) locs.add(target.id);
                }
                anySuccess = true;
                totalRepairs++;
                System.out.println("[RepairEngine] ✅ Re-replicated chunk "
                        + task.chunkId + " → " + target.id
                        + " (" + countAliveReplicas(locs) + "/"
                        + REPLICATION_FACTOR + " replicas)");
            }
        }

        // Persister les nouvelles locations si réparation réussie
        if (anySuccess) {
            try {
                metaStore.save(meta);
            } catch (Exception e) {
                System.err.println("[RepairEngine] Failed to persist meta: " + e.getMessage());
            }
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    //  3. PLACEMENT — choisir les OSD pour un nouveau chunk
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Sélectionne N OSD vivants pour stocker un nouveau chunk.
     * Privilégie les OSD les moins chargés (load balancing).
     */
    public List<SlaveInfo> pickForWrite(int n) {
        List<SlaveInfo> candidates = health.entrySet().stream()
                .filter(e -> e.getValue().isAlive())
                .sorted(Comparator.comparingInt(e -> e.getValue().chunkCount))
                .map(e -> osds.get(e.getKey()))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        if (candidates.size() < n) {
            System.out.println("[Replication] ⚠️ Only " + candidates.size()
                    + " OSD alive (wanted " + n + ")");
        }

        return candidates.subList(0, Math.min(n, candidates.size()));
    }

    /**
     * Pour la réparation : choisit des OSD cibles qui n'ont pas encore le chunk.
     */
    private List<SlaveInfo> pickTargets(List<String> existing, int needed) {
        // Priorité 1 : OSD UP non déjà dans existing
        List<SlaveInfo> candidates = health.entrySet().stream()
                .filter(e -> e.getValue().status == OSDHealth.Status.UP)
                .filter(e -> !existing.contains(e.getKey()))
                .sorted(Comparator.comparingInt(e -> e.getValue().chunkCount))
                .map(e -> osds.get(e.getKey()))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        // Si pas assez, compléter avec SUSPECTED (mieux que rien)
        if (candidates.size() < needed) {
            List<SlaveInfo> suspected = health.entrySet().stream()
                    .filter(e -> e.getValue().status == OSDHealth.Status.SUSPECTED)
                    .filter(e -> !existing.contains(e.getKey()))
                    .filter(e -> candidates.stream().noneMatch(s -> s.id.equals(e.getKey())))
                    .sorted(Comparator.comparingInt(e -> e.getValue().chunkCount))
                    .map(e -> osds.get(e.getKey()))
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
            candidates.addAll(suspected);
        }

        return candidates.stream().limit(needed).collect(Collectors.toList());
    }

    // ════════════════════════════════════════════════════════════════════════
    //  HELPERS
    // ════════════════════════════════════════════════════════════════════════

    private int countAliveReplicas(List<String> locs) {
        int count = 0;
        for (String id : locs) {
            OSDHealth h = health.get(id);
            if (h != null && h.isAlive()) count++;
        }
        return count;
    }

    private SlaveInfo findAliveSource(List<String> locs) {
        for (String id : locs) {
            OSDHealth h = health.get(id);
            if (h != null && h.isAlive()) {
                SlaveInfo s = osds.get(id);
                if (s != null) return s;
            }
        }
        return null;
    }

   private byte[] fetchChunk(SlaveInfo s, String chunkId) throws Exception {
    try (Socket socket = new Socket(s.host, s.port);
         DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
         DataInputStream dis = new DataInputStream(socket.getInputStream())) {

        dos.writeUTF("GET_CHUNK");
        dos.writeUTF(chunkId);

        String status = dis.readUTF();
        if (!"OK".equals(status)) throw new IOException("Chunk not found");

        int size = dis.readInt();
        byte[] data = new byte[size];
        dis.readFully(data);
        return data;
    }
}

private boolean sendChunk(SlaveInfo osd, String chunkId, byte[] chunk) {
    try (Socket socket = new Socket(osd.host, osd.port);
         DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
         DataInputStream dis = new DataInputStream(socket.getInputStream())) {

        dos.writeUTF("STORE_CHUNK");
        dos.writeUTF(chunkId);
        dos.writeInt(chunk.length);
        dos.write(chunk);
        dos.flush();

        String resp = dis.readUTF();
        if (!"OK".equals(resp)) return false;

        health.get(osd.id).chunkCount++;
        return true;

    } catch (Exception e) {
        System.out.println("[RepairEngine] Send failed → " + osd.id + ": " + e.getMessage());
        return false;
    }
}

    // ── Stats publiques ───────────────────────────────────────────────────────

    public String getStats() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== Replication Stats ===\n");
        sb.append("Factor      : ").append(REPLICATION_FACTOR).append("\n");
        sb.append("Repairs OK  : ").append(totalRepairs).append("\n");
        sb.append("Repairs KO  : ").append(failedRepairs).append("\n");
        sb.append("Queue size  : ").append(repairQueue.size()).append("\n\n");

        sb.append("=== OSD Health ===\n");
        for (OSDHealth h : health.values()) {
            sb.append(h.osdId)
                    .append(" | ").append(h.status)
                    .append(" | chunks=").append(h.chunkCount)
                    .append("\n");
        }
        return sb.toString();
    }
}
