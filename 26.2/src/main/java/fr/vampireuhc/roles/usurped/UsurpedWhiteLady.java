package fr.vampireuhc.roles.usurped;

import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import com.google.gson.JsonObject;

import fr.vampireuhc.player.Camp;
import fr.vampireuhc.player.VampireUHCPlayer;

/**
 * Dame Blanche copiée par le Sosie : la résurrection est calquée sur le vrai
 * rôle (camp du tueur — villageois : faiblesse de nuit, vampire : faiblesse
 * de jour, solitaire : aucune), mais elle ne s'applique qu'UNE seule fois.
 * La mort de la vraie Dame Blanche purge le malus de faiblesse du Sosie.
 */
public class UsurpedWhiteLady implements UsurpedPower {

    private VampireUHCPlayer sosie;
    private boolean resurrected;
    private boolean nightWeakness;
    private boolean dayWeakness;

    @Override
    public String getName() {
        return "Dame Blanche";
    }

    @Override
    public void onEnter(VampireUHCPlayer doppelganger) {
        this.sosie = doppelganger;
    }

    @Override
    public void onExit() {
    }

    // Vrai si le Sosie est ressuscité (une seule fois). Le camp détermine le
    // malus de faiblesse : villageois → de nuit, vampire → de jour.
    public boolean onDeath(VampireUHCPlayer killer) {
        if (sosie == null || resurrected || killer == null) {
            return false;
        }

        Camp camp = null;
        if (killer.getCamp() != null) {
            camp = killer.getCamp();
        } else if (killer.getRole() != null) {
            camp = killer.getRole().getDefaultCamp();
        }
        if (camp == null) {
            return false;
        }

        switch (camp) {
            case Camp.VILLAGEOIS:
                resurrected = true;
                nightWeakness = true;
                return true;
            case Camp.VAMPIRE:
                resurrected = true;
                dayWeakness = true;
                return true;
            case Camp.SOLO:
            default:
                return false;
        }
    }

    // La vraie Dame Blanche est morte : le Sosie perd son malus de faiblesse.
    public void onRealWhiteLadyDeath() {
        nightWeakness = false;
        dayWeakness = false;
    }

    // Effet passif : faiblesse de nuit (tuée par un villageois) ou faiblesse
    // de jour (tuée par un vampire). Appliqué par le chef des buffs.
    public void applyEffects(Player player, boolean night) {
        if (night && nightWeakness) {
            player.addPotionEffect(effect(PotionEffectType.WEAKNESS, 1));
        }
        if (!night && dayWeakness) {
            player.addPotionEffect(effect(PotionEffectType.WEAKNESS, 1));
        }
    }

    private PotionEffect effect(PotionEffectType type, int amplifier) {
        return new PotionEffect(type, 20 * 95, amplifier, true, false, true);
    }

    @Override
    public void saveState(JsonObject obj) {
        obj.addProperty("usurpedWhiteLadyResurrected", resurrected);
        obj.addProperty("usurpedWhiteLadyNightWeakness", nightWeakness);
        obj.addProperty("usurpedWhiteLadyDayWeakness", dayWeakness);
    }

    @Override
    public void restoreState(JsonObject obj) {
        resurrected = obj.has("usurpedWhiteLadyResurrected") && obj.get("usurpedWhiteLadyResurrected").getAsBoolean();
        nightWeakness = obj.has("usurpedWhiteLadyNightWeakness") && obj.get("usurpedWhiteLadyNightWeakness").getAsBoolean();
        dayWeakness = obj.has("usurpedWhiteLadyDayWeakness") && obj.get("usurpedWhiteLadyDayWeakness").getAsBoolean();
    }
}