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
        return "La Peseuse d'Âmes est un rôle à info mineur. À chaque épisode, avec /vuhc peser <Joueur1> <Joueur2>, elle pèse l'aura de deux joueurs. Trois résultats possibles : "
            + ChatColor.DARK_PURPLE + "« La balance s'équilibre... »" + ChatColor.GRAY + " : les deux joueurs ont exactement la même aura (très obscure, obscure, neutre...). "
            + ChatColor.DARK_PURPLE + "« La balance penche légèrement... »" + ChatColor.GRAY + " : ils ont une aura de la même catégorie (obscure, neutre ou lumineuse). "
            + ChatColor.DARK_PURPLE + "« La balance penche... »" + ChatColor.GRAY + " : ils ont une aura de catégories différentes. "
            + "Croisez ces informations avec les autres rôles pour démasquer les vampires !";
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
