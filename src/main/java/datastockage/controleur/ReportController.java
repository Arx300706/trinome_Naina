package controleur;

import model.FileReport;
import model.FileReport.Reason;
import model.FileReport.Status;
import service.ReportService;

import java.util.List;
import java.util.Map;

/**
 * Contrôleur pour la gestion des signalements de fichiers.
 */
public class ReportController {

    public static boolean creerSignalement(String nomFichier, String cheminFichier, 
                                           int rapporteurId, int proprietaireId,
                                           Reason raison, String description) {
        FileReport report = new FileReport();
        report.setNomFichier(nomFichier);
        report.setCheminFichier(cheminFichier);
        report.setRapporteurId(rapporteurId);
        report.setProprietaireId(proprietaireId);
        report.setRaison(raison);
        report.setDescription(description);
        return ReportService.creerReport(report);
    }

    public static List<FileReport> listerTousSignalements() {
        return ReportService.listerTousReports();
    }

    public static List<FileReport> listerSignalementsEnAttente() {
        return ReportService.listerReportsEnAttente();
    }

    public static List<FileReport> listerSignalementsParUtilisateur(int userId) {
        return ReportService.listerReportsParUtilisateur(userId);
    }

    public static FileReport getSignalementById(int id) {
        return ReportService.getReportById(id);
    }

    public static boolean approuverSignalement(int reportId, int adminId, String commentaire) {
        return ReportService.traiterReport(reportId, adminId, Status.TRAITE, commentaire);
    }

    public static boolean rejeterSignalement(int reportId, int adminId, String commentaire) {
        return ReportService.traiterReport(reportId, adminId, Status.REJETE, commentaire);
    }

    public static boolean supprimerSignalement(int id) {
        return ReportService.supprimerReport(id);
    }

    public static int compterSignalementsParFichier(String nomFichier) {
        return ReportService.compterReportsParFichier(nomFichier);
    }

    public static Map<String, Integer> getStatistiques() {
        return ReportService.getStatistiques();
    }
}
