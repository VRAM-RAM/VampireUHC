package fr.vampireuhc.roles;

import org.bukkit.Bukkit;

import fr.vampireuhc.markers.MarkerManager;
import fr.vampireuhc.player.Camp;
import fr.vampireuhc.player.VampireUHCPlayer;
import fr.vampireuhc.config.MessageUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;

public class SoulweigherRole implements Role {
    
    private VampireUHCPlayer soulWeighter;


    @Override
    public Camp getDefaultCamp() {
        return Camp.VILLAGEOIS;
    }

    @Override
    public boolean isVampire() {
        return false;
    }

    @Override
    public void onAssign(VampireUHCPlayer player) {
        this.soulWeighter = player;
    }

    @Override
    public String getName() {
        return "Peseuse d'âmes";
    }

    @Override
    public Component getDescription() {
        MiniMessage mm = MiniMessage.miniMessage();
        return mm.deserialize(
            "<gray>Vous pesez l'aura de deux joueurs pour déceler les différences.</gray>\n\n"
            + "<dark_purple>▸</dark_purple> <gray>Commande : <gold>/vuhc peser <joueur1> <joueur2></gold></gray>\n"
            + "<dark_purple>▸</dark_purple> <gray>Utilisable à chaque épisode.</gray>\n\n"
            + "<bold><dark_purple>Résultats possibles :</dark_purple></bold>\n"
            + "  <green>« La balance s'équilibre... »</green>\n"
            + "  <gray>→ Même aura exacte (très obscure, obscure, neutre...).</gray>\n"
            + "  <yellow>« La balance penche légèrement... »</yellow>\n"
            + "  <gray>→ Même catégorie (obscure, neutre ou lumineuse).</gray>\n"
            + "  <red>« La balance penche... »</red>\n"
            + "  <gray>→ Catégories différentes.</gray>\n\n"
        );
    }

    // Pouvoir actif spécifique à la peseuse d'âme

    public boolean weightAura(MarkerManager manager, VampireUHCPlayer targetOne, VampireUHCPlayer targetTwo) {
        if (soulWeighter == null) {
            return false;
        }

        var auraOfFirstTarget = manager.computeAuraTier(targetOne.getUuid());
        var auraOfSecondTarget = manager.computeAuraTier(targetTwo.getUuid());

        var bukkitPlayer = Bukkit.getPlayer(soulWeighter.getUuid());
        if (bukkitPlayer == null) {
            return false;
        }
        if (auraOfFirstTarget == auraOfSecondTarget) {
            bukkitPlayer.sendMessage(MessageUtil.success("La balance s'équilibre..."));
            return true;
        } 
        if (auraOfFirstTarget.getTight() == auraOfSecondTarget.getTight()) {
            bukkitPlayer.sendMessage(MessageUtil.warn("La balance penche légèrement..."));
            return true;
        }
        bukkitPlayer.sendMessage(MessageUtil.error("La balance penche..."));
        return false;
    }   
}
