package model;

import java.util.List;

public record HistoriqueEntry(String type, String joueur, int points, List<String> mots, String message) {

    public static HistoriqueEntry coup(String joueur, int points, List<String> mots) {
        return new HistoriqueEntry("COUP", joueur, points, List.copyOf(mots),
                joueur + " a joué pour " + points + " pts : " + String.join(", ", mots));
    }

    public static HistoriqueEntry passe(String joueur) {
        return new HistoriqueEntry("PASSE", joueur, 0, List.of(), joueur + " a passé son tour.");
    }

    public static HistoriqueEntry echange(String joueur, int nbTuiles) {
        return new HistoriqueEntry("ECHANGE", joueur, 0, List.of(),
                joueur + " a échangé " + nbTuiles + " tuile(s).");
    }

    public static HistoriqueEntry finPartie(String message) {
        return new HistoriqueEntry("FIN", "", 0, List.of(), message);
    }
}
