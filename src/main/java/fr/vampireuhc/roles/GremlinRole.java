package fr.vampireuhc.roles;

import fr.vampireuhc.player.VampireUHCPlayer;
import fr.vampireuhc.markers.MarkerManager;
import fr.vampireuhc.player.Camp;


public class GremlinRole implements Role {
    private VampireUHCPlayer gremlin;
    private boolean applied_this_episode;
    private int episode;

    public GremlinRole(VampireUHCPlayer player) {
        this.gremlin = player;
    }

    @Override
    public boolean isVampire() {
        return false;
    }

    @Override
    public String getDescription() {
        // TODO
        return "";
    }

    @Override
    public String getName() {
        return "Gremlin";
    }

    @Override
    public Camp getDefaultCamp() {
        return Camp.SOLO;
    }

    @Override
    public void onAssign(VampireUHCPlayer vampireUHCPlayer) {
        this.gremlin = vampireUHCPlayer;
    }

    // Pouvoirs spécifiques au rôle :
    
    public boolean SwitchMarkers(MarkerManager manager, VampireUHCPlayer target_1, VampireUHCPlayer target_2, int current_episode) {
        if (gremlin == null) {
            return false;
        }
        // En gros, s'il a déjà intervertit cet épisode, ça ne marche pas.
        if (episode == current_episode || applied_this_episode == true) {
            return false;
        }
        this.episode = current_episode; // On met à jour l'épisode.

        // On échange les marqueurs
        manager.SwitchMarkers(target_1.getUuid(), target_2.getUuid());
        
        this.applied_this_episode = true;
        return true;
    }

    // Todo ajouter un pouvoir pour le pvp
}
