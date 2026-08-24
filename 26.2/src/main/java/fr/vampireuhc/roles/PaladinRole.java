package fr.vampireuhc.roles;

import fr.vampireuhc.markers.AuraTier;
import fr.vampireuhc.markers.MarkerType;
import fr.vampireuhc.markers.MarkerManager;
import fr.vampireuhc.player.VampireUHCPlayer;
import fr.vampireuhc.player.Camp;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;

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
    public Component getDescription() {
        MiniMessage mm = MiniMessage.miniMessage();
        return mm.deserialize(
            "<gray>Votre puissance dépend de votre <yellow>aura</yellow>.</gray>\n\n"
            + "<bold><dark_purple>Effets selon l'aura :</dark_purple></bold>\n"
            + "  <dark_gray>• Très obscure →</dark_gray> <red>Perte d'un cœur + faiblesse légère.</red>\n"
            + "  <gray>• Obscure →</gray> <red>Faiblesse légère</red> <gray>(invisible).</gray>\n"
            + "  <white>• Neutre →</white> <gray>Aucun effet.</gray>\n"
            + "  <yellow>• Lumineuse →</yellow> <green>Force légère</green> <gray>(invisible).</gray>\n"
            + "  <gold>• Très lumineuse →</gold> <green>Force + 2 coeurs supplémentaires.</green>\n\n"
            + "<dark_purple>▸</dark_purple> <gray>Lorsque vous tuez un vampire, vous gagnez une <yellow>marque lumineuse</yellow>.</gray>\n"
        );
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
        // On teste le CAMP (et pas le rôle) : un joueur infecté a le camp VAMPIRE
        // tout en gardant son rôle villageois, et doit compter comme un vampire.
        if (paladin == null || killed.getCamp() != Camp.VAMPIRE) {
            return false;
        }
        markerManager.addMarker(paladin.getUuid(), MarkerType.LUMINEUX, paladin.getUuid());
        return true;
    }

    // Effets passifs liés à l'aura. Les effets sont invisibles (sans particules).
    // Le max health est géré centralement par RoleBuffManager (delta selon le tier) :
    // ne jamais écrire setMaxHealth ici, sinon les pénalités Cupidon seraient écrasées.
    public void applyAuraEffects(Player player, AuraTier tier) {
        switch (tier) {
            case TRES_OBSCURE -> player.addPotionEffect(effect(PotionEffectType.WEAKNESS, 1));
            case OBSCURE -> player.addPotionEffect(effect(PotionEffectType.WEAKNESS, 0));
            case NEUTRE -> { }
            case LUMINEUSE -> player.addPotionEffect(effect(PotionEffectType.STRENGTH, 0));
            case TRES_LUMINEUSE -> player.addPotionEffect(effect(PotionEffectType.STRENGTH, 1));
        }
    }

    // Effet invisible, sans particules et sans icône.
    private PotionEffect effect(PotionEffectType type, int amplifier) {
        return new PotionEffect(type, 20 * 95, amplifier, true, false, false);
    }
}
