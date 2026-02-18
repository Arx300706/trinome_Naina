package osd;

import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.concurrent.*;

public class OSDServer implements Runnable {

    static String dirHost = "10.134.17.222";
    static int dirPort = 7000;

    private String osdId;
    private int port;
    private volatile boolean running = true;

    private ScheduledExecutorService heartbeatExecutor;

    public OSDServer(int port) {
        this.port = port;
        this.osdId = "osd-" + port;
    }

    @Override
    public void run() {

        startHeartbeat();   // 🔥 IMPORTANT

        try (ServerSocket serverSocket = new ServerSocket(port, 50, java.net.InetAddress.getByName("0.0.0.0"))) {
            System.out.println("[OSD " + osdId + "] Started on port " + port + " (toutes interfaces)");

            while (running) {
                Socket client = serverSocket.accept();
                new Thread(() -> handle(client)).start();
            }

        } catch (BindException ex) {
            System.err.println("⚠️ Port " + port + " déjà utilisé.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void startHeartbeat() {
        heartbeatExecutor = Executors.newSingleThreadScheduledExecutor();

        heartbeatExecutor.scheduleAtFixedRate(() -> {
            try (Socket socket = new Socket(dirHost, dirPort);
                 DataOutputStream dos = new DataOutputStream(socket.getOutputStream())) {

                dos.writeUTF("HEARTBEAT");
                dos.writeUTF(osdId);
                dos.writeInt(port);
                dos.flush();

                System.out.println("[OSD " + osdId + "] Heartbeat sent");

            } catch (Exception ignored) {
            }

        }, 2, 5, TimeUnit.SECONDS);
    }

    public void stop() {
        running = false;

        if (heartbeatExecutor != null) {
            heartbeatExecutor.shutdownNow();
        }
    }

    private void handle(Socket socket) {
        try (DataInputStream dis = new DataInputStream(socket.getInputStream());
                DataOutputStream dos = new DataOutputStream(socket.getOutputStream())) {

            String command = dis.readUTF();
            switch (command) {

                case "STORE_CHUNK" -> {
                    String chunkId = dis.readUTF();
                    int size = dis.readInt();
                    byte[] data = new byte[size];
                    dis.readFully(data);

                    Path path = Paths.get("storage/" + chunkId);
                    Files.createDirectories(path.getParent());
                    Files.write(path, data);

                    dos.writeUTF("OK");
                    System.out.println("[OSD " + osdId + "] Stored " + chunkId);
                }

                case "GET_CHUNK" -> {
                    String chunkId = dis.readUTF();
                    Path path = Paths.get("storage/" + chunkId);
                    if (!Files.exists(path)) {
                        dos.writeUTF("NOT_FOUND");
                        return;
                    }
                    byte[] data = Files.readAllBytes(path);
                    dos.writeUTF("OK");
                    dos.writeInt(data.length);
                    dos.write(data);
                    System.out.println("[OSD " + osdId + "] Sent " + chunkId);
                }

                case "DELETE_CHUNK" -> {
                    String chunkId = dis.readUTF();
                    Files.deleteIfExists(Paths.get("storage/" + chunkId));
                    dos.writeUTF("OK");
                    System.out.println("[OSD " + osdId + "] Deleted " + chunkId);
                }

                default -> System.out.println("[OSD] Unknown command: " + command);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
