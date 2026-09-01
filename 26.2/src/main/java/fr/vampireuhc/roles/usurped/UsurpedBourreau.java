package fr.vampireuhc.roles.usurped;

import com.google.gson.JsonObject;

import fr.vampireuhc.player.VampireUHCPlayer;

/**
 * Bourreau copié par le Sosie : uniquement le bonus actif (premier coup de
 * l'épisode +50%), sans le kit (livre Sharpness II).
 */
public class UsurpedBourreau implements UsurpedPower {

    private VampireUHCPlayer sosie;
    // Gate "premier coup de l'épisode" : épisode du dernier premier coup (-1 = jamais).
    private int lastFirstHitEpisode = -1;

    @Override
    public String getName() {
        return "Bourreau";
    }

    @Override
    public void onEnter(VampireUHCPlayer doppelganger) {
        this.sosie = doppelganger;
    }

    @Override
    public void onExit() {
    }

    // Consomme le bonus premier-coup de l'épisode. Renvoie true si ce coup
    // bénéficie du bonus (+50%).
    public boolean tryApplyFirstHitBonus(int currentEpisode) {
        if (lastFirstHitEpisode == currentEpisode) {
            return false;
        }
        lastFirstHitEpisode = currentEpisode;
        return true;
    }

    @Override
    public void saveState(JsonObject obj) {
        obj.addProperty("usurpedBourreauLastFirstHitEpisode", lastFirstHitEpisode);
    }

    @Override
    public void restoreState(JsonObject obj) {
        lastFirstHitEpisode = obj.has("usurpedBourreauLastFirstHitEpisode")
                ? obj.get("usurpedBourreauLastFirstHitEpisode").getAsInt() : -1;
    }
}