package fr.vampireuhc.roles.usurped;

import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import com.google.gson.JsonObject;

import fr.vampireuhc.config.MessageUtil;
import fr.vampireuhc.markers.MarkerManager;
import fr.vampireuhc.markers.MarkerType;
import fr.vampireuhc.player.VampireUHCPlayer;
import net.kyori.adventure.text.minimessage.MiniMessage;

/**
 * Veilleur copié par le Sosie : copie exacte du vrai Veilleur (un marqueur
 * aléatoire, rayon 20 blocs, gate épisode + gate même joueur consécutif),
 * sans aucune particularité.
 */
public class UsurpedWatchman implements UsurpedPower {

    private VampireUHCPlayer sosie;
    private UUID last_watched;
    private int lastWatchEpisode = -1;

    @Override
    public String getName() {
        return "Veilleur";
    }

    @Override
    public void onEnter(VampireUHCPlayer doppelganger) {
        this.sosie = doppelganger;
    }

    @Override
    public void onExit() {
    }

    public void watchPlayer(VampireUHCPlayer target, MarkerManager markerManager, int current_episode) {
        if (sosie == null || target == null) {
            return;
        }

        Player bukkitWatchman = Bukkit.getPlayer(sosie.getUuid());
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

        Player bukkitTarget = Bukkit.getPlayer(target.getUuid());
        if (bukkitTarget == null) {
            bukkitWatchman.sendMessage(MessageUtil.error("Le joueur que vous ciblez n'est pas connecté."));
            return;
        }

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

        MiniMessage mm = MiniMessage.miniMessage();
        bukkitWatchman.sendMessage(mm.deserialize(
            "<dark_purple>Vous avez veillé sur <gold>" + target.getLastKnownName()
            + "</gold>. Vous observez la présence du marqueur :\n").append(marker.toComponent()));
        this.lastWatchEpisode = current_episode;
    }

    private boolean isWithinRadius(Player player1, Player player2, double radius) {
        return player1.getLocation().distanceSquared(player2.getLocation()) <= radius * radius;
    }

    @Override
    public void saveState(JsonObject obj) {
        obj.addProperty("usurpedWatchmanLastEpisode", lastWatchEpisode);
        if (last_watched != null) {
            obj.addProperty("usurpedWatchmanLastTarget", last_watched.toString());
        }
    }

    @Override
    public void restoreState(JsonObject obj) {
        lastWatchEpisode = obj.has("usurpedWatchmanLastEpisode")
                ? obj.get("usurpedWatchmanLastEpisode").getAsInt() : -1;
        if (obj.has("usurpedWatchmanLastTarget")) {
            last_watched = UUID.fromString(obj.get("usurpedWatchmanLastTarget").getAsString());
        }
    }
}