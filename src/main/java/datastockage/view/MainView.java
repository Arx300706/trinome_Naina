package view;

import controleur.FileController;
import controleur.UserController;
import model.FileModel;
import model.Session;
import model.User;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.util.List;

public class MainView extends JFrame {

    private JTable tableFiles;
    private DefaultTableModel tableModel;
    private JButton uploadButton;
    private JButton downloadButton;
    private JButton deleteButton;
    private JButton logoutButton;
    private User utilisateur;

    public MainView() {
        utilisateur = Session.getUtilisateurConnecte();
        if (utilisateur == null) {
            JOptionPane.showMessageDialog(this, "Utilisateur non connecté !");
            new LoginView().setVisible(true);
            dispose();
            return;
        }

        setTitle("DataStockage - Fichiers de " + utilisateur.getNom());
        setSize(700, 400);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        tableModel = new DefaultTableModel(new Object[]{"Nom", "Chemin", "Taille (octets)", "Date Upload"}, 0);
        tableFiles = new JTable(tableModel);
        refreshTable();

        add(new JScrollPane(tableFiles), BorderLayout.CENTER);

        
        JPanel panelButtons = new JPanel();
        uploadButton = new JButton("Uploader");
        downloadButton = new JButton("Télécharger");
        deleteButton = new JButton("Supprimer");
        logoutButton = new JButton("Déconnexion");

        panelButtons.add(uploadButton);
        panelButtons.add(downloadButton);
        panelButtons.add(deleteButton);
        panelButtons.add(logoutButton);

        add(panelButtons, BorderLayout.SOUTH);

        uploadButton.addActionListener(e -> uploadFile());
        downloadButton.addActionListener(e -> downloadFile());
        deleteButton.addActionListener(e -> deleteFile());
        logoutButton.addActionListener(e -> logout());
    }


    private void refreshTable() {
        tableModel.setRowCount(0);
        List<FileModel> fichiers = FileController.listerFichiers(utilisateur);
        for (FileModel f : fichiers) {
            tableModel.addRow(new Object[]{f.getNomFichier(), f.getTaille(), f.getDateUpload()});
        }
    }

 
    private void uploadFile() {
        JFileChooser chooser = new JFileChooser();
        int result = chooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File fichier = chooser.getSelectedFile();
            boolean success = FileController.upload(utilisateur, fichier);
            if (success) {
                JOptionPane.showMessageDialog(this, "Fichier uploadé avec succès !");
                refreshTable();
            } else {
                JOptionPane.showMessageDialog(this, "Erreur lors de l'upload !");
            }
        }
    }


    private void downloadFile() {
        int selectedRow = tableFiles.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Veuillez sélectionner un fichier !");
            return;
        }

        String nomFichier = tableModel.getValueAt(selectedRow, 0).toString();
        File fichierSource = FileController.telecharger(utilisateur, nomFichier);

        if (fichierSource != null && fichierSource.exists()) {
            String home = System.getProperty("user.home");
            File downloads = new File(home, "Téléchargements");
            if (!downloads.exists()) downloads.mkdirs();
            File fichierDestination = new File(downloads, nomFichier);

            try {
                java.nio.file.Files.copy(
                    fichierSource.toPath(),
                    fichierDestination.toPath(),
                    java.nio.file.StandardCopyOption.REPLACE_EXISTING
                );
                JOptionPane.showMessageDialog(this,
                        "Fichier téléchargé avec succès dans : " + fichierDestination.getAbsolutePath());
            } catch (Exception e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(this, "Erreur lors du téléchargement !");
            }

        } else {
            JOptionPane.showMessageDialog(this, "Erreur : fichier introuvable !");
        }
    }



    private void deleteFile() {
        int selectedRow = tableFiles.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Veuillez sélectionner un fichier !");
            return;
        }
        String nomFichier = tableModel.getValueAt(selectedRow, 0).toString();
        int confirm = JOptionPane.showConfirmDialog(this, "Supprimer " + nomFichier + " ?", "Confirmation", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            boolean success = FileController.supprimer(utilisateur, nomFichier);
            if (success) {
                JOptionPane.showMessageDialog(this, "Fichier supprimé !");
                refreshTable();
            } else {
                JOptionPane.showMessageDialog(this, "Erreur lors de la suppression !");
            }
        }
    }


    private void logout() {
        UserController.deconnexion();
        new LoginView().setVisible(true);
        dispose();
    }


    // public static void main(String[] args) {
    //     // Simulation utilisateur connecté
    //     // Session.setUtilisateurConnecte(new User(1, "Hasina", "hasina@mail.com", "1234", "hasina"));

    //     SwingUtilities.invokeLater(() -> new MainView().setVisible(true));
    // }
}
