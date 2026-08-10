package fr.vampireuhc.game;

import fr.vampireuhc.VampireUHC;
import fr.vampireuhc.config.ConfigManager;
import fr.vampireuhc.markers.MarkerManager;
import fr.vampireuhc.player.Camp;
import fr.vampireuhc.player.PlayerManager;
import fr.vampireuhc.player.VampireUHCPlayer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

public class GameManager {
    private final VampireUHC plugin;
    private final PlayerManager playerManager;
    private final MarkerManager markerManager;
    private final ConfigManager configManager;

    private GamePhase phase = GamePhase.PRE_ROLES;
    private BukkitTask tickTask;
    private long startMillis;
    private int elapsedMinutes = 0;

    public GameManager(VampireUHC plugin, PlayerManager playerManager, MarkerManager markerManager, ConfigManager configManager) {
        this.plugin = plugin;
        this.playerManager = playerManager;
        this.markerManager = markerManager;
        this.configManager = configManager;
    }

    public GamePhase getPhase() { return phase; }
    public boolean isPvPActive() { return phase == GamePhase.PVP_ACTIVE; }
    public int getElapsedMinutes() { return elapsedMinutes; }

    // L'épisode actuel (1 épisode = episodeLength minutes).
    public int getEpisode() {
        int len = configManager.getEpisodeLength();
        return len > 0 ? elapsedMinutes / len : 0;
    }

    // Fonction qui commence la partie
    public void start() {
        if (tickTask != null) return;

        phase = GamePhase.PRE_ROLES;
        elapsedMinutes = 0;
        startMillis = System.currentTimeMillis();

        plugin.getMapManager().prepareWorld();
        plugin.getMapManager().teleportPlayersToSpawn();

        for (Player online: Bukkit.getOnlinePlayers()) {
            playerManager.register(online);
        }

        tickTask = Bukkit.getScheduler().runTaskTimer(plugin, this::onMinuteElapsed, 20 * 60L, 20 * 60L);
        broadcast("Partie lancee. Phase de préparation. Bonne chance et bonne game !");

    }   


    // Et fonction qui la termine
    public void stop() {
        if (tickTask != null) {
            tickTask.cancel();
            tickTask = null;
        }
        phase = GamePhase.ENDED;
    }

    // Helpers 

    private void onMinuteElapsed() {
        elapsedMinutes++;

        if (elapsedMinutes == configManager.getRoleAssignementAt() && phase == GamePhase.PRE_ROLES) {
            assignRolesAndCamps();
            checkInfections();
            broadcast("Les rôles ont été distribués.");
            phase = GamePhase.PRE_PVP;
        }

        if (elapsedMinutes == configManager.getPvpActivationAt() && phase == GamePhase.PRE_PVP) {
            activatePvp();
            plugin.getVoteManager().openVote();
        }

        int voteEvery = configManager.getVoteEveryMinutes();
        if (phase == GamePhase.PVP_ACTIVE && voteEvery > 0) {
            int minutesSincePvp = elapsedMinutes - configManager.getPvpActivationAt();
            if (minutesSincePvp > 0 && minutesSincePvp % voteEvery == 0) {
                plugin.getVoteManager().closeAndResolve();
                plugin.getVoteManager().openVote();
            }
        }

        if (plugin.getBuffManager() != null) {
            plugin.getBuffManager().applyBuffs();
        }

        checkVictory();
    }

    public void checkWinCondition() {
        checkVictory();
    }

    // Détecte la fin de partie : il ne reste qu'un camp de joueurs vivants.
    private void checkVictory() {
        if (phase == GamePhase.ENDED) {
            return;
        }
        Set<Camp> camps = new HashSet<>();
        for (VampireUHCPlayer p : playerManager.getAll()) {
            if (p.isAlive() && p.getCamp() != null) {
                camps.add(p.getCamp());
            }
        }
        if (camps.size() == 1 && !camps.isEmpty()) {
            broadcast("Le camp " + camps.iterator().next() + " remporte la partie !");
            stop();
        }
    }

    private void assignRolesAndCamps() {
        List<VampireUHCPlayer> pool = new ArrayList<>(playerManager.getAll());
        Collections.shuffle(pool);

        int total = pool.size();
        double ratio = total / (double) Math.max(1, configManager.getReferencePlayerCount());

        int vampireMax = clamp((int)Math.round(configManager.getVampireMin() * ratio), 
                (int)Math.round(configManager.getVampireMax() * ratio));
        int solitaireMax = clamp((int)Math.round(configManager.getSoloMin() * ratio),
                (int)Math.round(configManager.getSoloMax() * ratio));

        vampireMax = Math.min(vampireMax, total);
        solitaireMax = Math.min(solitaireMax, Math.max(0, total - vampireMax));

        // Garantir au moins un vampire (le Maître) dès qu'il y a des joueurs,
        // même dans les parties très réduites (tests solo notamment).
        vampireMax = Math.min(total, Math.max(vampireMax, 1));

        int index = 0;
        for (int i = 0; i < vampireMax && index < total; i++, index++) {
            pool.get(index).setCamp(Camp.VAMPIRE);
        }
        for (int i = 0; i < solitaireMax && index < total; i++, index++) {
            pool.get(index).setCamp(Camp.SOLO);
        }
        for (; index < total; index++) {
            pool.get(index).setCamp(Camp.VILLAGEOIS);
        }

        for (VampireUHCPlayer vp : pool) {
            Player bukkitPlayer = Bukkit.getPlayer(vp.getUuid());
            if (bukkitPlayer != null) {
                bukkitPlayer.sendMessage(configManager.getPrefix() + 
                    ChatColor.translateAlternateColorCodes('&', "Votre camp : &e" + vp.getCamp()));
            }
        }

        // Deleguer a RoleManager pour attribuer les roles precis
        plugin.getRoleManager().assignRolesToPlayers();
    }

    private int clamp(int value, int max) { return Math.min(value, max); }

    private void activatePvp() {
        phase = GamePhase.PVP_ACTIVE;
        List<VampireUHCPlayer> vampires = playerManager.getByCamp(Camp.VAMPIRE);
        StringBuilder names = new StringBuilder();
        for (VampireUHCPlayer v : vampires) {
            v.setVampireListRevealation();
            if (names.length() > 0) names.append(", ");
            names.append(v.getLastKnownName());
        }

        for (VampireUHCPlayer v : vampires) {
            Player bukkitPlayer = Bukkit.getPlayer(v.getUuid());
            if (bukkitPlayer != null) {
                bukkitPlayer.sendMessage(configManager.getPrefix() + 
                    ChatColor.translateAlternateColorCodes('&', "&cVos allies vampires : &f" + names));
            }
        }

        broadcast("Le PVP est desormais actif !");
    }

    private void checkInfections() {
        for (VampireUHCPlayer vp : playerManager.getAll()) {
            if (!vp.isVampireListRevealed() && vp.getCamp() == Camp.VAMPIRE) {
                // Notification aux vampires uniquement
                Player bukkit = Bukkit.getPlayer(vp.getUuid());
                if (bukkit != null) {
                    bukkit.sendMessage(configManager.getPrefix() + 
                        ChatColor.translateAlternateColorCodes('&', "&cUn nouveau joueur a rejoint votre camp ! /vuhc role pour en savoir plus."));
                }
            }
        }
    }

    private void broadcast(String message) {
        Bukkit.broadcastMessage(configManager.getPrefix() + 
            ChatColor.translateAlternateColorCodes('&', message));
    }
}
