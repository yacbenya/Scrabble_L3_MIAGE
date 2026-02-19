package model;

public final class Tuile {
    private final char lettre;   
    private final int points;

    public Tuile(char lettre, int points) {
        this.lettre = Character.toUpperCase(lettre);
        this.points = points;
    }

    public char getLettre() { return lettre; }
    public int getPoints() { return points; }
    public boolean estJoker() { return lettre == '?'; }

    @Override
    public String toString() {
        return estJoker() ? "?" : String.valueOf(lettre);
    }
}
