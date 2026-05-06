#!/usr/bin/env bash
set -e
cd "$(dirname "$0")"
echo "Telechargement d'une liste complete de mots francais..."
curl -fL -o mots.txt "https://raw.githubusercontent.com/lorenbrichter/Words/master/Words/fr.txt"
echo "Liste telechargee dans mots_complet.txt."
echo "Pour l'utiliser, renommez ou copiez le fichier en mots.txt."
