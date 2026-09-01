package fr.vampireuhc.roles.usurped;

import com.google.gson.JsonObject;
import fr.vampireuhc.VampireUHC;
import fr.vampireuhc.config.MessageUtil;
import fr.vampireuhc.markers.MarkerManager;
import fr.vampireuhc.markers.MarkerType;
import fr.vampireuhc.player.VampireUHCPlayer;

import org.bukkit.entity.Player;

/**
 * Maître Vampire usurpé : le Sosie copie le marquage du Maître (une fois par
 * épisode, sur un joueur croisé l'épisode précédent), SANS le malus de 8 cœurs
 * et SANS l'infection. Les marques du Sosie sont neutres (variante
 * MARQUE_MAITRE_DOPPELGANGER) : aucune influence sur l'aura, jamais infectées.
 * Gate épisode propre au Sosie (compteurs indépendants).
 */
public class UsurpedMaster implements UsurpedPower {

    private VampireUHCPlayer sosie;

    private int lastMarkedEpisode = -1;

    @Override
    public String getName() {
        return "Maître";
    }

    @Override
    public void onEnter(VampireUHCPlayer doppelganger) {
        this.sosie = doppelganger;
    }

    @Override
    public void onExit() {
        // Les marques posées sont estompées par le Doppelganger lui-même
        // (removeMarkersBySource) à la mort de l'usurpé.
    }

    public boolean markPlayer(MarkerManager markerManager, VampireUHCPlayer target, int currentEpisode) {
        if (sosie == null || target == null || !target.isAlive()) {
            return false;
        }
        if (lastMarkedEpisode == currentEpisode) {
            return false;
        }

        // La charge est consommée même si l'action échoue ailleurs.
        this.lastMarkedEpisode = currentEpisode;

        // Marque neutre du Sosie : ni infection, ni obscurcissement d'aura.
        markerManager.addMarker(target.getUuid(), MarkerType.MARQUE_MAITRE_DOPPELGANGER, sosie.getUuid());

        Player bukkitPlayer = VampireUHC.getInstance().getServer().getPlayer(sosie.getUuid());
        if (bukkitPlayer != null) {
            bukkitPlayer.sendMessage(MessageUtil.successTarget("Marque Maître posée sur", target.getLastKnownName()));
        }
        return true;
    }

    @Override
    public void saveState(JsonObject obj) {
        obj.addProperty("usurpedMasterLastEpisode", lastMarkedEpisode);
    }

    @Override
    public void restoreState(JsonObject obj) {
        if (obj.has("usurpedMasterLastEpisode")) {
            lastMarkedEpisode = obj.get("usurpedMasterLastEpisode").getAsInt();
        }
    }

    public int getLastMarkedEpisode() {
        return lastMarkedEpisode;
    }
}