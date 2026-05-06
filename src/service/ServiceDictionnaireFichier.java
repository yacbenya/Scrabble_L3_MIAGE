package service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.Normalizer;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class ServiceDictionnaireFichier implements ServiceDictionnaire {
    private final Set<String> mots = new HashSet<>();

    public ServiceDictionnaireFichier(Path chemin) throws IOException {
        if (chemin == null) {
            throw new IllegalArgumentException("Chemin du dictionnaire manquant.");
        }
        if (!Files.exists(chemin)) {
            throw new IOException("Dictionnaire introuvable : " + chemin);
        }
        List<String> lignes = Files.readAllLines(chemin);
        for (String ligne : lignes) {
            String mot = normaliser(ligne);
            if (!mot.isBlank()) {
                mots.add(mot);
            }
        }
    }

    public int taille() {
        return mots.size();
    }

    @Override
    public boolean estValide(String mot) {
        if (mot == null) return false;
        return mots.contains(normaliser(mot));
    }

    private static String normaliser(String mot) {
        if (mot == null) return "";
        String majuscule = mot.trim().toUpperCase(Locale.ROOT);
        String sansAccents = Normalizer.normalize(majuscule, Normalizer.Form.NFD).replaceAll("\\p{M}+", "");
        return sansAccents.replaceAll("[^A-Z]", "");
    }
}
