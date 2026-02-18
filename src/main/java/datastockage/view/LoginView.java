package view;

import controleur.UserController;
import dir.OSDHealth;
import model.User;
import model.Session;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;


public class LoginView extends JFrame {

    private JTextField emailField;
    private JPasswordField passwordField;
    private JButton loginButton;
    private JButton registerButton;
    private JLabel messageLabel;

    public LoginView() {
        setTitle("DataStockage - Connexion");
        setSize(450, 300);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new GridBagLayout());
        getContentPane().setBackground(new Color(240, 240, 245));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);

        // Titre
        JLabel titleLabel = new JLabel("🗄️ DataStockage");
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 24));
        titleLabel.setForeground(new Color(50, 50, 100));
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2; gbc.anchor = GridBagConstraints.CENTER;
        add(titleLabel, gbc);

        gbc.gridwidth = 1;
        gbc.gridx = 0; gbc.gridy = 1; gbc.anchor = GridBagConstraints.EAST;
        add(new JLabel("Email:"), gbc);

        emailField = new JTextField(20);
        gbc.gridx = 1; gbc.gridy = 1; gbc.anchor = GridBagConstraints.WEST;
        add(emailField, gbc);

        gbc.gridx = 0; gbc.gridy = 2; gbc.anchor = GridBagConstraints.EAST;
        add(new JLabel("Mot de passe:"), gbc);

        passwordField = new JPasswordField(20);
        gbc.gridx = 1; gbc.gridy = 2; gbc.anchor = GridBagConstraints.WEST;
        add(passwordField, gbc);

        messageLabel = new JLabel("");
        messageLabel.setForeground(Color.RED);
        gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 2; gbc.anchor = GridBagConstraints.CENTER;
        add(messageLabel, gbc);

        
        loginButton = new JButton("🔑 Connexion");
        loginButton.setBackground(new Color(70, 130, 180));
        loginButton.setForeground(Color.WHITE);
        loginButton.setFocusPainted(false);
        
        registerButton = new JButton("📝 Inscription");
        registerButton.setBackground(new Color(60, 179, 113));
        registerButton.setForeground(Color.WHITE);
        registerButton.setFocusPainted(false);

        JPanel panelButtons = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 5));
        panelButtons.setBackground(new Color(240, 240, 245));
        panelButtons.add(loginButton);
        panelButtons.add(registerButton);

        gbc.gridx = 0; gbc.gridy = 4; gbc.gridwidth = 2;
        add(panelButtons, gbc);

        
        loginButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String email = emailField.getText().trim();
                String password = new String(passwordField.getPassword());
                
                if (email.isEmpty() || password.isEmpty()) {
                    messageLabel.setText("Veuillez remplir tous les champs !");
                    return;
                }
                
                boolean success = UserController.connexion(email, password);
                if (success) {
                    User user = Session.getUtilisateurConnecte();
                    
                    if (!user.isActif()) {
                        messageLabel.setText("Votre compte est désactivé !");
                        Session.deconnexion();
                        return;
                    }
                    
                    messageLabel.setForeground(new Color(60, 179, 113));
                    messageLabel.setText("Connexion réussie !");
                    
                    // Redirection selon le rôle
                    if (user.isAdmin()) {
                        new AdminView().setVisible(true);
                    } else {
                        new ClientView().setVisible(true);
                    }
                    dispose();
                } else {
                    messageLabel.setText("Email ou mot de passe incorrect !");
                }
            }
        });

        registerButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                new RegistrationView().setVisible(true);
                dispose();
            }
        });
        
        // Raccourcis clavier
        getRootPane().setDefaultButton(loginButton);
    }

    //  static void cluster(HttpExchange ex) throws IOException {
    //     StringBuilder sb = new StringBuilder();
    //     for (var e : osds.entrySet()) {
    //         OSDHealth h = health.get(e.getKey());
    //         String status = h != null ? h.status.toString() : "UNKNOWN";
    //         sb.append(e.getKey()).append("|").append(status)
    //                 .append("|").append(e.getValue().host).append(":").append(e.getValue().port).append("\n");
    //     }
    //     respond(ex, 200, sb.toString());
    // }
}
