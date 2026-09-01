package fr.vampireuhc.game;

import fr.vampireuhc.VampireUHC;
import fr.vampireuhc.config.ConfigManager;
import fr.vampireuhc.markers.AuraTier;
import fr.vampireuhc.markers.MarkerManager;
import fr.vampireuhc.player.Camp;
import fr.vampireuhc.player.VampireUHCPlayer;
import fr.vampireuhc.roles.ApprenticeSlayer;
import fr.vampireuhc.roles.BabaYagaRole;
import fr.vampireuhc.roles.DoppelgangerRole;
import fr.vampireuhc.roles.MasterRole;
import fr.vampireuhc.roles.PaladinRole;
import fr.vampireuhc.roles.WhiteLadyRole;
import fr.vampireuhc.roles.usurped.UsurpedPaladin;
import fr.vampireuhc.roles.usurped.UsurpedPower;
import fr.vampireuhc.roles.usurped.UsurpedSlayer;
import fr.vampireuhc.roles.usurped.UsurpedVampire;
import fr.vampireuhc.roles.usurped.UsurpedWhiteLady;

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

    // Cible en coeurs : base selon rôle + bonus/malus d'aura (Paladin) − pénalités actives.
    private int targetHearts(VampireUHCPlayer vp) {
        ConfigManager config = plugin.getConfigManager();
        int base = vp.getRole() instanceof MasterRole
                ? config.getMasterStartingHearts()
                : 20;

        int delta = 0;
        if (vp.getRole() instanceof PaladinRole) {
            AuraTier tier = plugin.getMarkerManager().computeAuraTier(vp.getUuid());
            delta = switch (tier) {
                case TRES_OBSCURE -> -1;
                case TRES_LUMINEUSE -> 2;
                default -> 0;
            };
        } else if (vp.getRole() instanceof DoppelgangerRole doppelganger
                && doppelganger.getActivePower() instanceof UsurpedPaladin) {
            // Paladin usurpé : très lumineuse → +1 cœur (vs +2 du vrai Paladin).
            AuraTier tier = plugin.getMarkerManager().computeAuraTier(vp.getUuid());
            if (tier == AuraTier.TRES_LUMINEUSE) {
                delta = 1;
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
        } else if (vp.getRole() instanceof BabaYagaRole babaYaga) {
            babaYaga.applyEffects(p);
        } else if (vp.getRole() instanceof DoppelgangerRole doppelganger) {
            // Pouvoir copié : on ré-applique les effets passifs du rôle usurpé.
            UsurpedPower power = doppelganger.getActivePower();
            if (power instanceof UsurpedPaladin usurpedPaladin) {
                AuraTier tier = markers.computeAuraTier(vp.getUuid());
                usurpedPaladin.applyAuraEffects(p, tier);
            } else if (power instanceof UsurpedSlayer usurpedSlayer) {
                boolean night = !p.getWorld().isDayTime();
                usurpedSlayer.applyMarkerEffects(p, night,
                        usurpedSlayer.countDarkMarkers(markers),
                        usurpedSlayer.countLightMarkers(markers));
            } else if (power instanceof UsurpedVampire usurpedVampire) {
                usurpedVampire.applyBuffs(p);
            } else if (power instanceof UsurpedWhiteLady usurpedWhiteLady) {
                boolean night = !p.getWorld().isDayTime();
                usurpedWhiteLady.applyEffects(p, night);
            }

            // Le Sosie qui a tué le tueur de sa cible usurpée gagne une
            // force + speed permanentes (réappliquées à chaque cycle).
            if (doppelganger.hasKilledKiller()) {
                p.addPotionEffect(effect(PotionEffectType.STRENGTH, 0));
                p.addPotionEffect(effect(PotionEffectType.SPEED, 0));
            }
        }
    }

    private void applyVampireBuffs(Player p, VampireUHCPlayer vp) {
        if (vp.getCamp() != Camp.VAMPIRE) {
            return;
        }

        var config = plugin.getConfigManager();
        int markedCount = plugin.getVoteManager().getMarkedPlayerCount();

        boolean day = p.getWorld().isDayTime();
        boolean weakness = config.isDayWeaknessEnabled() && day&& markedCount < config.getMarksToRemoveWeakness();
        boolean strength = !day && markedCount >= config.getMarksForNightStrength();

        if (weakness) {
            p.addPotionEffect(effect_icon(PotionEffectType.WEAKNESS, 0));
        } else {
            p.removePotionEffect(PotionEffectType.WEAKNESS);
        }

        if (strength) {
            p.addPotionEffect(effect_icon(PotionEffectType.STRENGTH, 0));
        } else {
            p.removePotionEffect(PotionEffectType.STRENGTH);
        }
    }

    private PotionEffect effect(PotionEffectType type, int amplifier) {
        return new PotionEffect(type, 20 * 95, amplifier, true, false, false);
    }

    private PotionEffect effect_icon(PotionEffectType type, int amplifier) {
        return new PotionEffect(type, 20 * 95, amplifier, true, false, true);
    }
}
