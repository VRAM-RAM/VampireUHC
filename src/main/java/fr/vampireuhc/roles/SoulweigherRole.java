package fr.vampireuhc.roles;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;

import fr.vampireuhc.markers.MarkerManager;
import fr.vampireuhc.player.Camp;
import fr.vampireuhc.player.VampireUHCPlayer;

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
    public String getDescription() {
        // TODO
        return null;
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
            bukkitPlayer.sendMessage(ChatColor.GOLD + "La balance s'équilibre...");
            return true;
        } 
        if (auraOfFirstTarget.getTight() == auraOfSecondTarget.getTight()) {
            bukkitPlayer.sendMessage(ChatColor.GOLD + "La balance penche légèrement...");
            return true;
        }
        bukkitPlayer.sendMessage(ChatColor.GOLD + "La balance penche...");
        return false;
    }   
}
