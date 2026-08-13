package fr.vampireuhc.game;

import fr.vampireuhc.VampireUHC;
import fr.vampireuhc.config.ConfigManager;
import fr.vampireuhc.markers.MarkerManager;
import fr.vampireuhc.player.Camp;
import fr.vampireuhc.player.PlayerManager;
import fr.vampireuhc.player.VampireUHCPlayer;
import fr.vampireuhc.roles.RoleType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

public class GameManager {
    private final VampireUHC plugin;
    private final PlayerManager playerManager;
    private final MarkerManager markerManager;
    private final ConfigManager configManager;

    private GamePhase phase = GamePhase.PRE_ROLES;
    private BukkitTask tickTask;
    private BukkitTask countdownTask;
    private long startMillis;
    private int elapsedMinutes = 0;
    private int countdownRemaining = 0;
    private boolean gameStarted = false;

    public GameManager(VampireUHC plugin, PlayerManager playerManager, MarkerManager markerManager, ConfigManager configManager) {
        this.plugin = plugin;
        this.playerManager = playerManager;
        this.markerManager = markerManager;
        this.configManager = configManager;
    }

    public GamePhase getPhase() { return phase; }
    public boolean isPvPActive() { return phase == GamePhase.PVP_ACTIVE; }
    public int getElapsedMinutes() { return elapsedMinutes; }
    public boolean isGameStarted() { return gameStarted; }

    public int getDefaultCountdownSeconds() {
        return configManager.getDefaultCountdownSeconds();
    }

    // Temps écoulé en secondes depuis le début de la partie (0 si pas encore lancée).
    public long getElapsedSeconds() {
        if (startMillis == 0) {
            return 0;
        }
        return (System.currentTimeMillis() - startMillis) / 1000L;
    }

    public void setElapsedMinutes(int time) {
        this.elapsedMinutes = time;
        var virtualStartMillis = System.currentTimeMillis() - (time * 60000);
        this.startMillis = virtualStartMillis;
    }

    // L'épisode actuel (1 épisode = episodeLength minutes).
    public int getEpisode() {
        int len = configManager.getEpisodeLength();
        return len > 0 ? elapsedMinutes / len : 0;
    }

