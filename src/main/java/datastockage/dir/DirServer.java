package dir;

import common.*;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class DirServer implements Runnable {

    private final Map<String, SlaveInfo> osds = new ConcurrentHashMap<>();
    private final Map<String, OSDHealth> health = new ConcurrentHashMap<>();

    public static final Map<String, FileMeta> files = new ConcurrentHashMap<>();
    private final MetaStore metaStore = new MetaStore("dir-meta");

    private final ReplicationManager replication;
    private volatile boolean running = true;
    private ServerSocket serverSocket;

    public DirServer() {
        replication = new ReplicationManager(osds, health, files, metaStore);
        // Charger les métadonnées persistées
        try {
            Map<String, FileMeta> loaded = metaStore.loadAll();
            files.putAll(loaded);
            System.out.println("[DIR] Métadonnées chargées: " + loaded.size() + " fichiers");
        } catch (Exception e) {
            System.err.println("[DIR] Erreur chargement métadonnées: " + e.getMessage());
        }
        replication.start();
    }

    public Map<String, FileMeta> getFiles() {
        return files;
    }

    public static void main(String[] args) throws Exception {
        new Thread(new DirServer()).start();
    }

    @Override
public void run() {
    try {
        serverSocket = new ServerSocket(7000, 50, java.net.InetAddress.getByName("0.0.0.0"));
        System.out.println("[DIR] Serveur lancé sur le port 7000 (toutes interfaces)");

        while (running) {
            Socket client = serverSocket.accept();
            System.out.println("[DIR] Client connecté : " + client.getInetAddress());
            new Thread(() -> handleClient(client)).start();
        }

    } catch (java.net.BindException ex) {
        System.err.println("Port 7000 deja utilise. Le serveur ne peut pas demarrer.");
    } catch (java.net.SocketException ex) {
        // Normal lors de l'arret du serveur
        if (running) ex.printStackTrace();
    } catch (Exception e) {
        e.printStackTrace();
    }
}


    private void handleClient(Socket socket) {
        try (DataInputStream dis = new DataInputStream(socket.getInputStream());
                DataOutputStream dos = new DataOutputStream(socket.getOutputStream())) {

            String command = dis.readUTF();

            switch (command) {

                case "UPLOAD":
                    handleUpload(dis, dos);
                    break;

                case "DOWNLOAD":
                    handleDownload(dis, dos);
                    break;

                case "HEARTBEAT":
                    handleHeartbeat(dis, socket);
                    break;

                case "CLUSTER":
                    StringBuilder clusterSb = new StringBuilder();
                    for (var e : osds.entrySet()) {
                        OSDHealth h = health.get(e.getKey());
                        String status = h != null ? h.status.toString() : "UNKNOWN";
                        clusterSb.append(e.getKey()).append("|").append(status)
                                .append("|").append(e.getValue().host)
                                .append(":").append(e.getValue().port).append("\n");
                    }
                    dos.writeUTF(clusterSb.toString());
                    dos.flush();
                    break;

                case "CHUNKS":
                    dos.writeUTF(buildChunks());
                    dos.flush();
                    break;

                case "STATS":
                    if (replication != null) {
                        dos.writeUTF(replication.getStats());
                    } else {
                        dos.writeUTF("ReplicationManager non initialisé");
                    }
                    dos.flush();
                    break;

                case "HEALTH":
                    StringBuilder healthSb = new StringBuilder();
                    for (OSDHealth h : health.values()) {
                        healthSb.append(h.osdId)
                                .append("|").append(h.status)
                                .append("|chunks=").append(h.chunkCount)
                                .append("\n");
                    }
                    dos.writeUTF(healthSb.toString());
                    dos.flush();
                    break;

                case "LIST_FILES":
                    handleListFiles(dis, dos);
                    break;

                case "DELETE_FILE":
                    handleDeleteFile(dis, dos);
                    break;

                case "LIST_ALL":
                    handleListAll(dos);
                    break;

                default:
                    dos.writeUTF("UNKNOWN_COMMAND");
                    dos.flush();
                    break;
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void handleUpload(DataInputStream dis, DataOutputStream dos) throws IOException {
        String userId = dis.readUTF();
        String fileName = dis.readUTF();
        long totalSize = dis.readLong();
        String fileKey = userId + "_" + fileName;

        FileMeta meta = new FileMeta();

        meta.fileName = fileName;
        meta.ownerId = userId;
        meta.totalSize = totalSize;

        int chunkSize = 1_000_000;
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();

        System.out.println("[DIR] Upload: " + fileKey + " (" + totalSize + " bytes)");

        while (true) {
            int size = dis.readInt();
            if (size == -1)
                break;

            byte[] data = new byte[size];
            dis.readFully(data);
            buffer.write(data);

            if (buffer.size() >= chunkSize) {
                processChunk(buffer.toByteArray(), userId, meta);
                buffer.reset();
            }
        }

        if (buffer.size() > 0)
            processChunk(buffer.toByteArray(), userId, meta);

        files.put(fileKey, meta);
        metaStore.save(meta);

        System.out.println("[DIR] Upload terminé: " + fileKey);
    }

    public void handleDownload(DataInputStream dis, DataOutputStream dos) throws IOException {

        String fileName = dis.readUTF();
        String userId = dis.readUTF();
        String fileKey = userId + "_" + fileName;

        FileMeta meta = files.get(fileKey);

        if (meta == null || meta.chunkIds.isEmpty()) {
            dos.writeBoolean(false);
            dos.flush();
            System.out.println("[DIR] Download KO - fichier introuvable: " + fileKey);
            return;
        }

        // Reassembler le fichier depuis les OSD
        ByteArrayOutputStream assembled = new ByteArrayOutputStream();
        boolean success = true;

        for (String chunkId : meta.chunkIds) {
            List<String> locs = meta.chunkLocations.get(chunkId);
            if (locs == null || locs.isEmpty()) {
                success = false;
                break;
            }

            byte[] chunkData = null;
            for (String osdId : locs) {
                SlaveInfo osd = osds.get(osdId);
                if (osd == null) continue;
                OSDHealth h = health.get(osdId);
                if (h != null && !h.isAlive()) continue;

                try {
                    chunkData = fetchChunkFromOSD(osd, chunkId);
                    if (chunkData != null) break;
                } catch (Exception e) {
                    System.out.println("[DIR] Chunk fetch failed from " + osdId + ": " + e.getMessage());
                }
            }

            if (chunkData == null) {
                success = false;
                break;
            }
            assembled.write(chunkData);
        }

        if (!success) {
            dos.writeBoolean(false);
            dos.flush();
            System.out.println("[DIR] Download KO - chunks manquants: " + fileKey);
            return;
        }

        byte[] fileData = assembled.toByteArray();
        dos.writeBoolean(true);
        dos.writeLong(fileData.length);
        dos.write(fileData);
        dos.flush();
        System.out.println("[DIR] Download OK: " + fileKey + " (" + fileData.length + " bytes)");
    }

    private byte[] fetchChunkFromOSD(SlaveInfo osd, String chunkId) throws Exception {
        try (Socket socket = new Socket(osd.host, osd.port);
             DataOutputStream out = new DataOutputStream(socket.getOutputStream());
             DataInputStream in = new DataInputStream(socket.getInputStream())) {

            out.writeUTF("GET_CHUNK");
            out.writeUTF(chunkId);
            out.flush();

            String status = in.readUTF();
            if (!"OK".equals(status)) return null;

            int size = in.readInt();
            byte[] data = new byte[size];
            in.readFully(data);
            return data;
        }
    }

    public void handleHeartbeat(DataInputStream dis, Socket socket) throws IOException {

        String osdId = dis.readUTF();
        int port = dis.readInt();
        OSDHealth h = health.get(osdId);
        // 1️⃣ Enregistrer OSD si nouveau (utilise l'adresse IP du client)
        String osdHost = socket.getInetAddress().getHostAddress();
        osds.putIfAbsent(osdId, new SlaveInfo(osdId, osdHost, port));

        // 2️⃣ Créer health si nouveau
        health.putIfAbsent(osdId, new OSDHealth(osdId));

        // 3️⃣ Marquer beat
        health.get(osdId).beat();
    }

    private void processChunk(byte[] chunk, String userId, FileMeta meta) {
        String chunkId = userId + "_" + UUID.randomUUID();

        List<SlaveInfo> targets = replication.pickForWrite(ReplicationManager.REPLICATION_FACTOR);
        meta.chunkIds.add(chunkId);
        meta.chunkLocations.put(chunkId, new ArrayList<>());

        int stored = 0;
        for (SlaveInfo s : targets) {
            if (sendChunkSocket(s, chunkId, chunk)) {
                meta.chunkLocations.get(chunkId).add(s.id);
                health.get(s.id).chunkCount++;
                stored++;
            }
        }
        System.out.println("[DIR] Chunk " + chunkId.substring(0, 8) + "... stored on " + stored + " OSD");
    }

    private boolean sendChunkSocket(SlaveInfo osd, String chunkId, byte[] chunk) {
        try (Socket socket = new Socket(osd.host, osd.port);
                DataOutputStream dos = new DataOutputStream(socket.getOutputStream())) {

            dos.writeUTF("STORE_CHUNK");
            dos.writeUTF(chunkId);
            dos.writeInt(chunk.length);
            dos.write(chunk);
            dos.flush();
            return true;

        } catch (Exception e) {
            System.out.println("[DIR] OSD unreachable: " + osd.id);
            return false;
        }
    }

    public void stop() {
        running = false;
        try {
            if (serverSocket != null && !serverSocket.isClosed())
                serverSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String buildChunks() {
        StringBuilder sb = new StringBuilder();

        if (files.isEmpty()) {
            return "Aucun fichier enregistré\n";
        }

        for (FileMeta m : files.values()) {
            sb.append("FILE=").append(m.fileName).append("\n");

            for (String chunk : m.chunkIds) {
                sb.append("  ")
                        .append(chunk.substring(0, 8))
                        .append("... => ")
                        .append(m.chunkLocations.get(chunk))
                        .append("\n");
            }
        }

        return sb.toString();
    }

    private void handleListFiles(DataInputStream dis, DataOutputStream dos) throws IOException {
    String userId = dis.readUTF();
    StringBuilder sb = new StringBuilder();

    for (FileMeta meta : files.values()) {
        if (meta.ownerId.equals(userId)) {
            long totalSize = meta.totalSize > 0 ? meta.totalSize : meta.chunkIds.size() * 1_000_000L;
            String dateUpload = "unknown";

            int chunkCount = meta.chunkIds.size();

            int maxReplicas = meta.chunkLocations.values().stream()
                    .mapToInt(List::size)
                    .max()
                    .orElse(0);

            sb.append(meta.fileName).append("|")
              .append(totalSize).append("|")
              .append(dateUpload).append("|")
              .append(chunkCount).append("|")
              .append(maxReplicas).append("\n");
        }
    }

    dos.writeUTF(sb.toString());
    dos.flush();
}

    private void handleDeleteFile(DataInputStream dis, DataOutputStream dos) throws IOException {
        String userId = dis.readUTF();
        String fileName = dis.readUTF();
        String fileKey = userId + "_" + fileName;

        FileMeta meta = files.remove(fileKey);
        if (meta == null) {
            dos.writeUTF("NOT_FOUND");
            dos.flush();
            return;
        }

        // Supprimer les chunks sur les OSD
        for (String chunkId : meta.chunkIds) {
            List<String> locs = meta.chunkLocations.get(chunkId);
            if (locs != null) {
                for (String osdId : locs) {
                    SlaveInfo osd = osds.get(osdId);
                    if (osd != null) {
                        try (Socket socket = new Socket(osd.host, osd.port);
                             DataOutputStream out = new DataOutputStream(socket.getOutputStream())) {
                            out.writeUTF("DELETE_CHUNK");
                            out.writeUTF(chunkId);
                            out.flush();
                        } catch (Exception e) {
                            System.err.println("[DIR] Delete chunk error: " + e.getMessage());
                        }
                    }
                }
            }
        }

        try {
            metaStore.delete(meta.fileName);
        } catch (Exception e) {
            System.err.println("[DIR] MetaStore delete error: " + e.getMessage());
        }

        dos.writeUTF("OK");
        dos.flush();
        System.out.println("[DIR] Fichier supprimé: " + fileKey);
    }

    private void handleListAll(DataOutputStream dos) throws IOException {
        StringBuilder sb = new StringBuilder();
        for (FileMeta meta : files.values()) {
            long totalSize = meta.totalSize > 0 ? meta.totalSize : meta.chunkIds.size() * 1_000_000L;
            sb.append(meta.ownerId).append("|")
              .append(meta.fileName).append("|")
              .append(totalSize).append("|")
              .append(meta.chunkIds.size()).append("\n");
        }
        dos.writeUTF(sb.toString());
        dos.flush();
    }

    // ════════════════════════════════════════════════════════════════════════
    // API REST - MÉTHODES POUR SERVEUR HTTP
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Gère l'upload depuis l'API REST HTTP
     */
    public boolean handleUploadData(String userId, String fileName, byte[] fileData) {
        try {
            String fileKey = userId + "_" + fileName;
            FileMeta meta = new FileMeta();
            meta.fileName = fileName;
            meta.ownerId = userId;
            meta.totalSize = fileData.length;

            int chunkSize = 1_000_000;
            int offset = 0;

            System.out.println("[DIR-HTTP] Upload: " + fileKey + " (" + fileData.length + " bytes)");

            // Diviser en chunks et distribuer
            while (offset < fileData.length) {
                int size = Math.min(chunkSize, fileData.length - offset);
                byte[] chunk = new byte[size];
                System.arraycopy(fileData, offset, chunk, 0, size);

                processChunk(chunk, userId, meta);
                offset += size;
            }

            files.put(fileKey, meta);
            metaStore.save(meta);

            System.out.println("[DIR-HTTP] Upload terminé: " + fileKey);
            return true;
        } catch (Exception e) {
            System.err.println("[DIR-HTTP] Upload error: " + e.getMessage());
            return false;
        }
    }

    /**
     * Gère le download depuis l'API REST HTTP
     */
    public byte[] handleDownloadRequest(String fileName, String userId) {
        try {
            String fileKey = userId + "_" + fileName;
            FileMeta meta = files.get(fileKey);

            if (meta == null || meta.chunkIds.isEmpty()) {
                System.out.println("[DIR-HTTP] Download KO - fichier introuvable: " + fileKey);
                return null;
            }

            // Reassembler le fichier depuis les OSD
            ByteArrayOutputStream assembled = new ByteArrayOutputStream();
            boolean success = true;

            for (String chunkId : meta.chunkIds) {
                List<String> locs = meta.chunkLocations.get(chunkId);
                if (locs == null || locs.isEmpty()) {
                    success = false;
                    break;
                }

                byte[] chunkData = null;
                // Essayer chaque réplica du chunk
                for (String osdId : locs) {
                    SlaveInfo osd = osds.get(osdId);
                    if (osd == null) continue;
                    
                    OSDHealth h = health.get(osdId);
                    if (h != null && !h.isAlive()) {
                        System.out.println("[DIR-HTTP] OSD " + osdId + " is DOWN, trying next replica...");
                        continue;
                    }

                    try {
                        chunkData = fetchChunkFromOSD(osd, chunkId);
                        if (chunkData != null) {
                            System.out.println("[DIR-HTTP] Chunk fetched from " + osdId);
                            break;
                        }
                    } catch (Exception e) {
                        System.out.println("[DIR-HTTP] Chunk fetch failed from " + osdId + ": " + e.getMessage());
                    }
                }

                if (chunkData == null) {
                    System.err.println("[DIR-HTTP] All replicas of chunk " + chunkId + " are down!");
                    success = false;
                    break;
                }
                assembled.write(chunkData);
            }

            if (!success) {
                System.out.println("[DIR-HTTP] Download KO - chunks manquants: " + fileKey);
                return null;
            }

            byte[] fileData = assembled.toByteArray();
            System.out.println("[DIR-HTTP] Download OK: " + fileKey + " (" + fileData.length + " bytes)");
            return fileData;
        } catch (Exception e) {
            System.err.println("[DIR-HTTP] Download error: " + e.getMessage());
            return null;
        }
    }

    /**
     * Retourne l'état du cluster au format texte
     */
    public String getClusterStatus() {
        StringBuilder sb = new StringBuilder();
        for (var e : osds.entrySet()) {
            String osdId = e.getKey();
            SlaveInfo osd = e.getValue();
            OSDHealth h = health.get(osdId);
            String status = h != null ? h.status.toString() : "UNKNOWN";
            
            sb.append(osdId).append("|").append(status)
                    .append("|").append(osd.host)
                    .append(":").append(osd.port).append("\n");
        }
        return sb.toString();
    }

}
