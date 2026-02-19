package model;

public final class Joueur {
    private final String nom;
    private int score = 0;
    private final Chevalet chevalet = new Chevalet();

    public Joueur(String nom) {
        this.nom = (nom == null || nom.isBlank()) ? "Joueur" : nom.trim();
    }

    public String getNom() { return nom; }
    public int getScore() { return score; }
    public void ajouterScore(int points) { score += points; }
    public Chevalet getChevalet() { return chevalet; }
}
