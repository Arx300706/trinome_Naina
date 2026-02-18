package service;

import model.FileModel;
import model.User;

import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import common.FileMeta;
import dir.DirServer;

public class FileService {

    private static final String APACHE_ROOT = "/var/www/datastockage/";
    private static final String DIR_HOST = "10.134.17.222";
    private static final int DIR_PORT = 7000;
    private static boolean useDistributedStorage = false;

    public static void setUseDistributedStorage(boolean use) {
        useDistributedStorage = use;
    }

    public static boolean isDistributedStorageEnabled() {
        return useDistributedStorage;
    }

    public static boolean uploadFichier(User user, File fichierSource) {
        try {
            String dossierUtilisateur = APACHE_ROOT + user.getDossierUtilisateur() + "/uploads/";
            File dossier = new File(dossierUtilisateur);
            if (!dossier.exists()) {
                dossier.mkdirs();
            }

            Path source = fichierSource.toPath();
            Path destination = Paths.get(dossierUtilisateur, fichierSource.getName());
            Files.copy(source, destination, StandardCopyOption.REPLACE_EXISTING);

            if (useDistributedStorage) {
                try {
                    uploadToDistributed(String.valueOf(user.getId()), fichierSource);
                } catch (Exception e) {
                    System.err.println("[FileService] Erreur upload distribue: " + e.getMessage());
                }
            }

            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Upload vers le serveur DIR via socket TCP (port 7000)
     */
    private static void uploadToDistributed(String userId, File file) throws Exception {
        try (Socket socket = new Socket(DIR_HOST, DIR_PORT);
             DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
             FileInputStream fis = new FileInputStream(file)) {

            dos.writeUTF("UPLOAD");
            dos.writeUTF(userId);
            dos.writeUTF(file.getName());
            dos.writeLong(file.length());

            byte[] buffer = new byte[1_000_000];
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) != -1) {
                dos.writeInt(bytesRead);
                dos.write(buffer, 0, bytesRead);
            }

            dos.writeInt(-1);
            dos.flush();

            System.out.println("[FileService] Upload distribue OK: " + file.getName());
        }
    }

    public static List<FileModel> listerFichiersUtilisateur(User user) {
        List<FileModel> fichiers = new ArrayList<>();
        String dossierUtilisateur = APACHE_ROOT + user.getDossierUtilisateur() + "/uploads/";
        File dossier = new File(dossierUtilisateur);

        if (dossier.exists() && dossier.isDirectory()) {
            File[] files = dossier.listFiles();
            if (files != null) {
                for (File f : files) {
                    if (f.isFile()) {
                        String dateUpload = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
                                .format(new Date(f.lastModified()));

                        FileModel model = new FileModel(f.getName(), f.length(), dateUpload);
                        model.setProprietaireId(user.getId());

                        String fileKey = user.getId() + "_" + f.getName();
                        FileMeta meta = DirServer.files.get(fileKey);

                        if (meta != null) {
                            model.setDistribue(!meta.chunkIds.isEmpty());
                            model.setChunkCount(meta.chunkIds.size());
                            model.setReplicas(meta.chunkLocations.values().stream()
                                    .mapToInt(List::size).max().orElse(0));
                        } else {
                            model.setDistribue(false);
                            model.setChunkCount(0);
                            model.setReplicas(0);
                        }

                        fichiers.add(model);
                    }
                }
            }
        }

        return fichiers;
    }

    public static File telechargerFichier(User user, String nomFichier) {
        String chemin = APACHE_ROOT + user.getDossierUtilisateur() + "/uploads/" + nomFichier;
        File f = new File(chemin);

        if (f.exists() && f.isFile()) {
            return f;
        }

        // Recuperer depuis le cluster via socket
        try {
            byte[] data = downloadFromDistributed(String.valueOf(user.getId()), nomFichier);
            if (data != null) {
                File dossier = new File(APACHE_ROOT + user.getDossierUtilisateur() + "/uploads/");
                if (!dossier.exists()) dossier.mkdirs();

                f = new File(dossier, nomFichier);
                Files.write(f.toPath(), data);
                System.out.println("[FileService] Fichier reconstruit depuis cluster: " + nomFichier);
                return f;
            }
        } catch (Exception e) {
            System.err.println("[FileService] Erreur reconstruction: " + e.getMessage());
        }

        return null;
    }

