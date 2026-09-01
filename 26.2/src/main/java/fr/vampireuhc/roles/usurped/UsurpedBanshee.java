package fr.vampireuhc.roles.usurped;

import fr.vampireuhc.VampireUHC;
import fr.vampireuhc.config.MessageUtil;
import fr.vampireuhc.markers.MarkerManager;
import fr.vampireuhc.markers.TightAuraTier;
import fr.vampireuhc.player.VampireUHCPlayer;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

/**
 * Banshee usurpée (26.2) : cri exact, mais le message de début d'épisode n'est
 * envoyé qu'au Doppelganger (pas à tous les joueurs).
 */
public class UsurpedBanshee implements UsurpedPower {

    private VampireUHCPlayer sosie;

    @Override
    public String getName() {
        return "Banshee";
    }

    @Override
    public void onEnter(VampireUHCPlayer doppelganger) {
        this.sosie = doppelganger;
    }

    @Override
    public void onExit() {
    }

    @Override
    public void onEpisodeStart(int episode) {
        scream();
    }

    private void scream() {
        if (sosie == null || !sosie.isAlive()) {
            return;
        }

        Player bukkitBanshee = Bukkit.getPlayer(sosie.getUuid());
        if (bukkitBanshee == null) {
            return;
        }

        int darkAuras = scan(bukkitBanshee);

        if (darkAuras == 0) {
            return;
        } else if (darkAuras <= 2) {
            bukkitBanshee.sendMessage(MessageUtil.info("Vous <gold>pleurez</gold>..."));
        } else {
            bukkitBanshee.sendMessage(MessageUtil.info("Vous poussez un <gold>cri</gold> déchirant !"));
        }
    }

    private int scan(Player bukkitBanshee) {
        int result = 0;
        MarkerManager markerManager = VampireUHC.getInstance().getMarkerManager();

        for (Entity entity : bukkitBanshee.getNearbyEntities(50, 50, 50)) {
            if (entity instanceof Player player && player.getGameMode() == GameMode.SURVIVAL) {
                TightAuraTier aura = markerManager.computeAuraTier(player.getUniqueId()).getTight();
                if (aura == TightAuraTier.OBSCURE) {
                    result++;
                }
            }
        }
        return result;
    }
}