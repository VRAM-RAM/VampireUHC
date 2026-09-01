package fr.vampireuhc.config;

import fr.vampireuhc.VampireUHC;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.List;

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

    // Fenêtre d'usurpation du Doppelganger (20 à 60 minutes de jeu incluses).
    public int getUsurpWindowStartMin() {
        return cfg().getInt("timeline.usurp-window-start-min", 20);
    }

    public int getUsurpWindowEndMin() {
        return cfg().getInt("timeline.usurp-window-end-min", 60);
    }

    /* Utilitaires pour la compo */

    private Composition composition = null;

    // Composition parsee du config.yml (avec cache), partagee par tout le plugin.
    public Composition getComposition() {
        if (composition == null) {
            composition = Composition.load(cfg());
        }
        return composition;
    }

    // A appeler apres un reloadConfig() pour reparser la composition.
    public void resetCompositionCache() {
        composition = null;
    }

    public int getVoteEveryMinutes() {
        // Les vampires votent pour une marque toutes les X minutes.
        return cfg().getInt("compo.vote-every-minutes", 10);
    }

    /* Utilitaires pour la map */

    public int getMapSize() {
        // Map carrée de X blocs de côté, centrée sur 0;0.
        return cfg().getInt("map.size", 1000);
    }

    public long getMapSeed() {
        // Seed de la map, pour garantir de bonnes caves.
        return cfg().getLong("map.seed", 0L);
    }

    public int getDarkForestRadius() {
        // Rayon (en blocs) de la foret sombre garantie autour de 0;0.
        return cfg().getInt("map.dark-forest-radius", 400);
    }

    public int getSpawnRadius() {
        // Rayon (en blocs) du disque dans lequel les joueurs sont éparpillés au début de la partie.
        return cfg().getInt("map.spawn-radius", 350);
    }

    public int getMinSpawnDistance() {
        // Distance minimale entre deux joueurs au début de la partie.
        return cfg().getInt("map.min-spawn-distance", 25);
    }

    public int getOreCoalPerChunk() {
        return cfg().getInt("map.ores.coal-per-chunk", 2);
    }

    public int getOreIronPerChunk() {
        return cfg().getInt("map.ores.iron-per-chunk", 2);
    }

    public int getOreGoldPerChunk() {
        return cfg().getInt("map.ores.gold-per-chunk", 1);
    }

    public int getOreRedstonePerChunk() {
        return cfg().getInt("map.ores.redstone-per-chunk", 1);
    }

    public int getOreDiamondPerChunk() {
        return cfg().getInt("map.ores.diamond-per-chunk", 1);
    }

    public int getPregenChunksPerTick() {
        // Nombre de chunks demandés par tick pendant la pré-génération du disque
        // d'éparpillement (lancée au début du compte à rebours de /vuhc start).
        return Math.max(1, cfg().getInt("map.pregen-chunks-per-tick", 8));
    }

    /* Utilitaires gameplay */

    public boolean isAutoSmeltEnabled() {
        return cfg().getBoolean("gameplay.auto-smelt", true);
    }

    public boolean isBetterLootEnabled() {
        return cfg().getBoolean("gameplay.better-loot", true);
    }

    public double getAppleDropChance() {
        return cfg().getDouble("gameplay.apple-drop-chance", 0.05);
    }

    public int getLeatherBonus() {
        return cfg().getInt("gameplay.leather-bonus", 2);
    }

    /* Utilitaires pour le cycle de vie */

    public List<String> getAdminPlayers() {
        // Pseudos autorisés à lancer/arrêter/réinitialiser la partie.
        return cfg().getStringList("admins.players");
    }

    public int getDisconnectGraceMinutes() {
        // Temps de grâce avant élimination d'un joueur déconnecté (en minutes).
        return cfg().getInt("game.disconnect-grace-minutes", 10);
    }

    public int getDefaultCountdownSeconds() {
        // Durée par défaut du compte à rebours de /vuhc start (en secondes).
        return cfg().getInt("game.countdown-seconds", 30);
    }

    public boolean isStartingKitEnabled() {
        // Kit de départ (pioche/hache/épée en pierre + nourriture + bois + torches).
        return cfg().getBoolean("game.starting-kit", true);
    }

    /* Utilitaires règles UHC */

    public boolean isNoNaturalRegenEnabled() {
        return cfg().getBoolean("rules.no-natural-regen", true);
    }

    public boolean areBedsBlocked() {
        return cfg().getBoolean("rules.beds-blocked", true);
    }

    public boolean isNetherEndBlocked() {
        return cfg().getBoolean("rules.nether-end-blocked", true);
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

    public int getMarksToRemoveWeakness() {
        // A partir de X joueurs marqués, les vampires perdent leur faiblesse de jour.
        return cfg().getInt("vampires.marks-to-remove-weakness", 2);
    }

    public int getMarksForNightStrength() {
        // A partir de nX joueurs marqués, les vampires gagnent de la force la nuit.
        return cfg().getInt("vampires.marks-for-night-strength", 4);
    }

    /* Utilitaires pour l'apprentie assassin */

    public int getSlayerDarkThreshold() {
        return cfg().getInt("solo.slayer.dark-threshold", 3);
    }

    public int getSlayerLightThreshold() {
        return cfg().getInt("solo.slayer.light-threshold", 3);
    }

    public int getSlayerDarkHighThreshold() {
        return cfg().getInt("solo.slayer.dark-high-threshold", 6);
    }

    public int getSlayerLightHighThreshold() {
        return cfg().getInt("solo.slayer.light-high-threshold", 6);
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

    // Traduit les codes couleur (&x) d'un message précédé du préfixe du plugin.
    @Deprecated
    public String translate(String message) {
        return ChatColor.translateAlternateColorCodes('&', getPrefix() + message);
    }

    // Traduit les codes couleur (&x) d'un message, sans préfixe.
    @Deprecated
    public String translateRaw(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }


}

