package fr.vampireuhc.roles;

import fr.vampireuhc.markers.MarkerType;
import fr.vampireuhc.markers.MarkerManager;
import fr.vampireuhc.player.VampireUHCPlayer;
import fr.vampireuhc.player.Camp;
import java.util.UUID;

public class VampireMinion implements Role {
    private VampireUHCPlayer minion;

    

    public VampireMinion(VampireUHCPlayer player) {
        this.minion = player;
    }

    @Override
    public boolean isVampire() {
        return true;
    }

    @Override
    public String getDescription() {
        return "todo!";
    }

    @Override
    public String getName() {
        return "Sbire";
    }

    @Override
    public Camp getDefaultCamp() {
        return Camp.VAMPIRE;
    }

    @Override
    public void onAssign(VampireUHCPlayer vampireUHCPlayer) {
        this.minion = vampireUHCPlayer;
    }

    // Pouvoir spécial : le vote pour la marque de vampire :

    public boolean voteForVampireMark(MarkerManager manager, VampireUHCPlayer target) {
        // Le vampire ne peut pas marquer un autre vampire
        if (minion == null || target.getRole().isVampire()) {
            return false;
        }
        return true;
    }
}
