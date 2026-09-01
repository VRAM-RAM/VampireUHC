package fr.vampireuhc.roles;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import fr.vampireuhc.VampireUHC;
import fr.vampireuhc.config.MessageUtil;
import fr.vampireuhc.markers.AuraTier;
import fr.vampireuhc.markers.MarkerManager;
import fr.vampireuhc.player.Camp;
import fr.vampireuhc.player.VampireUHCPlayer;

import net.kyori.adventure.text.Component;

/**
 * Comte : vampire. Comme le Sbire, il bénéficie des buffs vampire (faiblesse de
 * jour, force de nuit) et peut voter /vuhc voter <joueur>. Sa spécificité : à
 * chaque début d'épisode, il compte les auras lumineuses (LUMINEUSE ou
 * TRES_LUMINEUSE) dans un rayon de 50 blocs autour de lui et en est informé.
 */
public class ComteRole implements Role {
    private VampireUHCPlayer comte;

    @Override
    public Camp getDefaultCamp() {
        return Camp.VAMPIRE;
    }

    @Override
    public Component getDescription() {
        return net.kyori.adventure.text.minimessage.MiniMessage.miniMessage().deserialize(
            "<gray>Vous êtes un vampire de sang noble. Comme le Sbire, vous subissez une <red>faiblesse pendant le jour</red> et gagnez de la <green>force la nuit</green>.</gray>\n\n"
            + "<dark_purple>▸</dark_purple> <gray>À <yellow>20 minutes</yellow>, vous découvrez vos alliés et pouvez voter pour marquer des joueurs : <gold>/vuhc voter <joueur></gold></gray>\n\n"
            + "<bold><dark_purple>Progression :</dark_purple></bold>\n"
            + "  <gray>• <yellow>2 joueurs marqués</yellow> → Vous perdez la faiblesse de jour.</gray>\n"
            + "  <gray>• <yellow>4 joueurs marqués</yellow> → Vous gagnez <green>force la nuit</green>.</gray>\n\n"
            + "<bold><dark_purple>Spécificité :</dark_purple></bold> <gray>à chaque <yellow>début d'épisode</yellow>, vous découvrez le nombre d'auras <green>lumineuses</green> (ou très lumineuses) dans un rayon de <italic>50</italic> blocs autour de vous.</gray>"
        );
    }

    @Override
    public String getName() {
        return "Comte";
    }

    @Override
    public boolean isVampire() {
        return true;
    }

    @Override
    public void onAssign(VampireUHCPlayer player) {
        this.comte = player;
    }

    @Override
    public void onEpisodeStart(int episode) {
        scanLuminousAuras();
    }

    private void scanLuminousAuras() {
        if (comte == null || !comte.isAlive()) {
            return;
        }

        Player bukkitComte = Bukkit.getPlayer(comte.getUuid());
        if (bukkitComte == null) {
            return;
        }

        MarkerManager markerManager = VampireUHC.getInstance().getMarkerManager();
        int luminous = 0;

        for (Entity entity : bukkitComte.getNearbyEntities(50, 50, 50)) {
            if (entity instanceof Player player) {
                if (player.getGameMode() == GameMode.SURVIVAL) {
                    AuraTier aura = markerManager.computeAuraTier(player.getUniqueId());
                    if (aura == AuraTier.LUMINEUSE || aura == AuraTier.TRES_LUMINEUSE) {
                        luminous++;
                    }
                }
            }
        }

        bukkitComte.sendMessage(MessageUtil.info("<gold>Il y a <white>" + luminous + "</white>aura lumineuses autour de vous.</gold>"));
    }
}