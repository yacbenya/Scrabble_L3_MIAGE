package service;

import model.Coup;
import model.Direction;
import model.Plateau;
import model.Pose;
import model.Position;
import model.Tuile;

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
            throw new IllegalArgumentException("Aucune tuile posée.");
        }

        List<Pose> poses = coup.getPoses();
        Map<Position, Pose> nouvellesPoses = new HashMap<>();
        for (Pose pose : poses) {
            Position position = pose.position();
            int ligne = position.ligne();
            int colonne = position.colonne();

            if (!plateau.dansBornes(ligne, colonne)) throw new IllegalArgumentException("Hors plateau.");
            if (!plateau.getCase(ligne, colonne).estVide()) throw new IllegalArgumentException("Case déjà occupée.");
            if (nouvellesPoses.put(position, pose) != null) throw new IllegalArgumentException("Deux tuiles sur la même case.");

            if (pose.tuile().estJoker()) {
                Character face = pose.faceJoker();
                if (face == null || !Character.isLetter(face)) {
                    throw new IllegalArgumentException("Joker : choisis une lettre A-Z.");
                }
            }
        }

        Direction direction = deduireDirection(plateau, poses, coup.getDirection());
        if (!posesAlignees(poses, direction)) {
            throw new IllegalArgumentException("Les tuiles doivent être alignées.");
        }

        verifierContiguite(plateau, poses, direction, nouvellesPoses);

        if (premierCoup) {
            if (!nouvellesPoses.containsKey(new Position(7, 7))) {
                throw new IllegalArgumentException("Premier coup : le mot doit passer par le centre.");
            }
        } else if (!toucheUneTuileExistante(plateau, nouvellesPoses.keySet())) {
            throw new IllegalArgumentException("Le coup doit toucher une tuile déjà sur le plateau.");
        }

        List<InfoMot> mots = new ArrayList<>();
        Set<String> signatures = new HashSet<>();

        InfoMot motPrincipal = construireMot(plateau, nouvellesPoses, poses.getFirst().position(), direction);
        if (motPrincipal.positions().size() > 1) {
            ajouterSiNouveau(mots, signatures, motPrincipal);
        }

        Direction directionPerpendiculaire = direction.perpendiculaire();
        for (Pose pose : poses) {
            InfoMot motCroise = construireMot(plateau, nouvellesPoses, pose.position(), directionPerpendiculaire);
            if (motCroise.positions().size() > 1) {
                ajouterSiNouveau(mots, signatures, motCroise);
            }
        }

        if (mots.isEmpty()) {
            throw new IllegalArgumentException("Le coup doit former au moins un mot de deux lettres ou plus.");
        }

        List<String> motsNormalises = new ArrayList<>();
        for (InfoMot mot : mots) {
            String nettoye = normaliserMot(mot.texte());
            if (nettoye.length() < 2) {
                throw new IllegalArgumentException("Mot trop court.");
            }
            if (dictionnaire != null && !dictionnaire.estValide(nettoye)) {
                throw new IllegalArgumentException("Mot invalide : " + nettoye);
            }
            motsNormalises.add(nettoye);
        }

        return new ResultatValidation(List.copyOf(mots), List.copyOf(motsNormalises));
    }

    private static void ajouterSiNouveau(List<InfoMot> mots, Set<String> signatures, InfoMot infoMot) {
        String signature = infoMot.positions().toString();
        if (signatures.add(signature)) {
            mots.add(infoMot);
        }
    }

    private static Direction deduireDirection(Plateau plateau, List<Pose> poses, Direction directionDemandee) {
        if (poses.size() >= 2) {
            boolean memeLigne = poses.stream().map(p -> p.position().ligne()).distinct().count() == 1;
            boolean memeColonne = poses.stream().map(p -> p.position().colonne()).distinct().count() == 1;
            if (memeLigne && !memeColonne) return Direction.HORIZONTALE;
            if (memeColonne && !memeLigne) return Direction.VERTICALE;
            throw new IllegalArgumentException("Les tuiles doivent être sur une même ligne ou colonne.");
        }

        Pose pose = poses.getFirst();
        int ligne = pose.position().ligne();
        int colonne = pose.position().colonne();

        boolean aGaucheOuDroite = aUneTuile(plateau, ligne, colonne - 1) || aUneTuile(plateau, ligne, colonne + 1);
        boolean enHautOuBas = aUneTuile(plateau, ligne - 1, colonne) || aUneTuile(plateau, ligne + 1, colonne);

        if (directionDemandee != null) return directionDemandee;
        if (aGaucheOuDroite && !enHautOuBas) return Direction.HORIZONTALE;
        if (enHautOuBas && !aGaucheOuDroite) return Direction.VERTICALE;
        return Direction.HORIZONTALE;
    }

    private static boolean aUneTuile(Plateau plateau, int ligne, int colonne) {
        return plateau.dansBornes(ligne, colonne) && !plateau.getCase(ligne, colonne).estVide();
    }

    private static boolean posesAlignees(List<Pose> poses, Direction direction) {
        if (poses.size() < 2) return true;
        if (direction == Direction.HORIZONTALE) {
            int ligne = poses.getFirst().position().ligne();
            return poses.stream().allMatch(p -> p.position().ligne() == ligne);
        }
        int colonne = poses.getFirst().position().colonne();
        return poses.stream().allMatch(p -> p.position().colonne() == colonne);
    }

    private static void verifierContiguite(Plateau plateau, List<Pose> poses, Direction direction, Map<Position, Pose> nouvellesPoses) {
        if (poses.size() < 2) return;

        int fixe = direction == Direction.HORIZONTALE ? poses.getFirst().position().ligne() : poses.getFirst().position().colonne();
        int min = Integer.MAX_VALUE;
        int max = Integer.MIN_VALUE;

        for (Pose pose : poses) {
            int variable = direction == Direction.HORIZONTALE ? pose.position().colonne() : pose.position().ligne();
            min = Math.min(min, variable);
            max = Math.max(max, variable);
        }

        for (int variable = min; variable <= max; variable++) {
            int ligne = direction == Direction.HORIZONTALE ? fixe : variable;
            int colonne = direction == Direction.HORIZONTALE ? variable : fixe;
            Position position = new Position(ligne, colonne);
            boolean ok = nouvellesPoses.containsKey(position) || !plateau.getCase(ligne, colonne).estVide();
            if (!ok) throw new IllegalArgumentException("Mot non contigu.");
        }
    }

    private static boolean toucheUneTuileExistante(Plateau plateau, Set<Position> nouvellesPositions) {
        for (Position position : nouvellesPositions) {
            int ligne = position.ligne();
            int colonne = position.colonne();
            if (aUneTuile(plateau, ligne - 1, colonne)
                    || aUneTuile(plateau, ligne + 1, colonne)
                    || aUneTuile(plateau, ligne, colonne - 1)
                    || aUneTuile(plateau, ligne, colonne + 1)) {
                return true;
            }
        }
        return false;
    }

    private static InfoMot construireMot(Plateau plateau, Map<Position, Pose> nouvellesPoses, Position ancre, Direction direction) {
        int deltaLigne = direction == Direction.VERTICALE ? 1 : 0;
        int deltaColonne = direction == Direction.HORIZONTALE ? 1 : 0;

        int ligne = ancre.ligne();
        int colonne = ancre.colonne();

        while (plateau.dansBornes(ligne - deltaLigne, colonne - deltaColonne)
                && lettreA(plateau, nouvellesPoses, ligne - deltaLigne, colonne - deltaColonne) != 0) {
            ligne -= deltaLigne;
            colonne -= deltaColonne;
        }

        StringBuilder texte = new StringBuilder();
        List<Position> positions = new ArrayList<>();

        while (plateau.dansBornes(ligne, colonne) && lettreA(plateau, nouvellesPoses, ligne, colonne) != 0) {
            texte.append(lettreA(plateau, nouvellesPoses, ligne, colonne));
            positions.add(new Position(ligne, colonne));
            ligne += deltaLigne;
            colonne += deltaColonne;
        }

        return new InfoMot(texte.toString(), List.copyOf(positions));
    }

    private static char lettreA(Plateau plateau, Map<Position, Pose> nouvellesPoses, int ligne, int colonne) {
        Pose pose = nouvellesPoses.get(new Position(ligne, colonne));
        if (pose != null) return pose.lettreVisible();
        Tuile tuile = plateau.getCase(ligne, colonne).getTuile();
        if (tuile == null) return 0;
        return tuile.getLettre();
    }

    private static String normaliserMot(String mot) {
        String majuscule = mot.toUpperCase(Locale.ROOT);
        String sansAccents = Normalizer.normalize(majuscule, Normalizer.Form.NFD).replaceAll("\\p{M}+", "");
        return sansAccents.replaceAll("[^A-Z]", "");
    }
}
