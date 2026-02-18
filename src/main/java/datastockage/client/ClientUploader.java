package client;

import java.io.*;
import java.net.Socket;

public class ClientUploader {

    private static final String DIR_HOST = "10.134.17.222";
    private static final int DIR_PORT = 7000;
    private static final int CHUNK_SIZE = 1_000_000; // 1 Mo

    public static void upload(File file, String userId) {
        if (file == null || !file.exists()) return;

        try (Socket socket = new Socket(DIR_HOST, DIR_PORT);
             DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
             FileInputStream fis = new FileInputStream(file)) {

            dos.writeUTF("UPLOAD");
            dos.writeUTF(userId);
            dos.writeUTF(file.getName());
            dos.writeLong(file.length());

            byte[] buffer = new byte[CHUNK_SIZE];
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) != -1) {
                dos.writeInt(bytesRead);
                dos.write(buffer, 0, bytesRead);
            }

            dos.writeInt(-1); // fin du fichier
            dos.flush();

            System.out.println("[CLIENT] Upload envoyé au DIR : " + file.getName());

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
