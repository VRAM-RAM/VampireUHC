package fr.vampireuhc.config;

import fr.vampireuhc.VampireUHC;
import org.bukkit.configuration.file.FileConfiguration;

/*
 * Wrapper autour du config.yml pour eviter de trimballer des chemins
 * de config en dur (magic strings) dans tout le plugin.
 */
public class ConfigManager {
    private final VampireUHC plugin;

    // Assignation du plugin
    public ConfigManager(VampireUHC plugin) {
        this.plugin = plugin;
    }

    // Méthode qui retourne la config
    private FileConfiguration cfg() {
        return plugin.getConfig();
    }

    /* Utilitaires pour la timeline : */

    
    public int getRoleAssignementAt() {
        //Par défaut, on reçoit les rôles à 20 minutes, donc la méthode renvoie '20', mais ça peut être configurable.
        return cfg().getInt("timeline.role-assignment-at", 20);
    }

    public int getPvpActivationAt() {
        //Par défaut, le pvp est activé à 45 minutes de jeu, donc la méthode renvoie '45', mais ça peut être configurable.
        return cfg().getInt("timeline.pvp-activation-at", 45);
    }

    public int getEpisodeLength() {
        // En uhc, un épisode dure toujours 20 minutes (1 nuit + une journée), mais je veux que ça reste modulaire
        return cfg().getInt("timeline.episode-length", 20);
    }

    /* Utilitaires pour la compo */

    public int getReferencePlayerCount() {
        /*  On va mettre 28 joueurs par défaut. A terme, je pense diminuer ce nombre 
        (car pour une partie de test, je n'aurai pas énormément de rôles, 
        et surtout je pense que le mode de jeu peut être fun en plus petit commité, du style 23 personnes). */
        return cfg().getInt("compo.reference-player-count", 28);
    }

    public int getVampireMin() {
        // Par défaut, 6 vampires pour 28 joueurs me parait bien, surtout qu'il n'y a aucun rôle village qui peut connaitre précisement le rôle de qqn d'autre. Oublions pas aussi qu'il y a 1-2 rôles solitaires
        return cfg().getInt("compo.vampire-min", 6);
    }

    public int getVampireMax() {
        // Par défaut, 8 vampires max me parait bien.
        return cfg().getInt("compo.vampire-max", 8);
    }

    public int getSoloMin() {
        // Ducoup, un solo minimum
        return cfg().getInt("compo.solo-min", 1);
    }

    public int getSoloMax() {
        // Et 2 max. Je pense que ducoup, c'est assez équilibré (on a : compo minimum { 6 vampires + 1 solo + 21 villageois } , compo maximum { 8 vampires + 2 solos + 18 villageois })
        return cfg().getInt("compo.solo-max", 2);
    }

    /* Utilitaires pour les vampires et le maitre */

    public boolean isDayWeaknessEnabled() {
        // Pour rappel, pour perdre leur weakness, les vampires doivent marquer X joueurs.
        return cfg().getBoolean("vampires.day-weakness", true);
    }

    public int getMasterStartingHearts() {
        // Le maitre est un rôle incroyablement fort. Pour cette raison, il a un nombre de coeurs inférieur à 10 (par défaut, 8)
        return cfg().getInt("vampires.master.starting-hearts", 8);
    }

    public int getMarksToInfect() {
        // Par défaut, il faut que le maitre marque trois fois un joueur non-vampire pour l'infecter
        return cfg().getInt("vampires.master.marks-to-infect", 3);
    }

    /* Utilitaires pour marqueurs divers */

    public int getCupidonNotifyMinSeconds() {
        // Lorsqu'une marque de l'amour change de propriétaire, le cupidon en est informé dans un délai aléatoire entre t_min et t_max. Ici, on a t_min de 0 secondes.
        return cfg().getInt("markers.cupidon.notify-delay-min-seconds", 0);
    }

    public int getCupidonNotifyMaxSeconds() {
        // Lorsqu'une marque de l'amour change de propriétaire, le cupidon en est informé dans un délai aléatoire entre t_min et t_max. Ici, on a t_min de 600 secondes (10 minutes).
        return cfg().getInt("markers.cupidon.notify-delay-max-seconds", 600);
    }

    public int getAmourHeartsLost() {
        // Lorsque l'une des personnes portant une marque de l'amour meurt, l'autre personne perd alors un nombre X de coeurs (par défault, 5).
        return cfg().getInt("markers.amour-death.hearts-lost", 5);
    }

    public int getAmourPenaltyDurationSeconds() {
        // Elle perd 5 coeurs pendant 10 minutes
        return cfg().getInt("markers.amour-death.duration-seconds", 600);
    } 

    /* Helper pour messages */

    public String getPrefix() {
        return cfg().getString("messages.prefix", "&5&lVampireUHC &8» &r");
    }


}

