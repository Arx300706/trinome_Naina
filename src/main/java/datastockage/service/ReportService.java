package service;

import model.FileReport;
import model.FileReport.Reason;
import model.FileReport.Status;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Service de gestion des signalements de fichiers.
 */
public class ReportService {

    private static final String FICHIER_REPORTS = "src/main/resources/data/signalements.txt";
    private static int nextId = 1;

    static {
        List<FileReport> reports = listerTousReports();
        for (FileReport r : reports) {
            if (r.getId() >= nextId) {
                nextId = r.getId() + 1;
            }
        }
    }

    public static boolean creerReport(FileReport report) {
        try {
            report.setId(nextId++);
            report.setDateSignalement(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
            
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(FICHIER_REPORTS, true))) {
                String ligne = formatReportLigne(report);
                writer.write(ligne);
                writer.newLine();
            }
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static List<FileReport> listerTousReports() {
        List<FileReport> reports = new ArrayList<>();
        File fichier = new File(FICHIER_REPORTS);
        if (!fichier.exists()) {
            return reports;
        }
        
        try (BufferedReader reader = new BufferedReader(new FileReader(FICHIER_REPORTS))) {
            String ligne;
            while ((ligne = reader.readLine()) != null) {
                FileReport r = parseReportLigne(ligne);
                if (r != null) {
                    reports.add(r);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return reports;
    }

    public static List<FileReport> listerReportsEnAttente() {
        List<FileReport> enAttente = new ArrayList<>();
        for (FileReport r : listerTousReports()) {
            if (r.getStatus() == Status.EN_ATTENTE) {
                enAttente.add(r);
            }
        }
        return enAttente;
    }

    public static List<FileReport> listerReportsParUtilisateur(int userId) {
        List<FileReport> userReports = new ArrayList<>();
        for (FileReport r : listerTousReports()) {
            if (r.getProprietaireId() == userId) {
                userReports.add(r);
            }
        }
        return userReports;
    }

    public static FileReport getReportById(int id) {
        for (FileReport r : listerTousReports()) {
            if (r.getId() == id) {
                return r;
            }
        }
        return null;
    }

    public static boolean traiterReport(int reportId, int adminId, Status newStatus, String commentaire) {
        FileReport report = getReportById(reportId);
        if (report == null) return false;

        report.setStatus(newStatus);
        report.setAdminTraitantId(adminId);
        report.setCommentaireAdmin(commentaire);
        report.setDateTraitement(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));

        return modifierReport(report);
    }

    public static boolean modifierReport(FileReport report) {
        List<FileReport> reports = listerTousReports();
        
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(FICHIER_REPORTS))) {
            for (FileReport r : reports) {
                if (r.getId() == report.getId()) {
                    writer.write(formatReportLigne(report));
                } else {
                    writer.write(formatReportLigne(r));
                }
                writer.newLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public static boolean supprimerReport(int id) {
        List<FileReport> reports = listerTousReports();
        boolean supprime = false;
        
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(FICHIER_REPORTS))) {
            for (FileReport r : reports) {
                if (r.getId() == id) {
                    supprime = true;
                    continue;
                }
                writer.write(formatReportLigne(r));
                writer.newLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return supprime;
    }

    public static int compterReportsParFichier(String nomFichier) {
        int count = 0;
        for (FileReport r : listerTousReports()) {
            if (r.getNomFichier().equals(nomFichier)) {
                count++;
            }
        }
        return count;
    }

    private static String formatReportLigne(FileReport r) {
        return r.getId() + ";" + 
               r.getNomFichier() + ";" + 
               r.getCheminFichier() + ";" + 
               r.getRapporteurId() + ";" + 
               r.getProprietaireId() + ";" + 
               r.getRaison().name() + ";" + 
               escapeString(r.getDescription()) + ";" + 
               r.getStatus().name() + ";" + 
               r.getDateSignalement() + ";" + 
               (r.getDateTraitement() != null ? r.getDateTraitement() : "") + ";" + 
               r.getAdminTraitantId() + ";" + 
               escapeString(r.getCommentaireAdmin() != null ? r.getCommentaireAdmin() : "");
    }

    private static FileReport parseReportLigne(String ligne) {
        try {
            String[] parts = ligne.split(";");
            if (parts.length >= 9) {
                FileReport r = new FileReport();
                r.setId(Integer.parseInt(parts[0]));
                r.setNomFichier(parts[1]);
                r.setCheminFichier(parts[2]);
                r.setRapporteurId(Integer.parseInt(parts[3]));
                r.setProprietaireId(Integer.parseInt(parts[4]));
                r.setRaison(Reason.valueOf(parts[5]));
                r.setDescription(unescapeString(parts[6]));
                r.setStatus(Status.valueOf(parts[7]));
                r.setDateSignalement(parts[8]);
                if (parts.length > 9 && !parts[9].isEmpty()) {
                    r.setDateTraitement(parts[9]);
                }
                if (parts.length > 10 && !parts[10].isEmpty()) {
                    r.setAdminTraitantId(Integer.parseInt(parts[10]));
                }
                if (parts.length > 11) {
                    r.setCommentaireAdmin(unescapeString(parts[11]));
                }
                return r;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private static String escapeString(String s) {
        if (s == null) return "";
        return s.replace(";", "\\;").replace("\n", "\\n");
    }

    private static String unescapeString(String s) {
        if (s == null) return "";
        return s.replace("\\;", ";").replace("\\n", "\n");
    }

    public static Map<String, Integer> getStatistiques() {
        Map<String, Integer> stats = new HashMap<>();
        List<FileReport> reports = listerTousReports();
        
        int enAttente = 0, traites = 0, rejetes = 0;
        Map<Reason, Integer> parRaison = new HashMap<>();
        
        for (FileReport r : reports) {
            switch (r.getStatus()) {
                case EN_ATTENTE: enAttente++; break;
                case TRAITE: traites++; break;
                case REJETE: rejetes++; break;
            }
            parRaison.merge(r.getRaison(), 1, Integer::sum);
        }
        
        stats.put("total", reports.size());
        stats.put("en_attente", enAttente);
        stats.put("traites", traites);
        stats.put("rejetes", rejetes);
        
        for (Reason reason : Reason.values()) {
            stats.put("raison_" + reason.name(), parRaison.getOrDefault(reason, 0));
        }
        
        return stats;
    }
}
