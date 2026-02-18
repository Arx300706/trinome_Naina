package util;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class FileUtils {

    public static List<String> lireFichier(String chemin) {
        List<String> lignes = new ArrayList<>();
        try {
            if (Files.exists(Paths.get(chemin))) {
                lignes = Files.readAllLines(Paths.get(chemin));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return lignes;
    }

    public static boolean ecrireFichier(String chemin, String contenu, boolean append) {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(chemin, append))) {
            bw.write(contenu);
            bw.newLine();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static boolean existe(String chemin) {
        return Files.exists(Paths.get(chemin));
    }


    public static boolean creerDossier(String chemin) {
        File dossier = new File(chemin);
        if (!dossier.exists()) {
            return dossier.mkdirs();
        }
        return true; 
    }

    public static boolean supprimerRecursif(File fichierOuDossier) {
        if (fichierOuDossier.isDirectory()) {
            for (File f : fichierOuDossier.listFiles()) {
                supprimerRecursif(f);
            }
        }
        return fichierOuDossier.delete();
    }
}
