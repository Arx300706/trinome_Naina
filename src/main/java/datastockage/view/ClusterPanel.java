package view;

import common.ClusterManager;
import model.Domain;
import service.DomainService;
import service.FileService;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.util.List;

/**
 * Panneau de gestion du cluster avec domaines et sockets TCP.
 */
public class ClusterPanel extends JPanel {

    private JTextArea osdArea;
    private JTextArea chunkArea;
    private JTextArea statsArea;
    private JTextArea repairArea;

    private JLabel statusBar = new JLabel(" Connexion...", SwingConstants.CENTER);
    private JLabel statusLabel;

    private JButton startBtn;
    private JButton stopBtn;
    private JButton refreshBtn;
    private JCheckBox useDistributedCheckbox;

    private Timer refreshTimer;
    private DefaultListModel<String> filesModel;

    public ClusterPanel() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Barre de statut globale
        statusBar.setFont(new Font("Monospaced", Font.BOLD, 13));
        statusBar.setOpaque(true);
        statusBar.setBackground(new Color(45, 45, 48));
        statusBar.setForeground(Color.WHITE);
        statusBar.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));
        add(statusBar, BorderLayout.NORTH);

        // Panneau de controle
        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        controlPanel.setBorder(BorderFactory.createTitledBorder("Controle du Cluster"));

        startBtn = new JButton("Demarrer Cluster");
        startBtn.setBackground(new Color(60, 179, 113));
        startBtn.setForeground(Color.WHITE);

        stopBtn = new JButton("Arreter Cluster");
        stopBtn.setBackground(new Color(220, 53, 69));
        stopBtn.setForeground(Color.WHITE);

        refreshBtn = new JButton("Actualiser");

        useDistributedCheckbox = new JCheckBox("Utiliser stockage distribue",
                FileService.isDistributedStorageEnabled());

        statusLabel = new JLabel();
        updateStatus();

        controlPanel.add(startBtn);
        controlPanel.add(stopBtn);
        controlPanel.add(refreshBtn);
        controlPanel.add(Box.createHorizontalStrut(20));
        controlPanel.add(useDistributedCheckbox);
        controlPanel.add(Box.createHorizontalStrut(20));
        controlPanel.add(statusLabel);

        add(controlPanel, BorderLayout.SOUTH);

        // Panneau central : dashboard
        JPanel centerGrid = new JPanel(new GridLayout(2, 2, 5, 5));
        centerGrid.setBorder(BorderFactory.createTitledBorder("Dashboard du Cluster"));

        osdArea = new JTextArea();
        chunkArea = new JTextArea();
        statsArea = new JTextArea();
        repairArea = new JTextArea();

        centerGrid.add(createPanel("OSD Health", osdArea));
        centerGrid.add(createPanel("Chunks Map", chunkArea));
        centerGrid.add(createPanel("Replication Stats", statsArea));
        centerGrid.add(createPanel("Repair / Domaines", repairArea));

        add(centerGrid, BorderLayout.CENTER);

        // Panneau droit : fichiers + domaines
        JPanel rightPanel = new JPanel(new BorderLayout(5, 5));
        rightPanel.setPreferredSize(new Dimension(300, 200));

        // Liste des domaines
        JPanel domainesPanel = new JPanel(new BorderLayout());
        domainesPanel.setBorder(BorderFactory.createTitledBorder("Domaines existants"));
        DefaultListModel<String> domainesModel = new DefaultListModel<>();
        JList<String> domainesList = new JList<>(domainesModel);
        refreshDomaines(domainesModel);
        domainesPanel.add(new JScrollPane(domainesList), BorderLayout.CENTER);

        // Liste des fichiers
        JPanel filesPanel = new JPanel(new BorderLayout());
        filesPanel.setBorder(BorderFactory.createTitledBorder("Fichiers sur le Cluster"));
        filesModel = new DefaultListModel<>();
        JList<String> filesList = new JList<>(filesModel);
        filesPanel.add(new JScrollPane(filesList), BorderLayout.CENTER);

        JSplitPane splitRight = new JSplitPane(JSplitPane.VERTICAL_SPLIT, domainesPanel, filesPanel);
        splitRight.setDividerLocation(150);
        rightPanel.add(splitRight, BorderLayout.CENTER);

        add(rightPanel, BorderLayout.EAST);

        // Listeners boutons
        startBtn.addActionListener(e -> startCluster());
        stopBtn.addActionListener(e -> stopCluster());
        refreshBtn.addActionListener(e -> {
            refreshDashboard();
            refreshFilesList(filesModel);
            refreshDomaines(domainesModel);
        });
        useDistributedCheckbox.addActionListener(e ->
                FileService.setUseDistributedStorage(useDistributedCheckbox.isSelected()));

        // Timer refresh automatique (5 sec)
        refreshTimer = new Timer(5000, e -> {
            if (ClusterManager.isClusterRunning()) {
                refreshDashboard();
                refreshFilesList(filesModel);
            }
        });
        refreshTimer.start();

        // Charger les stats initiales
        refreshDashboard();
        refreshFilesList(filesModel);
    }

    private void refreshDomaines(DefaultListModel<String> model) {
        model.clear();
        List<Domain> domaines = DomainService.listerTousDomaines();
        if (domaines.isEmpty()) {
            model.addElement("(Aucun domaine)");
        } else {
            for (Domain d : domaines) {
                String prefix = d.isRacine() ? "[Racine] " : "   -> ";
                String status = d.isActif() ? "" : " (inactif)";
                model.addElement(prefix + d.getNom() + " - " + d.getDescription() + status);
            }
        }
    }

    private JPanel createPanel(String title, JTextArea area) {
        area.setEditable(false);
        area.setFont(new Font("Monospaced", Font.PLAIN, 12));
        area.setBackground(new Color(30, 30, 30));
        area.setForeground(new Color(200, 200, 200));

        JPanel panel = new JPanel(new BorderLayout());
        TitledBorder border = BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(80, 80, 80)), title);
        border.setTitleColor(new Color(180, 180, 255));
        border.setTitleFont(new Font("SansSerif", Font.BOLD, 12));
        panel.setBorder(border);

        panel.add(new JScrollPane(area), BorderLayout.CENTER);
        return panel;
    }

    private void startCluster() {
        try {
            ClusterManager.startCluster();
            statusLabel.setText("Cluster demarre");
            statusLabel.setForeground(new Color(60, 179, 113));
        } catch (Exception e) {
            statusLabel.setText("Erreur: " + e.getMessage());
            statusLabel.setForeground(Color.RED);
        }
        updateStatus();
        // Attendre un peu que les OSD envoient leur heartbeat
        new Timer(3000, ev -> {
            refreshDashboard();
            refreshFilesList(filesModel);
            ((Timer) ev.getSource()).stop();
        }).start();
    }

    private void stopCluster() {
        ClusterManager.stopCluster();
        statusLabel.setText("Cluster arrete");
        statusLabel.setForeground(new Color(220, 53, 69));
        updateStatus();
        osdArea.setText("(Cluster arrete)");
        chunkArea.setText("(Cluster arrete)");
        statsArea.setText("(Cluster arrete)");
        repairArea.setText("(Cluster arrete)");
    }

    private void updateStatus() {
        boolean running = ClusterManager.isClusterRunning();
        boolean serverAvailable = false;

        if (running) {
            serverAvailable = FileService.isDistributedServerAvailable();
        }

        if (running && serverAvailable) {
            statusLabel.setText("Cluster actif");
            statusLabel.setForeground(new Color(60, 179, 113));
            startBtn.setEnabled(false);
            stopBtn.setEnabled(true);
        } else if (running) {
            statusLabel.setText("Cluster en demarrage...");
            statusLabel.setForeground(new Color(255, 193, 7));
            startBtn.setEnabled(false);
            stopBtn.setEnabled(true);
        } else {
            statusLabel.setText("Cluster arrete");
            statusLabel.setForeground(new Color(220, 53, 69));
            startBtn.setEnabled(true);
            stopBtn.setEnabled(false);
        }
    }

    private void refreshFilesList(DefaultListModel<String> model) {
        model.clear();
        if (ClusterManager.isClusterRunning()) {
            List<String> fichiers = FileService.listerFichiersDistribues();
            if (fichiers.isEmpty()) {
                model.addElement("(Aucun fichier)");
            } else {
                fichiers.forEach(model::addElement);
            }
        } else {
            model.addElement("(Cluster non demarre)");
        }
    }

    public void stopTimer() {
        if (refreshTimer != null) refreshTimer.stop();
    }

    private void refreshDashboard() {
        if (!ClusterManager.isClusterRunning()) return;

        // OSD Health (GET via socket)
        String clusterRaw = fetchSocket("CLUSTER");
        osdArea.setText(formatOSDHealth(clusterRaw));

        // Chunks Map (GET via socket)
        String chunksRaw = fetchSocket("CHUNKS");
        chunkArea.setText(formatChunks(chunksRaw));

        // Replication Stats (GET via socket)
        String statsRaw = fetchSocket("STATS");
        statsArea.setText(statsRaw);

        // Repair + Domaines info (GET via socket)
        String healthRaw = fetchSocket("HEALTH");
        repairArea.setText(formatHealthAndDomaines(healthRaw));

        updateStatusBar(clusterRaw);
        updateStatus();
    }

    /**
     * Envoie une commande au DirServer via socket TCP (methode GET/POST)
     */
    private String fetchSocket(String command) {
        try (java.net.Socket socket = new java.net.Socket("10.134.17.222", 7000);
             java.io.DataOutputStream dos = new java.io.DataOutputStream(socket.getOutputStream());
             java.io.DataInputStream dis = new java.io.DataInputStream(socket.getInputStream())) {

            dos.writeUTF(command);
            dos.flush();
            return dis.readUTF();
        } catch (Exception e) {
            return "ERR " + e.getMessage();
        }
    }

    private String formatOSDHealth(String raw) {
        if (raw.startsWith("ERR")) return raw;

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("%-12s %-10s %-20s%n", "OSD ID", "STATUS", "ADDRESS"));
        sb.append("-".repeat(44)).append("\n");

        for (String line : raw.split("\n")) {
            if (line.isBlank()) continue;
            String[] parts = line.split("\\|");
            if (parts.length < 3) continue;

            String id = parts[0].trim();
            String status = parts[1].trim();
            String address = parts[2].trim();

            String icon;
            switch (status) {
                case "UP": icon = "[OK]"; break;
                case "SUSPECTED": icon = "[??]"; break;
                case "DOWN": icon = "[KO]"; break;
                default: icon = "[--]";
            }

            sb.append(String.format("%-12s %s %-8s  %s%n",
                    id, icon, status, address));
        }
        return sb.toString();
    }

    private String formatChunks(String raw) {
        if (raw == null || raw.isBlank()) return "(Aucun chunk enregistre)";
        if (raw.startsWith("ERR")) return "Erreur: " + raw;
        if (raw.startsWith("Aucun")) return raw;
        return raw;
    }

    private String formatHealthAndDomaines(String raw) {
        StringBuilder sb = new StringBuilder();

        // Section domaines
        sb.append("=== Domaines existants ===\n");
        List<Domain> domaines = DomainService.listerTousDomaines();
        if (domaines.isEmpty()) {
            sb.append("(Aucun domaine)\n");
        } else {
            for (Domain d : domaines) {
                sb.append(d.isRacine() ? "[R] " : "  > ");
                sb.append(d.getNom());
                sb.append(" (").append(d.getDescription()).append(")\n");
            }
        }

        sb.append("\n=== Charge par OSD ===\n");
        sb.append("-".repeat(40)).append("\n");

        if (raw.startsWith("ERR")) {
            sb.append("DIR non accessible\n").append(raw);
            return sb.toString();
        }

        for (String line : raw.split("\n")) {
            if (line.isBlank()) continue;
            String[] parts = line.split("\\|");
            if (parts.length < 3) continue;

            String id = parts[0].trim();
            String status = parts[1].trim();
            String chunks = parts[2].replace("chunks=", "").trim();
            int n = 0;
            try { n = Integer.parseInt(chunks); } catch (Exception ignored) {}

            String bar = buildBar(n, 30);
            String icon = status.equals("UP") ? "[OK]" : status.equals("SUSPECTED") ? "[??]" : "[KO]";
            sb.append(String.format("%s %-10s %s %3s chunks%n", icon, id, bar, n));
        }

        return sb.toString();
    }

    private void updateStatusBar(String clusterRaw) {
        if (clusterRaw.startsWith("ERR")) {
            statusBar.setText("  DIR inaccessible");
            statusBar.setBackground(new Color(120, 30, 30));
            return;
        }

        long total = clusterRaw.lines().filter(l -> !l.isBlank()).count();
        long up = clusterRaw.lines().filter(l -> l.contains("|UP|")).count();
        long suspect = clusterRaw.lines().filter(l -> l.contains("|SUSPECTED|")).count();
        long down = clusterRaw.lines().filter(l -> l.contains("|DOWN|")).count();

        // Ajouter info domaines
        int nbDomaines = DomainService.listerTousDomaines().size();

        String msg = String.format(
                "  Cluster | UP: %d  Suspected: %d  Down: %d  (total: %d OSD) | Domaines: %d",
                up, suspect, down, total, nbDomaines);

        statusBar.setText(msg);

        if (down > 0) statusBar.setBackground(new Color(120, 60, 30));
        else if (suspect > 0) statusBar.setBackground(new Color(100, 90, 20));
        else statusBar.setBackground(new Color(30, 80, 30));
    }

    private String buildBar(int value, int max) {
        int filled = max == 0 ? 0 : Math.min(10, value * 10 / Math.max(max, 1));
        return "[" + "#".repeat(filled) + ".".repeat(10 - filled) + "]";
    }
}
