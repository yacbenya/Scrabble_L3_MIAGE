package service;

import model.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class ServiceScore {

    public int calculer(Plateau plateau, Coup coup, List<InfoMot> mots) {
        Map<Position, Pose> nouvellesPoses = new HashMap<>();
        for (Pose pose : coup.getPoses()) nouvellesPoses.put(pose.position(), pose);

        int total = 0;

        for (InfoMot infoMot : mots) {
            int multiplicateurMot = 1;
            int somme = 0;

            for (Position pos : infoMot.positions()) {
                boolean estNouvelle = nouvellesPoses.containsKey(pos);

                int pointsLettre;
                if (estNouvelle) {
                    Pose pose = nouvellesPoses.get(pos);
                    pointsLettre = pose.pointsLettre();
                } else {
                    Tuile tuile = plateau.getCase(pos.ligne(), pos.colonne()).getTuile();
                    pointsLettre = tuile.getPoints();
                }

                int multiplicateurLettre = 1;

                if (estNouvelle) {
                    Prime prime = plateau.getCase(pos.ligne(), pos.colonne()).getPrime();

                    if (prime == Prime.LETTRE_DOUBLE) multiplicateurLettre = 2;
                    if (prime == Prime.LETTRE_TRIPLE) multiplicateurLettre = 3;

                    if (prime == Prime.MOT_DOUBLE || prime == Prime.DEPART) multiplicateurMot *= 2;
                    if (prime == Prime.MOT_TRIPLE) multiplicateurMot *= 3;
                }

                somme += pointsLettre * multiplicateurLettre;
            }

            total += somme * multiplicateurMot;
        }

        if (coup.getPoses().size() == 7) total += 50;

        return total;
    }
}
