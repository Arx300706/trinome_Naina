package controleur;

import model.User;
import model.Session;
import service.UserService;
import service.ApacheService;

import java.util.List;

public class UserController {

    public static boolean inscrireUtilisateur(User user) {
        boolean dossierCree = ApacheService.creerDossierUtilisateur(user.getDossierUtilisateur());
        if (!dossierCree) {
            System.err.println("Erreur création dossier Apache");
            return false;
        }

        boolean ajoute = UserService.ajouterUtilisateur(user);
        if (!ajoute) {
            System.err.println("Erreur ajout utilisateur");
            return false;
        }
        return true;
    }

    public static boolean connexion(String email, String motDePasse) {
        User user = UserService.verifierConnexion(email, motDePasse);
        if (user != null) {
            Session.setUtilisateurConnecte(user);
            return true;
        }
        return false;
    }

    public static void deconnexion() {
        Session.deconnexion();
    }

    public static List<User> listerUtilisateurs() {
        return UserService.listerUtilisateurs();
    }
}
