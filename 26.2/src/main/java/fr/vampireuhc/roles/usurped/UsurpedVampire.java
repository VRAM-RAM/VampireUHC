package fr.vampireuhc.roles.usurped;

import fr.vampireuhc.VampireUHC;
import fr.vampireuhc.config.ConfigManager;
import fr.vampireuhc.player.VampireUHCPlayer;

import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/**
 * Sbire Vampire usurpé (26.2) : le Sosie copie les pouvoirs des vampires —
 * faiblesse de jour / force de nuit selon le nombre de joueurs marqués — et
 * peut voter pour la marque vampire. Il ne connaît ni le résultat du vote ni
 * l'identité des autres vampires (feedback masqué, aucune notification).
 * Les buffs sont recalés par RoleBuffManager via ce pouvoir (le camp SOLO du
 * Sosie ne déclenche pas les buffs vampires de l'équipe).
 */
public class UsurpedVampire implements UsurpedPower {

    protected VampireUHCPlayer sosie;

    @Override
    public String getName() {
        return "Sbire";
    }

    @Override
    public void onEnter(VampireUHCPlayer doppelganger) {
        this.sosie = doppelganger;
    }

    @Override
    public void onExit() {
        if (sosie == null) {
            return;
        }
        Player p = VampireUHC.getInstance().getServer().getPlayer(sosie.getUuid());
        if (p != null) {
            p.removePotionEffect(PotionEffectType.WEAKNESS);
            p.removePotionEffect(PotionEffectType.STRENGTH);
        }
    }

    // Faiblesse de jour / force de nuit recalculées à chaque cycle de buffs,
    // selon le nombre de joueurs marqués (copie de la logique vampire).
    public void applyBuffs(Player p) {
        ConfigManager config = VampireUHC.getInstance().getConfigManager();
        int markedCount = VampireUHC.getInstance().getVoteManager().getMarkedPlayerCount();

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

    // Effet invisible, sans particules et sans icône.
    protected PotionEffect effect(PotionEffectType type, int amplifier) {
        return new PotionEffect(type, 20 * 95, amplifier, true, false, true);
    }
}