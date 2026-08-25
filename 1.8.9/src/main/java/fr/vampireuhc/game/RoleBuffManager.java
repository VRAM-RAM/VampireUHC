package fr.vampireuhc.game;

import fr.vampireuhc.VampireUHC;
import fr.vampireuhc.config.ConfigManager;
import fr.vampireuhc.markers.AuraTier;
import fr.vampireuhc.markers.MarkerManager;
import fr.vampireuhc.player.Camp;
import fr.vampireuhc.player.VampireUHCPlayer;
import fr.vampireuhc.roles.ApprenticeSlayer;
import fr.vampireuhc.roles.MasterRole;
import fr.vampireuhc.roles.PaladinRole;
import fr.vampireuhc.roles.WhiteLadyRole;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/**
 * Applique chaque minute les effets passifs liés à l'aura (Paladin),
 * aux marques (Apprentie assassin) et au camp vampire (faiblesse/force).
 * Tous les effets sont invisibles et sans particules.
 *
 * Autorité unique sur le max health : formule centrale
 *   base (Maître réduit, sinon 20) + delta aura Paladin − pénalités Cupidon.
 * Aucun autre composant ne doit écrire le max health directement.
 */
public class RoleBuffManager {
    private final VampireUHC plugin;

    // Cœurs temporairement perdus par joueur (pénalité de deuil du lien d'amour).
    private final Map<UUID, Integer> heartPenalties = new HashMap<>();

    public RoleBuffManager(VampireUHC plugin) {
        this.plugin = plugin;
    }

    public void applyBuffs() {
        for (VampireUHCPlayer vp : plugin.getPlayerManager().getAll()) {
            if (!vp.isAlive() || vp.getCamp() == null) {
                continue;
            }
            Player p = Bukkit.getPlayer(vp.getUuid());
            if (p == null || !p.isOnline()) {
                continue;
            }

            applyVampireBuffs(p, vp);
            applyRoleBuffs(p, vp);
            // Recalcul idempotent du max health pour tous les vivants en ligne.
            applyMaxHealth(p, vp);
        }
    }

    // --- Autorité centrale du max health ---

    /** Déclare que le joueur perd temporairement {@code hearts} cœurs (remplace la valeur précédente). */
    public void registerHeartPenalty(UUID uuid, int hearts) {
        heartPenalties.put(uuid, hearts);
        refreshMaxHealth(uuid);
    }

    /** Termine la pénalité de cœurs du joueur et recalcule son max health. */
    public void clearHeartPenalty(UUID uuid) {
        if (heartPenalties.remove(uuid) != null) {
            refreshMaxHealth(uuid);
        }
    }

    /** Efface toutes les pénalités (fin de partie). */
    public void clearHeartPenalties() {
        heartPenalties.clear();
    }

    /** Recalcule immédiatement le max health d'un joueur (hors cycle des buffs minute). */
    public void refreshMaxHealth(UUID uuid) {
        VampireUHCPlayer vp = plugin.getPlayerManager().get(uuid);
        if (vp == null || !vp.isAlive()) {
            return;
        }
        Player p = Bukkit.getPlayer(uuid);
        if (p == null || !p.isOnline()) {
            return;
        }
        applyMaxHealth(p, vp);
    }

    // Cible en cœurs : base selon rôle + bonus/malus d'aura (Paladin) − pénalités actives.
    private int targetHearts(VampireUHCPlayer vp) {
        ConfigManager config = plugin.getConfigManager();
        int base = vp.getRole() instanceof MasterRole
                ? config.getMasterStartingHearts()
                : 20;

        int delta = 0;
        if (vp.getRole() instanceof PaladinRole) {
            AuraTier tier = plugin.getMarkerManager().computeAuraTier(vp.getUuid());
            switch (tier) {
                case TRES_OBSCURE:
                    delta = -1;
                    break;
                case TRES_LUMINEUSE:
                    delta = 2;
                    break;
                default:
                    delta = 0;
                    break;
            }
        }

        int penalty = heartPenalties.getOrDefault(vp.getUuid(), 0);
        return Math.max(1, base + delta - penalty);
    }

    private void applyMaxHealth(Player p, VampireUHCPlayer vp) {
        double targetHp = targetHearts(vp) * 2.0;
        if (p.getMaxHealth() != targetHp) {
            p.setMaxHealth(targetHp);
        }
        if (p.getHealth() > p.getMaxHealth()) {
            p.setHealth(p.getMaxHealth());
        }
    }

    private void applyRoleBuffs(Player p, VampireUHCPlayer vp) {
        MarkerManager markers = plugin.getMarkerManager();

        if (vp.getRole() instanceof PaladinRole) {
            PaladinRole paladin = (PaladinRole) vp.getRole();
            AuraTier tier = markers.computeAuraTier(vp.getUuid());
            paladin.applyAuraEffects(p, tier);
        } else if (vp.getRole() instanceof ApprenticeSlayer) {
            ApprenticeSlayer slayer = (ApprenticeSlayer) vp.getRole();
            boolean night = p.getWorld().getTime() >= 12300;
            slayer.applyMarkerEffects(p, night,
                    slayer.countDarkMarkers(markers),
                    slayer.countLightMarkers(markers));
        } else if (vp.getRole() instanceof WhiteLadyRole) {
            WhiteLadyRole whiteLady = (WhiteLadyRole) vp.getRole();
            boolean night = p.getWorld().getTime() >= 12300;
            whiteLady.applyEffects(p, night);
        }
    }

    private void applyVampireBuffs(Player p, VampireUHCPlayer vp) {
        if (vp.getCamp() != Camp.VAMPIRE) {
            return;
        }

        ConfigManager config = plugin.getConfigManager();
        int markedCount = plugin.getVoteManager().getMarkedPlayerCount();

        boolean day = p.getWorld().getTime() < 12300; // pas d'isDayTime() en 1.8
        boolean weakness = config.isDayWeaknessEnabled() && day
                && markedCount < config.getMarksToRemoveWeakness();
        boolean strength = !day && markedCount >= config.getMarksForNightStrength();

        if (weakness) {
            p.addPotionEffect(effect(PotionEffectType.WEAKNESS, 0));
        } else {
            p.removePotionEffect(PotionEffectType.WEAKNESS);
        }

        if (strength) {
            p.addPotionEffect(effect(PotionEffectType.INCREASE_DAMAGE, 0));
        } else {
            p.removePotionEffect(PotionEffectType.INCREASE_DAMAGE);
        }
    }

    private PotionEffect effect(PotionEffectType type, int amplifier) {
        return new PotionEffect(type, 20 * 95, amplifier, true, false);
    }
}
