package model;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class Partie {

    @FunctionalInterface
    public interface Dictionnaire {
        boolean estValide(String mot);
    }

    private static final Dictionnaire TOUJOURS_VALIDE = mot -> true;

    private final Plateau plateau = new Plateau();
    private final SacTuiles sac = new SacTuiles();
    private final List<Joueur> joueurs = new ArrayList<>();

    private final Dictionnaire dictionnaire;

    private int indexJoueurCourant = 0;
    private int passesConsecutifs = 0;
    private boolean terminee = false;

    public Partie() {
        this(TOUJOURS_VALIDE);
    }

    public Partie(Dictionnaire dictionnaire) {
        this.dictionnaire = (dictionnaire == null) ? TOUJOURS_VALIDE : dictionnaire;
    }

    public void demarrer(List<String> noms) {
        joueurs.clear();
        for (String nom : noms) joueurs.add(new Joueur(nom));

        if (joueurs.size() < 2 || joueurs.size() > 4) {
            throw new IllegalArgumentException("Il faut entre 2 et 4 joueurs.");
        }

        for (Joueur j : joueurs) completerChevalet(j);

        indexJoueurCourant = 0;
        passesConsecutifs = 0;
        terminee = false;
    }

    public Plateau getPlateau() { return plateau; }
    public List<Joueur> getJoueurs() { return joueurs; }
    public Joueur getJoueurCourant() { return joueurs.get(indexJoueurCourant); }

    public boolean estPremierCoup() { return plateau.estVideCentre(); }
    public boolean estTerminee() { return terminee; }


    public ResultatTour jouerCoup(Coup coup) {
        if (terminee) throw new IllegalStateException("La partie est terminee.");
        if (coup == null || coup.getPoses() == null || coup.getPoses().isEmpty()) {
            throw new IllegalArgumentException("Aucune tuile posee.");
        }

        List<Pose> poses = coup.getPoses();

        Set<Position> uniques = new HashSet<>();
        for (Pose p : poses) {
            Position pos = p.position();
            if (!plateau.dansBornes(pos.ligne(), pos.colonne())) throw new IllegalArgumentException("Hors plateau.");
            if (!plateau.getCase(pos.ligne(), pos.colonne()).estVide()) throw new IllegalArgumentException("Case deja occupee.");
            if (!uniques.add(pos)) throw new IllegalArgumentException("Deux tuiles sur la meme case.");

            if (p.tuile().estJoker()) {
                Character face = p.faceJoker();
                if (face == null || !Character.isLetter(face)) throw new IllegalArgumentException("Joker: choisis une lettre A-Z.");
            }
        }

        Direction dir = (coup.getDirection() != null) ? coup.getDirection() : devinerDirection(poses);
        if (!posesAlignees(poses, dir)) throw new IllegalArgumentException("Les tuiles doivent etre alignees.");

        if (estPremierCoup() && uniques.stream().noneMatch(p -> p.ligne() == 7 && p.colonne() == 7)) {
            throw new IllegalArgumentException("Premier coup: ca doit passer par le centre.");
        }


        String mot = construireMotPrincipal(poses, dir);
        String nettoye = normaliserMot(mot);

        if (nettoye.length() < 2) throw new IllegalArgumentException("Mot trop court.");
        if (!dictionnaire.estValide(nettoye)) throw new IllegalArgumentException("Mot invalide: " + nettoye);

        int points = calculerPoints(poses);

        plateau.appliquerCoup(new Coup(poses, dir));

        Joueur joueur = getJoueurCourant();
        joueur.ajouterScore(points);
        completerChevalet(joueur);

        passesConsecutifs = 0;

        if (sac.estVide() && joueur.getChevalet().taille() == 0) terminee = true;

        String msg = terminee ? "Fin (0.8): sac vide et chevalet vide." : "Coup accepte (0.8).";
        if (!terminee) passerAuJoueurSuivant();

        return new ResultatTour(points, List.of(nettoye), msg, terminee);
    }

    public void passer() {
        if (terminee) return;

        passesConsecutifs++;
        int seuil = joueurs.size() * 2;
        if (passesConsecutifs >= seuil) {
            terminee = true;
            return;
        }
        passerAuJoueurSuivant();
    }

    private void passerAuJoueurSuivant() {
        indexJoueurCourant = (indexJoueurCourant + 1) % joueurs.size();
    }

    private void completerChevalet(Joueur joueur) {
        while (joueur.getChevalet().taille() < 7 && !sac.estVide()) {
            Tuile t = sac.piocher();
            if (t == null) break;
            joueur.getChevalet().ajouter(t);
        }
    }

    private static Direction devinerDirection(List<Pose> poses) {
        if (poses.size() < 2) return Direction.HORIZONTALE;

        long nbLignes = poses.stream().map(p -> p.position().ligne()).distinct().count();
        long nbCols = poses.stream().map(p -> p.position().colonne()).distinct().count();

        if (nbLignes == 1 && nbCols > 1) return Direction.HORIZONTALE;
        if (nbCols == 1 && nbLignes > 1) return Direction.VERTICALE;

        throw new IllegalArgumentException("Les tuiles doivent etre sur une meme ligne ou colonne.");
    }

    private static boolean posesAlignees(List<Pose> poses, Direction d) {
        if (d == Direction.HORIZONTALE) {
            int l = poses.get(0).position().ligne();
            return poses.stream().allMatch(p -> p.position().ligne() == l);
        } else {
            int c = poses.get(0).position().colonne();
            return poses.stream().allMatch(p -> p.position().colonne() == c);
        }
    }

    private static String construireMotPrincipal(List<Pose> poses, Direction d) {
        List<Pose> copie = new ArrayList<>(poses);

        copie.sort(Comparator.comparingInt(p ->
                (d == Direction.HORIZONTALE) ? p.position().colonne() : p.position().ligne()
        ));

        StringBuilder sb = new StringBuilder();
        for (Pose p : copie) sb.append(p.lettreVisible());
        return sb.toString();
    }

    private static int calculerPoints(List<Pose> poses) {
        int total = 0;
        for (Pose p : poses) total += p.pointsLettre();
        if (poses.size() == 7) total += 50;
        return total;
    }

    private static String normaliserMot(String mot) {
    String maj = mot.toUpperCase(Locale.ROOT);
    String sansAccents = Normalizer
            .normalize(maj, Normalizer.Form.NFD)
            .replaceAll("\\p{M}+", "");   // <- ICI
    return sansAccents.replaceAll("[^A-Z]", "");
}



}
