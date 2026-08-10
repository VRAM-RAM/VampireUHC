package fr.vampireuhc;

import fr.vampireuhc.commands.VUHCCommand;
import fr.vampireuhc.config.ConfigManager;
import fr.vampireuhc.game.GameManager;
import fr.vampireuhc.game.MapManager;
import fr.vampireuhc.game.RoleBuffManager;
import fr.vampireuhc.game.SidebarManager;
import fr.vampireuhc.game.SpectatorManager;
import fr.vampireuhc.listeners.GameplayListener;
import fr.vampireuhc.listeners.PlayerConnectionListener;
import fr.vampireuhc.listeners.PlayerDeathListener;
import fr.vampireuhc.listeners.PvPListener;
import fr.vampireuhc.listeners.RulesListener;
import fr.vampireuhc.markers.MarkerManager;
import fr.vampireuhc.player.PlayerManager;
import fr.vampireuhc.roles.RoleManager;
import fr.vampireuhc.vampire_vote.VampireVoteManager;

import java.io.File;

import org.bukkit.plugin.java.JavaPlugin;

public class VampireUHC extends JavaPlugin {
    private static VampireUHC instance;

    private ConfigManager configManager;
    private PlayerManager playerManager;
    private MarkerManager markerManager;
    private GameManager gameManager;
    private RoleManager roleManager;
    private VampireVoteManager voteManager;
    private MapManager mapManager;
    private RoleBuffManager buffManager;
    private SidebarManager sidebarManager;
    private SpectatorManager spectatorManager;
    private PlayerConnectionListener connectionListener;
    private RulesListener rulesListener;

    @Override
    public void onEnable() {
        instance = this;
        getLogger().info("VampireUHC plugin activated.");
        saveDefaultConfig();
        
        this.configManager = new ConfigManager(this);
        this.playerManager = new PlayerManager(this);
        this.markerManager = new MarkerManager(this);
        this.gameManager = new GameManager(this, playerManager, markerManager, configManager);
        
        //On initialise RoleManager après tous les autres managers.

        this.roleManager = new RoleManager(this, playerManager);

        this.voteManager = new VampireVoteManager(this);
        this.mapManager = new MapManager(this);
        this.buffManager = new RoleBuffManager(this);
        this.sidebarManager = new SidebarManager(this);
        this.spectatorManager = new SpectatorManager(this);
        this.connectionListener = new PlayerConnectionListener(this);
        this.rulesListener = new RulesListener(this);

        getServer().getPluginManager().registerEvents(new PvPListener(gameManager), this);
        getServer().getPluginManager().registerEvents(new PlayerDeathListener(this), this);
        getServer().getPluginManager().registerEvents(new GameplayListener(this), this);
        getServer().getPluginManager().registerEvents(mapManager, this);
        getServer().getPluginManager().registerEvents(spectatorManager, this);
        getServer().getPluginManager().registerEvents(connectionListener, this);
        getServer().getPluginManager().registerEvents(rulesListener, this);

        VUHCCommand command = new VUHCCommand(gameManager, playerManager, markerManager, roleManager, voteManager, spectatorManager);
        getCommand("vuhc").setExecutor(command);
        getCommand("vuhc").setTabCompleter(command);

        getLogger().info("VampireUHC activé !");
    }

    @Override
    public void onDisable() {
        if (gameManager != null) {
            gameManager.stop();
        }
        
        // Sauvegarder l'état du jeu avant désactivation
        if (roleManager != null && playerManager.getAll().iterator().hasNext()) {
            roleManager.saveGameToJson(getDataFolder().getAbsolutePath() + File.separator + "game-state.json");
        }
        
        getLogger().info("VampireUHC desactivé !");
    }

    public static VampireUHC getInstance() {
        return instance;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public PlayerManager getPlayerManager() {
        return playerManager;
    }

    public MarkerManager getMarkerManager() {
        return markerManager;
    }

    public GameManager getGameManager() {
        return gameManager;
    }

    public RoleManager getRoleManager() {
        return roleManager;
    }

    public VampireVoteManager getVoteManager() {
        return voteManager;
    }

    public MapManager getMapManager() {
        return mapManager;
    }

    public RoleBuffManager getBuffManager() {
        return buffManager;
    }

    public SidebarManager getSidebarManager() {
        return sidebarManager;
    }

    public SpectatorManager getSpectatorManager() {
        return spectatorManager;
    }

    public PlayerConnectionListener getConnectionListener() {
        return connectionListener;
    }
}