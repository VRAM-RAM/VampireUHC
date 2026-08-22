package fr.vampireuhc.listeners;

import fr.vampireuhc.VampireUHC;
import fr.vampireuhc.config.MessageUtil;
import fr.vampireuhc.game.GamePhase;
import fr.vampireuhc.player.VampireUHCPlayer;
import fr.vampireuhc.roles.SandMerchantRole;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitTask;

/**
 * Gère les connexions en cours de partie : spectateurs pour les late joiners,
 * grâce de déconnexion, et restauration de l'état à la reconnexion.
 */
public class PlayerConnectionListener implements Listener {
    private final VampireUHC plugin;
    private final Map<UUID, BukkitTask> graceTasks = new HashMap<>();

    public PlayerConnectionListener(VampireUHC plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        // Nom périmé sinon (messages, UI de vote, game-state.json) après un
        // changement de pseudo.
        VampireUHCPlayer known = plugin.getPlayerManager().get(player.getUniqueId());
        if (known != null && !player.getName().equals(known.getLastKnownName())) {
            known.setLastKnownName(player.getName());
        }

        VampireUHCPlayer vp = plugin.getPlayerManager().get(player.getUniqueId());

        if (!plugin.getGameManager().isGameStarted()) {
            return;
        }

        if (vp == null) {
            // Arrivée en pleine partie : impossible de la rejoindre, on spectate.
            toSpectator(player);
            return;
        }

        // Reconnexion d'un joueur enregistré : on annule sa grâce de déconnexion.
        BukkitTask task = graceTasks.remove(player.getUniqueId());
        if (task != null) {
            task.cancel();
            player.sendMessage(MessageUtil.success("Vous vous êtes reconnecté, votre partie continue !"));
        }

        // Effets du Marchand de sable différés : l'ensablé était peut-être hors
        // ligne au moment de la mort du marchand.
        for (VampireUHCPlayer p : plugin.getPlayerManager().getAll()) {
            if (p.getRole() instanceof SandMerchantRole merchant) {
                merchant.deliverPendingEffects(plugin.getMarkerManager(), player.getUniqueId());
                break;
            }
        }

        if (!vp.isAlive() || plugin.getGameManager().getPhase() == GamePhase.ENDED) {
            toSpectator(player);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        VampireUHCPlayer vp = plugin.getPlayerManager().get(player.getUniqueId());
        var game = plugin.getGameManager();

        if (vp == null || !vp.isAlive() || !game.isGameStarted()
                || game.getPhase() == GamePhase.ENDED) {
            return;
        }

        int graceMinutes = plugin.getConfigManager().getDisconnectGraceMinutes();
        MessageUtil.broadcast("<gray>" + vp.getLastKnownName() + " s'est déconnecté. Il sera éliminé dans <white>" + Math.max(1, graceMinutes) + "</white> min s'il ne revient pas.</gray>");
        startGrace(vp);
    }

    // Démarre la grâce de déconnexion d'un joueur (utilisé aussi à la restauration).
    public void startGrace(VampireUHCPlayer vp) {
        UUID uuid = vp.getUuid();
        int graceMinutes = Math.max(1, plugin.getConfigManager().getDisconnectGraceMinutes());
        graceTasks.put(uuid, Bukkit.getScheduler().runTaskLater(plugin, () -> {
            graceTasks.remove(uuid);
            if (Bukkit.getPlayer(uuid) == null && vp.isAlive()) {
                vp.setDead();
                MessageUtil.broadcast("<red>" + vp.getLastKnownName() + " <dark_red>a été éliminé (déconnexion).</dark_red></red>");
                plugin.getGameManager().checkWinCondition();
            }
        }, 20L * 60L * graceMinutes));
    }

    // Passe un joueur en spectateur (garde la position dans la map de la partie).
    public void toSpectator(Player player) {
        var world = plugin.getMapManager().getWorld();
        if (world != null) {
            player.teleport(world.getSpawnLocation());
        }
        player.setGameMode(GameMode.SPECTATOR);
        player.sendMessage(MessageUtil.info("Vous êtes spectateur."));
    }

    public void cancelAllGraceTasks() {
        graceTasks.values().forEach(BukkitTask::cancel);
        graceTasks.clear();
    }
}
