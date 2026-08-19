package fr.vampireuhc.roles;

import fr.vampireuhc.VampireUHC;
import fr.vampireuhc.config.ConfigManager;
import fr.vampireuhc.markers.MarkerManager;
import fr.vampireuhc.player.VampireUHCPlayer;
import fr.vampireuhc.player.Camp;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;

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
    public Component getDescription() {
        ConfigManager config = VampireUHC.getInstance().getConfigManager();
        int pvpAt = config.getPvpActivationAt();
        int weaknessThreshold = config.getMarksToRemoveWeakness();
        int strengthThreshold = config.getMarksForNightStrength();
        MiniMessage mm = MiniMessage.miniMessage();
        return mm.deserialize(
            "<gray>Vous êtes un sbire vampire. En début de partie, vous subissez une <red>faiblesse pendant le jour</red>.</gray>\n\n"
            + "<dark_purple>▸</dark_purple> <gray>À <yellow>" + pvpAt + " minutes</yellow>, vous découvrez vos alliés et pouvez voter pour marquer des joueurs.</gray>\n"
            + "<dark_purple>▸</dark_purple> <gray>Commande : <gold>/vuhc voter <joueur></gold></gray>\n\n"
            + "<bold><dark_purple>Progression :</dark_purple></bold>\n"
            + "  <gray>• <yellow>" + weaknessThreshold + " joueurs marqués</yellow> → Vous perdez la faiblesse de jour.</gray>\n"
            + "  <gray>• <yellow>" + strengthThreshold + " joueurs marqués</yellow> → Vous gagnez <green>force la nuit</green>.</gray>\n\n"
            + "<gray>En cas d'égalité lors du vote, le Maître tranche.</gray>"
        );
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
