@echo off
setlocal
cd /d %~dp0
echo Telechargement d'une liste complete de mots francais...
powershell -NoProfile -Command "Invoke-WebRequest -Uri 'https://raw.githubusercontent.com/lorenbrichter/Words/master/Words/fr.txt' -OutFile 'mots.txt'"
if errorlevel 1 (
    echo Echec du telechargement.
    exit /b 1
)
echo Liste telechargee dans mots_complet.txt.
echo Pour l'utiliser, renommez ou copiez le fichier en mots.txt.
endlocal
