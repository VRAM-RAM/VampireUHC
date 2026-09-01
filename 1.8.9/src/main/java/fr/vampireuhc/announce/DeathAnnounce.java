package fr.vampireuhc.announce;

import fr.vampireuhc.VampireUHC;
import fr.vampireuhc.config.ConfigManager;
import fr.vampireuhc.config.MessageUtil;
import fr.vampireuhc.markers.TightAuraTier;
import fr.vampireuhc.player.Camp;
import fr.vampireuhc.player.VampireUHCPlayer;

import java.util.Random;

/**
 * Annonce de la mort d'un joueur, personnalisée pour chaque observateur.
 *
 * Chaque observateur reçoit une version du message selon les probabilités
 * (en %) du camp auquel il appartient (villageois / solitaire / vampire) :
 *   - le nom du mort,
 *   - son rôle,
 *   - son aura (obscure, neutre, lumineuse).
 *
 * Pour un solitaire, le nom et l'aura sont toujours visibles (100%) ; seul le
 * rôle est soumis à la chance. Un champ non révélé est affiché obfusqué
 * (ChatColor.MAGIC) grâce à la balise MiniMessage &lt;obfuscated&gt;.
 */
public class DeathAnnounce {

    private static final Random RANDOM = new Random();

    private DeathAnnounce() {
    }

    /**
     * Construit le message d'annonce de la mort de <code>dead</code> pour
     * l'observateur <code>observer</code>. Renvoie une chaîne legacy §
     * préfixée, prête à être envoyée telle quelle.
     */
    public static String build(VampireUHCPlayer dead, TightAuraTier aura,
                               VampireUHCPlayer observer) {
        ConfigManager config = VampireUHC.getInstance().getConfigManager();
        String camp = campKey(observer.getCamp());

        String name = roll(config.getDeathAnnounceChance(camp, "nom"))
                ? dead.getLastKnownName() : obfuscated(dead.getLastKnownName());
        String role = roll(config.getDeathAnnounceChance(camp, "role"))
                ? roleName(dead) : obfuscated(roleName(dead));
        String auraLabel = roll(config.getDeathAnnounceChance(camp, "aura"))
                ? auraLabel(aura) : obfuscated(auraLabel(aura));

        String mini = MessageUtil.prefix()
                + "<gray>======================="
                + "<white>\nLe joueur <yellow>" + name + " <gray>est mort. Il était "
                + "<yellow>" + role + "<gray>, et avait une aura <yellow>" + auraLabel
                + "<gray>\n=======================";

        return MessageUtil.serialize(mini);
    }

    private static String campKey(Camp camp) {
        if (camp == null) {
            return "villageois";
        }
        switch (camp) {
            case SOLO:
                return "solitaire";
            case VAMPIRE:
                return "vampire";
            default:
                return "villageois";
        }
    }

    private static boolean roll(int chance) {
        return RANDOM.nextInt(100) < chance;
    }

    private static String roleName(VampireUHCPlayer dead) {
        return dead.getRole() != null ? dead.getRole().getName() : "inconnu";
    }

    private static String auraLabel(TightAuraTier aura) {
        if (aura == null) {
            return "neutre";
        }
        switch (aura) {
            case OBSCURE:
                return "obscure";
            case LUMINEUSE:
                return "lumineuse";
            default:
                return "neutre";
        }
    }

    private static String obfuscated(String content) {
        return "<obfuscated>" + content + "</obfuscated>";
    }
}
