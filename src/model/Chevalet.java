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

    public void ajouter(Tuile tuile) {
        tuiles.add(tuile);
    }

    public Tuile prendreA(int index) {
        return tuiles.remove(index);
    }

    public Tuile retirerParId(String id) {
        for (int i = 0; i < tuiles.size(); i++) {
            if (tuiles.get(i).getId().equals(id)) {
                return tuiles.remove(i);
            }
        }
        return null;
    }

    public Tuile trouverParId(String id) {
        for (Tuile tuile : tuiles) {
            if (tuile.getId().equals(id)) return tuile;
        }
        return null;
    }

    public int pointsRestants() {
        return tuiles.stream().mapToInt(Tuile::getPoints).sum();
    }
}
