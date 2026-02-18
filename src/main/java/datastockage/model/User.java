package model;

public class User {

    public enum Role { ADMIN, CLIENT }

    private int id;
    private String nom;
    private String email;
    private String motDePasse;
    private String dossierUtilisateur;
    private Role role;
    private long quotaMax; // quota en octets
    private long quotaUtilise;
    private String dateCreation;
    private boolean actif;

    public User() {
        this.role = Role.CLIENT;
        this.quotaMax = 1_073_741_824L; // 1 GB par défaut
        this.quotaUtilise = 0;
        this.actif = true;
    }

    public User(int id, String nom, String email, String motDePasse, String dossierUtilisateur) {
        this();
        this.id = id;
        this.nom = nom;
        this.email = email;
        this.motDePasse = motDePasse;
        this.dossierUtilisateur = dossierUtilisateur;
    }

    public User(int id, String nom, String email, String motDePasse, String dossierUtilisateur, Role role) {
        this(id, nom, email, motDePasse, dossierUtilisateur);
        this.role = role;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getMotDePasse() {
        return motDePasse;
    }

    public void setMotDePasse(String motDePasse) {
        this.motDePasse = motDePasse;
    }

    public String getDossierUtilisateur() {
        return dossierUtilisateur;
    }

    public void setDossierUtilisateur(String dossierUtilisateur) {
        this.dossierUtilisateur = dossierUtilisateur;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public boolean isAdmin() {
        return role == Role.ADMIN;
    }

    public long getQuotaMax() {
        return quotaMax;
    }

    public void setQuotaMax(long quotaMax) {
        this.quotaMax = quotaMax;
    }

    public long getQuotaUtilise() {
        return quotaUtilise;
    }

    public void setQuotaUtilise(long quotaUtilise) {
        this.quotaUtilise = quotaUtilise;
    }

    public String getDateCreation() {
        return dateCreation;
    }

    public void setDateCreation(String dateCreation) {
        this.dateCreation = dateCreation;
    }

    public boolean isActif() {
        return actif;
    }

    public void setActif(boolean actif) {
        this.actif = actif;
    }
}
