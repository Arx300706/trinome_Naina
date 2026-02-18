package view;

import controleur.UserController;
import model.User;
import service.UserService;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class RegistrationView extends JFrame {

    private JTextField nomField;
    private JTextField emailField;
    private JPasswordField passwordField;
    private JPasswordField confirmPasswordField;
    private JButton registerButton;
    private JButton backButton;
    private JLabel messageLabel;

    public RegistrationView() {
        setTitle("DataStockage - Inscription");
        setSize(450, 350);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new GridBagLayout());
        getContentPane().setBackground(new Color(240, 240, 245));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);

        // Titre
        JLabel titleLabel = new JLabel("📝 Créer un compte");
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 20));
        titleLabel.setForeground(new Color(50, 50, 100));
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2; gbc.anchor = GridBagConstraints.CENTER;
        add(titleLabel, gbc);
        
        gbc.gridwidth = 1;
        gbc.gridx = 0; gbc.gridy = 1; gbc.anchor = GridBagConstraints.EAST;
        add(new JLabel("Nom:"), gbc);

        nomField = new JTextField(20);
        gbc.gridx = 1; gbc.gridy = 1; gbc.anchor = GridBagConstraints.WEST;
        add(nomField, gbc);

        
        gbc.gridx = 0; gbc.gridy = 2; gbc.anchor = GridBagConstraints.EAST;
        add(new JLabel("Email:"), gbc);

        emailField = new JTextField(20);
        gbc.gridx = 1; gbc.gridy = 2; gbc.anchor = GridBagConstraints.WEST;
        add(emailField, gbc);

        
        gbc.gridx = 0; gbc.gridy = 3; gbc.anchor = GridBagConstraints.EAST;
        add(new JLabel("Mot de passe:"), gbc);

        passwordField = new JPasswordField(20);
        gbc.gridx = 1; gbc.gridy = 3; gbc.anchor = GridBagConstraints.WEST;
        add(passwordField, gbc);

        gbc.gridx = 0; gbc.gridy = 4; gbc.anchor = GridBagConstraints.EAST;
        add(new JLabel("Confirmer:"), gbc);

        confirmPasswordField = new JPasswordField(20);
        gbc.gridx = 1; gbc.gridy = 4; gbc.anchor = GridBagConstraints.WEST;
        add(confirmPasswordField, gbc);

        messageLabel = new JLabel("");
        messageLabel.setForeground(Color.RED);
        gbc.gridx = 0; gbc.gridy = 5; gbc.gridwidth = 2; gbc.anchor = GridBagConstraints.CENTER;
        add(messageLabel, gbc);

        
        registerButton = new JButton("✅ S'inscrire");
        registerButton.setBackground(new Color(60, 179, 113));
        registerButton.setForeground(Color.WHITE);
        registerButton.setFocusPainted(false);

        backButton = new JButton("⬅ Retour");
        backButton.setBackground(new Color(108, 117, 125));
        backButton.setForeground(Color.WHITE);
        backButton.setFocusPainted(false);

        JPanel panelButtons = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 5));
        panelButtons.setBackground(new Color(240, 240, 245));
        panelButtons.add(registerButton);
        panelButtons.add(backButton);

        gbc.gridx = 0; gbc.gridy = 6; gbc.gridwidth = 2;
        add(panelButtons, gbc);

        
        registerButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String nom = nomField.getText().trim();
                String email = emailField.getText().trim();
                String password = new String(passwordField.getPassword());
                String confirmPassword = new String(confirmPasswordField.getPassword());

                if (nom.isEmpty() || email.isEmpty() || password.isEmpty()) {
                    messageLabel.setText("Tous les champs sont obligatoires !");
                    return;
                }

                if (!password.equals(confirmPassword)) {
                    messageLabel.setText("Les mots de passe ne correspondent pas !");
                    return;
                }

                if (password.length() < 4) {
                    messageLabel.setText("Le mot de passe doit contenir au moins 4 caractères !");
                    return;
                }

                if (!email.contains("@") || !email.contains(".")) {
                    messageLabel.setText("Email invalide !");
                    return;
                }

                if (UserService.emailExiste(email)) {
                    messageLabel.setText("Cet email est déjà utilisé !");
                    return;
                }

                User user = new User();
                user.setNom(nom);
                user.setEmail(email);
                user.setMotDePasse(password);
                user.setDossierUtilisateur(nom.toLowerCase().replaceAll("\\s+", "_"));
                // Par défaut CLIENT
                user.setRole(User.Role.CLIENT);
                
                boolean success = UserController.inscrireUtilisateur(user);
                if (success) {
                    messageLabel.setForeground(new Color(60, 179, 113));
                    messageLabel.setText("Inscription réussie !");
                    JOptionPane.showMessageDialog(RegistrationView.this,
                        "Compte créé avec succès!\nVous pouvez maintenant vous connecter.",
                        "Inscription réussie", JOptionPane.INFORMATION_MESSAGE);
                    new LoginView().setVisible(true);
                    dispose();
                } else {
                    messageLabel.setForeground(Color.RED);
                    messageLabel.setText("Erreur lors de l'inscription !");
                }
            }
        });

        backButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                new LoginView().setVisible(true);
                dispose();
            }
        });
    }

    // // Pour tester indépendamment
    // public static void main(String[] args) {
    //     SwingUtilities.invokeLater(() -> new RegistrationView().setVisible(true));
    // }
}
