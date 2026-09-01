package fr.vampireuhc.roles.usurped;

import com.google.gson.JsonObject;
import fr.vampireuhc.config.MessageUtil;
import fr.vampireuhc.markers.Aura;
import fr.vampireuhc.markers.Marker;
import fr.vampireuhc.markers.MarkerManager;
import fr.vampireuhc.player.VampireUHCPlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

/**
 * Exorciste usurpé : pouvoir imparfait. Le Sosie ne supprime qu'UN SEUL des
 * marqueurs obscurs du joueur, et ne connaît que le NOMBRE de marqueurs
 * obscurs (pas leur nom). Gates propres au Sosie (une fois par épisode, une
 * seule fois par joueur).
 */
public class UsurpedExorcist implements UsurpedPower {

    private VampireUHCPlayer sosie;

    private int lastExorcisedEpisode = -1;
    private final List<UUID> alreadyExorcised = new ArrayList<>();

    @Override
    public String getName() {
        return "Exorciste";
    }

    @Override
    public void onEnter(VampireUHCPlayer doppelganger) {
        this.sosie = doppelganger;
    }

    @Override
    public void onExit() {
    }

    public void exorcisePlayer(VampireUHCPlayer target, MarkerManager markerManager, int current_episode) {
        if (sosie == null || target == null) {
            return;
        }

        Player bukkitExorcist = Bukkit.getPlayer(sosie.getUuid());
        if (bukkitExorcist == null) {
            return;
        }

        if (current_episode == lastExorcisedEpisode) {
            bukkitExorcist.sendMessage(MessageUtil.error("Vous ne pouvez exorciser un joueur qu'une seule fois par épisode !"));
            return;
        }

        if (alreadyExorcised.contains(target.getUuid())) {
            bukkitExorcist.sendMessage(MessageUtil.error("Le joueur " + target.getLastKnownName() + " a déjà été exorcisé !"));
            return;
        }

        alreadyExorcised.add(target.getUuid());

        int count = 0;
        for (Marker marker : markerManager.getMarkers(target.getUuid())) {
            if (marker.getAura() == Aura.OBSCURE) {
                count++;
            }
        }

        // Il ne supprime qu'un seul des marqueurs obscurs (pouvoir imparfait).
        for (Marker marker : markerManager.getMarkers(target.getUuid())) {
            if (marker.getAura() == Aura.OBSCURE) {
                markerManager.removeMarker(target.getUuid(), marker);
                break;
            }
        }

        this.lastExorcisedEpisode = current_episode;

        bukkitExorcist.sendMessage(MessageUtil.serialize(
                "<dark_purple>Vous avez exorcisé <gold>" + target.getLastKnownName() + "</gold>."
                + " Il portait <white>" + count + "</white> marqueur(s) d'aura obscure.</dark_purple>"));
    }

    @Override
    public void saveState(JsonObject obj) {
        obj.addProperty("usurpedExorcistLastEpisode", lastExorcisedEpisode);
    }

    @Override
    public void restoreState(JsonObject obj) {
        if (obj.has("usurpedExorcistLastEpisode")) {
            lastExorcisedEpisode = obj.get("usurpedExorcistLastEpisode").getAsInt();
        }
    }

    public int getLastExorcisedEpisode() {
        return lastExorcisedEpisode;
    }
}