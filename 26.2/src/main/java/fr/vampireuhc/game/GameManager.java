package fr.vampireuhc.game;

import fr.vampireuhc.VampireUHC;
import fr.vampireuhc.config.Composition;
import fr.vampireuhc.config.ConfigManager;
import fr.vampireuhc.config.MessageUtil;
import fr.vampireuhc.markers.MarkerManager;
import fr.vampireuhc.player.Camp;
import fr.vampireuhc.player.PlayerManager;
import fr.vampireuhc.player.VampireUHCPlayer;
import fr.vampireuhc.roles.Role;
import fr.vampireuhc.roles.RoleType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
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

    // Joueurs sélectionnés par l'admin pour la partie (uuid -> pseudo, insertion order).
    private final Map<UUID, String> roster = new LinkedHashMap<>();

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

    /* --- Roster : sélection des joueurs avant le lancement --- */

    // Ajoute un joueur à la sélection (uniquement avant le début de la partie).
    public boolean addToRoster(Player player) {
        if (isGameStarted()) {
            return false;
        }
        return roster.putIfAbsent(player.getUniqueId(), player.getName()) == null;
    }

    // Retire un joueur de la sélection (uniquement avant le début de la partie).
    public boolean removeFromRoster(Player player) {
        return roster.remove(player.getUniqueId()) != null;
    }

    // Vide la sélection (uniquement avant le début de la partie).
    public void clearRoster() {
        roster.clear();
    }

    public boolean isInRoster(Player player) {
        return roster.containsKey(player.getUniqueId());
    }

    public Map<UUID, String> getRoster() {
        return roster;
    }

    public int getRosterSize() {
        return roster.size();
    }

    // Auto-sélectionne des joueurs en ligne pour atteindre (au plus) le nombre
    // requis par la composition. Retourne le nombre de joueurs ajoutés.
    public int fillRoster() {
        if (isGameStarted()) {
            return 0;
        }
        int required = configManager.getComposition().getRequiredPlayers();
        int added = 0;
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (roster.size() >= required) {
                break;
            }
            if (!roster.containsKey(online.getUniqueId())) {
                roster.put(online.getUniqueId(), online.getName());
                added++;
            }
        }
        return added;
    }

    // Message d'erreur si la partie ne peut pas démarrer, sinon null.
    public String preStartValidation() {
        Composition composition = configManager.getComposition();
        if (!composition.isValid()) {
            return "Composition invalide : " + String.join(" / ", composition.getErrors());
        }
        int required = composition.getRequiredPlayers();
        if (roster.size() < required) {
            return "Il faut sélectionner " + required + " joueurs (roster : " + roster.size()
                    + "). /vuhc admin roster list pour voir l'état, add <pseudo> ou fill pour compléter.";
        }
        if (roster.size() > required) {
            return "Trop de joueurs sélectionnés (" + roster.size() + "), la composition prévoit "
                    + required + " joueurs. /vuhc admin roster remove <pseudo> pour retirer.";
        }
        List<String> offline = new ArrayList<>();
        for (String name : roster.values()) {
            if (Bukkit.getPlayerExact(name) == null) {
                offline.add(name);
            }
        }
        if (!offline.isEmpty()) {
            return "Joueurs sélectionnés hors ligne : " + String.join(", ", offline) + ".";
        }
        return null;
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

        // Garde de sécurité (le message détaillé est affiché côté commande via
        // preStartValidation) : composition valide + roster exactement rempli.
        if (preStartValidation() != null) {
            return false;
        }

        // Une partie précédente existe encore : on réinitialise d'abord.
        if (plugin.getMapManager().getWorld() != null || !playerManager.getAll().isEmpty()) {
            resetGame();
        }

        // Préchargement de la map PENDANT le compte à rebours : le monde est créé
        // immédiatement puis les chunks du disque d'éparpillement sont générés en
        // tâche de fond. Plus aucun freeze au moment du téléport des joueurs.
        plugin.getMapManager().loadWorld();
        plugin.getMapManager().startPregeneration();

        countdownRemaining = Math.max(0, seconds);
        if (countdownRemaining <= 0) {
            beginGame();
            return true;
        }

        broadcast("<dark_purple>La partie commence dans <white>" + countdownRemaining + "</white> secondes !</dark_purple>");
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
                broadcast("<dark_purple>La partie commence dans <white>" + countdownRemaining + "</white> secondes !</dark_purple>");
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

        for (Map.Entry<UUID, String> entry : roster.entrySet()) {
            Player online = Bukkit.getPlayer(entry.getKey());
            if (online != null) {
                playerManager.register(online);
                online.setGameMode(GameMode.SURVIVAL);
            } else {
                plugin.getLogger().warning("Joueur sélectionné absent au démarrage : " + entry.getValue());
            }
        }

        giveStartingKit();

        tickTask = Bukkit.getScheduler().runTaskTimer(plugin, this::onMinuteElapsed, 20 * 60L, 20 * 60L);
        plugin.getCrossTracker().start();
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
        plugin.getCrossTracker().start();

        if (plugin.getSidebarManager() != null) {
            plugin.getSidebarManager().start();
        }
        if (plugin.getSpectatorManager() != null) {
            plugin.getSpectatorManager().start();
        }
        broadcast("<dark_purple>La partie a été restaurée.</dark_purple>");
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
        plugin.getMapManager().cancelPregeneration();
        phase = GamePhase.ENDED;
        notifyRolesGameEnd();
        plugin.getCrossTracker().stop();
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
        roster.clear();
        markerManager.clearMarkersOnAll();
        plugin.getVoteManager().reset();
        plugin.getCrossTracker().stop();

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
        broadcast("<dark_purple>La partie a été réinitialisée.</dark_purple>");
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
            broadcast("<gold>Les rôles seront distribués dans 5 minutes !</gold>");
            playSoundAll(Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 1.5f);
        }
        if (elapsedMinutes == rolesAt - 1) {
            broadcast("<gold>Les rôles seront distribués dans 1 minute !</gold>");
            playSoundAll(Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 1.5f);
        }

        // Transitions en >= (et non ==) : un /vuhc dev setTime qui saute le seuil
        // déclenche la transition à la minute suivante au lieu de wedge la phase
        // pour toujours (invincibilité permanente, PvP/jamais activés).
        if (elapsedMinutes >= rolesAt && phase == GamePhase.PRE_ROLES) {
            assignRolesAndCamps();
            checkInfections();
            announceRoles();
            broadcast("Les rôles ont été distribués.");
            phase = GamePhase.PRE_PVP;
            playSoundAll(Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
        }

        if (elapsedMinutes == pvpAt - 5) {
            broadcast("<red>Le PvP sera activé dans 5 minutes !</red>");
            playSoundAll(Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 1.5f);
        }
        if (elapsedMinutes == pvpAt - 1) {
            broadcast("<red>Le PvP sera activé dans 1 minute !</red>");
            playSoundAll(Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 1.5f);
        }

        if (elapsedMinutes >= pvpAt && phase == GamePhase.PRE_PVP) {
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
            int newEpisode = elapsedMinutes / episodeLength + 1;
            broadcast("<dark_purple>Début de l'épisode <white>" + newEpisode + "</white> !</dark_purple>");
            playSoundAll(Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.2f);

            // Hook générique : chaque rôle décide s'il réagit à un début
            // d'épisode (ex : le cri de la Banshee). Boucle identique à
            // notifyRolesGameEnd(), GameManager reste agnostique des rôles.
            for (VampireUHCPlayer p : playerManager.getAll()) {
                if (p.getRole() != null) {
                    p.getRole().onEpisodeStart(newEpisode);
                }
            }

            // Frontière d'épisode : les croisements de l'épisode écoulé
            // deviennent éligibles pour le vote / la marque Maître.
            plugin.getCrossTracker().advanceEpisode();
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
            broadcast("<dark_purple>" + winner + " <white>remporte la partie !</white></dark_purple>");
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
        Composition composition = configManager.getComposition();
        if (!composition.isValid()) {
            plugin.getLogger().severe("Distribution impossible : composition invalide. "
                    + String.join(" / ", composition.getErrors()));
            broadcast("Composition invalide, aucun rôle n'a été distribué.");
            return;
        }

        List<VampireUHCPlayer> pool = new ArrayList<>(playerManager.getAll());
        List<RoleType> roleList = composition.buildRoleList();

        // Défense : le roster devait être exactement == joueurs au lancement.
        // Une déconnexion depuis a réduit le pool : on distribue aux présents,
        // les rôles non attribués sont les derniers de la liste (des Sbires).
        int assigned = Math.min(pool.size(), roleList.size());
        if (pool.size() != roleList.size()) {
            plugin.getLogger().warning("Joueurs présents (" + pool.size() + ") != composition ("
                    + roleList.size() + ") : distribution partielle.");
            broadcast("Joueurs présents (" + pool.size() + "/" + roleList.size()
                    + ") : certains rôles ne sont pas attribués.");
        }

        Collections.shuffle(pool);

        for (int i = 0; i < assigned; i++) {
            VampireUHCPlayer vp = pool.get(i);
            RoleType roleType = roleList.get(i);
            vp.setCamp(Composition.campOf(roleType));

            Role role = plugin.getRoleManager().createRoleFromType(roleType, vp);
            vp.setRole(role);
            if (role != null) {
                role.onAssign(vp, false);
            }

            // Les vampires de naissance peuvent voter pour la marque vampire (pas les infectés).
            if (vp.getCamp() == Camp.VAMPIRE) {
                vp.setVampireVote();
            }
            Player bukkitPlayer = Bukkit.getPlayer(vp.getUuid());
            if (bukkitPlayer != null) {
                bukkitPlayer.sendMessage(MessageUtil.info("Votre camp : <gold>" + vp.getCamp().getDisplayName() + "</gold>"));
            }
        }

        int vampires = 0;
        int villagers = 0;
        int solos = 0;
        for (int i = 0; i < assigned; i++) {
            switch (pool.get(i).getCamp()) {
                case VAMPIRE:
                    vampires++;
                    break;
                case VILLAGEOIS:
                    villagers++;
                    break;
                case SOLO:
                    solos++;
                    break;
                default:
                    break;
            }
        }
        plugin.getLogger().info("Rôles attribués : " + vampires + " vampires, " + villagers
                + " villageois, " + solos + " solitaires");
    }

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
                bukkitPlayer.sendMessage(MessageUtil.warn("Vos alliés vampires : <white>" + names + "</white>"));
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
                bukkitPlayer.sendMessage(MessageUtil.roleAnnounce(vp.getRole()));
            } else {
                bukkitPlayer.sendMessage(MessageUtil.info("Vous êtes : Villageois"));
            }
        }
    }

    // Notifie les joueurs infectés en cours de partie (jamais les vampires de naissance).
    private void checkInfections() {
        for (VampireUHCPlayer vp : playerManager.getAll()) {
            if (vp.isInfected() && !vp.isVampireListRevealed()) {
                Player bukkit = Bukkit.getPlayer(vp.getUuid());
                if (bukkit != null) {
                    bukkit.sendMessage(MessageUtil.warn("Un nouveau joueur a rejoint votre camp ! /vuhc role pour en savoir plus."));
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

    private void broadcast(String miniMessage) {
        MessageUtil.broadcast(miniMessage);
    }
}
