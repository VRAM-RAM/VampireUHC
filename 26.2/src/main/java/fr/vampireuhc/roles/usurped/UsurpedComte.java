package fr.vampireuhc.roles.usurped;

import fr.vampireuhc.VampireUHC;
import fr.vampireuhc.config.MessageUtil;
import fr.vampireuhc.markers.AuraTier;
import fr.vampireuhc.markers.MarkerManager;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

/**
 * Comte usurpé (26.2) : le Sosie gagne le pouvoir passif du Comte
 * (dénombrement des auras lumineuses dans un rayon de 50 blocs à chaque début
 * d'épisode, en message privé) ainsi que tout le pouvoir du Sbire usurpé
 * (buffs vampires + vote). Comme pour le Sbire, il ne connaît ni le résultat
 * du vote ni les autres vampires.
 */
public class UsurpedComte extends UsurpedVampire {

    @Override
    public String getName() {
        return "Comte";
    }

    @Override
    public void onEpisodeStart(int episode) {
        scanLuminousAuras();
    }

    private void scanLuminousAuras() {
        if (sosie == null || !sosie.isAlive()) {
            return;
        }

        Player bukkitComte = Bukkit.getPlayer(sosie.getUuid());
        if (bukkitComte == null) {
            return;
        }

        MarkerManager markerManager = VampireUHC.getInstance().getMarkerManager();
        int luminous = 0;

        for (Entity entity : bukkitComte.getNearbyEntities(50, 50, 50)) {
            if (entity instanceof Player player && player.getGameMode() == GameMode.SURVIVAL) {
                AuraTier aura = markerManager.computeAuraTier(player.getUniqueId());
                if (aura == AuraTier.LUMINEUSE || aura == AuraTier.TRES_LUMINEUSE) {
                    luminous++;
                }
            }
        }

        bukkitComte.sendMessage(MessageUtil.info(
                "<gold>Il y a <white>" + luminous + "</white>aura lumineuses autour de vous.</gold>"));
    }
}