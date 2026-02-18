package common;

import java.util.*;
import java.io.*;
import java.nio.file.*;

public class FileMeta {

    public String fileName;
    public String ownerId;
    public long totalSize;
    public List<String> chunkIds = new ArrayList<>();
    public Map<String, List<String>> chunkLocations = new HashMap<>();

    // ─────────────────────────────────────────────
    // Sérialisation JSON manuelle (pas de lib externe)
    // ─────────────────────────────────────────────

    /**
     * Convertit ce FileMeta en JSON.
     * Format :
     * {
     * {
     * "fileName": "test.txt",
     * "ownerId": "12",
     * "chunkIds": ["id1","id2"],
     * "chunkLocations": {
     * "id1": ["osd-9001","osd-9002"],
     * "id2": ["osd-9001"]
     * }
     * }
     * 
     * }
     * }
     */
    
    public String toJson() {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        sb.append("  \"fileName\": \"").append(escapeJson(fileName)).append("\",\n");
        sb.append("  \"ownerId\": \"").append(escapeJson(ownerId)).append("\",\n");
        sb.append("  \"totalSize\": ").append(totalSize).append(",\n");

        // chunkIds
        sb.append("  \"chunkIds\": [");
        for (int i = 0; i < chunkIds.size(); i++) {
            sb.append("\"").append(escapeJson(chunkIds.get(i))).append("\"");
            if (i < chunkIds.size() - 1)
                sb.append(",");
        }
        sb.append("],\n");

        // chunkLocations
        sb.append("  \"chunkLocations\": {\n");
        List<String> keys = new ArrayList<>(chunkLocations.keySet());
        for (int i = 0; i < keys.size(); i++) {
            String k = keys.get(i);
            List<String> locs = chunkLocations.get(k);
            sb.append("    \"").append(escapeJson(k)).append("\": [");
            for (int j = 0; j < locs.size(); j++) {
                sb.append("\"").append(escapeJson(locs.get(j))).append("\"");
                if (j < locs.size() - 1)
                    sb.append(",");
            }
            sb.append("]");
            if (i < keys.size() - 1)
                sb.append(",");
            sb.append("\n");
        }
        sb.append("  }\n");
        sb.append("}");
        return sb.toString();
    }

    /**
     * Reconstruit un FileMeta depuis une chaîne JSON.
     * Parser minimal mais suffisant pour notre format auto-généré.
     */
    public static FileMeta fromJson(String json) {
        FileMeta m = new FileMeta();

        // fileName
        m.fileName = extractString(json, "fileName");
        m.ownerId = extractString(json, "ownerId");
        // totalSize
        try {
            String marker = "\"totalSize\": ";
            int idx = json.indexOf(marker);
            if (idx >= 0) {
                int start = idx + marker.length();
                int end = json.indexOf(",", start);
                if (end < 0) end = json.indexOf("\n", start);
                m.totalSize = Long.parseLong(json.substring(start, end).trim());
            }
        } catch (Exception ignored) {}

        // chunkIds : tableau entre les [ ] après "chunkIds"
        String chunkIdsBlock = extractArrayBlock(json, "chunkIds");
        m.chunkIds = parseStringArray(chunkIdsBlock);

        // chunkLocations : objet entre les { } après "chunkLocations"
        String locBlock = extractObjectBlock(json, "chunkLocations");
        if (locBlock != null && !locBlock.isBlank()) {
            // chaque ligne : "chunkId": ["osd1","osd2"]
            // on split sur les entrées clé:valeur
            String[] lines = locBlock.split(",\\s*\n|\\n");
            for (String line : lines) {
                line = line.trim();
                if (line.isBlank())
                    continue;

                int colonIdx = line.indexOf("\":");
                if (colonIdx < 0)
                    continue;

                String key = line.substring(1, colonIdx).trim();
                String rest = line.substring(colonIdx + 2).trim();
                List<String> vals = parseStringArray(rest);
                m.chunkLocations.put(key, vals);
            }
        }

        return m;
    }

    // ─── helpers de parsing ───────────────────────

    private static String extractString(String json, String field) {
        String marker = "\"" + field + "\": \"";
        int start = json.indexOf(marker);
        if (start < 0)
            return "";
        start += marker.length();
        int end = json.indexOf("\"", start);
        return json.substring(start, end);
    }

    private static String extractArrayBlock(String json, String field) {
        String marker = "\"" + field + "\": [";
        int start = json.indexOf(marker);
        if (start < 0)
            return "";
        start += marker.length() - 1; // pointe sur le '['
        int depth = 0, i = start;
        while (i < json.length()) {
            char c = json.charAt(i);
            if (c == '[')
                depth++;
            else if (c == ']') {
                depth--;
                if (depth == 0)
                    return json.substring(start + 1, i);
            }
            i++;
        }
        return "";
    }

    private static String extractObjectBlock(String json, String field) {
        String marker = "\"" + field + "\": {";
        int start = json.indexOf(marker);
        if (start < 0)
            return "";
        start += marker.length() - 1; // pointe sur le '{'
        int depth = 0, i = start;
        while (i < json.length()) {
            char c = json.charAt(i);
            if (c == '{')
                depth++;
            else if (c == '}') {
                depth--;
                if (depth == 0)
                    return json.substring(start + 1, i);
            }
            i++;
        }
        return "";
    }

    private static List<String> parseStringArray(String block) {
        List<String> result = new ArrayList<>();
        if (block == null || block.isBlank())
            return result;
        int i = 0;
        while (i < block.length()) {
            int q1 = block.indexOf('"', i);
            if (q1 < 0)
                break;
            int q2 = block.indexOf('"', q1 + 1);
            if (q2 < 0)
                break;
            result.add(block.substring(q1 + 1, q2));
            i = q2 + 1;
        }
        return result;
    }

    private static String escapeJson(String s) {
        if (s == null)
            return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
