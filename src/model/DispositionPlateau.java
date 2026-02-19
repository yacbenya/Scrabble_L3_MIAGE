package model;

final class DispositionPlateau {
    private DispositionPlateau() { }

    static Prime primeA(int ligne, int colonne) {
        if (ligne == 7 && colonne == 7) return Prime.DEPART;
        return Prime.AUCUNE;
    }
}
