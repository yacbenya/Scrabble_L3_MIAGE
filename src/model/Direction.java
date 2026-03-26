package model;

public enum Direction {
    HORIZONTALE,
    VERTICALE;

    public Direction perpendiculaire() {
        return this == HORIZONTALE ? VERTICALE : HORIZONTALE;
    }
}
