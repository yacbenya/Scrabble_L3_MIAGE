package model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class Chevalet {
    private final List<Tuile> tuiles = new ArrayList<>();

    public List<Tuile> getTuiles() {
        return Collections.unmodifiableList(tuiles);
    }

    public int taille() {
        return tuiles.size();
    }

    public Tuile prendreA(int index) {
        return tuiles.remove(index);
    }

    public void ajouter(Tuile tuile) {
        tuiles.add(tuile);
    }

    public int pointsRestants() {
        return tuiles.stream().mapToInt(Tuile::getPoints).sum();
    }
}
