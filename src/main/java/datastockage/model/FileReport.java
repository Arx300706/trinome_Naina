package model;

/**
 * Représente un signalement de fichier.
 * Les utilisateurs peuvent signaler des fichiers inappropriés.
 */
public class FileReport {

    public enum Status { EN_ATTENTE, TRAITE, REJETE }
    public enum Reason { CONTENU_ILLEGAL, DROITS_AUTEUR, SPAM, MALWARE, AUTRE }

    private int id;
    private String nomFichier;
    private String cheminFichier;
    private int rapporteurId; // ID de l'utilisateur qui signale
    private int proprietaireId; // ID du propriétaire du fichier
    private Reason raison;
    private String description;
    private Status status;
    private String dateSignalement;
    private String dateTraitement;
    private int adminTraitantId; // ID de l'admin qui a traité
    private String commentaireAdmin;

    public FileReport() {
        this.status = Status.EN_ATTENTE;
    }

    public FileReport(int id, String nomFichier, String cheminFichier, int rapporteurId, 
                      int proprietaireId, Reason raison, String description) {
        this();
        this.id = id;
        this.nomFichier = nomFichier;
        this.cheminFichier = cheminFichier;
        this.rapporteurId = rapporteurId;
        this.proprietaireId = proprietaireId;
        this.raison = raison;
        this.description = description;
    }

    // Getters et Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getNomFichier() {
        return nomFichier;
    }

    public void setNomFichier(String nomFichier) {
        this.nomFichier = nomFichier;
    }

    public String getCheminFichier() {
        return cheminFichier;
    }

    public void setCheminFichier(String cheminFichier) {
        this.cheminFichier = cheminFichier;
    }

    public int getRapporteurId() {
        return rapporteurId;
    }

    public void setRapporteurId(int rapporteurId) {
        this.rapporteurId = rapporteurId;
    }

    public int getProprietaireId() {
        return proprietaireId;
    }

    public void setProprietaireId(int proprietaireId) {
        this.proprietaireId = proprietaireId;
    }

    public Reason getRaison() {
        return raison;
    }

    public void setRaison(Reason raison) {
        this.raison = raison;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public String getDateSignalement() {
        return dateSignalement;
    }

    public void setDateSignalement(String dateSignalement) {
        this.dateSignalement = dateSignalement;
    }

    public String getDateTraitement() {
        return dateTraitement;
    }

    public void setDateTraitement(String dateTraitement) {
        this.dateTraitement = dateTraitement;
    }

    public int getAdminTraitantId() {
        return adminTraitantId;
    }

    public void setAdminTraitantId(int adminTraitantId) {
        this.adminTraitantId = adminTraitantId;
    }

    public String getCommentaireAdmin() {
        return commentaireAdmin;
    }

    public void setCommentaireAdmin(String commentaireAdmin) {
        this.commentaireAdmin = commentaireAdmin;
    }

    public String getRaisonLibelle() {
        switch (raison) {
            case CONTENU_ILLEGAL: return "Contenu illégal";
            case DROITS_AUTEUR: return "Violation droits d'auteur";
            case SPAM: return "Spam";
            case MALWARE: return "Malware/Virus";
            case AUTRE: return "Autre";
            default: return "Non spécifié";
        }
    }

    public String getStatusLibelle() {
        switch (status) {
            case EN_ATTENTE: return "En attente";
            case TRAITE: return "Traité";
            case REJETE: return "Rejeté";
            default: return "Inconnu";
        }
    }
}
