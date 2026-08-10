package fr.vampireuhc.roles;
import fr.vampireuhc.markers.MarkerType;
import fr.vampireuhc.markers.MarkerManager;
import fr.vampireuhc.player.VampireUHCPlayer;
import fr.vampireuhc.player.Camp;
import java.util.UUID;

public class ApprenticeSlayer implements Role {
    private VampireUHCPlayer slayer;


    public ApprenticeSlayer(VampireUHCPlayer player) {
        this.slayer = player;
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
        return "Apprenti Chasseur";
    }

    @Override
    public Camp getDefaultCamp() {
        return Camp.SOLO;
    }

    @Override
    public void onAssign(VampireUHCPlayer vampireUHCPlayer) {
        this.slayer = vampireUHCPlayer;
    }

    // Pouvoirs spécifiques au rôle. 

    /*
    Pour l'apprentie assassin, j'aimerai deux pouvoirs :
    - à chaque kill, il reçoit les marqueurs du joueur tué
    - plus il porte un nombre de marqueurs conséquents, plus il obtient de pouvoir :
        > Plus de X marqueurs obscurs --> Force légère la nuit
        > Plus de X marqueurs lumineux --> Force légère le jour
        > Plus de nX marqueurs obscurs --> Force légère la nuit & Régénération naturelle d'un demi-coeur par minute la nuit.
        > Plus de nX marqueurs lumineux --> Force légère le jour & Régénération naturelle d'un demi-coeur par minute le jour.
    
    Donc en fin de game, potentiellement T4 + Force perma + regen lente.
    */
    public boolean CopyMarkersOnKill(MarkerManager manager, VampireUHCPlayer killed) {
        if (slayer == null) {
            return false;
        }
        var markers = manager.getMarkers(killed.getUuid());
        manager.addMarkers(slayer.getUuid(), markers);
        return true;
    }   
}
