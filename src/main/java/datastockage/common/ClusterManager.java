package common;

import dir.DirServer;
import osd.OSDServer;

import java.util.ArrayList;
import java.util.List;

public class ClusterManager {

    private static Thread dirThread;
    private static DirServer dirInstance;

    private static final List<Thread> osdThreads = new ArrayList<>();
    private static final List<OSDServer> osdInstances = new ArrayList<>();

    private static volatile boolean running = false;

    /** Démarre le cluster DIR + OSDs */
    public static synchronized void startCluster() {

        if (running) {
            System.out.println("[ClusterManager] Cluster déjà démarré");
            return;
        }

        System.out.println("[ClusterManager] Démarrage du cluster...");

        try {
            // ── 1️⃣ Start DIR ─────────────────────
            dirInstance = new DirServer();
            dirThread = new Thread(dirInstance, "DirServer");
            dirThread.start();

            // Attendre un peu que le DIR démarre
            Thread.sleep(800);

            // ── 2️⃣ Start OSDs ────────────────────
            for (int i = 0; i < 4; i++) {
                int port = 9001 + i;

                OSDServer osd = new OSDServer(port);
                Thread t = new Thread(osd, "OSD-" + port);

                osdInstances.add(osd);
                osdThreads.add(t);

                t.start();
            }

            running = true;

            System.out.println("[ClusterManager] Cluster démarré (DIR + "
                    + osdInstances.size() + " OSD)");

        } catch (Exception e) {
            System.err.println("[ClusterManager] Erreur au démarrage");
            e.printStackTrace();
            stopCluster(); // rollback propre
        }
    }

    /** Stoppe le cluster proprement */
    public static synchronized void stopCluster() {

        if (!running) {
            System.out.println("[ClusterManager] Cluster déjà arrêté");
            return;
        }

        System.out.println("[ClusterManager] Arrêt du cluster...");

        // ── Stop OSDs d'abord ───────────────────
        for (OSDServer osd : osdInstances) {
            osd.stop();
        }

        for (Thread t : osdThreads) {
            try { t.join(1000); } catch (InterruptedException ignored) {}
        }

        osdInstances.clear();
        osdThreads.clear();

        // ── Stop DIR ensuite ────────────────────
        if (dirInstance != null) {
            dirInstance.stop();
        }

        if (dirThread != null) {
            try { dirThread.join(1000); } catch (InterruptedException ignored) {}
        }

        running = false;

        System.out.println("[ClusterManager] Cluster arrêté");
    }

    public static boolean isClusterRunning() {
        return running;
    }

    public static DirServer getDirServer() {
        return dirInstance;
    }
}
