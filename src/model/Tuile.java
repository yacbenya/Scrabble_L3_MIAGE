package model;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

public final class Tuile {
    private static final AtomicInteger COMPTEUR = new AtomicInteger(1);

    private final String id;
    private final char lettre;
    private final int points;

    public Tuile(char lettre, int points) {
        this("T" + COMPTEUR.getAndIncrement(), lettre, points);
    }

    public Tuile(String id, char lettre, int points) {
        this.id = Objects.requireNonNull(id, "id");
        this.lettre = Character.toUpperCase(lettre);
        this.points = points;
    }

    public String getId() {
        return id;
    }

    public char getLettre() {
        return lettre;
    }

    public int getPoints() {
        return points;
    }

    public boolean estJoker() {
        return lettre == '?';
    }

    @Override
    public String toString() {
        return estJoker() ? "?" : String.valueOf(lettre);
    }
}
