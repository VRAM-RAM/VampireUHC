package fr.vampireuhc.roles.usurped;

import fr.vampireuhc.VampireUHC;
import fr.vampireuhc.config.ConfigManager;
import fr.vampireuhc.markers.Aura;
import fr.vampireuhc.markers.Marker;
import fr.vampireuhc.markers.MarkerManager;
import fr.vampireuhc.markers.MarkerType;
import fr.vampireuhc.player.VampireUHCPlayer;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/**
 * Apprentie Assassin usurpé : pouvoir imparfait.
 *  - Force de nuit/de jour nettement affaiblie : rien sous le seuil doublé
 *    (nX = 2 * seuil config), puis une force légère. Pas de régénération.
 *  - Le vol des marqueurs à la mort de la proie est copié exactement (tous les
 *    marqueurs de la cible sauf MARQUE_MAITRE/AMOUR/FIL).
 */
public class UsurpedSlayer implements UsurpedPower {

    private VampireUHCPlayer sosie;

    @Override
    public String getName() {
        return "Apprentie Assassin";
    }

    @Override
    public void onEnter(VampireUHCPlayer doppelganger) {
        this.sosie = doppelganger;
    }

    @Override
    public void onExit() {
        if (sosie != null) {
            Player p = VampireUHC.getInstance().getServer().getPlayer(sosie.getUuid());
            if (p != null) {
                p.removePotionEffect(PotionEffectType.INCREASE_DAMAGE);
                p.removePotionEffect(PotionEffectType.REGENERATION);
            }
        }
    }

    // Vol des marqueurs de la proie tuée (identiques au vrai Assassin).
    public void copyMarkersOnKill(MarkerManager markerManager, VampireUHCPlayer killed) {
        if (sosie == null || killed == null) {
            return;
        }

        List<Marker> stolen = new ArrayList<>();
        for (Marker marker : markerManager.getMarkers(killed.getUuid())) {
            if (marker.getType() == MarkerType.MARQUE_MAITRE
                    || marker.getType() == MarkerType.AMOUR
                    || marker.getType() == MarkerType.FIL) {
                continue;
            }
            stolen.add(marker);
        }
        if (stolen.isEmpty()) {
            return;
        }

        markerManager.addMarkers(sosie.getUuid(), stolen);

        Player bukkitSlayer = VampireUHC.getInstance().getServer().getPlayer(sosie.getUuid());
        if (bukkitSlayer != null) {
            bukkitSlayer.sendMessage("Vous récupérez les marqueurs de " + killed.getLastKnownName() + ".");
        }
    }

    // Effets de marqueurs nerf : le seuil est doublé (nX) et seuls les bonus de
    // force légère s'appliquent (pas de régénération ni de bonus marqués).
    public void applyMarkerEffects(Player p, boolean night, int darkCount, int lightCount) {
        ConfigManager config = VampireUHC.getInstance().getConfigManager();

        boolean darkForce = darkCount >= 2 * config.getSlayerDarkThreshold();
        boolean lightForce = lightCount >= 2 * config.getSlayerLightThreshold();

        boolean force = (night && darkForce) || (!night && lightForce);

        if (force) {
            p.addPotionEffect(effect(PotionEffectType.INCREASE_DAMAGE, 0));
        } else {
            p.removePotionEffect(PotionEffectType.INCREASE_DAMAGE);
        }
        p.removePotionEffect(PotionEffectType.REGENERATION);
    }

    public int countDarkMarkers(MarkerManager manager) {
        return countByAura(manager, Aura.OBSCURE);
    }

    public int countLightMarkers(MarkerManager manager) {
        return countByAura(manager, Aura.LUMINEUSE);
    }

    private int countByAura(MarkerManager manager, Aura aura) {
        int count = 0;
        for (Marker marker : manager.getMarkers(sosie.getUuid())) {
            if (marker.getAura() == aura) {
                count++;
            }
        }
        return count;
    }

    // Effet invisible, sans particules et sans icône.
    private PotionEffect effect(PotionEffectType type, int amplifier) {
        return new PotionEffect(type, 20 * 95, amplifier, true, false);
    }
}