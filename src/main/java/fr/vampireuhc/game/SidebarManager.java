package fr.vampireuhc.game;

import fr.vampireuhc.VampireUHC;
import fr.vampireuhc.player.VampireUHCPlayer;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;

/**
 * Menu affiché à droite de l'écran pendant la partie : nom du plugin,
 * épisode, temps écoulé et nombre de joueurs en jeu. Rafraîchi chaque seconde.
 */
public class SidebarManager {
    private final VampireUHC plugin;
    private BukkitTask task;

    private static final String OBJECTIVE_NAME = "vuhc_sidebar";
    private static final String TITLE = ChatColor.DARK_PURPLE + "" + ChatColor.BOLD + "VampireUHC";

    // Entrées affichées à la dernière mise à jour, pour reset celles qui changent.
    private final Map<UUID, Set<String>> lastEntries = new HashMap<>();

    public SidebarManager(VampireUHC plugin) {
        this.plugin = plugin;
    }

    public void start() {
        if (task != null) {
            return;
        }
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::update, 0L, 20L);
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
        for (VampireUHCPlayer vp : plugin.getPlayerManager().getAll()) {
            Player p = Bukkit.getPlayer(vp.getUuid());
            if (p != null && p.isOnline()) {
                p.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
            }
        }
        lastEntries.clear();
    }

    public void update() {
        int episode = plugin.getGameManager().getEpisode() + 1;
        String time = formatTime(plugin.getGameManager().getElapsedSeconds());
        int players = plugin.getPlayerManager().getAll().size();

        for (VampireUHCPlayer vp : plugin.getPlayerManager().getAll()) {
            Player p = Bukkit.getPlayer(vp.getUuid());
            if (p == null || !p.isOnline()) {
                continue;
            }
            apply(p, episode, time, players);
        }
    }

    private void apply(Player player, int episode, String time, int players) {
        Scoreboard board = player.getScoreboard();
        Objective obj = board.getObjective(OBJECTIVE_NAME);
        if (obj == null) {
            board = Bukkit.getScoreboardManager().getNewScoreboard();
            obj = board.registerNewObjective(OBJECTIVE_NAME, "dummy", TITLE);
            obj.setDisplaySlot(DisplaySlot.SIDEBAR);
            player.setScoreboard(board);
        } else {
            obj.setDisplayName(TITLE);
        }

        Set<String> previous = lastEntries.remove(player.getUniqueId());
        if (previous != null) {
            for (String entry : previous) {
                board.resetScores(entry);
            }
        }

        Set<String> current = new HashSet<>();
        obj.getScore(ChatColor.GRAY + "Épisode : " + ChatColor.WHITE + episode).setScore(4);
        obj.getScore(ChatColor.GRAY + "Temps : " + ChatColor.WHITE + time).setScore(3);
        obj.getScore(ChatColor.GRAY + "Joueurs : " + ChatColor.WHITE + players).setScore(2);
        obj.getScore(" ").setScore(1);
        current.add(ChatColor.GRAY + "Épisode : " + ChatColor.WHITE + episode);
        current.add(ChatColor.GRAY + "Temps : " + ChatColor.WHITE + time);
        current.add(ChatColor.GRAY + "Joueurs : " + ChatColor.WHITE + players);
        current.add(" ");

        lastEntries.put(player.getUniqueId(), current);
    }

    private String formatTime(long seconds) {
        long h = seconds / 3600;
        long m = (seconds % 3600) / 60;
        long s = seconds % 60;
        if (h > 0) {
            return String.format("%d:%02d:%02d", h, m, s);
        }
        return String.format("%02d:%02d", m, s);
    }
}
