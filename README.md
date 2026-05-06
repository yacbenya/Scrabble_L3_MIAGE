# Scrabble

Implémentation jouable du Scrabble en mode multijoueur local (2 à 4 joueurs sur la même machine, à tour de rôle). Le projet est composé d'un backend Java qui porte toute la logique du jeu et d'un frontend React qui affiche le plateau, les chevalets et les actions disponibles.

## Aperçu

- Plateau 15×15 avec primes (lettre/mot, double/triple, case de départ)
- Sac de 102 tuiles + 2 jokers
- Validation des coups : alignement, contiguïté, contact, premier coup au centre, mots croisés, longueur minimale
- Vérification de chaque mot via un dictionnaire chargé au démarrage
- Calcul de score complet (multiplicateurs lettre/mot, +50 pour 7 tuiles posées)
- Échange de tuiles, passe, fin de partie par chevalet vide ou par excès de passes consécutives
- Historique de partie, écran de transition entre joueurs, écran de fin avec classement

## Stack

| Côté        | Technologie                                       |
| ----------- | ------------------------------------------------- |
| Backend     | Java 17+, `com.sun.net.httpserver`, JSON maison   |
| Frontend    | React 18, Vite                                    |
| Dictionnaire| Fichier texte chargé en mémoire (`HashSet`)       |

Aucune dépendance Java externe (Maven/Gradle non requis). Côté frontend, seul `npm install` est nécessaire.

## Prérequis

- Un JDK 17 ou supérieur (`java -version`, `javac -version`)
- Node.js 18+ et npm pour le frontend (`node -v`, `npm -v`)

## Lancer le projet

### 1. Backend

Sous Windows :

```
run-backend.bat
```

Sous macOS/Linux :

```
./run-backend.sh
```

Le serveur démarre sur `http://localhost:8080`. Au lancement, il affiche le nombre de mots chargés depuis `dictionnaire/mots.txt` (ou un avertissement si le fichier est absent — dans ce cas tous les mots sont acceptés).

### 2. Frontend

Dans un second terminal :

```
cd frontend-react
npm install        # première fois uniquement
npm run dev
```

Le frontend est servi sur `http://localhost:5173`. Il appelle automatiquement le backend sur le port 8080.

### 3. Jouer

Ouvrir `http://localhost:5173`, saisir 2 à 4 noms de joueurs et démarrer. À chaque tour, sélectionner les tuiles du chevalet, les déposer sur le plateau (clic ou glisser-déposer), choisir la direction si nécessaire, puis valider le coup. Les boutons « Passer » et « Échanger des tuiles » sont disponibles à tout moment.

Entre deux tours, un écran de transition demande de passer l'appareil au joueur suivant pour ne pas révéler son chevalet.

## Le dictionnaire

Le fichier `dictionnaire/mots.txt` contient une base de mots français suffisante pour démarrer. Format : un mot par ligne, encodage UTF-8, accents/casse sans importance (la normalisation est faite au chargement).

Pour charger une liste plus complète, deux options :

- exécuter `dictionnaire/telecharger.bat` (Windows) ou `dictionnaire/telecharger.sh` (Unix) qui récupère une liste publique de mots français, puis renommer le fichier `mots_complet.txt` en `mots.txt` ;
- placer manuellement dans `dictionnaire/mots.txt` n'importe quelle liste de votre choix.

Le backend recharge le dictionnaire à chaque redémarrage.

## Structure du dépôt

```
scrabble_v4/
├── src/                Code Java (backend)
│   ├── view/           Point d'entrée
│   ├── api/            Serveur HTTP + JSON
│   ├── controller/     Orchestration
│   ├── service/        Règles métier (validation, score, dictionnaire)
│   └── model/          Entités du domaine
├── frontend-react/     Code React + Vite
│   └── src/            App.jsx, api.js, styles.css, main.jsx
├── dictionnaire/       Liste de mots + scripts de téléchargement
├── run-backend.bat     Compile + lance le backend (Windows)
├── run-backend.sh      Compile + lance le backend (Unix)
├── README.md           Ce fichier
└── DOSSIER_TECHNIQUE.md Document technique complet
```

## API HTTP

Toutes les routes sont préfixées par `/api`. Réponse en JSON UTF-8.

| Route               | Méthode | Description                                  |
| ------------------- | ------- | -------------------------------------------- |
| `/api/health`       | GET     | Test de vie                                  |
| `/api/game/state`   | GET     | État courant complet                         |
| `/api/game/start`   | POST    | Démarrer une partie (`{playerNames: [...]}`) |
| `/api/game/reset`   | POST    | Réinitialiser                                |
| `/api/game/play`    | POST    | Jouer un coup (`{direction, placements}`)    |
| `/api/game/pass`    | POST    | Passer le tour                               |
| `/api/game/exchange`| POST    | Échanger des tuiles (`{tileIds}`)            |

Les erreurs métier renvoient un code HTTP 400 avec un corps `{"error": "..."}`.

Pour le détail des formats et des cas d'usage, voir [`DOSSIER_TECHNIQUE.md`](DOSSIER_TECHNIQUE.md).


## Auteurs

- **Hugo** — backend (serveur HTTP, contrôleur, modèle plateau/sac/tuile)
- **Maria** — métier (services, validation des coups, score, dictionnaire, historique)
- **Yacine** — meneur et architecte du projet, frontend React, intégration et présentation

## Licence

Projet réalisé dans le cadre d'un projet de S6.
