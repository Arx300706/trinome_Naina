package server;

import java.io.*;
import java.net.Socket;

public class ClientHandler implements Runnable {

    private Socket socket;

    public ClientHandler(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try (DataInputStream dis = new DataInputStream(socket.getInputStream())) {

            String requestType = dis.readUTF();

            if ("UPLOAD".equals(requestType)) {

                String userId = dis.readUTF();
                String fileName = dis.readUTF();
                long fileSize = dis.readLong();

                File userDir = new File("storage/" + userId);
                userDir.mkdirs();

                File outputFile = new File(userDir, fileName);

                try (FileOutputStream fos = new FileOutputStream(outputFile)) {
                    byte[] buffer = new byte[4096];
                    long remaining = fileSize;
                    int bytesRead;

                    while (remaining > 0 &&
                           (bytesRead = dis.read(buffer, 0, (int)Math.min(buffer.length, remaining))) != -1) {
                        fos.write(buffer, 0, bytesRead);
                        remaining -= bytesRead;
                    }
                }

                System.out.println("Fichier reçu : " + fileName + " de user " + userId);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
