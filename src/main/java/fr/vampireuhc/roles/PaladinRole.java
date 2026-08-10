package fr.vampireuhc.roles;

import fr.vampireuhc.markers.AuraTier;
import fr.vampireuhc.markers.MarkerType;
import fr.vampireuhc.markers.MarkerManager;
import fr.vampireuhc.player.VampireUHCPlayer;
import fr.vampireuhc.player.Camp;

import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class PaladinRole implements Role {
    private VampireUHCPlayer paladin;
    
    @Override
    public boolean isVampire() {
        return false;
    }

    public PaladinRole(VampireUHCPlayer player) {
        this.paladin = player;
    }

    @Override
    public String getDescription() {
        return "Le Paladin est un rôle villageois dépendant de l'aura. Ses effets varient selon son aura : très obscure => perte d'un coeur et faiblesse légère ; obscure => faiblesse légère (invisible) ; neutre => aucun effet ; lumineuse => force légère (invisible) ; très lumineuse => force et deux coeurs supplémentaires. Lorsqu'il tue un vampire, il gagne une marque lumineuse.";
    }

    @Override
    public String getName() {
        return "Paladin";
    }

    @Override
    public Camp getDefaultCamp() {
        return Camp.VILLAGEOIS;
    }

    @Override
    public void onAssign(VampireUHCPlayer vampireUHCPlayer) {
        this.paladin = vampireUHCPlayer;
    }

    // Pouvoir actif spécifique au paladin : lorsqu'il tue un joueur vampire, il gagne un marqueur lumineux

    public boolean gainLuminousMarkerOnKill(MarkerManager markerManager, VampireUHCPlayer killed) {
        if (paladin == null || !killed.getRole().isVampire()) {
            return false;
        }
        markerManager.addMarker(paladin.getUuid(), MarkerType.LUMINEUX, paladin.getUuid());
        return true;
    }

    // Effets passifs liés à l'aura. Les effets sont invisibles (sans particules).
    public void applyAuraEffects(Player player, AuraTier tier) {
        switch (tier) {
            case TRES_OBSCURE -> {
                player.setMaxHealth(18);
                player.addPotionEffect(effect(PotionEffectType.WEAKNESS, 1));
            }
            case OBSCURE -> {
                player.setMaxHealth(20);
                player.addPotionEffect(effect(PotionEffectType.WEAKNESS, 0));
            }
            case NEUTRE -> {
                player.setMaxHealth(20);
                player.getActivePotionEffects().forEach(e -> {
                    if (e.getType() == PotionEffectType.WEAKNESS || e.getType() == PotionEffectType.STRENGTH) {
                        player.removePotionEffect(e.getType());
                    }
                });
            }
            case LUMINEUSE -> {
                player.setMaxHealth(20);
                player.addPotionEffect(effect(PotionEffectType.STRENGTH, 0));
            }
            case TRES_LUMINEUSE -> {
                player.setMaxHealth(24);
                player.addPotionEffect(effect(PotionEffectType.STRENGTH, 1));
            }
        }
        if (player.getHealth() > player.getMaxHealth()) {
            player.setHealth(player.getMaxHealth());
        }
    }

    // Effet invisible, sans particules et sans icône.
    private PotionEffect effect(PotionEffectType type, int amplifier) {
        return new PotionEffect(type, 20 * 95, amplifier, true, false, false);
    }
}
