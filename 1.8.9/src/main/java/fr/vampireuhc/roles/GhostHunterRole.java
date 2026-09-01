package fr.vampireuhc.roles;

import fr.vampireuhc.VampireUHC;
import fr.vampireuhc.config.MessageUtil;
import fr.vampireuhc.player.Camp;
import fr.vampireuhc.player.VampireUHCPlayer;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

/**
 * Chasseur de Fantômes : rôle villageois à informations.
 *
 * À chaque épisode, /vuhc traquer <joueur> ouvre une fenêtre de 20 minutes
 * pendant laquelle les croisements du joueur ciblé sont comptabilisés (rayon
 * du CrossTracker). À la fin, le Chasseur reçoit selon le nombre de joueurs
 * non-villageois croisés (camp final) :
 *  - 0   -> aucune présence spectrale
 *  - 1-2 -> une présence spectrale
 *  - 3+  -> de nombreux spectres
 *
 * Le joueur ciblé compte lui-même s'il est non-villageois.
 * Contraintes : une fois par épisode, jamais deux fois le même joueur en
 * partie, pas d'auto-ciblage.
 *
 * La fenêtre de traque en cours n'est pas persistée en JSON (le CrossTracker
 * ne l'est pas non plus) : seuls les gates et les joueurs déjà traqués le sont.
 */
public class GhostHunterRole implements Role {

    // Durée de la traque : 20 minutes.
    private static final long TRAQUE_TICKS = 20 * 60 * 20L;

    private VampireUHCPlayer hunter;

    // Gates : une fois par épisode, jamais deux fois le même joueur.
    private int lastTraqueEpisode = -1;
    private final Set<UUID> trackedPlayers = new HashSet<>();

    // Fenêtre de traque en cours.
    private UUID activeTarget;
    private BukkitTask resolveTask;

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
        this.hunter = player;
    }

    @Override
    public String getName() {
        return "Chasseur de Fantômes";
    }

    @Override
    public void onGameEnd() {
        cancelTraque();
        lastTraqueEpisode = -1;
        trackedPlayers.clear();
    }

    @Override
    public String getDescription() {
        return
            "<gray>Vous devez gagner avec le <green>village</green>. Vous pouvez détecter les présences spectrales.</gray>\n\n"
            + "<dark_purple>▸</dark_purple> <gray>À chaque épisode, à l'aide de la commande <gold>/vuhc traquer <joueur></gold>, vous traquez un joueur pendant <yellow>20 minutes</yellow>.</gray>\n\n"
            + "  <gray>À la fin, selon le nombre de joueurs <red>non-villageois</red> qu'il a croisés :</gray>\n"
            + "  <gray>→ <white>0</white> : aucune présence spectrale.</gray>\n"
            + "  <gray>→ <white>1-2</white> : une présence spectrale.</gray>\n"
            + "  <gray>→ <white>3+</white> : de nombreux spectres.</gray>\n\n"
            + "<red>⚠ Vous ne pouvez pas traquer le même joueur deux fois en partie, ni vous traquer vous-même.</red>";
    }

    // Pouvoir spécifique au Chasseur de Fantômes :

    public void traque(VampireUHCPlayer target, int currentEpisode) {
        if (hunter == null || target == null) {
            return;
        }

        Player bukkitHunter = Bukkit.getPlayer(hunter.getUuid());
        if (bukkitHunter == null) {
            return;
        }

        if (currentEpisode == lastTraqueEpisode) {
            bukkitHunter.sendMessage(MessageUtil.error("Vous ne pouvez traquer qu'une seule fois par épisode !"));
            return;
        }

        if (target.getUuid().equals(hunter.getUuid())) {
            bukkitHunter.sendMessage(MessageUtil.error("Vous ne pouvez pas vous traquer vous-même."));
            return;
        }

        if (trackedPlayers.contains(target.getUuid())) {
            bukkitHunter.sendMessage(MessageUtil.error("Vous avez déjà traqué ce joueur au cours de la partie."));
            return;
        }

        this.lastTraqueEpisode = currentEpisode;
        this.trackedPlayers.add(target.getUuid());
        this.activeTarget = target.getUuid();

        VampireUHC.getInstance().getCrossTracker().startTracking(activeTarget);
        resolveTask = Bukkit.getScheduler().runTaskLater(
                VampireUHC.getInstance(), this::resolveTraque, TRAQUE_TICKS);

        bukkitHunter.sendMessage(MessageUtil.success("Vous traquez <gold>" + target.getLastKnownName()
                + "</gold>. Présences spectrales comptées pendant 20 minutes."));
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

        Player bukkitHunter = Bukkit.getPlayer(hunter.getUuid());
        if (bukkitHunter == null) {
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
        bukkitHunter.sendMessage(MessageUtil.info(message));
    }

    // Annule une fenêtre en cours (fin de partie, restauration, ...).
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

    // --- Accès pour la sauvegarde JSON (RoleManager) ---

    public int getLastTraqueEpisode() {
        return lastTraqueEpisode;
    }

    public Set<UUID> getTrackedPlayers() {
        return trackedPlayers;
    }

    public void restoreState(int lastEpisode, Set<UUID> tracked) {
        this.lastTraqueEpisode = lastEpisode;
        if (tracked != null) {
            this.trackedPlayers.clear();
            this.trackedPlayers.addAll(tracked);
        }
        // Une fenêtre de traque en cours ne survit pas à un redémarrage.
        cancelTraque();
    }
}