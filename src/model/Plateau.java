package model;

import java.util.List;

public final class Plateau {
    public static final int TAILLE = 15;
    private final CasePlateau[][] cases = new CasePlateau[TAILLE][TAILLE];

    public Plateau() {
        for (int ligne = 0; ligne < TAILLE; ligne++) {
            for (int colonne = 0; colonne < TAILLE; colonne++) {
                cases[ligne][colonne] = new CasePlateau(ligne, colonne, DispositionPlateau.primeA(ligne, colonne));
            }
        }
    }

    public boolean dansBornes(int ligne, int colonne) {
        return ligne >= 0 && ligne < TAILLE && colonne >= 0 && colonne < TAILLE;
    }

    public CasePlateau getCase(int ligne, int colonne) {
        return cases[ligne][colonne];
    }

    public void appliquerCoup(Coup coup) {
        List<Pose> poses = coup.getPoses();
        for (Pose pose : poses) {
            Position position = pose.position();
            Tuile tuile = pose.tuile();
            if (tuile.estJoker()) {
                tuile = new Tuile(pose.lettreVisible(), 0);
            }
            getCase(position.ligne(), position.colonne()).poserTuile(tuile);
        }
    }

    public boolean estVideCentre() {
        return getCase(7, 7).estVide();
    }
}
