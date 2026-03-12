package controller;

import model.*;
import service.ServiceDictionnaire;
import java.util.*;

public class ControleurPartie{
	private final Partie partie;
	
	
	public ControleurPartie(ServiceDictionnaire dictionnaire) {
        this.partie = new Partie(dictionnaire);
    }
	
	public void nouvellePartie(List<String> noms) {
        partie.demarrer(noms);
    }

    public Partie getPartie() {
        return partie;
    }
	public void passerTour(){
		
	}
	
	
    public Tuile getTuileChevalet(int index) {
        Joueur joueur = partie.getJoueurCourant();
        if (index < 0 || index >= joueur.getChevalet().taille()) return null;
        return joueur.getChevalet().getTuiles().get(index);
    }
	
	public List<Tuile> getTuileChevalet() {
        Joueur joueur = partie.getJoueurCourant();
        return joueur.getChevalet().getTuiles();
    }
}