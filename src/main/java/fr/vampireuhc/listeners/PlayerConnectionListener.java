package fr.vampireuhc.listeners;

import fr.vampireuhc.VampireUHC;
import fr.vampireuhc.game.GamePhase;
import fr.vampireuhc.player.VampireUHCPlayer;

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
            player.sendMessage(plugin.getConfigManager().translate("&aVous vous êtes reconnecté, votre partie continue !"));
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

        int graceMinutes = Math.max(1, plugin.getConfigManager().getDisconnectGraceMinutes());
        graceTasks.put(player.getUniqueId(), Bukkit.getScheduler().runTaskLater(plugin, () -> {
            graceTasks.remove(player.getUniqueId());
            if (!player.isOnline() && vp.isAlive()) {
                vp.setDead();
                Bukkit.broadcastMessage(plugin.getConfigManager().translate(
                        "&c" + vp.getLastKnownName() + " &4a été éliminé (déconnexion)."));
                game.checkWinCondition();
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
        player.sendMessage(plugin.getConfigManager().translate("&7Vous êtes spectateur."));
    }

    public void cancelAllGraceTasks() {
        graceTasks.values().forEach(BukkitTask::cancel);
        graceTasks.clear();
    }
}
