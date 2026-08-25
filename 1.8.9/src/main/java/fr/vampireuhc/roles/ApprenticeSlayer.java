package fr.vampireuhc.roles;
import fr.vampireuhc.VampireUHC;
import fr.vampireuhc.config.ConfigManager;
import fr.vampireuhc.markers.Aura;
import fr.vampireuhc.markers.Marker;
import fr.vampireuhc.markers.MarkerType;
import fr.vampireuhc.markers.MarkerManager;
import fr.vampireuhc.player.VampireUHCPlayer;
import fr.vampireuhc.player.Camp;

import java.util.List;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class ApprenticeSlayer implements Role {
    private VampireUHCPlayer slayer;


    public ApprenticeSlayer(VampireUHCPlayer player) {
        this.slayer = player;
    }

    @Override
    public boolean isVampire() {
        return false;
    }

    @Override
    public String getDescription() {
        ConfigManager config = VampireUHC.getInstance().getConfigManager();
        int darkThreshold = config.getSlayerDarkThreshold();
        int lightThreshold = config.getSlayerLightThreshold();
        int darkHighThreshold = config.getSlayerDarkHighThreshold();
        int lightHighThreshold = config.getSlayerLightHighThreshold();
        return (
            "<light_purple>Vous gagnez seule en éliminant tous les autres joueurs.</light_purple>\n\n"
            + "<dark_purple>▸</dark_purple> <gray>À chaque kill, vous récupérez les marqueurs du joueur tué (sauf les marques Maître, d'Amour et de Fil).</gray>\n\n"
            + "<bold><dark_purple>Pouvoirs cumulatifs :</dark_purple></bold>\n"
            + "  <gray>• <yellow>" + darkThreshold + "+ marqueurs obscurs</yellow> → <green>Force légère la nuit.</green></gray>\n"
            + "  <gray>• <yellow>" + config.getSlayerLightThreshold() + "+ marqueurs lumineux</yellow> → <green>Force légère le jour.</green></gray>\n"
            + "  <gray>• <yellow>" + darkHighThreshold + "+ marqueurs obscurs</yellow> → <green>Force + régénération la nuit.</green></gray>\n"
            + "  <gray>• <yellow>" + lightHighThreshold + "+ marqueurs lumineux</yellow> → <green>Force + régénération le jour.</green></gray>\n\n"
            + "<red>⚠</red> <gray>Les marques Maître que vous recevez du maître directement vous rendent vulnérable (perte d'absorption, voire infection).</gray>"
        );
    }
    @Override
    public String getName() {
        return "Apprentie Assassin";
    }

    @Override
    public Camp getDefaultCamp() {
        return Camp.SOLO;
    }

    @Override
    public void onAssign(VampireUHCPlayer vampireUHCPlayer) {
        this.slayer = vampireUHCPlayer;
    }

    // Pouvoirs spécifiques au rôle. 

    // Récupère les marqueurs du joueur tué, sauf les marques Maître (infection obligatoire),
    // d'Amour et de Fil (créeraient des holders fantômes pour le Cupidon et le Tisseur).
    public boolean CopyMarkersOnKill(MarkerManager manager, VampireUHCPlayer killed) {
        if (slayer == null) {
            return false;
        }
        List<Marker> markers = manager.getMarkers(killed.getUuid()).stream()
                .filter(marker -> marker.getType() != MarkerType.MARQUE_MAITRE)
                .filter(marker -> marker.getType() != MarkerType.AMOUR)
                .filter(marker -> marker.getType() != MarkerType.FIL)
                .collect(java.util.stream.Collectors.toList());
        manager.addMarkers(slayer.getUuid(), markers);
        return true;
    }

    // Compter les marqueurs obscurs/lumineux portés par l'assassin.
    public int countDarkMarkers(MarkerManager manager) {
        return countByAura(manager, Aura.OBSCURE);
    }

    public int countLightMarkers(MarkerManager manager) {
        return countByAura(manager, Aura.LUMINEUSE);
    }

    private int countByAura(MarkerManager manager, Aura aura) {
        int count = 0;
        for (Marker marker : manager.getMarkers(slayer.getUuid())) {
            if (marker.getAura() == aura) {
                count++;
            }
        }
        return count;
    }

    // Effets passifs selon les marques portées (effets invisibles, sans particules).
    public void applyMarkerEffects(Player player, boolean night, int darkCount, int lightCount) {
        ConfigManager config = fr.vampireuhc.VampireUHC.getInstance().getConfigManager();
        boolean darkStrength = darkCount >= config.getSlayerDarkThreshold();
        boolean lightStrength = lightCount >= config.getSlayerLightThreshold();
        boolean darkRegen = darkCount >= config.getSlayerDarkHighThreshold();
        boolean lightRegen = lightCount >= config.getSlayerLightHighThreshold();

        if (night) {
            if (darkRegen) {
                player.addPotionEffect(effect(PotionEffectType.INCREASE_DAMAGE, 1));
                player.addPotionEffect(effect(PotionEffectType.REGENERATION, 0));
            } else if (darkStrength) {
                player.addPotionEffect(effect(PotionEffectType.INCREASE_DAMAGE, 0));
            }
        } else {
            if (lightRegen) {
                player.addPotionEffect(effect(PotionEffectType.INCREASE_DAMAGE, 1));
                player.addPotionEffect(effect(PotionEffectType.REGENERATION, 0));
            } else if (lightStrength) {
                player.addPotionEffect(effect(PotionEffectType.INCREASE_DAMAGE, 0));
            }
        }
    }

    private PotionEffect effect(PotionEffectType type, int amplifier) {
        return new PotionEffect(type, 20 * 95, amplifier, true, false);
    }
}
