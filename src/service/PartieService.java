package service;

import model.Coup;
import model.HistoriqueEntry;
import model.Joueur;
import model.Partie;
import model.Pose;
import model.ResultatTour;
import model.SacTuiles;
import model.Tuile;

import java.util.ArrayList;
import java.util.List;

public final class PartieService {
    private final ValidateurCoup validateur;
    private final ServiceScore serviceScore;

    public PartieService(ServiceDictionnaire dictionnaire) {
        ServiceDictionnaire serviceDictionnaire = dictionnaire == null ? new ServiceDictionnaireToujoursValide() : dictionnaire;
        this.validateur = new ValidateurCoup(serviceDictionnaire);
        this.serviceScore = new ServiceScore();
    }

    public Partie demarrer(List<String> noms) {
        if (noms == null || noms.size() < 2 || noms.size() > 4) {
            throw new IllegalArgumentException("Il faut entre 2 et 4 joueurs.");
        }

        List<Joueur> joueurs = new ArrayList<>();
        for (String nom : noms) {
            joueurs.add(new Joueur(nom));
        }

        Partie partie = new Partie();
        partie.reinitialiserJoueurs(joueurs);
        for (Joueur joueur : partie.getJoueurs()) {
            completerChevalet(partie.getSac(), joueur);
        }
        partie.setDernierMessage("Partie créée. À " + partie.getJoueurCourant().getNom() + " de jouer.");
        return partie;
    }

    public ResultatTour jouerCoup(Partie partie, Coup coup) {
        verifierPartie(partie);
        if (partie.estTerminee()) throw new IllegalStateException("La partie est terminée.");

        ResultatValidation resultatValidation = validateur.valider(partie.getPlateau(), coup, partie.estPremierCoup());
        int points = serviceScore.calculer(partie.getPlateau(), coup, resultatValidation.mots());

        partie.getPlateau().appliquerCoup(coup);

        Joueur joueur = partie.getJoueurCourant();
        joueur.ajouterScore(points);
        completerChevalet(partie.getSac(), joueur);
        partie.setPassesConsecutifs(0);

        boolean finParChevaletVide = partie.getSac().estVide() && joueur.getChevalet().taille() == 0;
        if (finParChevaletVide) {
            appliquerFinDePartieChevaletVide(partie, joueur);
            partie.setTerminee(true);
        }

        List<String> mots = resultatValidation.motsNormalises();
        String message = finParChevaletVide
                ? "Fin : un joueur a vidé son chevalet et le sac est vide."
                : "Coup validé pour " + points + " points.";

        partie.ajouterHistorique(HistoriqueEntry.coup(joueur.getNom(), points, mots));
        if (finParChevaletVide) {
            partie.ajouterHistorique(HistoriqueEntry.finPartie(message));
        }

        partie.setDerniersPoints(points);
        partie.setDerniersMots(mots);
        partie.setDernierMessage(message);

        if (!partie.estTerminee()) {
            partie.passerAuJoueurSuivant();
        }

        return new ResultatTour(points, mots, message, partie.estTerminee());
    }

    public String passer(Partie partie) {
        verifierPartie(partie);
        if (partie.estTerminee()) return "La partie est déjà terminée.";

        String nomJoueur = partie.getJoueurCourant().getNom();
        int nouvellesPasses = partie.getPassesConsecutifs() + 1;
        partie.setPassesConsecutifs(nouvellesPasses);
        partie.setDerniersPoints(0);
        partie.setDerniersMots(List.of());

        partie.ajouterHistorique(HistoriqueEntry.passe(nomJoueur));

        int seuil = partie.getJoueurs().size() * 2;
        if (nouvellesPasses >= seuil) {
            appliquerFinDePartieParPasses(partie);
            partie.setTerminee(true);
            partie.setDernierMessage("Fin : trop de passes consécutifs.");
            partie.ajouterHistorique(HistoriqueEntry.finPartie(partie.getDernierMessage()));
            return partie.getDernierMessage();
        }

        partie.passerAuJoueurSuivant();
        partie.setDernierMessage("Tour passé.");
        return partie.getDernierMessage();
    }

    public String echanger(Partie partie, List<String> idsTuiles) {
        verifierPartie(partie);
        if (partie.estTerminee()) throw new IllegalStateException("La partie est terminée.");
        if (idsTuiles == null || idsTuiles.isEmpty()) throw new IllegalArgumentException("Choisis au moins une tuile à échanger.");
        if (partie.getSac().taille() < 7) throw new IllegalStateException("Le sac doit contenir au moins 7 tuiles pour échanger.");
        if (partie.getSac().taille() < idsTuiles.size()) throw new IllegalStateException("Pas assez de tuiles dans le sac pour cet échange.");

        Joueur joueur = partie.getJoueurCourant();
        List<Tuile> retirees = new ArrayList<>();
        List<Tuile> rackSnapshot = new ArrayList<>(joueur.getChevalet().getTuiles());

        try {
            for (String id : idsTuiles) {
                Tuile tuile = joueur.getChevalet().retirerParId(id);
                if (tuile == null) throw new IllegalArgumentException("Tuile introuvable dans le chevalet : " + id);
                retirees.add(tuile);
            }
        } catch (RuntimeException e) {
            for (Tuile tuile : retirees) {
                joueur.getChevalet().ajouter(tuile);
            }
            throw e;
        }

        List<Tuile> nouvelles = partie.getSac().piocher(idsTuiles.size());
        for (Tuile tuile : nouvelles) {
            joueur.getChevalet().ajouter(tuile);
        }
        partie.getSac().remettre(retirees);

        partie.ajouterHistorique(HistoriqueEntry.echange(joueur.getNom(), idsTuiles.size()));

        int nouvellesPasses = partie.getPassesConsecutifs() + 1;
        partie.setPassesConsecutifs(nouvellesPasses);
        partie.setDerniersPoints(0);
        partie.setDerniersMots(List.of());

        int seuil = partie.getJoueurs().size() * 2;
        if (nouvellesPasses >= seuil) {
            appliquerFinDePartieParPasses(partie);
            partie.setTerminee(true);
            partie.setDernierMessage("Fin : trop de passes consécutifs après échange.");
            partie.ajouterHistorique(HistoriqueEntry.finPartie(partie.getDernierMessage()));
            return partie.getDernierMessage();
        }

        partie.passerAuJoueurSuivant();
        partie.setDernierMessage("Échange effectué.");
        return partie.getDernierMessage();
    }

    private static void verifierPartie(Partie partie) {
        if (partie == null || partie.getJoueurs().isEmpty()) {
            throw new IllegalStateException("Aucune partie en cours.");
        }
    }

    private static void completerChevalet(SacTuiles sac, Joueur joueur) {
        while (joueur.getChevalet().taille() < 7 && !sac.estVide()) {
            Tuile tuile = sac.piocher();
            if (tuile == null) break;
            joueur.getChevalet().ajouter(tuile);
        }
    }

    private static void appliquerFinDePartieChevaletVide(Partie partie, Joueur gagnantPotentiel) {
        int sommeRestante = 0;
        for (Joueur joueur : partie.getJoueurs()) {
            int reste = joueur.getChevalet().pointsRestants();
            joueur.ajouterScore(-reste);
            sommeRestante += reste;
        }
        gagnantPotentiel.ajouterScore(sommeRestante);
    }

    private static void appliquerFinDePartieParPasses(Partie partie) {
        for (Joueur joueur : partie.getJoueurs()) {
            int reste = joueur.getChevalet().pointsRestants();
            joueur.ajouterScore(-reste);
        }
    }
}
