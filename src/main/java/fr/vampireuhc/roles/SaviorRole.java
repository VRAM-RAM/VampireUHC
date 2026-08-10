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
        return "Le Salvateur est un rôle villageois. À chaque épisode, il place une marque Salvation sur un joueur. Cette marque possède une aura lumineuse. Si le joueur ciblé par un vampire ou le Maître possède la Salvation, la marque n'est pas appliquée et la marque Salvation disparaît, mais les vampires pensent quand même que leur action a fonctionné. Vous créez donc de fausses informations, tout en protégeant ceux que vous pensez être safe. Vous ne pouvez pas protéger le même joueur deux épisodes consécutifs.";
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

        // On ne peut pas protéger le même joueur deux épisodes consécutifs
        if (last_applied_Uuid != null && last_applied_Uuid.equals(target.getUuid())) {
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