    /**
     * Telecharge depuis le serveur DIR via socket TCP (GET)
     */
    private static byte[] downloadFromDistributed(String userId, String fileName) throws Exception {
        try (Socket socket = new Socket(DIR_HOST, DIR_PORT);
             DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
             DataInputStream dis = new DataInputStream(socket.getInputStream())) {

            dos.writeUTF("DOWNLOAD");
            dos.writeUTF(fileName);
            dos.writeUTF(userId);
            dos.flush();

            boolean exists = dis.readBoolean();
            if (!exists) return null;

            long size = dis.readLong();
            byte[] data = new byte[(int) size];
            dis.readFully(data);
            return data;
        }
    }

    public static boolean supprimerFichier(User user, String nomFichier) {
        String chemin = APACHE_ROOT + user.getDossierUtilisateur() + "/uploads/" + nomFichier;
        File f = new File(chemin);
        boolean localDeleted = false;

        if (f.exists() && f.isFile()) {
            localDeleted = f.delete();
        }

        // Supprimer aussi du cluster via socket (POST delete)
        try {
            deleteFromDistributed(String.valueOf(user.getId()), nomFichier);
        } catch (Exception e) {
            System.err.println("[FileService] Erreur suppression distribuee: " + e.getMessage());
        }

        return localDeleted;
    }

    /**
     * Supprime du serveur DIR via socket TCP (POST delete)
     */
    private static void deleteFromDistributed(String userId, String fileName) throws Exception {
        try (Socket socket = new Socket(DIR_HOST, DIR_PORT);
             DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
             DataInputStream dis = new DataInputStream(socket.getInputStream())) {

            dos.writeUTF("DELETE_FILE");
            dos.writeUTF(userId);
            dos.writeUTF(fileName);
            dos.flush();

            String response = dis.readUTF();
            System.out.println("[FileService] Delete cluster: " + response);
        }
    }

    public static String calculerChecksum(File fichier) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] fileBytes = Files.readAllBytes(fichier.toPath());
            byte[] digest = md.digest(fileBytes);

            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            return null;
        }
    }

    public static boolean verifierIntegrite(File fichier, String checksumAttendu) {
        String checksumActuel = calculerChecksum(fichier);
        return checksumActuel != null && checksumActuel.equals(checksumAttendu);
    }

    public static long calculerEspaceUtilise(User user) {
        long total = 0;
        for (FileModel f : listerFichiersUtilisateur(user)) {
            total += f.getTaille();
        }
        return total;
    }

    /**
     * Verifie si le serveur DIR est accessible via socket (GET health)
     */
    public static boolean isDistributedServerAvailable() {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(DIR_HOST, DIR_PORT), 2000);
            DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
            DataInputStream dis = new DataInputStream(socket.getInputStream());
            dos.writeUTF("STATS");
            dos.flush();
            dis.readUTF();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Liste les fichiers stockes sur le cluster via socket (GET list)
     */
    public static List<String> listerFichiersDistribues() {
        List<String> fichiers = new ArrayList<>();
        try (Socket socket = new Socket(DIR_HOST, DIR_PORT);
             DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
             DataInputStream dis = new DataInputStream(socket.getInputStream())) {

            dos.writeUTF("LIST_ALL");
            dos.flush();

            String response = dis.readUTF();
            if (response != null && !response.isBlank()) {
                for (String line : response.split("\n")) {
                    if (!line.isBlank()) {
                        String[] parts = line.split("\\|");
                        if (parts.length >= 3) {
                            fichiers.add("User:" + parts[0] + " | " + parts[1] +
                                " (" + formatSize(Long.parseLong(parts[2])) + ")");
                        } else {
                            fichiers.add(line.trim());
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("[FileService] Erreur listing cluster: " + e.getMessage());
        }
        return fichiers;
    }

    /**
     * Obtient les statistiques du cluster via socket (GET stats)
     */
    public static String getClusterStats() {
        try (Socket socket = new Socket(DIR_HOST, DIR_PORT);
             DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
             DataInputStream dis = new DataInputStream(socket.getInputStream())) {

            dos.writeUTF("STATS");
            dos.flush();
            return dis.readUTF();
        } catch (Exception e) {
            return "Serveur distribue non disponible";
        }
    }

    private static String formatSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1048576) return String.format("%.2f KB", bytes / 1024.0);
        if (bytes < 1073741824) return String.format("%.2f MB", bytes / 1048576.0);
        return String.format("%.2f GB", bytes / 1073741824.0);
    }
}
