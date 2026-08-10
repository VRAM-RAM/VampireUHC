package fr.vampireuhc.roles;

import fr.vampireuhc.roles.RoleType;
import fr.vampireuhc.markers.MarkerType;
import fr.vampireuhc.markers.MarkerManager;
import fr.vampireuhc.player.VampireUHCPlayer;
import fr.vampireuhc.player.Camp;
import java.util.UUID;

public class PaladinRole implements Role {
    private VampireUHCPlayer paladin;
    
    @Override
    public boolean isVampire() {
        return false;
    }

    public PaladinRole(VampireUHCPlayer player) {
        this.paladin = player;
    }

    @Override
    public String getDescription() {
        return "todo!";
    }

    @Override
    public String getName() {
        return "Paladin";
    }

    @Override
    public Camp getDefaultCamp() {
        return Camp.VILLAGEOIS;
    }

    @Override
    public void onAssign(VampireUHCPlayer vampireUHCPlayer) {
        this.paladin = vampireUHCPlayer;
    }

    // Pouvoir actif spécifique au paladin : lorsqu'il tue un joueur vampire, il gagne un marqueur lumineux

    public boolean gainLuminousMarkerOnKill(MarkerManager markerManager, VampireUHCPlayer killed) {
        if (paladin == null || !killed.getRole().isVampire()) {
            return false;
        }
        markerManager.addMarker(paladin.getUuid(), MarkerType.LUMINEUX, paladin.getUuid());
        return true;
    }   
}
