package fr.vampireuhc.roles.usurped;

import com.google.gson.JsonObject;

import fr.vampireuhc.player.VampireUHCPlayer;

/**
 * Bourreau copié par le Sosie : le passif (50% de dégâts supplémentaires sur
 * le premier coup de chaque épisode) est identique, mais sans l'épée
 * enchantée. Le bonus est décidé dans PvPListener via tryApplyFirstHitBonus.
 */
public class UsurpedBourreau implements UsurpedPower {

    private int lastFirstHitEpisode = -1;

    @Override
    public String getName() {
        return "Bourreau";
    }

    @Override
    public void onEnter(VampireUHCPlayer doppelganger) {
    }

    @Override
    public void onExit() {
    }

    // Vrai si le bonus de premier coup peut s'appliquer à l'épisode courant
    // (un seul premier coup par épisode).
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