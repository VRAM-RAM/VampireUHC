package fr.vampireuhc.game;

import fr.vampireuhc.VampireUHC;
import fr.vampireuhc.markers.AuraTier;
import fr.vampireuhc.markers.MarkerManager;
import fr.vampireuhc.player.Camp;
import fr.vampireuhc.player.VampireUHCPlayer;
import fr.vampireuhc.roles.ApprenticeSlayer;
import fr.vampireuhc.roles.PaladinRole;
import fr.vampireuhc.roles.WhiteLadyRole;

import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/**
 * Applique chaque minute les effets passifs liés à l'aura (Paladin),
 * aux marques (Apprentie assassin) et au camp vampire (faiblesse/force).
 * Tous les effets sont invisibles et sans particules.
 */
public class RoleBuffManager {
    private final VampireUHC plugin;

    public RoleBuffManager(VampireUHC plugin) {
        this.plugin = plugin;
    }

    public void applyBuffs() {
        for (VampireUHCPlayer vp : plugin.getPlayerManager().getAll()) {
            if (!vp.isAlive() || vp.getCamp() == null) {
                continue;
            }
            Player p = org.bukkit.Bukkit.getPlayer(vp.getUuid());
            if (p == null || !p.isOnline()) {
                continue;
            }

            applyVampireBuffs(p, vp);
            applyRoleBuffs(p, vp);
        }
    }

    private void applyRoleBuffs(Player p, VampireUHCPlayer vp) {
        MarkerManager markers = plugin.getMarkerManager();

        if (vp.getRole() instanceof PaladinRole paladin) {
            AuraTier tier = markers.computeAuraTier(vp.getUuid());
            paladin.applyAuraEffects(p, tier);
        } else if (vp.getRole() instanceof ApprenticeSlayer slayer) {
            boolean night = !p.getWorld().isDayTime();
            slayer.applyMarkerEffects(p, night,
                    slayer.countDarkMarkers(markers),
                    slayer.countLightMarkers(markers));
        } else if (vp.getRole() instanceof WhiteLadyRole whiteLady) {
            boolean night = !p.getWorld().isDayTime();
            whiteLady.applyEffects(p, night);
        }
    }

    private void applyVampireBuffs(Player p, VampireUHCPlayer vp) {
        if (vp.getCamp() != Camp.VAMPIRE) {
            return;
        }

        var config = plugin.getConfigManager();
        int markedCount = plugin.getVoteManager().getMarkedPlayerCount();

        boolean day = p.getWorld().isDayTime();
        boolean weakness = config.isDayWeaknessEnabled() && day
                && markedCount < config.getMarksToRemoveWeakness();
        boolean strength = !day && markedCount >= config.getMarksForNightStrength();

        if (weakness) {
            p.addPotionEffect(effect(PotionEffectType.WEAKNESS, 0));
        } else {
            p.removePotionEffect(PotionEffectType.WEAKNESS);
        }

        if (strength) {
            p.addPotionEffect(effect(PotionEffectType.STRENGTH, 0));
        } else {
            p.removePotionEffect(PotionEffectType.STRENGTH);
        }
    }

    private PotionEffect effect(PotionEffectType type, int amplifier) {
        return new PotionEffect(type, 20 * 95, amplifier, true, false, false);
    }
}
