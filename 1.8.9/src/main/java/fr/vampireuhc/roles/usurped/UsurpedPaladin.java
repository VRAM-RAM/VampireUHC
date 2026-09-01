package fr.vampireuhc.roles.usurped;

import fr.vampireuhc.markers.AuraTier;
import fr.vampireuhc.markers.MarkerManager;
import fr.vampireuhc.markers.MarkerType;
import fr.vampireuhc.player.VampireUHCPlayer;

import java.util.Random;

import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/**
 * Paladin usurpé : pouvoir imparfait.
 *  - Très obscure : faiblesse légère. / Obscure : rien. / Neutre : rien.
 *  - Lumineuse : force légère (invisible). / Très lumineuse : force légère +
 *    un cœur supplémentaire (delta géré par RoleBuffManager).
 *  - 50% de chances de gagner une marque lumineuse en tuant un joueur, peu
 *    importe le camp du joueur tué.
 */
public class UsurpedPaladin implements UsurpedPower {

    private VampireUHCPlayer sosie;
    private final Random random = new Random();

    @Override
    public String getName() {
        return "Paladin";
    }

    @Override
    public void onEnter(VampireUHCPlayer doppelganger) {
        this.sosie = doppelganger;
    }

    @Override
    public void onExit() {
    }

    // 50% de chances de gagner une marque lumineuse en tuant un joueur.
    // Peu importe le camp du joueur tué (le vrai Paladin exige un vampire).
    public boolean gainLuminousMarkerOnKill(MarkerManager markerManager, VampireUHCPlayer killed) {
        if (sosie == null || killed == null) {
            return false;
        }
        if (random.nextInt(100) >= 50) {
            return false;
        }
        markerManager.addMarker(sosie.getUuid(), MarkerType.LUMINEUX_DOPPELGANGER, sosie.getUuid());
        return true;
    }

    // Effets passifs liés à l'aura (nerf vs Paladin original). Le cœur
    // supplémentaire est géré par RoleBuffManager (ne jamais mettre setMaxHealth ici).
    public void applyAuraEffects(Player player, AuraTier tier) {
        switch (tier) {
            case TRES_OBSCURE:
                // Faiblesse légère (le vrai Paladin inflige faiblesse II).
                player.addPotionEffect(effect(PotionEffectType.WEAKNESS, 0));
                break;
            case OBSCURE:
            case NEUTRE:
                break;
            case LUMINEUSE:
            case TRES_LUMINEUSE:
                // Force légère (invisible). Le delta de cœur est centralisé.
                player.addPotionEffect(effect(PotionEffectType.INCREASE_DAMAGE, 0));
                break;
        }
    }

    // Effet invisible, sans particules et sans icône.
    private PotionEffect effect(PotionEffectType type, int amplifier) {
        return new PotionEffect(type, 20 * 95, amplifier, true, false);
    }
}