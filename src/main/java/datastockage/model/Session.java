package model;

public class Session {

    private static User utilisateurConnecte;

    public static void setUtilisateurConnecte(User user) {
        utilisateurConnecte = user;
    }

    public static User getUtilisateurConnecte() {
        return utilisateurConnecte;
    }

    public static boolean estConnecte() {
        return utilisateurConnecte != null;
    }

    public static void deconnexion() {
        utilisateurConnecte = null;
    }
}
