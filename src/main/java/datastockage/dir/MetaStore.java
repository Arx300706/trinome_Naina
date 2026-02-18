package dir;

import common.FileMeta;

import java.io.*;
import java.nio.file.*;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Gère la persistance des métadonnées de fichiers sur disque.
 *
 * Structure sur disque :
 *   dir-meta/
 *     fichier1.txt.json
 *     image.png.json
 *     ...
 *
 * Chaque fichier .json contient le FileMeta sérialisé.
 * Atomic write : on écrit dans un fichier .tmp puis on renomme
 * pour éviter la corruption en cas de crash.
 */
public class MetaStore {

    private final Path baseDir;

    public MetaStore(String dirPath) {
        this.baseDir = Paths.get(dirPath);
        try{
            Files.createDirectories(baseDir);
        }
        catch(IOException e){
            e.printStackTrace();
        }
    }

    // ─── Sauvegarde un FileMeta sur disque ────────────────────────────────────

    public void save(FileMeta meta) throws IOException {
        String json = meta.toJson();
        Path target = metaPath(meta.fileName);
        Path tmp    = metaPath(meta.fileName + ".tmp");

        // écriture atomique : tmp → rename
        Files.writeString(tmp, json);
        Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
    }

    // ─── Supprime le fichier .json d'un FileMeta ─────────────────────────────

    public void delete(String fileName) throws IOException {
        Files.deleteIfExists(metaPath(fileName));
    }

    // ─── Charge tous les FileMeta depuis le disque ───────────────────────────
    //     Appelé une seule fois au démarrage du DirServer

    public Map<String, FileMeta> loadAll() throws IOException {
        Map<String, FileMeta> result = new ConcurrentHashMap<>();

        if (!Files.exists(baseDir)) return result;

        try (DirectoryStream<Path> stream =
                     Files.newDirectoryStream(baseDir, "*.json")) {

            for (Path p : stream) {
                try {
                    String json = Files.readString(p);
                    FileMeta meta = FileMeta.fromJson(json);

                    if (meta.fileName != null && !meta.fileName.isBlank()) {
                        String key = meta.ownerId + "_" + meta.fileName;
                        result.put(key, meta);
                        System.out.println("[MetaStore] Loaded: " + key
                                + " (" + meta.chunkIds.size() + " chunks)");
                    }

                } catch (Exception e) {
                    System.err.println("[MetaStore] Skipping corrupt file: "
                            + p.getFileName() + " → " + e.getMessage());
                }
            }
        }

        System.out.println("[MetaStore] Total files loaded: " + result.size());
        return result;
    }

    // ─── helper ──────────────────────────────────────────────────────────────

    private Path metaPath(String fileName) {
        // Sanitize : évite les path traversal (ex: "../../etc/passwd")
        String safe = fileName.replaceAll("[^a-zA-Z0-9._\\-]", "_");
        return baseDir.resolve(safe + ".json");
    }
}
