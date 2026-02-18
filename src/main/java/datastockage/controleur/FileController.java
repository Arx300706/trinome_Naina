package controleur;

import model.FileModel;
import model.User;
import service.FileService;
import service.UserService;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class FileController {

    public static boolean upload(User user, File fichierSource) {
        boolean success = FileService.uploadFichier(user, fichierSource);
        if (success) {
            // Mettre à jour le quota utilisé
            UserService.mettreAJourQuota(user.getId(), fichierSource.length());
        }
        return success;
    }

    public static List<FileModel> listerFichiers(User user) {
        return FileService.listerFichiersUtilisateur(user);
    }

    public static File telecharger(User user, String nomFichier) {
        return FileService.telechargerFichier(user, nomFichier);
    }

    public static boolean supprimer(User user, String nomFichier) {
        // Récupérer la taille du fichier avant suppression
        List<FileModel> fichiers = FileService.listerFichiersUtilisateur(user);
        long taille = 0;
        for (FileModel f : fichiers) {
            if (f.getNomFichier().equals(nomFichier)) {
                taille = f.getTaille();
                break;
            }
        }

        boolean success = FileService.supprimerFichier(user, nomFichier);
        if (success && taille > 0) {
            // Mettre à jour le quota (soustraire)
            UserService.mettreAJourQuota(user.getId(), -taille);
        }
        return success;
    }

    public static long getEspaceUtilise(User user) {
        return FileService.calculerEspaceUtilise(user);
    }

    public static long getEspaceDisponible(User user) {
        return user.getQuotaMax() - user.getQuotaUtilise();
    }

    public static boolean isDistributedServerAvailable() {
        return FileService.isDistributedServerAvailable();
    }

    public static List<FileModel> listerFichiersCluster(User user) {

        List<FileModel> result = new ArrayList<>();

        try (Socket socket = new Socket("10.134.17.222", 7000);
                DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
                DataInputStream dis = new DataInputStream(socket.getInputStream())) {

            dos.writeUTF("LIST_FILES");
            dos.writeUTF(String.valueOf(user.getId()));
            dos.flush();

            String response = dis.readUTF();

            if (!response.isEmpty()) {
                String[] lines = response.split("\n");

                for (String line : lines) {
                    if (line.isBlank()) continue;
                    String[] parts = line.split("\\|");

                    String name = parts[0];
                    long size = Long.parseLong(parts[1]);
                    String date = parts.length > 2 ? parts[2] : "unknown";

                    FileModel fm = new FileModel(name, size, date);

                    // Chunks et replicas si disponibles
                    if (parts.length > 3) {
                        try { fm.setChunkCount(Integer.parseInt(parts[3])); } catch (Exception e) {}
                    }
                    if (parts.length > 4) {
                        try { fm.setReplicas(Integer.parseInt(parts[4])); } catch (Exception e) {}
                    }

                    fm.setDistribue(true);
                    fm.setProprietaireId(user.getId());
                    result.add(fm);
                }
            }

        } catch (Exception e) {
            System.err.println("[FileController] Erreur listing cluster: " + e.getMessage());
        }

        return result;
    }

}
