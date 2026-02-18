package view;

import model.*;
import model.User.Role;
import service.*;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;
import java.util.Map;

/**
 * Vue d'administration principale.
 * Permet de gérer les utilisateurs, domaines, signalements et voir le stockage.
 */
public class AdminView extends JFrame {

    private JTabbedPane tabbedPane;
    private User admin;

    // Onglet Utilisateurs
    private JTable tableUsers;
    private DefaultTableModel userTableModel;

    // Onglet Domaines
    private JTable tableDomains;
    private DefaultTableModel domainTableModel;

    // Onglet Signalements
    private JTable tableReports;
    private DefaultTableModel reportTableModel;

    // Onglet Stockage/Stats
    private JPanel panelStats;
    
    // Onglet Cluster
    private ClusterPanel clusterPanel;

    public AdminView() {
        admin = Session.getUtilisateurConnecte();
        if (admin == null || !admin.isAdmin()) {
            JOptionPane.showMessageDialog(this, "Accès non autorisé !");
            new LoginView().setVisible(true);
            dispose();
            return;
        }

        initUI();
    }

    private void initUI() {
        setTitle("DataStockage - Administration (" + admin.getNom() + ")");
        setSize(1000, 700);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        tabbedPane = new JTabbedPane();

        // Créer les onglets
        tabbedPane.addTab("👥 Utilisateurs", createUsersPanel());
        tabbedPane.addTab("📁 Domaines", createDomainsPanel());
        tabbedPane.addTab("🚨 Signalements", createReportsPanel());
        tabbedPane.addTab("📊 Statistiques", createStatsPanel());
        tabbedPane.addTab("💾 Mon Stockage", createMyStoragePanel());
        
        // Onglet Cluster distribué
        clusterPanel = new ClusterPanel();
        tabbedPane.addTab("🖥️ Cluster", clusterPanel);

        add(tabbedPane, BorderLayout.CENTER);

        // Barre de navigation en bas
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton refreshBtn = new JButton("🔄 Actualiser");
        JButton logoutBtn = new JButton("🚪 Déconnexion");
        
        refreshBtn.addActionListener(e -> refreshAllTables());
        logoutBtn.addActionListener(e -> logout());
        
        bottomPanel.add(refreshBtn);
        bottomPanel.add(logoutBtn);
        add(bottomPanel, BorderLayout.SOUTH);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // ONGLET UTILISATEURS
    // ═══════════════════════════════════════════════════════════════════════════

    private JPanel createUsersPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Table des utilisateurs
        userTableModel = new DefaultTableModel(
            new Object[]{"ID", "Nom", "Email", "Rôle", "Quota Utilisé", "Actif"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        tableUsers = new JTable(userTableModel);
        refreshUserTable();

        JScrollPane scrollPane = new JScrollPane(tableUsers);
        panel.add(scrollPane, BorderLayout.CENTER);

        // Panneau de boutons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        
        JButton addUserBtn = new JButton("➕ Ajouter Admin");
        JButton toggleRoleBtn = new JButton("🔄 Changer Rôle");
        JButton toggleActiveBtn = new JButton("🔒 Activer/Désactiver");
        JButton deleteUserBtn = new JButton("🗑️ Supprimer");
        JButton editQuotaBtn = new JButton("📦 Modifier Quota");

        addUserBtn.addActionListener(e -> showAddAdminDialog());
        toggleRoleBtn.addActionListener(e -> toggleUserRole());
        toggleActiveBtn.addActionListener(e -> toggleUserActive());
        deleteUserBtn.addActionListener(e -> deleteUser());
        editQuotaBtn.addActionListener(e -> editUserQuota());

        buttonPanel.add(addUserBtn);
        buttonPanel.add(toggleRoleBtn);
        buttonPanel.add(toggleActiveBtn);
        buttonPanel.add(deleteUserBtn);
        buttonPanel.add(editQuotaBtn);

        panel.add(buttonPanel, BorderLayout.NORTH);

        return panel;
    }

    private void refreshUserTable() {
        userTableModel.setRowCount(0);
        List<User> users = UserService.listerUtilisateurs();
        for (User u : users) {
            userTableModel.addRow(new Object[]{
                u.getId(),
                u.getNom(),
                u.getEmail(),
                u.getRole().name(),
                formatSize(u.getQuotaUtilise()) + " / " + formatSize(u.getQuotaMax()),
                u.isActif() ? "✅ Oui" : "❌ Non"
            });
        }
    }

    private void showAddAdminDialog() {
        JTextField nomField = new JTextField(20);
        JTextField emailField = new JTextField(20);
        JPasswordField passwordField = new JPasswordField(20);

        JPanel panel = new JPanel(new GridLayout(3, 2, 5, 5));
        panel.add(new JLabel("Nom:"));
        panel.add(nomField);
        panel.add(new JLabel("Email:"));
        panel.add(emailField);
        panel.add(new JLabel("Mot de passe:"));
        panel.add(passwordField);

        int result = JOptionPane.showConfirmDialog(this, panel, "Ajouter un administrateur",
            JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (result == JOptionPane.OK_OPTION) {
            String nom = nomField.getText().trim();
            String email = emailField.getText().trim();
            String password = new String(passwordField.getPassword());

            if (nom.isEmpty() || email.isEmpty() || password.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Tous les champs sont obligatoires !");
                return;
            }

            if (UserService.emailExiste(email)) {
                JOptionPane.showMessageDialog(this, "Cet email existe déjà !");
                return;
            }

            User newAdmin = new User();
            newAdmin.setNom(nom);
            newAdmin.setEmail(email);
            newAdmin.setMotDePasse(password);
            newAdmin.setDossierUtilisateur(nom.toLowerCase().replaceAll("\\s+", "_"));
            newAdmin.setRole(Role.ADMIN);

            if (UserService.ajouterUtilisateur(newAdmin)) {
                ApacheService.creerDossierUtilisateur(newAdmin.getDossierUtilisateur());
                JOptionPane.showMessageDialog(this, "Administrateur ajouté avec succès !");
                refreshUserTable();
            } else {
                JOptionPane.showMessageDialog(this, "Erreur lors de l'ajout !");
            }
        }
    }

    private void toggleUserRole() {
        int row = tableUsers.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Sélectionnez un utilisateur !");
            return;
        }

        int userId = (int) userTableModel.getValueAt(row, 0);
        User user = UserService.getUserById(userId);
        
        if (user != null) {
            Role newRole = user.getRole() == Role.ADMIN ? Role.CLIENT : Role.ADMIN;
            if (UserService.changerRole(userId, newRole)) {
                JOptionPane.showMessageDialog(this, "Rôle changé en " + newRole.name());
                refreshUserTable();
            }
        }
    }

    private void toggleUserActive() {
        int row = tableUsers.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Sélectionnez un utilisateur !");
            return;
        }

        int userId = (int) userTableModel.getValueAt(row, 0);
        User user = UserService.getUserById(userId);
        
        if (user != null) {
            boolean newStatus = !user.isActif();
            if (UserService.activerDesactiverUtilisateur(userId, newStatus)) {
                JOptionPane.showMessageDialog(this, "Utilisateur " + (newStatus ? "activé" : "désactivé"));
                refreshUserTable();
            }
        }
    }

    private void deleteUser() {
        int row = tableUsers.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Sélectionnez un utilisateur !");
            return;
        }

        String email = (String) userTableModel.getValueAt(row, 2);
        
        int confirm = JOptionPane.showConfirmDialog(this, 
            "Supprimer l'utilisateur " + email + " ?", 
            "Confirmation", JOptionPane.YES_NO_OPTION);
        
        if (confirm == JOptionPane.YES_OPTION) {
            if (UserService.supprimerUtilisateur(email)) {
                JOptionPane.showMessageDialog(this, "Utilisateur supprimé !");
                refreshUserTable();
            }
        }
    }

