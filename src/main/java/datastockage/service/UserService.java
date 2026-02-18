package service;

import model.User;
import model.User.Role;
import util.FileUtils;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class UserService {

    private static final String FICHIER_UTILISATEURS = "src/main/resources/data/utilisateurs.txt";
 
    public static boolean ajouterUtilisateur(User user) {
        try {
            // Assigner un ID si nécessaire
            if (user.getId() == 0) {
                user.setId(genererNouvelId());
            }
            if (user.getDateCreation() == null) {
                user.setDateCreation(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
            }
            
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(FICHIER_UTILISATEURS, true))) {
                String ligne = formatUserLigne(user);
                writer.write(ligne);
                writer.newLine();
                return true;
            }
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    private static int genererNouvelId() {
        int maxId = 0;
        for (User u : listerUtilisateurs()) {
            if (u.getId() > maxId) {
                maxId = u.getId();
            }
        }
        return maxId + 1;
    }

    private static String formatUserLigne(User user) {
        return user.getId() + ";" + 
               user.getNom() + ";" +
               user.getEmail() + ";" + 
               user.getMotDePasse() + ";" +
               user.getDossierUtilisateur() + ";" +
               user.getRole().name() + ";" +
               user.getQuotaMax() + ";" +
               user.getQuotaUtilise() + ";" +
               (user.getDateCreation() != null ? user.getDateCreation() : "") + ";" +
               user.isActif();
    }


    public static List<User> listerUtilisateurs() {
        List<User> utilisateurs = new ArrayList<>();
        File fichier = new File(FICHIER_UTILISATEURS);
        if (!fichier.exists()) {
            return utilisateurs;
        }
        
        try (BufferedReader reader = new BufferedReader(new FileReader(FICHIER_UTILISATEURS))) {
            String ligne;
            while ((ligne = reader.readLine()) != null) {
                User u = parseUserLigne(ligne);
                if (u != null) {
                    utilisateurs.add(u);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return utilisateurs;
    }

    private static User parseUserLigne(String ligne) {
        try {
            String[] parts = ligne.split(";");
            if (parts.length >= 5) {
                User u = new User(
                        Integer.parseInt(parts[0]),
                        parts[1],
                        parts[2],
                        parts[3],
                        parts[4]
                );
                // Nouveaux champs (rétrocompatibilité)
                if (parts.length > 5) {
                    u.setRole(Role.valueOf(parts[5]));
                }
                if (parts.length > 6) {
                    u.setQuotaMax(Long.parseLong(parts[6]));
                }
                if (parts.length > 7) {
                    u.setQuotaUtilise(Long.parseLong(parts[7]));
                }
                if (parts.length > 8 && !parts[8].isEmpty()) {
                    u.setDateCreation(parts[8]);
                }
                if (parts.length > 9) {
                    u.setActif(Boolean.parseBoolean(parts[9]));
                }
                return u;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static List<User> listerAdmins() {
        List<User> admins = new ArrayList<>();
        for (User u : listerUtilisateurs()) {
            if (u.isAdmin()) {
                admins.add(u);
            }
        }
        return admins;
    }

    public static List<User> listerClients() {
        List<User> clients = new ArrayList<>();
        for (User u : listerUtilisateurs()) {
            if (!u.isAdmin()) {
                clients.add(u);
            }
        }
        return clients;
    }

    public static User getUserById(int id) {
        for (User u : listerUtilisateurs()) {
            if (u.getId() == id) {
                return u;
            }
        }
        return null;
    }

    public static User getUserByEmail(String email) {
        for (User u : listerUtilisateurs()) {
            if (u.getEmail().equalsIgnoreCase(email)) {
                return u;
            }
        }
        return null;
    }


    public static User verifierConnexion(String email, String motDePasse) {
        List<User> utilisateurs = listerUtilisateurs();
        for (User u : utilisateurs) {
            if (u.getEmail().equalsIgnoreCase(email) && u.getMotDePasse().equals(motDePasse)) {
                return u;
            }
        }
        return null; 
    }

    
    public static boolean supprimerUtilisateur(String email) {
        List<User> utilisateurs = listerUtilisateurs();
        boolean supprime = false;
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(FICHIER_UTILISATEURS))) {
            for (User u : utilisateurs) {
                if (u.getEmail().equalsIgnoreCase(email)) {
                    supprime = true; 
                    continue;
                }
                String ligne = formatUserLigne(u);
                writer.write(ligne);
                writer.newLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return supprime;
    }

    public static boolean modifierUtilisateur(User user) {
        List<User> utilisateurs = listerUtilisateurs();
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(FICHIER_UTILISATEURS))) {
            for (User u : utilisateurs) {
                if (u.getId() == user.getId()) {
                    writer.write(formatUserLigne(user));
                } else {
                    writer.write(formatUserLigne(u));
                }
                writer.newLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public static boolean changerRole(int userId, Role newRole) {
        User user = getUserById(userId);
        if (user != null) {
            user.setRole(newRole);
            return modifierUtilisateur(user);
        }
        return false;
    }

    public static boolean activerDesactiverUtilisateur(int userId, boolean actif) {
        User user = getUserById(userId);
        if (user != null) {
            user.setActif(actif);
            return modifierUtilisateur(user);
        }
        return false;
    }

    public static void mettreAJourQuota(int userId, long taille) {
        User user = getUserById(userId);
        if (user != null) {
            user.setQuotaUtilise(user.getQuotaUtilise() + taille);
            modifierUtilisateur(user);
        }
    }

    public static boolean emailExiste(String email) {
        return getUserByEmail(email) != null;
    }

    public static Map<String, Object> getStatistiques() {
        Map<String, Object> stats = new HashMap<>();
        List<User> utilisateurs = listerUtilisateurs();
        
        int admins = 0, clients = 0, actifs = 0, inactifs = 0;
        long totalQuotaUtilise = 0;
        
        for (User u : utilisateurs) {
            if (u.isAdmin()) admins++;
            else clients++;
            if (u.isActif()) actifs++;
            else inactifs++;
            totalQuotaUtilise += u.getQuotaUtilise();
        }
        
        stats.put("total", utilisateurs.size());
        stats.put("admins", admins);
        stats.put("clients", clients);
        stats.put("actifs", actifs);
        stats.put("inactifs", inactifs);
        stats.put("quotaTotal", totalQuotaUtilise);
        
        return stats;
    }
}
