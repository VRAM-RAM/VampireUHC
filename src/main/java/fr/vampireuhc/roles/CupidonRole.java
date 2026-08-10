package fr.vampireuhc.roles;
import fr.vampireuhc.player.VampireUHCPlayer;
import fr.vampireuhc.markers.MarkerType;
import fr.vampireuhc.markers.MarkerManager;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;

import fr.vampireuhc.player.Camp;

public class CupidonRole implements Role {
    private VampireUHCPlayer cupidon;
    private boolean marked;

    public CupidonRole(VampireUHCPlayer player) {
        this.cupidon = player;
    }
    
    @Override
    public boolean isVampire() {
        return false;
    }

    @Override
    public String getDescription() {
        // TODO
        return "";
    }

    @Override
    public String getName() {
        return "Cupidon";
    }

    @Override
    public Camp getDefaultCamp() {
        return Camp.VILLAGEOIS;
    }

    @Override
    public void onAssign(VampireUHCPlayer vampireUHCPlayer) {
        this.cupidon = vampireUHCPlayer;
    }

    // Pouvoirs spécifiques au rôle :

    // Marquer deux joueurs en début de partie.
    public boolean MarkLovers(MarkerManager manager, VampireUHCPlayer target_1, VampireUHCPlayer target_2) {
        if (cupidon == null || marked == true) {
            return false;
        } 
        var uuid_cupidon = cupidon.getUuid();
        manager.addMarker(target_1.getUuid(), MarkerType.AMOUR, uuid_cupidon);
        manager.addMarker(target_2.getUuid(), MarkerType.AMOUR, uuid_cupidon);

        var bukkitCupidon = Bukkit.getPlayer(uuid_cupidon);
        if (bukkitCupidon != null) {
            this.marked = true;
            bukkitCupidon.sendMessage(ChatColor.DARK_PURPLE + "Vous avez marqué les joueurs " + ChatColor.GOLD + target_1.getLastKnownName() + ChatColor.DARK_PURPLE + " et " + target_2.getLastKnownName());
        }
        return true;

    }

    public void NotifyChanges() {
        // TODO
    }
}
