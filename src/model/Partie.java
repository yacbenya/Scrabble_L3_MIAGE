package model;

import service.*;

import java.util.ArrayList;
import java.util.List;

public final class Partie {
    private final Plateau plateau = new Plateau();
    private final SacTuiles sac = new SacTuiles();
    private final List<Joueur> joueurs = new ArrayList<>();

    private int indexJoueurCourant = 0;
    private int passesConsecutifs = 0;
    private boolean terminee = false;

    private final ValidateurCoup validateur;
    private final ServiceScore scoreur;

    public Partie(ServiceDictionnaire dictionnaire) {
        this.validateur = new ValidateurCoup(dictionnaire);
        this.scoreur = new ServiceScore();
    }

    public void demarrer(List<String> noms) {
        joueurs.clear();
        for (String nom : noms) joueurs.add(new Joueur(nom));

        if (joueurs.size() < 2 || joueurs.size() > 4) {
            throw new IllegalArgumentException("Il faut entre 2 et 4 joueurs.");
        }

        for (Joueur joueur : joueurs) completerChevalet(joueur);

        indexJoueurCourant = 0;
        passesConsecutifs = 0;
        terminee = false;
    }

    public Plateau getPlateau() { return plateau; }
    public List<Joueur> getJoueurs() { return joueurs; }
    public Joueur getJoueurCourant() { return joueurs.get(indexJoueurCourant); }

    public boolean estPremierCoup() {
        return plateau.getCase(7, 7).estVide();
    }

    public boolean estTerminee() {
        return terminee;
    }

    public ResultatTour jouerCoup(Coup coup) {
        if (terminee) throw new IllegalStateException("La partie est terminee.");

        ResultatValidation resultatValidation = validateur.valider(plateau, coup, estPremierCoup());
        int points = scoreur.calculer(plateau, coup, resultatValidation.mots());

        plateau.appliquerCoup(coup);

        Joueur joueur = getJoueurCourant();
        joueur.ajouterScore(points);
        completerChevalet(joueur);

        passesConsecutifs = 0;

        boolean finParChevaletVide = sac.estVide() && joueur.getChevalet().taille() == 0;
        if (finParChevaletVide) {
            appliquerFinDePartieChevaletVide(joueur);
            terminee = true;
        }

        List<String> mots = resultatValidation.mots().stream().map(InfoMot::texte).toList();
        String message = finParChevaletVide ? "Fin: un joueur a vide son chevalet et le sac est vide." : "Coup valide.";

        if (!terminee) passerAuJoueurSuivant();
        return new ResultatTour(points, mots, message, terminee);
    }

    public void passer() {
        if (terminee) return;

        passesConsecutifs++;

        // Regle simple: si tout le monde passe 2 tours de suite, on arrete.
        int seuil = joueurs.size() * 2;
        if (passesConsecutifs >= seuil) {
            appliquerFinDePartieParPasses();
            terminee = true;
            return;
        }

        passerAuJoueurSuivant();
    }

    private void passerAuJoueurSuivant() {
        indexJoueurCourant = (indexJoueurCourant + 1) % joueurs.size();
    }

    private void completerChevalet(Joueur joueur) {
        while (joueur.getChevalet().taille() < 7 && !sac.estVide()) {
            Tuile tuile = sac.piocher();
            if (tuile == null) break;
            joueur.getChevalet().ajouter(tuile);
        }
    }

    private void appliquerFinDePartieChevaletVide(Joueur gagnantPotentiel) {
        int sommeRestante = 0;

        for (Joueur j : joueurs) {
            int reste = j.getChevalet().pointsRestants();
            j.ajouterScore(-reste);
            sommeRestante += reste;
        }

        gagnantPotentiel.ajouterScore(sommeRestante);
    }

    private void appliquerFinDePartieParPasses() {
        for (Joueur j : joueurs) {
            int reste = j.getChevalet().pointsRestants();
            j.ajouterScore(-reste);
        }
    }
}
