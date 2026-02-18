package model;

import java.util.ArrayList;
import java.util.List;

/**
 * Représente un domaine de stockage.
 * Chaque domaine peut avoir des sous-domaines et des fichiers.
 */
public class Domain {

    private int id;
    private String nom;
    private String description;
    private int parentId; // -1 si domaine racine
    private int proprietaireId; // ID de l'admin qui gère ce domaine
    private long quotaMax;
    private long quotaUtilise;
    private String dateCreation;
    private boolean actif;
    private List<Domain> sousDomaines;

    public Domain() {
        this.parentId = -1;
        this.quotaMax = 10_737_418_240L; // 10 GB par défaut
        this.quotaUtilise = 0;
        this.actif = true;
        this.sousDomaines = new ArrayList<>();
    }

    public Domain(int id, String nom, String description, int proprietaireId) {
        this();
        this.id = id;
        this.nom = nom;
        this.description = description;
        this.proprietaireId = proprietaireId;
    }

    // Getters et Setters
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getParentId() {
        return parentId;
    }

    public void setParentId(int parentId) {
        this.parentId = parentId;
    }

    public int getProprietaireId() {
        return proprietaireId;
    }

    public void setProprietaireId(int proprietaireId) {
        this.proprietaireId = proprietaireId;
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

    public List<Domain> getSousDomaines() {
        return sousDomaines;
    }

    public void setSousDomaines(List<Domain> sousDomaines) {
        this.sousDomaines = sousDomaines;
    }

    public void addSousDomaine(Domain sousDomaine) {
        this.sousDomaines.add(sousDomaine);
    }

    public boolean isRacine() {
        return parentId == -1;
    }

    public String getCheminComplet() {
        // À implémenter avec la hiérarchie complète
        return "/" + nom;
    }

    @Override
    public String toString() {
        return nom + (isRacine() ? " (racine)" : "");
    }
}
