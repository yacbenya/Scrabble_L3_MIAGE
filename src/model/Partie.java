package model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class Partie {
    private final Plateau plateau = new Plateau();
    private final SacTuiles sac = new SacTuiles();
    private final List<Joueur> joueurs = new ArrayList<>();

    private int indexJoueurCourant;
    private int passesConsecutifs;
    private boolean terminee;
    private String dernierMessage = "";
    private int derniersPoints;
    private List<String> derniersMots = List.of();
    private final List<HistoriqueEntry> historique = new ArrayList<>();

    public Plateau getPlateau() {
        return plateau;
    }

    public SacTuiles getSac() {
        return sac;
    }

    public List<Joueur> getJoueurs() {
        return Collections.unmodifiableList(joueurs);
    }

    public void reinitialiserJoueurs(List<Joueur> nouveauxJoueurs) {
        joueurs.clear();
        joueurs.addAll(nouveauxJoueurs);
        indexJoueurCourant = 0;
        passesConsecutifs = 0;
        terminee = false;
        dernierMessage = "Partie initialisée.";
        derniersPoints = 0;
        derniersMots = List.of();
        historique.clear();
    }

    public Joueur getJoueurCourant() {
        if (joueurs.isEmpty()) {
            throw new IllegalStateException("La partie n'a pas de joueurs.");
        }
        return joueurs.get(indexJoueurCourant);
    }

    public int getIndexJoueurCourant() {
        return indexJoueurCourant;
    }

    public void passerAuJoueurSuivant() {
        if (!joueurs.isEmpty()) {
            indexJoueurCourant = (indexJoueurCourant + 1) % joueurs.size();
        }
    }

    public boolean estPremierCoup() {
        return plateau.estVideCentre();
    }

    public int getPassesConsecutifs() {
        return passesConsecutifs;
    }

    public void setPassesConsecutifs(int passesConsecutifs) {
        this.passesConsecutifs = passesConsecutifs;
    }

    public boolean estTerminee() {
        return terminee;
    }

    public void setTerminee(boolean terminee) {
        this.terminee = terminee;
    }

    public String getDernierMessage() {
        return dernierMessage;
    }

    public void setDernierMessage(String dernierMessage) {
        this.dernierMessage = dernierMessage == null ? "" : dernierMessage;
    }

    public int getDerniersPoints() {
        return derniersPoints;
    }

    public void setDerniersPoints(int derniersPoints) {
        this.derniersPoints = derniersPoints;
    }

    public List<String> getDerniersMots() {
        return derniersMots;
    }

    public void setDerniersMots(List<String> derniersMots) {
        this.derniersMots = derniersMots == null ? List.of() : List.copyOf(derniersMots);
    }

    public List<HistoriqueEntry> getHistorique() {
        return Collections.unmodifiableList(historique);
    }

    public void ajouterHistorique(HistoriqueEntry entry) {
        historique.add(entry);
    }

    public String getNomGagnant() {
        if (!terminee || joueurs.isEmpty()) return null;
        Joueur gagnant = joueurs.getFirst();
        for (int i = 1; i < joueurs.size(); i++) {
            if (joueurs.get(i).getScore() > gagnant.getScore()) {
                gagnant = joueurs.get(i);
            }
        }
        return gagnant.getNom();
    }
}
