package model;

public final class CasePlateau {
    private final int ligne;
    private final int colonne;
    private final Prime prime;
    private Tuile tuile;

    public CasePlateau(int ligne, int colonne, Prime prime) {
        this.ligne = ligne;
        this.colonne = colonne;
        this.prime = (prime == null) ? Prime.AUCUNE : prime;
    }

    public boolean estVide() {
        return tuile == null;
    }

    public Prime getPrime() {
        return prime;
    }

    public Tuile getTuile() {
        return tuile;
    }

    public void poserTuile(Tuile tuile) {
        this.tuile = tuile;
    }

    public int getLigne() {
        return ligne;
    }

    public int getColonne() {
        return colonne;
    }
}
