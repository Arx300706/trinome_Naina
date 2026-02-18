package model;

public class FileModel {

    private String nomFichier;
    private long taille;
    private String dateUpload;
    private int proprietaireId;

    // Infos cluster
    private boolean distribue;
    private int replicas;
    private int chunkCount;

    public FileModel() {}

    public FileModel(String nomFichier, long taille, String dateUpload) {
        this.nomFichier = nomFichier;
        this.taille = taille;
        this.dateUpload = dateUpload;
        this.distribue = true; // cluster par défaut
    }

    // ─── Getters / Setters ─────────────────────────────

    public String getNomFichier() {
        return nomFichier;
    }

    public void setNomFichier(String nomFichier) {
        this.nomFichier = nomFichier;
    }

    public long getTaille() {
        return taille;
    }

    public void setTaille(long taille) {
        this.taille = taille;
    }

    public String getDateUpload() {
        return dateUpload;
    }

    public void setDateUpload(String dateUpload) {
        this.dateUpload = dateUpload;
    }

    public int getProprietaireId() {
        return proprietaireId;
    }

    public void setProprietaireId(int proprietaireId) {
        this.proprietaireId = proprietaireId;
    }

    public boolean isDistribue() {
        return distribue;
    }

    public void setDistribue(boolean distribue) {
        this.distribue = distribue;
    }

    public int getReplicas() {
        return replicas;
    }

    public void setReplicas(int replicas) {
        this.replicas = replicas;
    }

    public int getChunkCount() {
        return chunkCount;
    }

    public void setChunkCount(int chunkCount) {
        this.chunkCount = chunkCount;
    }

    // ─── Format taille ─────────────────────────────

    public String getTailleFormatee() {
        if (taille < 1024) return taille + " B";
        if (taille < 1048576) return String.format("%.2f KB", taille / 1024.0);
        if (taille < 1073741824) return String.format("%.2f MB", taille / 1048576.0);
        return String.format("%.2f GB", taille / 1073741824.0);
    }
}
