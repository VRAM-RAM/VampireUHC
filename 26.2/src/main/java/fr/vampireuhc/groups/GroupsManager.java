package fr.vampireuhc.groups;

import fr.vampireuhc.VampireUHC;
import fr.vampireuhc.config.ConfigManager;
import fr.vampireuhc.player.PlayerManager;
import fr.vampireuhc.markers.MarkerManager;

public class GroupsManager {
    private final VampireUHC plugin;
    private final PlayerManager playerManager;



    public GroupsManager(VampireUHC plugin, PlayerManager playerManager, MarkerManager markerManager, ConfigManager configManager) {
        this.plugin = plugin;
        this.playerManager = playerManager;
    }

    public int getPeopleByGroups() {
        int vampires = playerManager.getNumberOfVampires();

        if (vampires == 0) {
            return 5;
        }
        
        if (vampires > 5) {
            return 5;
        }

        if (vampires == 4) {
            return 4;
        }

        return 3;
    }
}