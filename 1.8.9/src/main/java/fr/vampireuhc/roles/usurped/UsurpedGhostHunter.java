package fr.vampireuhc.roles.usurped;

import fr.vampireuhc.VampireUHC;
import fr.vampireuhc.config.MessageUtil;
import fr.vampireuhc.player.Camp;
import fr.vampireuhc.player.VampireUHCPlayer;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * Chasseur de Fantômes copié par le Sosie : copie exacte du vrai Chasseur avec petit nerf
 * (fenêtre de 10 minutes au lieu de 20, gates épisode + jamais deux fois le même joueur,
 * pas d'auto-ciblage), sans aucune particularité.
 */
public class UsurpedGhostHunter implements UsurpedPower {

    // Durée de la traque : 10 minutes.
    private static final long TRAQUE_TICKS = 20 * 60 * 10L;

    private VampireUHCPlayer sosie;
    private int lastTraqueEpisode = -1;
    private final Set<UUID> trackedPlayers = new HashSet<>();
    private UUID activeTarget;
    private BukkitTask resolveTask;

    @Override
    public String getName() {
        return "Chasseur de Fantômes";
    }

    @Override
    public void onEnter(VampireUHCPlayer doppelganger) {
        this.sosie = doppelganger;
    }

    @Override
    public void onExit() {
        cancelTraque();
        trackedPlayers.clear();
        lastTraqueEpisode = -1;
    }

    @Override
    public void onGameEnd() {
        onExit();
    }

    public void traque(VampireUHCPlayer target, int currentEpisode) {
        if (sosie == null || target == null) {
            return;
        }

        Player bukkitSosie = Bukkit.getPlayer(sosie.getUuid());
        if (bukkitSosie == null) {
            return;
        }

        if (currentEpisode == lastTraqueEpisode) {
            bukkitSosie.sendMessage(MessageUtil.error("Vous ne pouvez traquer qu'une seule fois par épisode !"));
            return;
        }

        if (target.getUuid().equals(sosie.getUuid())) {
            bukkitSosie.sendMessage(MessageUtil.error("Vous ne pouvez pas vous traquer vous-même."));
            return;
        }

        if (trackedPlayers.contains(target.getUuid())) {
            bukkitSosie.sendMessage(MessageUtil.error("Vous avez déjà traqué ce joueur au cours de la partie."));
            return;
        }

        this.lastTraqueEpisode = currentEpisode;
        this.trackedPlayers.add(target.getUuid());
        this.activeTarget = target.getUuid();

        VampireUHC.getInstance().getCrossTracker().startTracking(activeTarget);
        resolveTask = Bukkit.getScheduler().runTaskLater(
                VampireUHC.getInstance(), this::resolveTraque, TRAQUE_TICKS);

        bukkitSosie.sendMessage(MessageUtil.success("Vous traquez <gold>" + target.getLastKnownName()
                + "</gold>. Présences spectrales comptées pendant 10 minutes."));
    }

    // Fin de la fenêtre : compte des non-villageois croisés (camp final).
    private void resolveTraque() {
        UUID targetUuid = activeTarget;
        if (targetUuid == null) {
            return;
        }
        activeTarget = null;
        resolveTask = null;

        Set<UUID> crossed = VampireUHC.getInstance().getCrossTracker().stopTracking(targetUuid);
        int count = 0;
        for (UUID id : crossed) {
            VampireUHCPlayer crossedPlayer = VampireUHC.getInstance().getPlayerManager().get(id);
            if (crossedPlayer != null && crossedPlayer.getCamp() != null
                    && crossedPlayer.getCamp() != Camp.VILLAGEOIS) {
                count++;
            }
        }

        // Le joueur ciblé compte lui-même s'il est non-villageois.
        VampireUHCPlayer target = VampireUHC.getInstance().getPlayerManager().get(targetUuid);
        if (target != null && target.getCamp() != null && target.getCamp() != Camp.VILLAGEOIS) {
            count++;
        }

        Player bukkitSosie = Bukkit.getPlayer(sosie.getUuid());
        if (bukkitSosie == null) {
            return;
        }

        String message;
        if (count == 0) {
            message = "Vous n'avez enregistré aucune présence spectrale.";
        } else if (count <= 2) {
            message = "Vous avez enregistré une présence spectrale.";
        } else {
            message = "Vous avez enregistré la présence de nombreux spectres !";
        }
        bukkitSosie.sendMessage(MessageUtil.info(message));
    }

    // Annule une fenêtre en cours (mort de l'usurpé, fin de partie, ...).
    private void cancelTraque() {
        if (resolveTask != null) {
            resolveTask.cancel();
            resolveTask = null;
        }
        if (activeTarget != null) {
            VampireUHC.getInstance().getCrossTracker().stopTracking(activeTarget);
            activeTarget = null;
        }
    }

    @Override
    public void saveState(JsonObject obj) {
        obj.addProperty("usurpedGhostHunterLastEpisode", lastTraqueEpisode);
        JsonObject trackedObj = new JsonObject();
        for (UUID id : trackedPlayers) {
            trackedObj.addProperty(id.toString(), true);
        }
        obj.add("usurpedGhostHunterTracked", trackedObj);
    }

    @Override
    public void restoreState(JsonObject obj) {
        lastTraqueEpisode = obj.has("usurpedGhostHunterLastEpisode")
                ? obj.get("usurpedGhostHunterLastEpisode").getAsInt() : -1;
        trackedPlayers.clear();
        if (obj.has("usurpedGhostHunterTracked") && obj.get("usurpedGhostHunterTracked").isJsonObject()) {
            for (Map.Entry<String, JsonElement> e : obj.getAsJsonObject("usurpedGhostHunterTracked").entrySet()) {
                try {
                    trackedPlayers.add(UUID.fromString(e.getKey()));
                } catch (IllegalArgumentException ignored) {
                    // Entrée invalide : on saute ce joueur.
                }
            }
        }
        // Une fenêtre de traque en cours ne survit pas à un redémarrage.
        cancelTraque();
    }
}