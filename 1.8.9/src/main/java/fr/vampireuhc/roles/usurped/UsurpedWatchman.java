package fr.vampireuhc.roles.usurped;

import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import com.google.gson.JsonObject;

import fr.vampireuhc.config.MessageUtil;
import fr.vampireuhc.markers.MarkerManager;
import fr.vampireuhc.markers.MarkerType;
import fr.vampireuhc.player.VampireUHCPlayer;

/**
 * Veilleur copié par le Sosie : copie exacte (révélation d'un marqueur au
 * hasard, rayon 20 blocs, une fois par épisode et pas deux fois de suite sur
 * la même cible).
 */
public class UsurpedWatchman implements UsurpedPower {

    private VampireUHCPlayer sosie;
    private UUID lastWatched;
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

        if (target.getUuid().equals(lastWatched)) {
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

        this.lastWatched = target.getUuid();

        MarkerType marker = markerManager.getRandomMarkertypeofPlayer(lastWatched);

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

    private boolean isWithinRadius(Player player1, Player player2, double radius) {
        return player1.getLocation().distanceSquared(player2.getLocation()) <= radius * radius;
    }

    @Override
    public void saveState(JsonObject obj) {
        obj.addProperty("usurpedWatchmanLastEpisode", lastWatchEpisode);
        if (lastWatched != null) {
            obj.addProperty("usurpedWatchmanLastTarget", lastWatched.toString());
        }
    }

    @Override
    public void restoreState(JsonObject obj) {
        lastWatchEpisode = obj.has("usurpedWatchmanLastEpisode")
                ? obj.get("usurpedWatchmanLastEpisode").getAsInt() : -1;
        if (obj.has("usurpedWatchmanLastTarget")) {
            lastWatched = UUID.fromString(obj.get("usurpedWatchmanLastTarget").getAsString());
        }
    }
}