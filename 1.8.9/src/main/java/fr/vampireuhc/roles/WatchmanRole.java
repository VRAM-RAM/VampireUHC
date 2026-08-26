package fr.vampireuhc.roles;

import fr.vampireuhc.player.VampireUHCPlayer;
import java.util.UUID;
import fr.vampireuhc.markers.MarkerManager;
import fr.vampireuhc.markers.MarkerType;
import fr.vampireuhc.config.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import fr.vampireuhc.player.Camp;

public class WatchmanRole implements Role {
    private VampireUHCPlayer watchman;
    private UUID last_watched;
    // Gates "une fois par épisode" : l'épisode de dernière utilisation (-1 = jamais).
    private int lastWatchEpisode = -1;

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
    public String getDescription() {
        return   
            "<gray>Vous devez gagner avec le <green>village</green>. Pour ce faire, vous disposez de la capacité de veiller, à chaque épisode, sur un joueur.</gray>\n\n"
            + "<dark_purple>▸</dark_purple> <gray>À chaque épisode, à l'aide de la commande <gold>/vuhc veiller <joueur></gold>, vous pouvez veiller sur un joueur situé à moins de 20 blocs de vous, ce qui aura pour effet :</gray>\n\n"
            + "  <gray>→ De vous informer du nom d'<gold>un</gold> des marqueurs que porte le joueur.</gray>\n"
            + "<red>⚠ Ce pouvoir n'est pas utilisable sur le même joueur deux fois de suite !</red>"
        ;
    }

    @Override
    public String getName() {
        return "Veilleur";
    }

    // Pouvoir spécifique au veilleur :

    public void watchPlayer(VampireUHCPlayer target, MarkerManager markerManager, int current_episode) {
        if (watchman == null || target == null) {
            return;
        }

        Player bukkitWatchman = Bukkit.getPlayer(watchman.getUuid());

        if (bukkitWatchman == null) {
            return;
        }

        if (current_episode == lastWatchEpisode) {
            bukkitWatchman.sendMessage(MessageUtil.error("Vous ne pouvez veiller sur un joueur qu'une seule fois par épisode !"));
            return;
        }

        if (target.getUuid() == last_watched) {
            bukkitWatchman.sendMessage(MessageUtil.error("Vous ne pouvez pas veiller sur le même joueur deux épisodes de suite !"));
            return;
        }

        // On cast les joueurs bukkit à partir des joueurs VampireUHC
        Player bukkitTarget = Bukkit.getPlayer(target.getUuid());

        // En cas d'erreur, on retourne
        if (bukkitTarget == null) {
            bukkitWatchman.sendMessage(MessageUtil.error("Le joueur que vous ciblez n'est pas connecté."));
            return;
        }

         // Si la target ne se trouve pas dans la range de 20 blocs de rayon, on retourne
        if (!isWithinRadius(bukkitWatchman, bukkitTarget, 20)) {
            bukkitWatchman.sendMessage(MessageUtil.error("Le joueur que vous ciblez n'est pas suffisamment proche de vous !"));
            return;
        }

        this.last_watched = target.getUuid();

        MarkerType marker = markerManager.getRandomMarkertypeofPlayer(last_watched);

        if (marker == null) {
            bukkitWatchman.sendMessage(MessageUtil.success("Vous avez veillé sur le joueur et n'avez trouvé aucun marqueur !"));
            return;
        }

        String message = MessageUtil.serialize(
                "<dark_purple>Vous avez veillé sur <gold>" + target.getLastKnownName()
                + "</gold>. Vous observez la présence du marqueur :\n")
                + marker.toLegacy();
        bukkitWatchman.sendMessage(message);
        this.lastWatchEpisode = current_episode;
    }

    // Helper pour savoir si le joueur se trouve dans le rayon du tisseur
    private boolean isWithinRadius(Player player1, Player player2, double radius) {
        return player1.getLocation().distanceSquared(player2.getLocation()) <= radius * radius;
    }

}
