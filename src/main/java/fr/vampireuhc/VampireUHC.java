package fr.vampireuhc;

import fr.vampireuhc.commands.VUHCCommand;
import fr.vampireuhc.config.ConfigManager;
import fr.vampireuhc.game.GameManager;
import fr.vampireuhc.listeners.PvPListener;
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

        getServer().getPluginManager().registerEvents(new PvPListener(gameManager), this);

        VUHCCommand command = new VUHCCommand(gameManager, playerManager, markerManager, roleManager, voteManager);
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
}