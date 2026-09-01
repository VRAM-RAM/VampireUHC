package fr.vampireuhc.game;

import fr.vampireuhc.VampireUHC;
import fr.vampireuhc.player.VampireUHCPlayer;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

/**
 * Suit les rencontres entre joueurs (« croisements »).
 *
 * Un scan périodique (toutes les 30 secondes) enregistre les paires de joueurs
 * vivants qui se trouvent à moins de CROSS_RADIUS blocs l'un de l'autre, dans
 * l'épisode en cours. À chaque frontière d'épisode, les données de l'épisode
 * précédent deviennent "éligibles" : c'est sur cette fenêtre que s'appuient le
 * vote vampire et la marque Maître (« un joueur qu'ils ont croisé durant
 * l'épisode précédent »).
 *
 * Non persisté en JSON : après un redémarrage, aucune rencontre éligible tant
 * qu'un épisode entier n'a pas été re-scanné.
 */
public class CrossTracker {

    // Rayon de croisement (en blocs).
    private static final double CROSS_RADIUS = 30.0;
    // Fréquence du scan.
    private static final long SCAN_INTERVAL_TICKS = 30 * 20L;

    private final VampireUHC plugin;

    // Croisements accumulateur pour l'épisode courant.
    private final Map<UUID, Set<UUID>> currentCrossings = new HashMap<>();
    // Croisements de l'épisode précédent : c'est la fenêtre éligible.
    private final Map<UUID, Set<UUID>> eligibleCrossings = new HashMap<>();

    private BukkitTask scanTask;

    public CrossTracker(VampireUHC plugin) {
        this.plugin = plugin;
    }

    // Démarre le scan (appelé au lancement/restauration de la partie).
    public void start() {
        if (scanTask != null) {
            return;
        }
        scanTask = Bukkit.getScheduler().runTaskTimer(plugin, this::scan, 30 * 20L, SCAN_INTERVAL_TICKS);
    }

    // Stoppe le scan et efface les données (fin/réinitialisation de partie).
    public void stop() {
        if (scanTask != null) {
            scanTask.cancel();
            scanTask = null;
        }
        currentCrossings.clear();
        eligibleCrossings.clear();
    }

    // Frontière d'épisode : les croisements de l'épisode qui vient de s'écouler
    // deviennent éligibles pour l'épisode qui démarre.
    public void advanceEpisode() {
        eligibleCrossings.clear();
        eligibleCrossings.putAll(currentCrossings);
        currentCrossings.clear();
    }

    // Renvoie true si a a croisé b durant l'épisode précédent.
    public boolean hasCrossed(UUID a, UUID b) {
        Set<UUID> crossed = eligibleCrossings.get(a);
        return crossed != null && crossed.contains(b);
    }

    // Scan périodique des positions des joueurs vivants.
    private void scan() {
        Set<UUID> alive = new HashSet<>();
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.getGameMode() == GameMode.SPECTATOR) {
                continue;
            }
            VampireUHCPlayer vp = plugin.getPlayerManager().get(p.getUniqueId());
            if (vp == null || !vp.isAlive() || vp.getCamp() == null) {
                continue;
            }
            alive.add(p.getUniqueId());
        }

        // Enregistre les paires dans les deux sens : la consultation par
        // hasCrossed(a, b) ne dépend plus de l'ordre du scan.
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (!alive.contains(p.getUniqueId())) {
                continue;
            }
            for (UUID other : alive) {
                if (other.equals(p.getUniqueId())) {
                    continue;
                }
                Player otherPlayer = Bukkit.getPlayer(other);
                if (otherPlayer == null || !sameWorld(p, otherPlayer)) {
                    continue;
                }
                if (p.getLocation().distanceSquared(otherPlayer.getLocation()) <= CROSS_RADIUS * CROSS_RADIUS) {
                    currentCrossings.computeIfAbsent(p.getUniqueId(), k -> new HashSet<>()).add(other);
                }
            }
        }
    }

    private boolean sameWorld(Player a, Player b) {
        World wa = a.getLocation().getWorld();
        World wb = b.getLocation().getWorld();
        return wa != null && wa.equals(wb);
    }
}