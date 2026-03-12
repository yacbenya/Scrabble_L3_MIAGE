package controller;

import model.*;
import service.ServiceDictionnaire;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ControleurPartie {
    private final Partie partie;

    private final Map<Position, Tuile> placementsEnAttente = new LinkedHashMap<>();
    private final Map<Position, Character> facesJokersEnAttente = new HashMap<>();

    private int indexChevaletSelectionne = -1;

    public ControleurPartie(ServiceDictionnaire dictionnaire) {
        this.partie = new Partie(dictionnaire);
    }

    public void nouvellePartie(List<String> noms) {
        partie.demarrer(noms);
        placementsEnAttente.clear();
        facesJokersEnAttente.clear();
        indexChevaletSelectionne = -1;
    }

    public Partie getPartie() {
        return partie;
    }

    public Map<Position, Tuile> getPlacementsEnAttente() {
        return Collections.unmodifiableMap(placementsEnAttente);
    }

    public int getIndexChevaletSelectionne() {
        return indexChevaletSelectionne;
    }

    public void selectionnerIndexChevalet(int index) {
        this.indexChevaletSelectionne = index;
    }

    public Tuile getTuileChevalet(int index) {
        Joueur joueur = partie.getJoueurCourant();
        if (index < 0 || index >= joueur.getChevalet().taille()) return null;
        return joueur.getChevalet().getTuiles().get(index);
    }

    public void placerTuileSelectionnee(int ligne, int colonne, Character faceJoker) {
        if (indexChevaletSelectionne < 0) return;

        Position pos = new Position(ligne, colonne);
        if (placementsEnAttente.containsKey(pos)) return;
        if (!partie.getPlateau().getCase(ligne, colonne).estVide()) return;

        Joueur joueur = partie.getJoueurCourant();
        if (indexChevaletSelectionne >= joueur.getChevalet().taille()) return;

        Tuile tuile = joueur.getChevalet().prendreA(indexChevaletSelectionne);
        placementsEnAttente.put(pos, tuile);

        if (tuile.estJoker()) {
            facesJokersEnAttente.put(pos, faceJoker);
        }

        indexChevaletSelectionne = -1;
    }

    public void retirerPlacementEnAttente(int ligne, int colonne) {
        Position pos = new Position(ligne, colonne);
        Tuile tuile = placementsEnAttente.remove(pos);
        facesJokersEnAttente.remove(pos);

        if (tuile != null) {
            partie.getJoueurCourant().getChevalet().ajouter(tuile);
        }
    }

    public void annulerCoup() {
        Joueur joueur = partie.getJoueurCourant();
        for (Tuile tuile : placementsEnAttente.values()) joueur.getChevalet().ajouter(tuile);

        placementsEnAttente.clear();
        facesJokersEnAttente.clear();
        indexChevaletSelectionne = -1;
    }

    public ResultatTour validerCoup() {
        if (placementsEnAttente.isEmpty()) throw new IllegalArgumentException("Aucune tuile posee.");

        List<Pose> poses = new ArrayList<>();
        for (var entree : placementsEnAttente.entrySet()) {
            Position pos = entree.getKey();
            Tuile tuile = entree.getValue();
            Character face = tuile.estJoker() ? facesJokersEnAttente.get(pos) : null;
            poses.add(new Pose(pos, tuile, face));
        }

        
        Coup coup = new Coup(poses, Direction.HORIZONTALE); // modifier et detecter la direction

        try {
            ResultatTour resultat = partie.jouerCoup(coup);
            placementsEnAttente.clear();
            facesJokersEnAttente.clear();
            indexChevaletSelectionne = -1;
            return resultat;
        } catch (RuntimeException e) {
            Joueur joueur = partie.getJoueurCourant();
            for (Tuile tuile : placementsEnAttente.values()) joueur.getChevalet().ajouter(tuile);

            placementsEnAttente.clear();
            facesJokersEnAttente.clear();
            indexChevaletSelectionne = -1;

            throw e;
        }
    }

    public String passerTour() {
        annulerCoup();
        partie.passer();
        return partie.estTerminee() ? "Fin: trop de passes consecutifs." : "Tour passe.";
    }

}
