package service;

import model.*;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class ValidateurCoup {
    private final ServiceDictionnaire dictionnaire;

    public ValidateurCoup(ServiceDictionnaire dictionnaire) {
        this.dictionnaire = dictionnaire;
    }

    public ResultatValidation valider(Plateau plateau, Coup coup, boolean premierCoup) {
        if (coup == null || coup.getPoses() == null || coup.getPoses().isEmpty()) {
            throw new IllegalArgumentException("Aucune tuile posee.");
        }

        List<Pose> poses = coup.getPoses();
        Map<Position, Pose> nouvellesPoses = new HashMap<>();
        for (Pose p : poses) {
            Position pos = p.position();
            int l = pos.ligne();
            int c = pos.colonne();

            if (!plateau.dansBornes(l, c)) throw new IllegalArgumentException("Hors plateau.");
            if (!plateau.getCase(l, c).estVide()) throw new IllegalArgumentException("Case deja occupee.");
            if (nouvellesPoses.put(pos, p) != null) throw new IllegalArgumentException("Deux tuiles sur la meme case.");

            if (p.tuile().estJoker()) {
                Character face = p.faceJoker();
                if (face == null || !Character.isLetter(face)) throw new IllegalArgumentException("Joker: choisis une lettre A-Z.");
            }
        }

        Direction direction = deduireDirection(plateau, poses, coup.getDirection());
        if (!posesAlignees(poses, direction)) throw new IllegalArgumentException("Les tuiles doivent etre alignees.");
        verifierContiguite(plateau, poses, direction, nouvellesPoses);

        if (premierCoup) {
            if (!nouvellesPoses.containsKey(new Position(7, 7))) {
                throw new IllegalArgumentException("Premier coup: ca doit passer par le centre.");
            }
        } else {
            if (!toucheUneTuileExistante(plateau, nouvellesPoses.keySet())) {
                throw new IllegalArgumentException("Le coup doit toucher une tuile deja sur le plateau.");
            }
        }

        Pose ancre = poses.get(0);
        InfoMot motPrincipal = construireMot(plateau, nouvellesPoses, ancre.position(), direction);

        String nettoye = normaliserMot(motPrincipal.texte());
        if (nettoye.length() < 2) throw new IllegalArgumentException("Mot trop court.");
        if (dictionnaire != null && !dictionnaire.estValide(nettoye)) throw new IllegalArgumentException("Mot invalide: " + nettoye);

        InfoMot motOk = new InfoMot(nettoye, motPrincipal.positions());
        return new ResultatValidation(List.of(motOk));
    }

    private static Direction deduireDirection(Plateau plateau, List<Pose> poses, Direction directionDemandee) {
        if (poses.size() >= 2) {
            boolean memeLigne = poses.stream().map(p -> p.position().ligne()).distinct().count() == 1;
            boolean memeColonne = poses.stream().map(p -> p.position().colonne()).distinct().count() == 1;
            if (memeLigne && !memeColonne) return Direction.HORIZONTALE;
            if (memeColonne && !memeLigne) return Direction.VERTICALE;
            throw new IllegalArgumentException("Les tuiles doivent etre sur une meme ligne ou colonne.");
        }

        Pose pose = poses.get(0);
        int l = pose.position().ligne();
        int c = pose.position().colonne();

        boolean aGaucheOuDroite = aUneTuile(plateau, l, c - 1) || aUneTuile(plateau, l, c + 1);
        boolean enHautOuBas = aUneTuile(plateau, l - 1, c) || aUneTuile(plateau, l + 1, c);

        if (aGaucheOuDroite && !enHautOuBas) return Direction.HORIZONTALE;
        if (enHautOuBas && !aGaucheOuDroite) return Direction.VERTICALE;

        return (directionDemandee != null) ? directionDemandee : Direction.HORIZONTALE;
    }

    private static boolean aUneTuile(Plateau plateau, int ligne, int colonne) {
        return plateau.dansBornes(ligne, colonne) && !plateau.getCase(ligne, colonne).estVide();
    }

    private static boolean posesAlignees(List<Pose> poses, Direction direction) {
        if (poses.size() < 2) return true;

        if (direction == Direction.HORIZONTALE) {
            int ligne = poses.get(0).position().ligne();
            return poses.stream().allMatch(p -> p.position().ligne() == ligne);
        } else {
            int colonne = poses.get(0).position().colonne();
            return poses.stream().allMatch(p -> p.position().colonne() == colonne);
        }
    }

    private static void verifierContiguite(Plateau plateau, List<Pose> poses, Direction direction, Map<Position, Pose> nouvellesPoses) {
        if (poses.size() < 2) return;

        int fixe = (direction == Direction.HORIZONTALE) ? poses.get(0).position().ligne() : poses.get(0).position().colonne();
        int min = Integer.MAX_VALUE;
        int max = Integer.MIN_VALUE;

        for (Pose p : poses) {
            int v = (direction == Direction.HORIZONTALE) ? p.position().colonne() : p.position().ligne();
            min = Math.min(min, v);
            max = Math.max(max, v);
        }

        for (int v = min; v <= max; v++) {
            int l = (direction == Direction.HORIZONTALE) ? fixe : v;
            int c = (direction == Direction.HORIZONTALE) ? v : fixe;

            Position pos = new Position(l, c);
            boolean ok = nouvellesPoses.containsKey(pos) || !plateau.getCase(l, c).estVide();
            if (!ok) throw new IllegalArgumentException("Mot non contigu.");
        }
    }

    private static boolean toucheUneTuileExistante(Plateau plateau, Set<Position> nouvellesPositions) {
        for (Position p : nouvellesPositions) {
            int l = p.ligne();
            int c = p.colonne();
            if (aUneTuile(plateau, l - 1, c) || aUneTuile(plateau, l + 1, c) || aUneTuile(plateau, l, c - 1) || aUneTuile(plateau, l, c + 1)) {
                return true;
            }
        }
        return false;
    }

    private static InfoMot construireMot(Plateau plateau, Map<Position, Pose> nouvellesPoses, Position ancre, Direction direction) {
        int dl = (direction == Direction.VERTICALE) ? 1 : 0;
        int dc = (direction == Direction.HORIZONTALE) ? 1 : 0;

        int l = ancre.ligne();
        int c = ancre.colonne();

        while (plateau.dansBornes(l - dl, c - dc) && lettreA(plateau, nouvellesPoses, l - dl, c - dc) != 0) {
            l -= dl;
            c -= dc;
        }

        StringBuilder sb = new StringBuilder();
        List<Position> positions = new ArrayList<>();

        while (plateau.dansBornes(l, c) && lettreA(plateau, nouvellesPoses, l, c) != 0) {
            sb.append(lettreA(plateau, nouvellesPoses, l, c));
            positions.add(new Position(l, c));
            l += dl;
            c += dc;
        }

        return new InfoMot(sb.toString(), positions);
    }

    private static char lettreA(Plateau plateau, Map<Position, Pose> nouvellesPoses, int ligne, int colonne) {
        Pose pose = nouvellesPoses.get(new Position(ligne, colonne));
        if (pose != null) return pose.lettreVisible();

        Tuile tuile = plateau.getCase(ligne, colonne).getTuile();
        if (tuile == null) return 0;
        return tuile.getLettre();
    }

    private static String normaliserMot(String mot) {
        String maj = mot.toUpperCase(Locale.ROOT);
        String sansAccents = Normalizer.normalize(maj, Normalizer.Form.NFD).replaceAll("\\p{M}+", "");
        return sansAccents.replaceAll("[^A-Z]", "");
    }
}
