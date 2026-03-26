package model;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Random;

public final class SacTuiles {
    private final Deque<Tuile> pile = new ArrayDeque<>();
    private final Random random = new Random();

    public SacTuiles() {
        List<Tuile> toutes = new ArrayList<>();

        ajouter(toutes, 'A', 1, 9);
        ajouter(toutes, 'B', 3, 2);
        ajouter(toutes, 'C', 3, 2);
        ajouter(toutes, 'D', 2, 3);
        ajouter(toutes, 'E', 1, 15);
        ajouter(toutes, 'F', 4, 2);
        ajouter(toutes, 'G', 2, 2);
        ajouter(toutes, 'H', 4, 2);
        ajouter(toutes, 'I', 1, 8);
        ajouter(toutes, 'J', 8, 1);
        ajouter(toutes, 'K', 10, 1);
        ajouter(toutes, 'L', 1, 5);
        ajouter(toutes, 'M', 2, 3);
        ajouter(toutes, 'N', 1, 6);
        ajouter(toutes, 'O', 1, 6);
        ajouter(toutes, 'P', 3, 2);
        ajouter(toutes, 'Q', 8, 1);
        ajouter(toutes, 'R', 1, 6);
        ajouter(toutes, 'S', 1, 6);
        ajouter(toutes, 'T', 1, 6);
        ajouter(toutes, 'U', 1, 6);
        ajouter(toutes, 'V', 4, 2);
        ajouter(toutes, 'W', 10, 1);
        ajouter(toutes, 'X', 10, 1);
        ajouter(toutes, 'Y', 10, 1);
        ajouter(toutes, 'Z', 10, 1);
        ajouter(toutes, '?', 0, 2);

        Collections.shuffle(toutes, random);
        for (Tuile tuile : toutes) pile.addLast(tuile);
    }

    private static void ajouter(List<Tuile> liste, char lettre, int points, int quantite) {
        for (int i = 0; i < quantite; i++) {
            liste.add(new Tuile(lettre, points));
        }
    }

    public Tuile piocher() {
        return pile.pollFirst();
    }

    public List<Tuile> piocher(int quantite) {
        List<Tuile> resultat = new ArrayList<>();
        for (int i = 0; i < quantite; i++) {
            Tuile tuile = piocher();
            if (tuile == null) break;
            resultat.add(tuile);
        }
        return resultat;
    }

    public void remettre(List<Tuile> tuiles) {
        if (tuiles == null || tuiles.isEmpty()) return;
        List<Tuile> contenu = new ArrayList<>(pile);
        contenu.addAll(tuiles);
        Collections.shuffle(contenu, random);
        pile.clear();
        pile.addAll(contenu);
    }

    public boolean estVide() {
        return pile.isEmpty();
    }

    public int taille() {
        return pile.size();
    }
}
