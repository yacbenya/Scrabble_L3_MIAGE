package service;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public final class ServiceDictionnaireMemoire implements ServiceDictionnaire {
    private final Set<String> mots = new HashSet<>();

    public ServiceDictionnaireMemoire(Iterable<String> mots) {
        if (mots != null) {
            for (String mot : mots) ajouter(mot);
        }
    }

    public void ajouter(String mot) {
        if (mot == null) return;
        String valeur = mot.trim().toUpperCase(Locale.ROOT);
        if (!valeur.isBlank()) mots.add(valeur);
    }

    @Override
    public boolean estValide(String mot) {
        if (mot == null) return false;
        return mots.contains(mot.trim().toUpperCase(Locale.ROOT));
    }
}
