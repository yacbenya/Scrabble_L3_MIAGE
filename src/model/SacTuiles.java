package model;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;

public final class SacTuiles {
    private final Deque<Tuile> pile = new ArrayDeque<>();

    public SacTuiles() {
        List<Tuile> toutes = new ArrayList<>();

        // Distribution FR classique (simplifiee mais correcte)
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

        Collections.shuffle(toutes);
        for (Tuile t : toutes) pile.addLast(t);
    }

    private static void ajouter(List<Tuile> liste, char lettre, int points, int quantite) {
        for (int i = 0; i < quantite; i++) liste.add(new Tuile(lettre, points));
    }

    public Tuile piocher() { return pile.pollFirst(); }
    public boolean estVide() { return pile.isEmpty(); }
    public int taille() { return pile.size(); }
}
