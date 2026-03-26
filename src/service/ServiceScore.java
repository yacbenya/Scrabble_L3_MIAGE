package service;

import model.Coup;
import model.Plateau;
import model.Pose;
import model.Position;
import model.Prime;
import model.Tuile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class ServiceScore {

    public int calculer(Plateau plateau, Coup coup, List<InfoMot> mots) {
        Map<Position, Pose> nouvellesPoses = new HashMap<>();
        for (Pose pose : coup.getPoses()) {
            nouvellesPoses.put(pose.position(), pose);
        }

        int total = 0;

        for (InfoMot infoMot : mots) {
            int multiplicateurMot = 1;
            int somme = 0;

            for (Position position : infoMot.positions()) {
                boolean estNouvelle = nouvellesPoses.containsKey(position);
                int pointsLettre;

                if (estNouvelle) {
                    Pose pose = nouvellesPoses.get(position);
                    pointsLettre = pose.pointsLettre();
                } else {
                    Tuile tuile = plateau.getCase(position.ligne(), position.colonne()).getTuile();
                    pointsLettre = tuile.getPoints();
                }

                int multiplicateurLettre = 1;
                if (estNouvelle) {
                    Prime prime = plateau.getCase(position.ligne(), position.colonne()).getPrime();
                    if (prime == Prime.LETTRE_DOUBLE) multiplicateurLettre = 2;
                    if (prime == Prime.LETTRE_TRIPLE) multiplicateurLettre = 3;
                    if (prime == Prime.MOT_DOUBLE || prime == Prime.DEPART) multiplicateurMot *= 2;
                    if (prime == Prime.MOT_TRIPLE) multiplicateurMot *= 3;
                }

                somme += pointsLettre * multiplicateurLettre;
            }

            total += somme * multiplicateurMot;
        }

        if (coup.getPoses().size() == 7) {
            total += 50;
        }

        return total;
    }
}
