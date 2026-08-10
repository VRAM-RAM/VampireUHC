package fr.vampireuhc.roles;

import fr.vampireuhc.player.VampireUHCPlayer;
import fr.vampireuhc.player.Camp;

import java.util.UUID;

import fr.vampireuhc.markers.MarkerManager;
import fr.vampireuhc.markers.MarkerType;



public class SaviorRole implements Role {
    private VampireUHCPlayer salva;
    private boolean applied_this_episode;
    private int episode;
    private UUID last_applied_Uuid;

    public SaviorRole(VampireUHCPlayer player) {
        this.salva = player;
    }

    @Override
    public boolean isVampire() {
        return false;
    }

    @Override
    public String getDescription() {
        return "todo!";
    }

    @Override
    public String getName() {
        return "Salvateur";
    }

    @Override
    public Camp getDefaultCamp() {
        return Camp.VILLAGEOIS;
    }

    @Override
    public void onAssign(VampireUHCPlayer vampireUHCPlayer) {
        this.salva = vampireUHCPlayer;
    }

    // Pouvoir spécifique au Salvateur : la marque de la salvation

    public boolean applySalvation(MarkerManager manager, VampireUHCPlayer target, int current_episode) {
        if (salva == null) {
            return false;
        } 

        if (episode == current_episode || applied_this_episode == true) {
            return false;
        }
        this.episode = current_episode;

        // On clear l'ancien marqueur salvation
        manager.clearMarkersOfType(last_applied_Uuid, MarkerType.SALVATION);


        manager.addMarker(target.getUuid(), MarkerType.SALVATION, salva.getUuid());
        
        this.last_applied_Uuid = target.getUuid();
        this.applied_this_episode = true;
        return true;
    }
}
