package model;

import java.util.HashSet;
import java.util.Set;

final class DispositionPlateau {
    private DispositionPlateau() { }

    private static final Set<Long> MOT_TRIPLE = new HashSet<>();
    private static final Set<Long> MOT_DOUBLE = new HashSet<>();
    private static final Set<Long> LETTRE_TRIPLE = new HashSet<>();
    private static final Set<Long> LETTRE_DOUBLE = new HashSet<>();

    static {
        ajouterSym(MOT_TRIPLE, 0, 0);
        ajouterSym(MOT_TRIPLE, 0, 7);
        ajouterSym(MOT_TRIPLE, 0, 14);
        ajouterSym(MOT_TRIPLE, 7, 0);
        ajouterSym(MOT_TRIPLE, 7, 14);
        ajouterSym(MOT_TRIPLE, 14, 0);
        ajouterSym(MOT_TRIPLE, 14, 7);
        ajouterSym(MOT_TRIPLE, 14, 14);

        int[][] md = {
                {1, 1}, {2, 2}, {3, 3}, {4, 4},
                {1, 13}, {2, 12}, {3, 11}, {4, 10},
                {10, 4}, {11, 3}, {12, 2}, {13, 1},
                {10, 10}, {11, 11}, {12, 12}, {13, 13}
        };
        for (int[] p : md) ajouterSym(MOT_DOUBLE, p[0], p[1]);

        int[][] lt = {
                {1, 5}, {1, 9}, {5, 1}, {5, 5}, {5, 9}, {5, 13},
                {9, 1}, {9, 5}, {9, 9}, {9, 13}, {13, 5}, {13, 9}
        };
        for (int[] p : lt) ajouterSym(LETTRE_TRIPLE, p[0], p[1]);

        int[][] ld = {
                {0, 3}, {0, 11}, {2, 6}, {2, 8}, {3, 0}, {3, 7}, {3, 14},
                {6, 2}, {6, 6}, {6, 8}, {6, 12}, {7, 3}, {7, 11},
                {8, 2}, {8, 6}, {8, 8}, {8, 12}, {11, 0}, {11, 7}, {11, 14},
                {12, 6}, {12, 8}, {14, 3}, {14, 11}
        };
        for (int[] p : ld) ajouterSym(LETTRE_DOUBLE, p[0], p[1]);
    }

    private static void ajouterSym(Set<Long> ensemble, int ligne, int colonne) {
        int[] ls = {ligne, 14 - ligne};
        int[] cs = {colonne, 14 - colonne};

        for (int l : ls) for (int c : cs) ensemble.add(cle(l, c));
        for (int l : ls) for (int c : cs) ensemble.add(cle(c, l));
    }

    private static long cle(int ligne, int colonne) {
        return (((long) ligne) << 32) ^ (colonne & 0xffffffffL);
    }

    static Prime primeA(int ligne, int colonne) {
        if (ligne == 7 && colonne == 7) return Prime.DEPART;

        long k = cle(ligne, colonne);
        if (MOT_TRIPLE.contains(k)) return Prime.MOT_TRIPLE;
        if (MOT_DOUBLE.contains(k)) return Prime.MOT_DOUBLE;
        if (LETTRE_TRIPLE.contains(k)) return Prime.LETTRE_TRIPLE;
        if (LETTRE_DOUBLE.contains(k)) return Prime.LETTRE_DOUBLE;

        return Prime.AUCUNE;
    }
}
