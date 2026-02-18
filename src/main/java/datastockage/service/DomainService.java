package service;

import model.Domain;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Service de gestion des domaines et sous-domaines.
 */
public class DomainService {

    private static final String FICHIER_DOMAINES = "src/main/resources/data/domaines.txt";
    private static int nextId = 1;

    static {
        // Charger le prochain ID disponible
        List<Domain> domaines = listerTousDomaines();
        for (Domain d : domaines) {
            if (d.getId() >= nextId) {
                nextId = d.getId() + 1;
            }
        }
    }

    public static boolean creerDomaine(Domain domain) {
        try {
            domain.setId(nextId++);
            domain.setDateCreation(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
            
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(FICHIER_DOMAINES, true))) {
                String ligne = formatDomainLigne(domain);
                writer.write(ligne);
                writer.newLine();
            }
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static List<Domain> listerTousDomaines() {
        List<Domain> domaines = new ArrayList<>();
        File fichier = new File(FICHIER_DOMAINES);
        if (!fichier.exists()) {
            return domaines;
        }
        
        try (BufferedReader reader = new BufferedReader(new FileReader(FICHIER_DOMAINES))) {
            String ligne;
            while ((ligne = reader.readLine()) != null) {
                Domain d = parseDomainLigne(ligne);
                if (d != null) {
                    domaines.add(d);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return domaines;
    }

    public static List<Domain> listerDomainesRacine() {
        List<Domain> racines = new ArrayList<>();
        for (Domain d : listerTousDomaines()) {
            if (d.isRacine()) {
                racines.add(d);
            }
        }
        return racines;
    }

    public static List<Domain> listerSousDomaines(int parentId) {
        List<Domain> sousDomaines = new ArrayList<>();
        for (Domain d : listerTousDomaines()) {
            if (d.getParentId() == parentId) {
                sousDomaines.add(d);
            }
        }
        return sousDomaines;
    }

    public static Domain getDomainById(int id) {
        for (Domain d : listerTousDomaines()) {
            if (d.getId() == id) {
                return d;
            }
        }
        return null;
    }

    public static boolean supprimerDomaine(int id) {
        List<Domain> domaines = listerTousDomaines();
        boolean supprime = false;
        
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(FICHIER_DOMAINES))) {
            for (Domain d : domaines) {
                if (d.getId() == id || d.getParentId() == id) {
                    supprime = true;
                    continue;
                }
                writer.write(formatDomainLigne(d));
                writer.newLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return supprime;
    }

    public static boolean modifierDomaine(Domain domain) {
        List<Domain> domaines = listerTousDomaines();
        
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(FICHIER_DOMAINES))) {
            for (Domain d : domaines) {
                if (d.getId() == domain.getId()) {
                    writer.write(formatDomainLigne(domain));
                } else {
                    writer.write(formatDomainLigne(d));
                }
                writer.newLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public static void mettreAJourQuota(int domainId, long taille) {
        Domain d = getDomainById(domainId);
        if (d != null) {
            d.setQuotaUtilise(d.getQuotaUtilise() + taille);
            modifierDomaine(d);
        }
    }

    private static String formatDomainLigne(Domain d) {
        return d.getId() + ";" + 
               d.getNom() + ";" + 
               d.getDescription() + ";" + 
               d.getParentId() + ";" + 
               d.getProprietaireId() + ";" + 
               d.getQuotaMax() + ";" + 
               d.getQuotaUtilise() + ";" + 
               d.getDateCreation() + ";" + 
               d.isActif();
    }

    private static Domain parseDomainLigne(String ligne) {
        try {
            String[] parts = ligne.split(";");
            if (parts.length >= 9) {
                Domain d = new Domain();
                d.setId(Integer.parseInt(parts[0]));
                d.setNom(parts[1]);
                d.setDescription(parts[2]);
                d.setParentId(Integer.parseInt(parts[3]));
                d.setProprietaireId(Integer.parseInt(parts[4]));
                d.setQuotaMax(Long.parseLong(parts[5]));
                d.setQuotaUtilise(Long.parseLong(parts[6]));
                d.setDateCreation(parts[7]);
                d.setActif(Boolean.parseBoolean(parts[8]));
                return d;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static String getCheminComplet(Domain domain) {
        if (domain.isRacine()) {
            return "/" + domain.getNom();
        }
        Domain parent = getDomainById(domain.getParentId());
        if (parent != null) {
            return getCheminComplet(parent) + "/" + domain.getNom();
        }
        return "/" + domain.getNom();
    }

    public static long calculerEspaceDisponible(int domainId) {
        Domain d = getDomainById(domainId);
        if (d != null) {
            return d.getQuotaMax() - d.getQuotaUtilise();
        }
        return 0;
    }
}
