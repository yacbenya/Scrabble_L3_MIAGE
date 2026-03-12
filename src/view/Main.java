package view;
import controller.ControleurPartie;
import model.Partie;
import java.util.*;
import service.ServiceDictionnaireMemoire;

public class Main{
	public static void main(String[] args){
		System.out.println("lancer");
		
		ArrayList<String> list = new ArrayList();
		list.add("a");
		list.add("b");
		
		Partie p = new Partie();
		
		Set<String> mots = new HashSet<>();
		mots.add("test");
		ServiceDictionnaireMemoire dictionnaire = new ServiceDictionnaireMemoire(mots);
		
		ControleurPartie cp = new ControleurPartie(dictionnaire);
		
		cp.nouvellePartie(list);
		
	}
}