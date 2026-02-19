package model;

public record Pose(Position position, Tuile tuile, Character faceJoker) {

    public char lettreVisible() {
        if (!tuile.estJoker()) return tuile.getLettre();
        if (faceJoker == null) return '?';
        return Character.toUpperCase(faceJoker);
    }

    public int pointsLettre() {
        return tuile.estJoker() ? 0 : tuile.getPoints();
    }
}
