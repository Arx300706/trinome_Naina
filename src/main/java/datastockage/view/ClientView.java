package view;

import controleur.FileController;
import controleur.UserController;
import model.*;
import model.FileReport.Reason;
import service.FileService;
import service.ReportService;
import service.UserService;
import service.DomainService;
import client.ClientDownloader;
import client.ClientUploader;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;
import java.util.List;
import java.util.HashSet;
import java.util.Set;

/**
 * Vue Client avec download fonctionnel, fallback local+cluster, domaines.
 */
public class ClientView extends JFrame {

    private JTable tableFiles;
    private DefaultTableModel tableModel;
    private JButton uploadButton;
    private JButton downloadButton;
    private JButton deleteButton;
    private JButton reportButton;
    private JButton logoutButton;
    private JButton refreshButton;
    private User utilisateur;
    private JLabel quotaLabel;
    private JProgressBar quotaBar;
    private JComboBox<String> domaineCombo;

    public ClientView() {
        utilisateur = Session.getUtilisateurConnecte();
        if (utilisateur == null) {
            JOptionPane.showMessageDialog(this, "Utilisateur non connecte !");
            new LoginView().setVisible(true);
            dispose();
            return;
        }

        initUI();
    }

    private void initUI() {
        setTitle("DataStockage - Espace de " + utilisateur.getNom());
        setSize(900, 550);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));
        getContentPane().setBackground(new Color(240, 240, 245));

        // Header avec info utilisateur
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 5, 10));
        headerPanel.setBackground(new Color(70, 130, 180));

        JLabel welcomeLabel = new JLabel("  Bienvenue, " + utilisateur.getNom());
        welcomeLabel.setForeground(Color.WHITE);
        welcomeLabel.setFont(new Font("SansSerif", Font.BOLD, 16));
        headerPanel.add(welcomeLabel, BorderLayout.WEST);

        // Info quota + domaine
        JPanel rightHeader = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        rightHeader.setBackground(new Color(70, 130, 180));

        // Selecteur de domaine
        domaineCombo = new JComboBox<>();
        domaineCombo.addItem("Tous les domaines");
        List<Domain> domaines = DomainService.listerTousDomaines();
        for (Domain d : domaines) {
            if (d.isActif()) {
                domaineCombo.addItem(d.getNom());
            }
        }
        domaineCombo.setPreferredSize(new Dimension(160, 25));
        rightHeader.add(new JLabel("Domaine: ") {{ setForeground(Color.WHITE); }});
        rightHeader.add(domaineCombo);
        rightHeader.add(Box.createHorizontalStrut(15));

        quotaLabel = new JLabel();
        quotaLabel.setForeground(Color.WHITE);
        updateQuotaInfo();

        quotaBar = new JProgressBar(0, 100);
        quotaBar.setPreferredSize(new Dimension(150, 20));
        quotaBar.setStringPainted(true);
        updateQuotaBar();

        rightHeader.add(quotaLabel);
        rightHeader.add(quotaBar);
        headerPanel.add(rightHeader, BorderLayout.EAST);

        add(headerPanel, BorderLayout.NORTH);

        // Table des fichiers
        tableModel = new DefaultTableModel(
                new Object[]{"Nom", "Taille", "Chunks", "Replicas", "Status"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        tableFiles = new JTable(tableModel);
        tableFiles.setRowHeight(25);
        tableFiles.getTableHeader().setBackground(new Color(70, 130, 180));
        tableFiles.getTableHeader().setForeground(Color.WHITE);
        refreshTable();

        JScrollPane scrollPane = new JScrollPane(tableFiles);
        scrollPane.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        add(scrollPane, BorderLayout.CENTER);

        // Panneau de boutons
        JPanel panelButtons = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 10));
        panelButtons.setBackground(new Color(240, 240, 245));

        uploadButton = createStyledButton("Upload", new Color(60, 179, 113));
        downloadButton = createStyledButton("Download", new Color(70, 130, 180));
        deleteButton = createStyledButton("Supprimer", new Color(220, 53, 69));
        reportButton = createStyledButton("Signaler", new Color(255, 193, 7));
        refreshButton = createStyledButton("Actualiser", new Color(100, 100, 100));
        logoutButton = createStyledButton("Deconnexion", new Color(108, 117, 125));

        panelButtons.add(uploadButton);
        panelButtons.add(downloadButton);
        panelButtons.add(deleteButton);
        panelButtons.add(reportButton);
        panelButtons.add(refreshButton);
        panelButtons.add(logoutButton);

        add(panelButtons, BorderLayout.SOUTH);

        // Listeners
        uploadButton.addActionListener(e -> {
            File f = getfile();
            if (f != null) {
                ClientUploader.upload(f, String.valueOf(utilisateur.getId()));
                JOptionPane.showMessageDialog(this, "Upload envoye au cluster !");
                // Petit delai pour laisser le serveur traiter
                new Timer(1000, ev -> {
                    refreshTable();
                    ((Timer) ev.getSource()).stop();
                }).start();
            }
        });

        downloadButton.addActionListener(e -> {
            int selectedRow = tableFiles.getSelectedRow();
            if (selectedRow == -1) {
                JOptionPane.showMessageDialog(this,
                        "Veuillez selectionner un fichier !");
                return;
            }
            String fileName = tableModel.getValueAt(selectedRow, 0).toString();
            ClientDownloader.download(fileName);
        });

        deleteButton.addActionListener(e -> deleteFile());
        reportButton.addActionListener(e -> reportFile());
        refreshButton.addActionListener(e -> refreshTable());
        logoutButton.addActionListener(e -> logout());
    }

    private JButton createStyledButton(String text, Color bgColor) {
        JButton button = new JButton(text);
        button.setBackground(bgColor);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setFont(new Font("SansSerif", Font.PLAIN, 12));
        return button;
    }

    private void updateQuotaInfo() {
        utilisateur = UserService.getUserById(utilisateur.getId());
        if (utilisateur != null) {
            quotaLabel.setText("Espace: " + formatSize(utilisateur.getQuotaUtilise()) +
                    " / " + formatSize(utilisateur.getQuotaMax()) + "  ");
        }
    }

    private void updateQuotaBar() {
        if (utilisateur != null && utilisateur.getQuotaMax() > 0) {
            int percentage = (int) ((utilisateur.getQuotaUtilise() * 100) / utilisateur.getQuotaMax());
            quotaBar.setValue(percentage);

            if (percentage > 90) {
                quotaBar.setForeground(new Color(220, 53, 69));
            } else if (percentage > 70) {
                quotaBar.setForeground(new Color(255, 193, 7));
            } else {
                quotaBar.setForeground(new Color(60, 179, 113));
            }
        }
    }

    private void refreshTable() {
        tableModel.setRowCount(0);
        Set<String> seen = new HashSet<>();

        // 1) Essayer de lister depuis le cluster via socket
        try {
            List<FileModel> clusterFiles = FileController.listerFichiersCluster(utilisateur);
            for (FileModel f : clusterFiles) {
                seen.add(f.getNomFichier());
                String status = "Cluster";
                String chunks = String.valueOf(f.getChunkCount());
                String replicas = String.valueOf(f.getReplicas());
                tableModel.addRow(new Object[]{
                        f.getNomFichier(),
                        f.getTailleFormatee(),
                        chunks,
                        replicas,
                        status
                });
            }
        } catch (Exception e) {
            System.err.println("[ClientView] Cluster non disponible, fallback local");
        }

        // 2) Ajouter les fichiers locaux pas deja dans le cluster
        try {
            List<FileModel> localFiles = FileController.listerFichiers(utilisateur);
            for (FileModel f : localFiles) {
                if (!seen.contains(f.getNomFichier())) {
                    seen.add(f.getNomFichier());
                    tableModel.addRow(new Object[]{
                            f.getNomFichier(),
                            f.getTailleFormatee(),
                            f.isDistribue() ? String.valueOf(f.getChunkCount()) : "-",
                            f.isDistribue() ? String.valueOf(f.getReplicas()) : "-",
                            f.isDistribue() ? "Local+Cluster" : "Local"
                    });
                }
            }
        } catch (Exception e) {
            System.err.println("[ClientView] Erreur listing local: " + e.getMessage());
        }

        if (tableModel.getRowCount() == 0) {
            tableModel.addRow(new Object[]{"(Aucun fichier)", "-", "-", "-", "-"});
        }

        updateQuotaInfo();
        updateQuotaBar();
    }

    private File getfile() {
        JFileChooser chooser = new JFileChooser();
        int result = chooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            return chooser.getSelectedFile();
        }
        return null;
    }

    private void deleteFile() {
        int selectedRow = tableFiles.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Veuillez selectionner un fichier !");
            return;
        }

        String nomFichier = tableModel.getValueAt(selectedRow, 0).toString();
        if (nomFichier.startsWith("(")) return; // placeholder

        int confirm = JOptionPane.showConfirmDialog(this,
                "Supprimer " + nomFichier + " ?",
                "Confirmation", JOptionPane.YES_NO_OPTION);

        if (confirm == JOptionPane.YES_OPTION) {
            // Supprimer du cluster via socket
            try (java.net.Socket socket = new java.net.Socket("10.134.17.222", 7000);
                 java.io.DataOutputStream dos = new java.io.DataOutputStream(socket.getOutputStream());
                 java.io.DataInputStream dis = new java.io.DataInputStream(socket.getInputStream())) {

                dos.writeUTF("DELETE_FILE");
                dos.writeUTF(String.valueOf(utilisateur.getId()));
                dos.writeUTF(nomFichier);
                dos.flush();
                String resp = dis.readUTF();
                System.out.println("[ClientView] Delete cluster: " + resp);
            } catch (Exception e) {
                System.err.println("[ClientView] Erreur suppression cluster: " + e.getMessage());
            }

            // Supprimer aussi en local
            boolean success = FileController.supprimer(utilisateur, nomFichier);
            JOptionPane.showMessageDialog(this, "Fichier supprime !");
            refreshTable();
        }
    }

    private void reportFile() {
        int selectedRow = tableFiles.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Veuillez selectionner un fichier a signaler !");
            return;
        }

        String nomFichier = tableModel.getValueAt(selectedRow, 0).toString();
        if (nomFichier.startsWith("(")) return;

        JPanel panel = new JPanel(new GridLayout(3, 2, 5, 5));

        JComboBox<String> reasonCombo = new JComboBox<>(new String[]{
                "Contenu illegal",
                "Violation droits d'auteur",
                "Spam",
                "Malware/Virus",
                "Autre"
        });

        JTextArea descArea = new JTextArea(3, 20);
        descArea.setLineWrap(true);

        panel.add(new JLabel("Raison:"));
        panel.add(reasonCombo);
        panel.add(new JLabel("Description:"));
        panel.add(new JScrollPane(descArea));

        int result = JOptionPane.showConfirmDialog(this, panel,
                "Signaler: " + nomFichier,
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (result == JOptionPane.OK_OPTION) {
            Reason reason;
            switch (reasonCombo.getSelectedIndex()) {
                case 0: reason = Reason.CONTENU_ILLEGAL; break;
                case 1: reason = Reason.DROITS_AUTEUR; break;
                case 2: reason = Reason.SPAM; break;
                case 3: reason = Reason.MALWARE; break;
                default: reason = Reason.AUTRE;
            }

            FileReport report = new FileReport();
            report.setNomFichier(nomFichier);
            report.setCheminFichier(utilisateur.getDossierUtilisateur() + "/uploads/" + nomFichier);
            report.setRapporteurId(utilisateur.getId());
            report.setProprietaireId(utilisateur.getId());
            report.setRaison(reason);
            report.setDescription(descArea.getText());

            if (ReportService.creerReport(report)) {
                JOptionPane.showMessageDialog(this,
                        "Signalement envoye !\nUn administrateur examinera votre demande.",
                        "Signalement", JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(this, "Erreur lors du signalement !");
            }
        }
    }

    private void logout() {
        UserController.deconnexion();
        new LoginView().setVisible(true);
        dispose();
    }

    private String formatSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1048576) return String.format("%.2f KB", bytes / 1024.0);
        if (bytes < 1073741824) return String.format("%.2f MB", bytes / 1048576.0);
        return String.format("%.2f GB", bytes / 1073741824.0);
    }
}
