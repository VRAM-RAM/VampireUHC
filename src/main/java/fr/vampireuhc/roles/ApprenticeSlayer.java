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
import java.util.UUID;

import org.bukkit.ChatColor;
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
        return "L'Apprentie assassin est un rôle solitaire. Son but : gagner seule en éliminant tous les autres joueurs. À chaque kill, elle récupère les marques (sauf les marques Maître) du joueur tué. En fonction des marques qu'elle possède, ses pouvoirs varient (cumulatifs) : plus de "
            + ChatColor.DARK_PURPLE + darkThreshold + ChatColor.GRAY
            + " marqueurs obscurs => force légère la nuit ; plus de "
            + ChatColor.DARK_PURPLE + lightThreshold + ChatColor.GRAY
            + " marqueurs lumineux => force légère le jour ; plus de "
            + ChatColor.DARK_PURPLE + darkHighThreshold + ChatColor.GRAY
            + " marqueurs obscurs => force légère la nuit et régénération la nuit ; plus de "
            + ChatColor.DARK_PURPLE + lightHighThreshold + ChatColor.GRAY
            + " marqueurs lumineux => force légère le jour et régénération le jour.";
    }
    @Override
    public String getName() {
        return "Apprenti Assassin";
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

    // Récupère les marqueurs du joueur tué (sauf les marques Maître, pour éviter une infection obligatoire).
    public boolean CopyMarkersOnKill(MarkerManager manager, VampireUHCPlayer killed) {
        if (slayer == null) {
            return false;
        }
        List<Marker> markers = manager.getMarkers(killed.getUuid()).stream()
                .filter(marker -> marker.getType() != MarkerType.MARQUE_MAITRE)
                .toList();
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
        var config = fr.vampireuhc.VampireUHC.getInstance().getConfigManager();
        boolean darkStrength = darkCount >= config.getSlayerDarkThreshold();
        boolean lightStrength = lightCount >= config.getSlayerLightThreshold();
        boolean darkRegen = darkCount >= config.getSlayerDarkHighThreshold();
        boolean lightRegen = lightCount >= config.getSlayerLightHighThreshold();

        if (night) {
            if (darkRegen) {
                player.addPotionEffect(effect(PotionEffectType.STRENGTH, 1));
                player.addPotionEffect(effect(PotionEffectType.REGENERATION, 0));
            } else if (darkStrength) {
                player.addPotionEffect(effect(PotionEffectType.STRENGTH, 0));
            }
        } else {
            if (lightRegen) {
                player.addPotionEffect(effect(PotionEffectType.STRENGTH, 1));
                player.addPotionEffect(effect(PotionEffectType.REGENERATION, 0));
            } else if (lightStrength) {
                player.addPotionEffect(effect(PotionEffectType.STRENGTH, 0));
            }
        }
    }

    private PotionEffect effect(PotionEffectType type, int amplifier) {
        return new PotionEffect(type, 20 * 95, amplifier, true, false, false);
    }
}
