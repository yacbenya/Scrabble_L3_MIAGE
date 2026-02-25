package service;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public final class ServiceDictionnaireMemoire implements ServiceDictionnaire {
    private final Set<String> mots = new HashSet<>();

    public ServiceDictionnaireMemoire(Iterable<String> mots) {
        if (mots != null) for (String m : mots) ajouter(m);
    }

    public void ajouter(String mot) {
        if (mot == null) return;
        String v = mot.trim().toUpperCase(Locale.ROOT);
        if (!v.isBlank()) mots.add(v);
    }

    @Override
    public boolean estValide(String mot) {
        if (mot == null) return false;
        String v = mot.trim().toUpperCase(Locale.ROOT);
        return mots.contains(v);
    }
}
