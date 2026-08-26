package fr.vampireuhc.roles;

import fr.vampireuhc.markers.TightAuraTier;
import fr.vampireuhc.markers.Aura;

import fr.vampireuhc.roles.Role;
import net.kyori.adventure.text.Component;
import fr.vampireuhc.player.VampireUHCPlayer;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import fr.vampireuhc.markers.MarkerManager;
import fr.vampireuhc.markers.MarkerType;
import fr.vampireuhc.config.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import net.kyori.adventure.text.minimessage.MiniMessage;
import fr.vampireuhc.player.Camp;

public class WatchmanRole implements Role {
    private VampireUHCPlayer watchman;
    private UUID last_watched;

    @Override
    public boolean isVampire() {
        return false;
    }

    @Override
    public Camp getDefaultCamp() {
        return Camp.VILLAGEOIS;
    }

    @Override
    public void onAssign(VampireUHCPlayer player) {
        this.watchman = player;
    } 

    @Override
    public Component getDescription() {
        MiniMessage mm = MiniMessage.miniMessage();
        return mm.deserialize(   
            "<gray>Vous devez gagner avec le <green>village</green>. Pour ce faire, vous disposez de la capacité de veiller, à chaque épisode, sur un joueur.</gray>\n\n"
            + "<dark_purple>▸</dark_purple> <gray>À chaque épisode, à l'aide de la commande <gold>/vuhc veiller <joueur></gold>, vous pouvez veiller sur un joueur situé à moins de 20 blocs de vous, ce qui aura pour effet :</gray>\n\n"
            + "  <gray>→ De vous informer du nom d'<gold>un</gold> des marqueurs que porte le joueur.</gray>\n"
            + "<red>⚠ Ce pouvoir n'est pas utilisable sur le même joueur deux fois de suite !</red>"
        );
    }

    @Override
    public String getName() {
        return "Veilleur";
    }

    // Pouvoir spécifique au veilleur :

    public void watchPlayer(VampireUHCPlayer target, MarkerManager markerManager) {
        if (watchman == null || target == null) {
            return;
        }

        Player bukkitWatchman = bukkit.getPlayer(watchman.getUuid());

        if (bukkitWatchman == null) {
            return;
        }

        if (target.getUuid() == last_watched) {
            bukkitWatchman.sendMessage(MessageUtil.error("Vous ne pouvez pas veiller sur le même joueur deux épisodes de suite !"));
            return;
        }

        this.last_watched = target.getUuid();
        
    }

}