    // Lance un compte à rebours puis démarre la partie.
    public boolean startCountdown(int seconds) {
        if (countdownTask != null || tickTask != null) {
            return false;
        }

        // Une partie précédente existe encore : on réinitialise d'abord.
        if (plugin.getMapManager().getWorld() != null || !playerManager.getAll().isEmpty()) {
            resetGame();
        }

        countdownRemaining = Math.max(0, seconds);
        if (countdownRemaining <= 0) {
            beginGame();
            return true;
        }

        broadcast("&5La partie commence dans &f" + countdownRemaining + "&5 secondes !");
        playSoundAll(Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 1f);

        countdownTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            countdownRemaining--;
            if (countdownRemaining <= 0) {
                countdownTask.cancel();
                countdownTask = null;
                beginGame();
                return;
            }
            if (countdownRemaining <= 10) {
                broadcast("&5La partie commence dans &f" + countdownRemaining + "&5 secondes !");
                playSoundAll(Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 1f);
            }
        }, 20L, 20L);
        return true;
    }

    // Démarre la partie avec le compte à rebours par défaut.
    public void start() {
        startCountdown(configManager.getDefaultCountdownSeconds());
    }

    private void beginGame() {
        phase = GamePhase.PRE_ROLES;
        elapsedMinutes = 0;
        startMillis = System.currentTimeMillis();
        gameStarted = true;

        plugin.getMapManager().prepareWorld();
        plugin.getMapManager().teleportPlayersToSpawn();

        for (Player online : Bukkit.getOnlinePlayers()) {
            playerManager.register(online);
            online.setGameMode(GameMode.SURVIVAL);
        }

        giveStartingKit();

        tickTask = Bukkit.getScheduler().runTaskTimer(plugin, this::onMinuteElapsed, 20 * 60L, 20 * 60L);
        broadcast("Partie lancée. Phase de préparation. Bonne chance et bonne game !");
        if (plugin.getSidebarManager() != null) {
            plugin.getSidebarManager().start();
        }
        if (plugin.getSpectatorManager() != null) {
            plugin.getSpectatorManager().start();
        }
    }

    // Reprend une partie sauvegardée après un redémarrage du serveur.
    public void restoreGame(GamePhase restoredPhase, int restoredElapsedMinutes) {
        if (tickTask != null) {
            tickTask.cancel();
        }
        if (countdownTask != null) {
            countdownTask.cancel();
            countdownTask = null;
        }
        phase = restoredPhase;
        elapsedMinutes = Math.max(0, restoredElapsedMinutes);
        startMillis = System.currentTimeMillis() - elapsedMinutes * 60_000L;
        gameStarted = true;

        // Charge la world existante SANS ré-éparpiller les joueurs.
        plugin.getMapManager().prepareWorld();

        for (Player online : Bukkit.getOnlinePlayers()) {
            VampireUHCPlayer vp = playerManager.get(online.getUniqueId());
            online.setGameMode(vp != null && vp.isAlive() ? GameMode.SURVIVAL : GameMode.SPECTATOR);
        }

        // Grâce de déconnexion pour les vivants hors ligne.
        for (VampireUHCPlayer vp : playerManager.getAll()) {
            if (vp.isAlive() && Bukkit.getPlayer(vp.getUuid()) == null) {
                plugin.getConnectionListener().startGrace(vp);
            }
        }

        long delay = (60 - (getElapsedSeconds() % 60)) * 20L;
        tickTask = Bukkit.getScheduler().runTaskTimer(plugin, this::onMinuteElapsed, delay, 20 * 60L);

        if (plugin.getSidebarManager() != null) {
            plugin.getSidebarManager().start();
        }
        if (plugin.getSpectatorManager() != null) {
            plugin.getSpectatorManager().start();
        }
        broadcast("&5La partie a été restaurée.");
    }

    // Et fonction qui la termine
    public void stop() {
        boolean wasStarted = gameStarted || tickTask != null;
        if (tickTask != null) {
            tickTask.cancel();
            tickTask = null;
        }
        if (countdownTask != null) {
            countdownTask.cancel();
            countdownTask = null;
        }
        phase = GamePhase.ENDED;
        notifyRolesGameEnd();
        if (plugin.getSidebarManager() != null) {
            plugin.getSidebarManager().stop();
        }
        if (plugin.getSpectatorManager() != null) {
            plugin.getSpectatorManager().stop();
        }
        if (plugin.getConnectionListener() != null) {
            plugin.getConnectionListener().cancelAllGraceTasks();
        }
        if (wasStarted) {
            for (Player player : Bukkit.getOnlinePlayers()) {
                player.setGameMode(GameMode.SPECTATOR);
            }
        }
    }

    // Réinitialise complètement la partie (joueurs, marqueurs, map) pour une nouvelle partie.
    public void resetGame() {
        if (tickTask != null) {
            tickTask.cancel();
            tickTask = null;
        }
        if (countdownTask != null) {
            countdownTask.cancel();
            countdownTask = null;
        }
        phase = GamePhase.NOT_STARTED;
        gameStarted = false;
        startMillis = 0;
        elapsedMinutes = 0;

        notifyRolesGameEnd();
        playerManager.reset();
        markerManager.clearMarkersOnAll();
        plugin.getVoteManager().reset();

        if (plugin.getSidebarManager() != null) {
            plugin.getSidebarManager().stop();
        }
        if (plugin.getSpectatorManager() != null) {
            plugin.getSpectatorManager().stop();
        }
        if (plugin.getConnectionListener() != null) {
            plugin.getConnectionListener().cancelAllGraceTasks();
        }

        plugin.getMapManager().resetWorld();

        // Remise à zéro des joueurs : mode survie dans le monde principal, inventaire vidé.
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.setGameMode(GameMode.SURVIVAL);
            player.getInventory().clear();
            player.setMaxHealth(20.0);
            player.setTotalExperience(0);
            player.getActivePotionEffects().clear();
            player.setHealth(20.0);
            player.setFoodLevel(20);
            player.setSaturation(20f);
            player.teleport(Bukkit.getWorlds().get(0).getSpawnLocation());
        }
        broadcast("&5La partie a été réinitialisée.");
    }

    // Helpers 

    // Notifie chaque rôle que la partie est terminée/réinitialisée (nettoyage des tâches planifiées).
    private void notifyRolesGameEnd() {
        for (VampireUHCPlayer p : playerManager.getAll()) {
            if (p.getRole() != null) {
                p.getRole().onGameEnd();
            }
        }
    }

    private void onMinuteElapsed() {
        elapsedMinutes++;

        int rolesAt = configManager.getRoleAssignementAt();
        int pvpAt = configManager.getPvpActivationAt();

        if (elapsedMinutes == rolesAt - 5) {
            broadcast("&eLes rôles seront distribués dans 5 minutes !");
            playSoundAll(Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 1.5f);
        }
        if (elapsedMinutes == rolesAt - 1) {
            broadcast("&eLes rôles seront distribués dans 1 minute !");
            playSoundAll(Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 1.5f);
        }

        if (elapsedMinutes == rolesAt && phase == GamePhase.PRE_ROLES) {
            assignRolesAndCamps();
            checkInfections();
            announceRoles();
            broadcast("Les rôles ont été distribués.");
            phase = GamePhase.PRE_PVP;
            playSoundAll(Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
        }

        if (elapsedMinutes == pvpAt - 5) {
            broadcast("&cLe PvP sera activé dans 5 minutes !");
            playSoundAll(Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 1.5f);
        }
        if (elapsedMinutes == pvpAt - 1) {
            broadcast("&cLe PvP sera activé dans 1 minute !");
            playSoundAll(Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 1.5f);
        }

        if (elapsedMinutes == pvpAt && phase == GamePhase.PRE_PVP) {
            activatePvp();
            plugin.getVoteManager().openVote();
            playSoundAll(Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
        }

        int voteEvery = configManager.getVoteEveryMinutes();
        if (phase == GamePhase.PVP_ACTIVE && voteEvery > 0) {
            int minutesSincePvp = elapsedMinutes - pvpAt;
            if (minutesSincePvp > 0 && minutesSincePvp % voteEvery == 0) {
                plugin.getVoteManager().closeAndResolve();
                plugin.getVoteManager().openVote();
            }
        }

        // Annonce du début de chaque épisode.
        int episodeLength = configManager.getEpisodeLength();
        if (episodeLength > 0 && elapsedMinutes > 0 && elapsedMinutes % episodeLength == 0) {
            broadcast("&5Début de l'épisode &f" + (elapsedMinutes / episodeLength + 1) + "&5 !");
            playSoundAll(Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.2f);
        }

        if (plugin.getBuffManager() != null) {
            plugin.getBuffManager().applyBuffs();
        }

        checkVictory();
    }

    public void checkWinCondition() {
        checkVictory();
    }

    // Détecte la fin de partie : il ne reste qu'un camp (ou un seul solitaire) en vie.
    private void checkVictory() {
        if (phase == GamePhase.ENDED) {
            return;
        }

        Set<String> factions = new LinkedHashSet<>();
        UUID soloWinner = null;
        for (VampireUHCPlayer p : playerManager.getAll()) {
            if (!p.isAlive() || p.getCamp() == null) {
                continue;
            }
            if (p.getCamp() == Camp.SOLO) {
                factions.add("solo:" + p.getUuid());
                soloWinner = p.getUuid();
            } else {
                factions.add(p.getCamp().name());
            }
        }

        if (factions.size() == 1 && !factions.isEmpty()) {
            String winner;
            if (soloWinner != null) {
                VampireUHCPlayer solo = playerManager.get(soloWinner);
                winner = "Le Solitaire " + (solo != null ? solo.getLastKnownName() : "?");
            } else if (factions.contains(Camp.VAMPIRE.name())) {
                winner = "Les Vampires";
            } else {
                winner = "Les Villageois";
            }
            broadcast("&5" + winner + " &fremporte la partie !");
            playSoundAll(Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
            stop();
        }
    }

    // Pour debug et OP seulement
    public void setRole(Player player, RoleType type) {
        var vampirePlayer = playerManager.get(player.getUniqueId());
        if (vampirePlayer == null) {
            player.sendMessage("Vous n'êtes pas en partie.");
            return;
        }
        fr.vampireuhc.roles.RoleManager manager = plugin.getRoleManager();
        manager.setRoleFromType(vampirePlayer, type);
        announceRoles();
        broadcast("Votre rôle a été distribué. Si vous n'êtes pas en partie de devtest, alors il y a un probleme...");
        playSoundAll(Sound.ENTITY_WITHER_DEATH, 1f, 1f);
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
            // Les vampires de naissance peuvent voter pour la marque vampire (pas les infectés).
            if (vp.getCamp() == Camp.VAMPIRE) {
                vp.setVampireVote();
            }
            Player bukkitPlayer = Bukkit.getPlayer(vp.getUuid());
            if (bukkitPlayer != null) {
                bukkitPlayer.sendMessage(configManager.translate("Votre camp : &e" + vp.getCamp().getDisplayName()));
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
                bukkitPlayer.sendMessage(configManager.translate("&cVos alliés vampires : &f" + names));
            }
        }

        broadcast("Le PVP est desormais actif !");
    }

    // Annonce à chaque joueur son rôle précis (après l'assignation).
    private void announceRoles() {
        for (VampireUHCPlayer vp : playerManager.getAll()) {
            Player bukkitPlayer = Bukkit.getPlayer(vp.getUuid());
            if (bukkitPlayer == null) {
                continue;
            }
            if (vp.getRole() != null) {
                bukkitPlayer.sendMessage(configManager.translate("&eVous êtes : &f" + vp.getRole().getName()));
                bukkitPlayer.sendMessage(configManager.translateRaw("&7" + vp.getRole().getDescription()));
            } else {
                bukkitPlayer.sendMessage(configManager.translate("&eVous êtes : &fVillageois"));
            }
        }
    }

    // Notifie les joueurs infectés en cours de partie (jamais les vampires de naissance).
    private void checkInfections() {
        for (VampireUHCPlayer vp : playerManager.getAll()) {
            if (vp.isInfected() && !vp.isVampireListRevealed()) {
                Player bukkit = Bukkit.getPlayer(vp.getUuid());
                if (bukkit != null) {
                    bukkit.sendMessage(configManager.translate("&cUn nouveau joueur a rejoint votre camp ! /vuhc role pour en savoir plus."));
                }
            }
        }
    }

    // Kit de départ : pioche/hache/épée en pierre + nourriture + bois + torches.
    private void giveStartingKit() {
        if (!configManager.isStartingKitEnabled()) {
            return;
        }
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.getInventory().clear();
            player.setHealth(player.getMaxHealth());
            player.setFoodLevel(20);
            player.setSaturation(20f);
            player.getInventory().addItem(
                    new ItemStack(Material.STONE_PICKAXE),
                    new ItemStack(Material.STONE_AXE),
                    new ItemStack(Material.STONE_SWORD),
                    new ItemStack(Material.COOKED_BEEF, 16),
                    new ItemStack(Material.OAK_LOG, 8),
                    new ItemStack(Material.TORCH, 16)
            );
        }
    }

    private void playSoundAll(Sound sound, float volume, float pitch) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.playSound(player.getLocation(), sound, volume, pitch);
        }
    }

    private void broadcast(String message) {
        Bukkit.broadcastMessage(configManager.translate(message));
    }
}
