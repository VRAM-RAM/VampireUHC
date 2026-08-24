# VampireUHC - Version compatible 1.8.9

Cette version de **VampireUHC** est la version adaptée pour MineCraft `1.8.9`.

## Prérequis

Vous aurez besoin de [`Java 8`](https://adoptium.net/fr/temurin/releases?version=8), [maven](https://maven.apache.org/install.html) et [spigot 1.8.8](https://www.spigotmc.org/wiki/buildtools/).

## Quickstart

Pour l'utiliser, veuillez d'abord cloner le repo et vous rendre à la racine de cette version du plugin :

```bash
git clone https://github.com/VRAM-RAM/VampireUHC
cd 1.8.9/
```

Déposez votre `spigot-1.8.8.jar` dans le dossier, et lancez :

```bash
mvn install:install-file   -Dfile=spigot-1.8.8.jar   -DgroupId=org.spigotmc   -DartifactId=spigot   -Dversion=1.8.8-R0.1-SNAPSHOT   -Dpackaging=jar
```

Puis, pour compiler le plugin :

```bash
mvn clean package
```
## Mode de jeu et rôles.

Pour l'instant, les règles du mode de jeu sont indiquées dans le [readme](/README.md) du projet, et les rôles sont disponibles à [`/doc/ROLES.md`](/doc/ROLES.md).



