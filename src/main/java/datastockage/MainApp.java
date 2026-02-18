import view.LoginView;
import common.ClusterManager;
import server.HTTPServer;
import javax.swing.SwingUtilities;

public class MainApp {
    
    private static HTTPServer httpServer;
    
    public static void main(String[] args) {
        System.out.println("╔═════════════════════════════════════════════════════╗");
        System.out.println("║        CLOUD DISTRIBUÉ - DÉMARRAGE                 ║");
        System.out.println("╚═════════════════════════════════════════════════════╝");
        
        // ─────────────────────────────────────────────────────────────────
        // 1️⃣ Démarrer le Cluster (inclut DIR + OSDs)
        // ─────────────────────────────────────────────────────────────────
        new Thread(() -> {
            try {
                System.out.println("[MainApp] Démarrage du cluster (DIR + OSDs)...");
                ClusterManager.startCluster();
            } catch (Exception e) {
                System.err.println("[MainApp] ✗ Erreur démarrage cluster: " + e.getMessage());
            }
        }, "cluster-startup").start();

        // ─────────────────────────────────────────────────────────────────
        // 2️⃣ Démarrer le serveur HTTP REST (port 8080)
        // ─────────────────────────────────────────────────────────────────
        new Thread(() -> {
            try {
                Thread.sleep(3000); // Attendre que cluster démarre
                httpServer = new HTTPServer(ClusterManager.getDirServer());
                httpServer.start();
                System.out.println("[MainApp] ✓ Serveur HTTP/REST démarré sur port 8080");
                System.out.println("[MainApp] 🌐 Accès: http://10.134.17.222:8080");
            } catch (Exception e) {
                System.err.println("[MainApp] ✗ Erreur démarrage HTTP: " + e.getMessage());
                e.printStackTrace();
            }
        }, "http-startup").start();

        // ─────────────────────────────────────────────────────────────────
        // 3️⃣ Lancer l'interface de login (optionnel)
        // ─────────────────────────────────────────────────────────────────
        SwingUtilities.invokeLater(() -> new LoginView().setVisible(true));

        // ─────────────────────────────────────────────────────────────────
        // 4️⃣ Shutdown hook pour arrêt propre
        // ─────────────────────────────────────────────────────────────────
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\n[MainApp] 🛑 Arrêt du système...");
            
            if (httpServer != null) {
                httpServer.stop();
            }
            
            ClusterManager.stopCluster();
            
            System.out.println("[MainApp] ✓ Système arrêté proprement");
        }));
    }
}
