package fr.vampireuhc.game;

import fr.vampireuhc.VampireUHC;
import fr.vampireuhc.config.ConfigManager;

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

        WorldBorder border = world.getWorldBorder();
        border.setCenter(0, 0);
        border.setSize(config.getMapSize());

        int y = world.getHighestBlockYAt(0, 0);
        world.setSpawnLocation(0, y, 0);
        plugin.getLogger().info("Map vuhc_world prête : " + config.getMapSize() + "x" + config.getMapSize() + " centrée sur 0;0.");
    }

    public void teleportPlayersToSpawn() {
        if (world == null) {
            return;
        }
        Location spawn = world.getSpawnLocation();
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.teleport(spawn);
        }
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
