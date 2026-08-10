package fr.vampireuhc.roles;

import fr.vampireuhc.VampireUHC;
import fr.vampireuhc.config.ConfigManager;
import fr.vampireuhc.markers.MarkerType;
import fr.vampireuhc.markers.MarkerManager;
import fr.vampireuhc.player.VampireUHCPlayer;
import fr.vampireuhc.player.Camp;
import java.util.UUID;

import org.bukkit.ChatColor;

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
        ConfigManager config = VampireUHC.getInstance().getConfigManager();
        int pvpAt = config.getPvpActivationAt();
        int weaknessThreshold = config.getMarksToRemoveWeakness();
        int strengthThreshold = config.getMarksForNightStrength();
        return "Vous êtes un Sbire vampire. En début de partie, vous subissez une faiblesse pendant le jour. À "
            + ChatColor.DARK_PURPLE + pvpAt + ChatColor.GRAY
            + " minutes, vous découvrez la liste de vos alliés et pouvez voter pour attribuer des marques vampires (/vuhc voter <joueur>). Vos pouvoirs augmentent avec le nombre de joueurs marqués : à partir de "
            + ChatColor.DARK_PURPLE + weaknessThreshold + ChatColor.GRAY
            + " joueurs marqués vous perdez votre faiblesse, et à partir de "
            + ChatColor.DARK_PURPLE + strengthThreshold + ChatColor.GRAY
            + " joueurs marqués vous gagnez de la force la nuit. En cas d'égalité lors du vote, c'est le Maître qui tranche.";
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
