package fr.vampireuhc.roles.usurped;

import java.util.UUID;

import com.google.gson.JsonObject;

import fr.vampireuhc.markers.MarkerManager;
import fr.vampireuhc.markers.MarkerType;
import fr.vampireuhc.player.VampireUHCPlayer;

/**
 * Salvateur copié par le Sosie : la pose de la marque est identique au vrai
 * Salvateur, mais le blocage d'une marque maître/vampire n'est que partiel
 * (50%) — la décision est prise dans MarkerManager.tryApplyMark sur la
 * variante SALVATION_DOPPELGANGER.
 */
public class UsurpedSavior implements UsurpedPower {

    private VampireUHCPlayer sosie;
    private int lastAppliedEpisode = -1;
    private UUID lastAppliedUuid;

    @Override
    public String getName() {
        return "Salvateur";
    }

    @Override
    public void onEnter(VampireUHCPlayer doppelganger) {
        this.sosie = doppelganger;
    }

    @Override
    public void onExit() {
    }

    public boolean applySalvation(MarkerManager manager, VampireUHCPlayer target, int current_episode) {
        if (sosie == null) {
            return false;
        }

        if (lastAppliedEpisode == current_episode) {
            return false;
        }

        if (lastAppliedUuid != null && lastAppliedUuid.equals(target.getUuid())) {
            return false;
        }

        this.lastAppliedEpisode = current_episode;

        if (lastAppliedUuid != null) {
            manager.clearMarkersOfType(lastAppliedUuid, MarkerType.SALVATION_DOPPELGANGER);
        }

        manager.addMarker(target.getUuid(), MarkerType.SALVATION_DOPPELGANGER, sosie.getUuid());

        this.lastAppliedUuid = target.getUuid();
        return true;
    }

    @Override
    public void saveState(JsonObject obj) {
        obj.addProperty("usurpedSaviorLastEpisode", lastAppliedEpisode);
        if (lastAppliedUuid != null) {
            obj.addProperty("usurpedSaviorLastTarget", lastAppliedUuid.toString());
        }
    }

    @Override
    public void restoreState(JsonObject obj) {
        lastAppliedEpisode = obj.has("usurpedSaviorLastEpisode")
                ? obj.get("usurpedSaviorLastEpisode").getAsInt() : -1;
        if (obj.has("usurpedSaviorLastTarget")) {
            lastAppliedUuid = UUID.fromString(obj.get("usurpedSaviorLastTarget").getAsString());
        }
    }
}