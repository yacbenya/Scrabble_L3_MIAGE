package model;

import java.util.List;

public final class Plateau {
    public static final int TAILLE = 15;
    private final CasePlateau[][] cases = new CasePlateau[TAILLE][TAILLE];

    public Plateau() {
        for (int l = 0; l < TAILLE; l++) {
            for (int c = 0; c < TAILLE; c++) {
                cases[l][c] = new CasePlateau(l, c, DispositionPlateau.primeA(l, c));
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
        for (Pose p : poses) {
            Position pos = p.position();
            getCase(pos.ligne(), pos.colonne()).poserTuile(p.tuile());
        }
    }

    public boolean estVideCentre() {
        return getCase(7, 7).estVide();
    }
}
