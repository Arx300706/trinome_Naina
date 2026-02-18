package service;

import java.io.File;

public class ApacheService {

    private static final String APACHE_ROOT = "/var/www/datastockage/";

    public static boolean creerDossierUtilisateur(String nomUtilisateur) {
        String chemin = APACHE_ROOT + nomUtilisateur + "/uploads/";
        File dossier = new File(chemin);
        if (!dossier.exists()) {
            return dossier.mkdirs();
        }
        return true;
    }

    public static boolean dossierExiste(String nomUtilisateur) {
        String chemin = APACHE_ROOT + nomUtilisateur + "/uploads/";
        File dossier = new File(chemin);
        return dossier.exists() && dossier.isDirectory();
    }

    private static boolean supprimerRecursif(File fichierOuDossier) {
        if (fichierOuDossier.isDirectory()) {
            for (File f : fichierOuDossier.listFiles()) {
                supprimerRecursif(f);
            }
        }
        return fichierOuDossier.delete();
    }

    public static boolean supprimerDossierUtilisateur(String nomUtilisateur) {
        String chemin = APACHE_ROOT + nomUtilisateur + "/uploads/";
        File dossier = new File(chemin);
        if (dossier.exists()) {
            return supprimerRecursif(dossier);
        }
        return false;
    }

}
