package fr.vampireuhc;

import fr.vampireuhc.commands.VUHCCommand;
import fr.vampireuhc.config.ConfigManager;
import fr.vampireuhc.game.GameManager;
import fr.vampireuhc.game.CrossTracker;
import fr.vampireuhc.game.MapManager;
import fr.vampireuhc.game.RoleBuffManager;
import fr.vampireuhc.game.SidebarManager;
import fr.vampireuhc.game.SpectatorManager;
import fr.vampireuhc.groups.GroupsManager;
import fr.vampireuhc.listeners.ChatListener;
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
    private CrossTracker crossTracker;
    private MapManager mapManager;
    private RoleBuffManager buffManager;
    private SidebarManager sidebarManager;
    private SpectatorManager spectatorManager;
    private PlayerConnectionListener connectionListener;
    private RulesListener rulesListener;
    private GroupsManager groupsManager;

    @Override
    public void onEnable() {
        instance = this;
        getLogger().info("VampireUHC plugin activated.");
        saveDefaultConfig();
        
        this.configManager = new ConfigManager(this);
        this.playerManager = new PlayerManager(this);
        this.markerManager = new MarkerManager(this);
        this.gameManager = new GameManager(this, playerManager, markerManager, configManager);
        
        this.roleManager = new RoleManager(this, playerManager);

        this.voteManager = new VampireVoteManager(this);
        this.crossTracker = new CrossTracker(this);
        this.mapManager = new MapManager(this);
        this.buffManager = new RoleBuffManager(this);
        this.sidebarManager = new SidebarManager(this);
        this.spectatorManager = new SpectatorManager(this);
        this.connectionListener = new PlayerConnectionListener(this);
        this.rulesListener = new RulesListener(this);
        this.groupsManager = new GroupsManager(this, playerManager, markerManager, configManager);

        getServer().getPluginManager().registerEvents(new PvPListener(gameManager), this);
        getServer().getPluginManager().registerEvents(new PlayerDeathListener(this), this);
        getServer().getPluginManager().registerEvents(new GameplayListener(this), this);
        getServer().getPluginManager().registerEvents(mapManager, this);
        getServer().getPluginManager().registerEvents(spectatorManager, this);
        getServer().getPluginManager().registerEvents(connectionListener, this);
        getServer().getPluginManager().registerEvents(rulesListener, this);
        getServer().getPluginManager().registerEvents(new ChatListener(this), this);

        VUHCCommand command = new VUHCCommand(gameManager, playerManager, markerManager, roleManager, voteManager, spectatorManager);
        getCommand("vuhc").setExecutor(command);
        getCommand("vuhc").setTabCompleter(command);

        // Reprise d'une partie en cours après un redémarrage du serveur.
        RoleManager.LoadedGameState state = roleManager.loadGameFromJson(
                getDataFolder().getAbsolutePath() + File.separator + "game-state.json");
        if (state != null) {
            gameManager.restoreGame(state.phase(), state.elapsedMinutes());
        }

        getLogger().info("VampireUHC activé !");
    }

    @Override
    public void onDisable() {
        // Sauvegarder l'état du jeu AVANT l'arrêt (sinon la phase serait marquée ENDED).
        if (roleManager != null && playerManager != null && playerManager.getAll().iterator().hasNext()) {
            roleManager.saveGameToJson(getDataFolder().getAbsolutePath() + File.separator + "game-state.json");
        }

        if (gameManager != null) {
            gameManager.stop();
        }

        // TODO(1.8.9) : substitut du glow Archer (pas de glowing avant 1.9).
        // Candidat : éclair visuel sans dégâts (strikeLightningEffect) au toucher.

        getLogger().info("VampireUHC desactivé !");
    }

    public static VampireUHC getInstance() {
        return instance;
    }

    public GroupsManager getGroupsManager() {
        return groupsManager;
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

    public CrossTracker getCrossTracker() {
        return crossTracker;
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