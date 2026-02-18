package client;

import model.Session;
import model.User;

import javax.swing.*;
import java.io.*;
import java.net.Socket;

public class ClientDownloader {

    public static void download(String fileName) {
        User user = Session.getUtilisateurConnecte();

        if (fileName == null || fileName.isEmpty()) {
            JOptionPane.showMessageDialog(null, "Aucun fichier selectionne !");
            return;
        }

        if (user == null) {
            JOptionPane.showMessageDialog(null, "Utilisateur non connecte !");
            return;
        }

        try (Socket socket = new Socket("10.134.17.222", 7000);
             DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
             DataInputStream dis = new DataInputStream(socket.getInputStream())) {

            // Envoyer commande DOWNLOAD (GET)
            dos.writeUTF("DOWNLOAD");
            dos.writeUTF(fileName);
            dos.writeUTF(String.valueOf(user.getId()));
            dos.flush();

            // Lire reponse serveur
            boolean exists = dis.readBoolean();

            if (!exists) {
                JOptionPane.showMessageDialog(null,
                    "Fichier introuvable ou non autorise !");
                return;
            }

            long size = dis.readLong();
            byte[] data = new byte[(int) size];
            dis.readFully(data);

            // Sauvegarder dans le dossier Telechargements
            String home = System.getProperty("user.home");
            File downloads = new File(home, "Telechargements");
            if (!downloads.exists()) {
                downloads = new File(home, "Downloads");
            }
            if (!downloads.exists()) {
                downloads.mkdirs();
            }

            File outputFile = new File(downloads, fileName);

            // Si le fichier existe deja, ajouter un suffixe
            if (outputFile.exists()) {
                String baseName = fileName;
                String extension = "";
                int dotIdx = fileName.lastIndexOf('.');
                if (dotIdx > 0) {
                    baseName = fileName.substring(0, dotIdx);
                    extension = fileName.substring(dotIdx);
                }
                int counter = 1;
                while (outputFile.exists()) {
                    outputFile = new File(downloads, baseName + "_(" + counter + ")" + extension);
                    counter++;
                }
            }

            FileOutputStream fos = new FileOutputStream(outputFile);
            fos.write(data);
            fos.close();

            JOptionPane.showMessageDialog(null,
                "Telechargement termine !\n" + outputFile.getAbsolutePath());
            System.out.println("[CLIENT] Downloaded: " + outputFile.getAbsolutePath());

        } catch (java.net.ConnectException e) {
            JOptionPane.showMessageDialog(null,
                "Impossible de se connecter au serveur.\nVerifiez que le cluster est demarre.",
                "Erreur connexion", JOptionPane.ERROR_MESSAGE);
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null,
                "Erreur lors du telechargement: " + e.getMessage(),
                "Erreur", JOptionPane.ERROR_MESSAGE);
        }
    }
}
