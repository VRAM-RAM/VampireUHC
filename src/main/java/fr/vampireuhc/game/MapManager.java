package fr.vampireuhc.game;

import fr.vampireuhc.VampireUHC;
import fr.vampireuhc.config.ConfigManager;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.WorldBorder;
import org.bukkit.WorldCreator;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;

/**
 * Crée la map spéciale (1000x1000 centrée sur 0;0, forêt sombre au centre) et
 * gère la téléportation des joueurs ainsi que l'interdiction de la quitter.
 */
public class MapManager implements Listener {
    private final VampireUHC plugin;
    private World world;

    public MapManager(VampireUHC plugin) {
        this.plugin = plugin;
    }

    public World getWorld() {
        return world;
    }

    public void prepareWorld() {
        if (world != null) {
            return;
        }

        ConfigManager config = plugin.getConfigManager();

        WorldCreator creator = new WorldCreator("vuhc_world");
        creator.seed(config.getMapSeed());
        creator.generateStructures(true);
        creator.biomeProvider(new UhcBiomeProvider(config.getDarkForestRadius()));

        world = Bukkit.createWorld(creator);
        if (world == null) {
            plugin.getLogger().severe("Impossible de créer la map vuhc_world !");
            return;
        }

        // Minerais supplémentaires (fer, or, diamant) dans les caves.
        world.getPopulators().add(new OrePopulator(config));

        WorldBorder border = world.getWorldBorder();
        border.setCenter(0, 0);
        border.setSize(config.getMapSize());

        int y = world.getHighestBlockYAt(0, 0);
        world.setSpawnLocation(0, y, 0);
        plugin.getLogger().info("Map vuhc_world prête : " + config.getMapSize() + "x" + config.getMapSize() + " centrée sur 0;0.");
    }

    // Supprime la map de la partie pour préparer la prochaine partie.
    public void resetWorld() {
        if (world == null) {
            return;
        }

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getWorld().equals(world)) {
                player.teleport(Bukkit.getWorlds().get(0).getSpawnLocation());
            }
        }

        Bukkit.unloadWorld(world, false);
        deleteRecursively(world.getWorldFolder());
        world = null;
        plugin.getLogger().info("Map vuhc_world supprimée.");
    }

    private void deleteRecursively(File file) {
        if (file == null || !file.exists()) {
            return;
        }
        File[] children = file.listFiles();
        if (children != null) {
            for (File child : children) {
                deleteRecursively(child);
            }
        }
        file.delete();
    }

    // Éparpille les joueurs à des positions aléatoires au début de la partie.
    public void teleportPlayersToSpawn() {
        if (world == null) {
            return;
        }
        ConfigManager config = plugin.getConfigManager();
        int radius = Math.min(config.getSpawnRadius(), Math.max(50, config.getDarkForestRadius() - 50));
        int minDist = Math.max(10, Math.min(config.getMinSpawnDistance(), radius / 2));

        Random random = new Random();
        List<Location> taken = new ArrayList<>();
        for (Player player : Bukkit.getOnlinePlayers()) {
            Location loc = randomSpawn(radius, minDist, taken, random);
            player.teleport(loc);
            taken.add(loc);
        }
        plugin.getLogger().info("Joueurs éparpillés aléatoirement (rayon " + radius + ", distance min " + minDist + ").");
    }

    private Location randomSpawn(int radius, int minDist, List<Location> taken, Random random) {
        Location fallback = null;
        for (int attempt = 0; attempt < 50; attempt++) {
            double angle = random.nextDouble() * Math.PI * 2;
            double dist = minDist + random.nextDouble() * (radius - minDist);
            int x = (int) Math.round(Math.cos(angle) * dist);
            int z = (int) Math.round(Math.sin(angle) * dist);
            int y = world.getHighestBlockYAt(x, z) + 1;
            Location loc = new Location(world, x + 0.5, y, z + 0.5, random.nextFloat() * 360f, 0f);
            if (isFarEnough(loc, taken, minDist)) {
                return loc;
            }
            fallback = loc;
        }
        return fallback;
    }

    private boolean isFarEnough(Location loc, List<Location> taken, int minDist) {
        for (Location other : taken) {
            double dx = loc.getX() - other.getX();
            double dz = loc.getZ() - other.getZ();
            if (dx * dx + dz * dz < (double) minDist * minDist) {
                return false;
            }
        }
        return true;
    }

    // Empeche les joueurs de quitter la map de la partie.
    @EventHandler
    public void onPlayerChangedWorld(PlayerChangedWorldEvent event) {
        if (world == null || plugin.getGameManager().getPhase() == GamePhase.ENDED) {
            return;
        }
        if (!event.getPlayer().getWorld().equals(world)) {
            event.getPlayer().teleport(world.getSpawnLocation());
        }
    }
}
