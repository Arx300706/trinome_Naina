package controleur;

import model.Domain;
import service.DomainService;

import java.util.List;

/**
 * Contrôleur pour la gestion des domaines.
 */
public class DomainController {

    public static boolean creerDomaine(Domain domain) {
        return DomainService.creerDomaine(domain);
    }

    public static boolean creerSousDomaine(String nom, String description, int parentId, int proprietaireId) {
        Domain domain = new Domain();
        domain.setNom(nom);
        domain.setDescription(description);
        domain.setParentId(parentId);
        domain.setProprietaireId(proprietaireId);
        return DomainService.creerDomaine(domain);
    }

    public static List<Domain> listerTousDomaines() {
        return DomainService.listerTousDomaines();
    }

    public static List<Domain> listerDomainesRacine() {
        return DomainService.listerDomainesRacine();
    }

    public static List<Domain> listerSousDomaines(int parentId) {
        return DomainService.listerSousDomaines(parentId);
    }

    public static Domain getDomainById(int id) {
        return DomainService.getDomainById(id);
    }

    public static boolean supprimerDomaine(int id) {
        return DomainService.supprimerDomaine(id);
    }

    public static boolean modifierDomaine(Domain domain) {
        return DomainService.modifierDomaine(domain);
    }

    public static String getCheminComplet(Domain domain) {
        return DomainService.getCheminComplet(domain);
    }

    public static long getEspaceDisponible(int domainId) {
        return DomainService.calculerEspaceDisponible(domainId);
    }
}
