package fr.vampireuhc.game;

import fr.vampireuhc.VampireUHC;
import fr.vampireuhc.player.VampireUHCPlayer;

import java.util.Arrays;
import java.util.HashSet;
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
 *
 * Les valeurs affichées sont globales à tous les joueurs : elles sont calculées
 * une seule fois par tick et les scoreboards ne sont touchés que lorsqu'une
 * valeur change réellement (aucune allocation par joueur/seconde).
 */
public class SidebarManager {
    private final VampireUHC plugin;
    private BukkitTask task;

    private static final String OBJECTIVE_NAME = "vuhc_sidebar";
    private static final String TITLE = ChatColor.DARK_PURPLE + "" + ChatColor.BOLD + "VampireUHC";

    // Lignes affichées lors de la dernière mise à jour (identiques pour tous).
    private String[] lastLines = null;
    // Joueurs dont le scoreboard affiche déjà ces lignes.
    private final Set<UUID> rendered = new HashSet<>();

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
        lastLines = null;
        rendered.clear();
    }

    public void update() {
        int episode = plugin.getGameManager().getEpisode() + 1;
        String time = formatTime(plugin.getGameManager().getElapsedSeconds());
        int players = plugin.getPlayerManager().getAll().size();
        int groupsOf = plugin.getGroupsManager().getPeopleByGroups();

        String[] lines = {
                ChatColor.GRAY + "Groupes de : " + ChatColor.WHITE + groupsOf,
                ChatColor.GRAY + "Épisode : " + ChatColor.WHITE + episode,
                ChatColor.GRAY + "Temps : " + ChatColor.WHITE + time,
                ChatColor.GRAY + "Joueurs : " + ChatColor.WHITE + players,
                " "
        };

        boolean changed = lastLines == null || !Arrays.equals(lines, lastLines);

        for (VampireUHCPlayer vp : plugin.getPlayerManager().getAll()) {
            Player p = Bukkit.getPlayer(vp.getUuid());
            if (p == null || !p.isOnline()) {
                continue;
            }
            if (!changed && rendered.contains(vp.getUuid())) {
                continue;
            }
            apply(p, lines);
            rendered.add(vp.getUuid());
        }

        lastLines = lines;
    }

    private void apply(Player player, String[] lines) {
        Scoreboard board = player.getScoreboard();
        Objective obj = board.getObjective(OBJECTIVE_NAME);
        if (obj == null) {
            board = Bukkit.getScoreboardManager().getNewScoreboard();
            obj = board.registerNewObjective(OBJECTIVE_NAME, "dummy", TITLE);
            obj.setDisplaySlot(DisplaySlot.SIDEBAR);
            player.setScoreboard(board);
        }

        // Retire uniquement les anciennes lignes qui n'existent plus.
        if (lastLines != null) {
            for (String old : lastLines) {
                boolean stillUsed = false;
                for (String line : lines) {
                    if (line.equals(old)) {
                        stillUsed = true;
                        break;
                    }
                }
                if (!stillUsed) {
                    board.resetScores(old);
                }
            }
        }

        int score = lines.length;
        for (String line : lines) {
            obj.getScore(line).setScore(score--);
        }
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
