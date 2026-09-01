package fr.vampireuhc.roles.usurped;

import fr.vampireuhc.VampireUHC;
import fr.vampireuhc.config.ConfigManager;
import fr.vampireuhc.player.VampireUHCPlayer;

import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/**
 * Sbire Vampire usurpé : le Sosie copie les pouvoirs des vampires — faiblesse
 * de jour / force de nuit selon le nombre de joueurs marqués — et peut voter
 * pour la marque vampire. Il ne connaît ni le résultat du vote ni l'identité
 * des autres vampires (feedback de vote masqué, aucune notification de groupe).
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
        // On retire les buffs vampiriques dès que le pouvoir copié disparaît.
        if (sosie == null) {
            return;
        }
        Player p = VampireUHC.getInstance().getServer().getPlayer(sosie.getUuid());
        if (p != null) {
            p.removePotionEffect(PotionEffectType.WEAKNESS);
            p.removePotionEffect(PotionEffectType.INCREASE_DAMAGE);
        }
    }

    // Faiblesse de jour / force de nuit recalculées à chaque cycle de buffs,
    // selon le nombre de joueurs portant une marque vampire (copie exacte des
    // buffs vampires, sous condition du Sosie au lieu du camp VAMPIRE).
    public void applyBuffs(Player p) {
        ConfigManager config = VampireUHC.getInstance().getConfigManager();
        int markedCount = VampireUHC.getInstance().getVoteManager().getMarkedPlayerCount();

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

    // Effet invisible, sans particules et sans icône.
    protected PotionEffect effect(PotionEffectType type, int amplifier) {
        return new PotionEffect(type, 20 * 95, amplifier, true, false);
    }
}