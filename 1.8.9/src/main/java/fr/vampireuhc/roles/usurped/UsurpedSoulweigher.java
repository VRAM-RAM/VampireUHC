package fr.vampireuhc.roles.usurped;

import com.google.gson.JsonObject;
import fr.vampireuhc.VampireUHC;
import fr.vampireuhc.config.MessageUtil;
import fr.vampireuhc.markers.AuraTier;
import fr.vampireuhc.markers.MarkerManager;
import fr.vampireuhc.player.VampireUHCPlayer;

/**
 * Peseuse d'âmes usurpée : pouvoir exact (pas de nerf), seule la gate
 * "une fois par épisode" est propre au Sosie (compteurs indépendants).
 */
public class UsurpedSoulweigher implements UsurpedPower {

    private VampireUHCPlayer sosie;

    private int lastWeightEpisode = -1;

    @Override
    public String getName() {
        return "Peseuse d'âmes";
    }

    @Override
    public void onEnter(VampireUHCPlayer doppelganger) {
        this.sosie = doppelganger;
    }

    @Override
    public void onExit() {
        // Pas de tâche planifiée à nettoyer pour l'instant.
    }

    public boolean weightAura(MarkerManager markerManager, VampireUHCPlayer targetOne, VampireUHCPlayer targetTwo, int currentEpisode) {
        if (sosie == null) {
            return false;
        }
        if (lastWeightEpisode == currentEpisode) {
            org.bukkit.entity.Player bukkitWeighter = VampireUHC.getInstance().getServer().getPlayer(sosie.getUuid());
            if (bukkitWeighter != null) {
                bukkitWeighter.sendMessage(MessageUtil.error("Vous avez déjà pesé des âmes cet épisode."));
            }
            return false;
        }

        this.lastWeightEpisode = currentEpisode;

        AuraTier auraOfFirstTarget = markerManager.computeAuraTier(targetOne.getUuid());
        AuraTier auraOfSecondTarget = markerManager.computeAuraTier(targetTwo.getUuid());

        org.bukkit.entity.Player bukkitPlayer = VampireUHC.getInstance().getServer().getPlayer(sosie.getUuid());
        if (bukkitPlayer == null) {
            return true;
        }
        bukkitPlayer.sendMessage(
                auraOfFirstTarget == auraOfSecondTarget
                        ? MessageUtil.success("La balance s'équilibre...")
                        : auraOfFirstTarget.getTight() == auraOfSecondTarget.getTight()
                                ? MessageUtil.warn("La balance penche légèrement...")
                                : MessageUtil.error("La balance penche..."));
        return true;
    }

    @Override
    public void saveState(JsonObject obj) {
        obj.addProperty("usurpedSoulLastEpisode", lastWeightEpisode);
    }

    @Override
    public void restoreState(JsonObject obj) {
        if (obj.has("usurpedSoulLastEpisode")) {
            lastWeightEpisode = obj.get("usurpedSoulLastEpisode").getAsInt();
        }
    }

    public int getLastWeightEpisode() {
        return lastWeightEpisode;
    }
}