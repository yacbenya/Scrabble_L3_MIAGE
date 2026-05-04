package controller;

import model.Coup;
import model.Direction;
import model.HistoriqueEntry;
import model.Joueur;
import model.Partie;
import model.Pose;
import model.Position;
import model.Prime;
import model.ResultatTour;
import model.Tuile;
import service.PartieService;
import service.ServiceDictionnaire;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ControleurPartie {
    private final PartieService partieService;
    private Partie partie;

    public ControleurPartie(ServiceDictionnaire dictionnaire) {
        this.partieService = new PartieService(dictionnaire);
    }

    public Partie nouvellePartie(List<String> noms) {
        this.partie = partieService.demarrer(noms);
        return partie;
    }

    public Partie getPartie() {
        return partie;
    }

    public void reinitialiser() {
        this.partie = null;
    }

    public ResultatTour jouerCoup(List<PlacementCommande> placements, Direction direction) {
        Partie partieCourante = exigerPartie();
        if (placements == null || placements.isEmpty()) {
            throw new IllegalArgumentException("Aucune tuile posée.");
        }

        Joueur joueur = partieCourante.getJoueurCourant();
        List<Pose> poses = new ArrayList<>();
        List<Tuile> prelevees = new ArrayList<>();

        try {
            for (PlacementCommande placement : placements) {
                if (placement.tileId() == null || placement.tileId().isBlank()) {
                    throw new IllegalArgumentException("tileId manquant.");
                }

                Tuile tuile = joueur.getChevalet().retirerParId(placement.tileId());
                if (tuile == null) {
                    throw new IllegalArgumentException("La tuile " + placement.tileId() + " n'existe plus dans le chevalet.");
                }
                prelevees.add(tuile);
                poses.add(new Pose(new Position(placement.ligne(), placement.colonne()), tuile, placement.faceJoker()));
            }

            return partieService.jouerCoup(partieCourante, new Coup(poses, direction));
        } catch (RuntimeException e) {
            for (Tuile tuile : prelevees) {
                joueur.getChevalet().ajouter(tuile);
            }
            throw e;
        }
    }

    public String passerTour() {
        return partieService.passer(exigerPartie());
    }

    public String echangerTuiles(List<String> idsTuiles) {
        return partieService.echanger(exigerPartie(), idsTuiles);
    }

    public Map<String, Object> exporterEtat() {
        Partie partieCourante = this.partie;
        Map<String, Object> racine = new LinkedHashMap<>();
        racine.put("gameStarted", partieCourante != null && !partieCourante.getJoueurs().isEmpty());

        if (partieCourante == null || partieCourante.getJoueurs().isEmpty()) {
            racine.put("finished", false);
            racine.put("board", boardVide());
            racine.put("players", List.of());
            racine.put("rack", List.of());
            racine.put("bagCount", 0);
            racine.put("currentPlayerIndex", -1);
            racine.put("currentPlayerName", null);
            racine.put("consecutivePasses", 0);
            racine.put("lastMessage", "Aucune partie en cours.");
            racine.put("lastPoints", 0);
            racine.put("lastWords", List.of());
            racine.put("canExchange", false);
            racine.put("winner", null);
            racine.put("history", List.of());
            return racine;
        }

        racine.put("finished", partieCourante.estTerminee());
        racine.put("currentPlayerIndex", partieCourante.getIndexJoueurCourant());
        racine.put("currentPlayerName", partieCourante.getJoueurCourant().getNom());
        racine.put("bagCount", partieCourante.getSac().taille());
        racine.put("consecutivePasses", partieCourante.getPassesConsecutifs());
        racine.put("lastMessage", partieCourante.getDernierMessage());
        racine.put("lastPoints", partieCourante.getDerniersPoints());
        racine.put("lastWords", partieCourante.getDerniersMots());
        racine.put("canExchange", partieCourante.getSac().taille() >= 7 && !partieCourante.estTerminee());
        racine.put("winner", partieCourante.getNomGagnant());
        racine.put("players", serialiserJoueurs(partieCourante));
        racine.put("rack", serialiserChevalet(partieCourante.getJoueurCourant()));
        racine.put("board", serialiserPlateau(partieCourante));
        racine.put("history", serialiserHistorique(partieCourante));
        return racine;
    }

    private Partie exigerPartie() {
        if (partie == null || partie.getJoueurs().isEmpty()) {
            throw new IllegalStateException("Aucune partie en cours.");
        }
        return partie;
    }

    private static List<Object> serialiserJoueurs(Partie partie) {
        List<Object> joueurs = new ArrayList<>();
        for (int i = 0; i < partie.getJoueurs().size(); i++) {
            Joueur joueur = partie.getJoueurs().get(i);
            Map<String, Object> ligne = new LinkedHashMap<>();
            ligne.put("name", joueur.getNom());
            ligne.put("score", joueur.getScore());
            ligne.put("rackCount", joueur.getChevalet().taille());
            ligne.put("current", i == partie.getIndexJoueurCourant());
            joueurs.add(ligne);
        }
        return joueurs;
    }

    private static List<Object> serialiserChevalet(Joueur joueur) {
        List<Object> rack = new ArrayList<>();
        for (Tuile tuile : joueur.getChevalet().getTuiles()) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", tuile.getId());
            item.put("letter", String.valueOf(tuile.getLettre()));
            item.put("points", tuile.getPoints());
            item.put("joker", tuile.estJoker());
            rack.add(item);
        }
        return rack;
    }

    private static List<Object> boardVide() {
        Partie temporaire = new Partie();
        return serialiserPlateau(temporaire);
    }

    private static List<Object> serialiserHistorique(Partie partie) {
        List<Object> liste = new ArrayList<>();
        for (HistoriqueEntry entry : partie.getHistorique()) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("type", entry.type());
            item.put("joueur", entry.joueur());
            item.put("points", entry.points());
            item.put("mots", entry.mots());
            item.put("message", entry.message());
            liste.add(item);
        }
        return liste;
    }

    private static List<Object> serialiserPlateau(Partie partie) {
        List<Object> lignes = new ArrayList<>();
        for (int ligne = 0; ligne < 15; ligne++) {
            List<Object> colonnes = new ArrayList<>();
            for (int colonne = 0; colonne < 15; colonne++) {
                Map<String, Object> cellule = new LinkedHashMap<>();
                cellule.put("row", ligne);
                cellule.put("col", colonne);
                Prime prime = partie.getPlateau().getCase(ligne, colonne).getPrime();
                cellule.put("prime", prime.name());
                Tuile tuile = partie.getPlateau().getCase(ligne, colonne).getTuile();
                if (tuile == null) {
                    cellule.put("tile", null);
                } else {
                    Map<String, Object> infoTuile = new LinkedHashMap<>();
                    infoTuile.put("letter", String.valueOf(tuile.getLettre()));
                    infoTuile.put("points", tuile.getPoints());
                    infoTuile.put("joker", tuile.estJoker());
                    cellule.put("tile", infoTuile);
                }
                colonnes.add(cellule);
            }
            lignes.add(colonnes);
        }
        return lignes;
    }
}