    private void editUserQuota() {
        int row = tableUsers.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Sélectionnez un utilisateur !");
            return;
        }

        int userId = (int) userTableModel.getValueAt(row, 0);
        User user = UserService.getUserById(userId);
        
        if (user != null) {
            String input = JOptionPane.showInputDialog(this, 
                "Nouveau quota (en GB):", user.getQuotaMax() / 1073741824);
            
            if (input != null && !input.isEmpty()) {
                try {
                    long newQuota = Long.parseLong(input) * 1073741824L;
                    user.setQuotaMax(newQuota);
                    if (UserService.modifierUtilisateur(user)) {
                        JOptionPane.showMessageDialog(this, "Quota mis à jour !");
                        refreshUserTable();
                    }
                } catch (NumberFormatException e) {
                    JOptionPane.showMessageDialog(this, "Valeur invalide !");
                }
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // ONGLET DOMAINES
    // ═══════════════════════════════════════════════════════════════════════════

    private JPanel createDomainsPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        domainTableModel = new DefaultTableModel(
            new Object[]{"ID", "Nom", "Description", "Parent", "Quota Utilisé", "Actif"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        tableDomains = new JTable(domainTableModel);
        refreshDomainTable();

        JScrollPane scrollPane = new JScrollPane(tableDomains);
        panel.add(scrollPane, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        
        JButton addDomainBtn = new JButton("➕ Nouveau Domaine");
        JButton addSubDomainBtn = new JButton("📂 Sous-Domaine");
        JButton editDomainBtn = new JButton("✏️ Modifier");
        JButton deleteDomainBtn = new JButton("🗑️ Supprimer");

        addDomainBtn.addActionListener(e -> showAddDomainDialog(false));
        addSubDomainBtn.addActionListener(e -> showAddDomainDialog(true));
        editDomainBtn.addActionListener(e -> editDomain());
        deleteDomainBtn.addActionListener(e -> deleteDomain());

        buttonPanel.add(addDomainBtn);
        buttonPanel.add(addSubDomainBtn);
        buttonPanel.add(editDomainBtn);
        buttonPanel.add(deleteDomainBtn);

        panel.add(buttonPanel, BorderLayout.NORTH);

        return panel;
    }

    private void refreshDomainTable() {
        domainTableModel.setRowCount(0);
        List<Domain> domains = DomainService.listerTousDomaines();
        for (Domain d : domains) {
            String parent = d.isRacine() ? "—" : getDomainName(d.getParentId());
            domainTableModel.addRow(new Object[]{
                d.getId(),
                d.getNom(),
                d.getDescription(),
                parent,
                formatSize(d.getQuotaUtilise()) + " / " + formatSize(d.getQuotaMax()),
                d.isActif() ? "✅ Oui" : "❌ Non"
            });
        }
    }

    private String getDomainName(int id) {
        Domain d = DomainService.getDomainById(id);
        return d != null ? d.getNom() : "Inconnu";
    }

    private void showAddDomainDialog(boolean isSubDomain) {
        JTextField nomField = new JTextField(20);
        JTextField descField = new JTextField(20);
        JTextField quotaField = new JTextField("10", 10);
        JComboBox<String> parentCombo = new JComboBox<>();
        
        parentCombo.addItem("— Aucun (Domaine racine) —");
        for (Domain d : DomainService.listerTousDomaines()) {
            parentCombo.addItem(d.getId() + " - " + d.getNom());
        }

        JPanel panel = new JPanel(new GridLayout(4, 2, 5, 5));
        panel.add(new JLabel("Nom:"));
        panel.add(nomField);
        panel.add(new JLabel("Description:"));
        panel.add(descField);
        panel.add(new JLabel("Quota (GB):"));
        panel.add(quotaField);
        panel.add(new JLabel("Domaine parent:"));
        panel.add(parentCombo);

        if (isSubDomain) {
            int row = tableDomains.getSelectedRow();
            if (row >= 0) {
                int parentId = (int) domainTableModel.getValueAt(row, 0);
                for (int i = 1; i < parentCombo.getItemCount(); i++) {
                    if (parentCombo.getItemAt(i).startsWith(parentId + " - ")) {
                        parentCombo.setSelectedIndex(i);
                        break;
                    }
                }
            }
        }

        int result = JOptionPane.showConfirmDialog(this, panel, 
            isSubDomain ? "Ajouter un sous-domaine" : "Ajouter un domaine",
            JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (result == JOptionPane.OK_OPTION) {
            String nom = nomField.getText().trim();
            String desc = descField.getText().trim();
            
            if (nom.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Le nom est obligatoire !");
                return;
            }

            Domain domain = new Domain();
            domain.setNom(nom);
            domain.setDescription(desc);
            domain.setProprietaireId(admin.getId());
            
            try {
                domain.setQuotaMax(Long.parseLong(quotaField.getText()) * 1073741824L);
            } catch (NumberFormatException e) {
                domain.setQuotaMax(10_737_418_240L);
            }

            String selectedParent = (String) parentCombo.getSelectedItem();
            if (!selectedParent.startsWith("—")) {
                int parentId = Integer.parseInt(selectedParent.split(" - ")[0]);
                domain.setParentId(parentId);
            }

            if (DomainService.creerDomaine(domain)) {
                JOptionPane.showMessageDialog(this, "Domaine créé avec succès !");
                refreshDomainTable();
            } else {
                JOptionPane.showMessageDialog(this, "Erreur lors de la création !");
            }
        }
    }

    private void editDomain() {
        int row = tableDomains.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Sélectionnez un domaine !");
            return;
        }

        int domainId = (int) domainTableModel.getValueAt(row, 0);
        Domain domain = DomainService.getDomainById(domainId);
        
        if (domain != null) {
            JTextField nomField = new JTextField(domain.getNom(), 20);
            JTextField descField = new JTextField(domain.getDescription(), 20);

            JPanel panel = new JPanel(new GridLayout(2, 2, 5, 5));
            panel.add(new JLabel("Nom:"));
            panel.add(nomField);
            panel.add(new JLabel("Description:"));
            panel.add(descField);

            int result = JOptionPane.showConfirmDialog(this, panel, "Modifier le domaine",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

            if (result == JOptionPane.OK_OPTION) {
                domain.setNom(nomField.getText().trim());
                domain.setDescription(descField.getText().trim());
                
                if (DomainService.modifierDomaine(domain)) {
                    JOptionPane.showMessageDialog(this, "Domaine modifié !");
                    refreshDomainTable();
                }
            }
        }
    }

    private void deleteDomain() {
        int row = tableDomains.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Sélectionnez un domaine !");
            return;
        }

        int domainId = (int) domainTableModel.getValueAt(row, 0);
        String nom = (String) domainTableModel.getValueAt(row, 1);
        
        int confirm = JOptionPane.showConfirmDialog(this, 
            "Supprimer le domaine '" + nom + "' et ses sous-domaines ?", 
            "Confirmation", JOptionPane.YES_NO_OPTION);
        
        if (confirm == JOptionPane.YES_OPTION) {
            if (DomainService.supprimerDomaine(domainId)) {
                JOptionPane.showMessageDialog(this, "Domaine supprimé !");
                refreshDomainTable();
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // ONGLET SIGNALEMENTS
    // ═══════════════════════════════════════════════════════════════════════════

    private JPanel createReportsPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        reportTableModel = new DefaultTableModel(
            new Object[]{"ID", "Fichier", "Raison", "Description", "Status", "Date"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        tableReports = new JTable(reportTableModel);
        refreshReportTable();

        JScrollPane scrollPane = new JScrollPane(tableReports);
        panel.add(scrollPane, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        
        JButton approveBtn = new JButton("✅ Approuver (Supprimer fichier)");
        JButton rejectBtn = new JButton("❌ Rejeter");
        JButton viewBtn = new JButton("👁️ Voir détails");

        approveBtn.addActionListener(e -> handleReport(true));
        rejectBtn.addActionListener(e -> handleReport(false));
        viewBtn.addActionListener(e -> viewReportDetails());

        buttonPanel.add(approveBtn);
        buttonPanel.add(rejectBtn);
        buttonPanel.add(viewBtn);

        // Filtre
        JComboBox<String> filterCombo = new JComboBox<>(new String[]{"Tous", "En attente", "Traités", "Rejetés"});
        filterCombo.addActionListener(e -> {
            String filter = (String) filterCombo.getSelectedItem();
            refreshReportTable(filter);
        });
        buttonPanel.add(new JLabel("  Filtre:"));
        buttonPanel.add(filterCombo);

        panel.add(buttonPanel, BorderLayout.NORTH);

        return panel;
    }

    private void refreshReportTable() {
        refreshReportTable("Tous");
    }

    private void refreshReportTable(String filter) {
        reportTableModel.setRowCount(0);
        List<FileReport> reports = ReportService.listerTousReports();
        
        for (FileReport r : reports) {
            boolean show = filter.equals("Tous") ||
                (filter.equals("En attente") && r.getStatus() == FileReport.Status.EN_ATTENTE) ||
                (filter.equals("Traités") && r.getStatus() == FileReport.Status.TRAITE) ||
                (filter.equals("Rejetés") && r.getStatus() == FileReport.Status.REJETE);
            
            if (show) {
                reportTableModel.addRow(new Object[]{
                    r.getId(),
                    r.getNomFichier(),
                    r.getRaisonLibelle(),
                    truncate(r.getDescription(), 30),
                    r.getStatusLibelle(),
                    r.getDateSignalement()
                });
            }
        }
    }

    private void handleReport(boolean approve) {
        int row = tableReports.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Sélectionnez un signalement !");
            return;
        }

        int reportId = (int) reportTableModel.getValueAt(row, 0);
        FileReport report = ReportService.getReportById(reportId);
        
        if (report == null) return;
        
        if (report.getStatus() != FileReport.Status.EN_ATTENTE) {
            JOptionPane.showMessageDialog(this, "Ce signalement a déjà été traité !");
            return;
        }

        String commentaire = JOptionPane.showInputDialog(this, "Commentaire (optionnel):");
        
        FileReport.Status newStatus = approve ? FileReport.Status.TRAITE : FileReport.Status.REJETE;
        
        if (ReportService.traiterReport(reportId, admin.getId(), newStatus, commentaire)) {
            if (approve) {
                // Supprimer le fichier signalé
                User proprietaire = UserService.getUserById(report.getProprietaireId());
                if (proprietaire != null) {
                    FileService.supprimerFichier(proprietaire, report.getNomFichier());
                }
            }
            JOptionPane.showMessageDialog(this, approve ? "Fichier supprimé !" : "Signalement rejeté !");
            refreshReportTable();
        }
    }

    private void viewReportDetails() {
        int row = tableReports.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Sélectionnez un signalement !");
            return;
        }

        int reportId = (int) reportTableModel.getValueAt(row, 0);
        FileReport report = ReportService.getReportById(reportId);
        
        if (report != null) {
            User rapporteur = UserService.getUserById(report.getRapporteurId());
            User proprietaire = UserService.getUserById(report.getProprietaireId());
            
            StringBuilder details = new StringBuilder();
            details.append("═══ SIGNALEMENT #").append(report.getId()).append(" ═══\n\n");
            details.append("Fichier: ").append(report.getNomFichier()).append("\n");
            details.append("Chemin: ").append(report.getCheminFichier()).append("\n");
            details.append("Raison: ").append(report.getRaisonLibelle()).append("\n");
            details.append("Description: ").append(report.getDescription()).append("\n\n");
            details.append("Signalé par: ").append(rapporteur != null ? rapporteur.getNom() : "Inconnu").append("\n");
            details.append("Propriétaire: ").append(proprietaire != null ? proprietaire.getNom() : "Inconnu").append("\n");
            details.append("Date: ").append(report.getDateSignalement()).append("\n\n");
            details.append("Status: ").append(report.getStatusLibelle()).append("\n");
            
            if (report.getStatus() != FileReport.Status.EN_ATTENTE) {
                User adminTraitant = UserService.getUserById(report.getAdminTraitantId());
                details.append("Traité par: ").append(adminTraitant != null ? adminTraitant.getNom() : "Inconnu").append("\n");
                details.append("Date traitement: ").append(report.getDateTraitement()).append("\n");
                details.append("Commentaire: ").append(report.getCommentaireAdmin()).append("\n");
            }

            JTextArea textArea = new JTextArea(details.toString());
            textArea.setEditable(false);
            JScrollPane scrollPane = new JScrollPane(textArea);
            scrollPane.setPreferredSize(new Dimension(400, 300));
            
            JOptionPane.showMessageDialog(this, scrollPane, "Détails du signalement", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // ONGLET STATISTIQUES
    // ═══════════════════════════════════════════════════════════════════════════

    private JPanel createStatsPanel() {
        panelStats = new JPanel(new GridLayout(2, 2, 10, 10));
        panelStats.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        refreshStats();
        
        return panelStats;
    }

    private void refreshStats() {
        panelStats.removeAll();
        
        // Stats utilisateurs
        Map<String, Object> userStats = UserService.getStatistiques();
        JPanel userStatsPanel = createStatCard("👥 Utilisateurs", new String[]{
            "Total: " + userStats.get("total"),
            "Admins: " + userStats.get("admins"),
            "Clients: " + userStats.get("clients"),
            "Actifs: " + userStats.get("actifs"),
            "Inactifs: " + userStats.get("inactifs")
        });
        panelStats.add(userStatsPanel);

        // Stats signalements
        Map<String, Integer> reportStats = ReportService.getStatistiques();
        JPanel reportStatsPanel = createStatCard("🚨 Signalements", new String[]{
            "Total: " + reportStats.get("total"),
            "En attente: " + reportStats.get("en_attente"),
            "Traités: " + reportStats.get("traites"),
            "Rejetés: " + reportStats.get("rejetes")
        });
        panelStats.add(reportStatsPanel);

        // Stats domaines
        List<Domain> domains = DomainService.listerTousDomaines();
        long totalQuotaDomains = 0;
        long totalUsedDomains = 0;
        for (Domain d : domains) {
            totalQuotaDomains += d.getQuotaMax();
            totalUsedDomains += d.getQuotaUtilise();
        }
        JPanel domainStatsPanel = createStatCard("📁 Domaines", new String[]{
            "Total domaines: " + domains.size(),
            "Domaines racine: " + DomainService.listerDomainesRacine().size(),
            "Espace total: " + formatSize(totalQuotaDomains),
            "Espace utilisé: " + formatSize(totalUsedDomains)
        });
        panelStats.add(domainStatsPanel);

        // Système
        Runtime runtime = Runtime.getRuntime();
        JPanel systemStatsPanel = createStatCard("💻 Système", new String[]{
            "Mémoire utilisée: " + formatSize(runtime.totalMemory() - runtime.freeMemory()),
            "Mémoire libre: " + formatSize(runtime.freeMemory()),
            "Mémoire max: " + formatSize(runtime.maxMemory()),
            "Processeurs: " + runtime.availableProcessors()
        });
        panelStats.add(systemStatsPanel);

        panelStats.revalidate();
        panelStats.repaint();
    }

    private JPanel createStatCard(String title, String[] lines) {
        JPanel card = new JPanel(new BorderLayout());
        card.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(Color.GRAY), title,
            TitledBorder.LEFT, TitledBorder.TOP,
            new Font("SansSerif", Font.BOLD, 14)
        ));
        
        JPanel content = new JPanel(new GridLayout(lines.length, 1, 5, 5));
        content.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        for (String line : lines) {
            content.add(new JLabel(line));
        }
        
        card.add(content, BorderLayout.CENTER);
        return card;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // ONGLET MON STOCKAGE
    // ═══════════════════════════════════════════════════════════════════════════

    private JPanel createMyStoragePanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        DefaultTableModel storageModel = new DefaultTableModel(
            new Object[]{"Nom", "Taille", "Date Upload"}, 0);
        JTable tableStorage = new JTable(storageModel);

        // Charger les fichiers de l'admin
        List<FileModel> fichiers = FileService.listerFichiersUtilisateur(admin);
        for (FileModel f : fichiers) {
            storageModel.addRow(new Object[]{
                f.getNomFichier(),
                f.getTailleFormatee(),
                f.getDateUpload()
            });
        }

        JScrollPane scrollPane = new JScrollPane(tableStorage);
        panel.add(scrollPane, BorderLayout.CENTER);

        // Info quota
        JPanel infoPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        infoPanel.add(new JLabel("Espace utilisé: " + formatSize(admin.getQuotaUtilise()) + 
            " / " + formatSize(admin.getQuotaMax())));
        
        JProgressBar quotaBar = new JProgressBar(0, 100);
        int percentage = (int) ((admin.getQuotaUtilise() * 100) / admin.getQuotaMax());
        quotaBar.setValue(percentage);
        quotaBar.setStringPainted(true);
        infoPanel.add(quotaBar);
        
        panel.add(infoPanel, BorderLayout.NORTH);

        return panel;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // HELPERS
    // ═══════════════════════════════════════════════════════════════════════════

    private void refreshAllTables() {
        refreshUserTable();
        refreshDomainTable();
        refreshReportTable();
        refreshStats();
        JOptionPane.showMessageDialog(this, "Données actualisées !");
    }

    private void logout() {
        if (clusterPanel != null) {
            clusterPanel.stopTimer();
        }
        Session.deconnexion();
        new LoginView().setVisible(true);
        dispose();
    }

    private String formatSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1048576) return String.format("%.2f KB", bytes / 1024.0);
        if (bytes < 1073741824) return String.format("%.2f MB", bytes / 1048576.0);
        return String.format("%.2f GB", bytes / 1073741824.0);
    }

    private String truncate(String text, int maxLength) {
        if (text == null) return "";
        if (text.length() <= maxLength) return text;
        return text.substring(0, maxLength - 3) + "...";
    }
}
