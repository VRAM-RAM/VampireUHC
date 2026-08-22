package fr.vampireuhc.game;

import fr.vampireuhc.VampireUHC;
import fr.vampireuhc.config.MessageUtil;
import fr.vampireuhc.player.VampireUHCPlayer;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.scheduler.BukkitTask;

/**
 * Permet aux spectateurs de suivre un joueur vivant (commande /vuhc spectate
 * ou clic droit dessus).
 */
public class SpectatorManager implements Listener {
    private final VampireUHC plugin;
    private final Map<UUID, UUID> follow = new HashMap<>();
    private BukkitTask task;

    public SpectatorManager(VampireUHC plugin) {
        this.plugin = plugin;
    }

    public void start() {
        if (task != null) {
            return;
        }
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::update, 0L, 10L);
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
        follow.clear();
    }

    // Le spectateur suit la cible (retourne false si impossible).
    public boolean follow(Player spectator, Player target) {
        if (spectator.getGameMode() != GameMode.SPECTATOR) {
            spectator.sendMessage(MessageUtil.error("Vous devez être spectateur pour utiliser cette commande."));
            return false;
        }
        VampireUHCPlayer vp = plugin.getPlayerManager().get(target.getUniqueId());
        if (vp == null || !vp.isAlive()) {
            spectator.sendMessage(MessageUtil.error("Ce joueur n'est pas en partie ou est mort."));
            return false;
        }
        follow.put(spectator.getUniqueId(), target.getUniqueId());
        spectator.sendMessage(MessageUtil.successTarget("Vous suivez désormais", target.getName()));
        return true;
    }

    public boolean unfollow(Player spectator) {
        return follow.remove(spectator.getUniqueId()) != null;
    }

    // Clic droit sur un joueur vivant : le spectateur le suit (ou arrête de le suivre).
    @EventHandler
    public void onInteractAtEntity(PlayerInteractAtEntityEvent event) {
        Player spectator = event.getPlayer();
        if (spectator.getGameMode() != GameMode.SPECTATOR) {
            return;
        }
        if (!(event.getRightClicked() instanceof Player target)) {
            return;
        }
        event.setCancelled(true);
        if (follow.containsKey(spectator.getUniqueId())
                && follow.get(spectator.getUniqueId()).equals(target.getUniqueId())) {
            unfollow(spectator);
            spectator.sendMessage(MessageUtil.info("Vous ne suivez plus personne."));
        } else {
            follow(spectator, target);
        }
    }

    private void update() {
        if (follow.isEmpty()) {
            return;
        }
        for (Map.Entry<UUID, UUID> entry : new HashMap<>(follow).entrySet()) {
            Player spectator = Bukkit.getPlayer(entry.getKey());
            Player target = Bukkit.getPlayer(entry.getValue());
            if (spectator == null || !spectator.isOnline()
                    || target == null || !target.isOnline()
                    || spectator.getGameMode() != GameMode.SPECTATOR) {
                follow.remove(entry.getKey());
                continue;
            }
            Location targetLoc = target.getLocation();
            Location current = spectator.getLocation();
            // Déjà sur place (monde + position) : inutile de re-téléporter.
            if (current.getWorld().equals(targetLoc.getWorld())
                    && current.distanceSquared(targetLoc) < 0.01) {
                continue;
            }
            spectator.teleport(targetLoc);
        }
    }
}
