package fr.vampireuhc.commands;

import fr.vampireuhc.game.GameManager;
import fr.vampireuhc.game.SpectatorManager;
import fr.vampireuhc.markers.MarkerManager;
import fr.vampireuhc.player.PlayerManager;
import fr.vampireuhc.player.Camp;
import fr.vampireuhc.player.VampireUHCPlayer;
import fr.vampireuhc.roles.RoleManager;
import fr.vampireuhc.vampire_vote.VampireVoteManager;
import fr.vampireuhc.vampire_vote.VoteResult;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import fr.vampireuhc.markers.AuraTier;
import fr.vampireuhc.roles.CupidonRole;
import fr.vampireuhc.roles.GremlinRole;
import fr.vampireuhc.roles.MasterRole;
import fr.vampireuhc.roles.SaviorRole;
import fr.vampireuhc.roles.VampireMinion;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * /vuhc start [sec]       -> lance la partie (avec compte à rebours optionnel)
 * /vuhc stop              -> arrête la partie
 * /vuhc reset             -> réinitialise la partie
 * /vuhc spectate <joueur> -> le spectateur suit un joueur vivant
 * /vuhc status            -> affiche la phase et le temps ecoule
 * /vuhc who               -> (debug) affiche camp de chaque joueur connecte
 * /vuhc aura <joueur>     -> (debug) affiche le score/tier d'aura d'un joueur
 * /vuhc marquer <nom>     -> Maître Vampire marque un joueur
 * /vuhc trancher <nom>    -> Maître Vampire tranche une égalité de vote
 * /vuhc proteger <nom>    -> Salvateur protège un joueur (marque Salvation)
 * /vuhc voter <nom>       -> Sbire vampire vote pour la marque vampire
 * /vuhc switch <j1> <j2>  -> Gremlin échange les marques de deux joueurs
 * /vuhc role              -> Affiche son rôle ou la liste des vampires si infecté
 * /vuhc setTime <Time>    -> Change le timer au temps désigné (pour admin et debug seulement, en game classique, si on l'execute, ça casse pas mal de choses)
 */
public class VUHCCommand implements CommandExecutor, TabCompleter {
    private final GameManager gameManager;
    private final PlayerManager playerManager;
    private final MarkerManager markerManager;
    private final RoleManager roleManager;
    private final VampireVoteManager voteManager;
    private final SpectatorManager spectatorManager;

    private static final List<String> SUBCOMMANDS = Arrays.asList("start", "stop", "reset", "spectate", "status", "who", "aura", "marquer", "trancher", "proteger", "voter", "switch", "role", "setTime");

    public VUHCCommand(GameManager gameManager, PlayerManager playerManager, MarkerManager markerManager, RoleManager roleManager, VampireVoteManager voteManager, SpectatorManager spectatorManager) {
        this.gameManager = gameManager;
        this.playerManager = playerManager;
        this.markerManager = markerManager;
        this.roleManager = roleManager;
        this.voteManager = voteManager;
        this.spectatorManager = spectatorManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }
       
        switch (args[0].toLowerCase()) {
            case "start":
                if (!hasAdminPermission(sender)) {
                    sender.sendMessage(ChatColor.RED + "Vous n'avez pas la permission.");
                    return true;
                }
                int seconds = gameManager.getDefaultCountdownSeconds();
                if (args.length >= 2) {
                    try {
                        seconds = Math.max(0, Integer.parseInt(args[1]));
                    } catch (NumberFormatException e) {
                        sender.sendMessage(ChatColor.RED + "Durée invalide : " + args[1]);
                        return true;
                    }
                }
                if (gameManager.startCountdown(seconds)) {
                    sender.sendMessage(ChatColor.GREEN + "Partie VampireUHC lancée (démarrage dans " + seconds + "s).");
                } else {
                    sender.sendMessage(ChatColor.RED + "Une partie est déjà en cours ou en cours de lancement.");
                }
                return true;

            case "settime":
                if (!hasAdminPermission(sender)) {
                    sender.sendMessage(ChatColor.RED + "Vous n'avez pas la permission.");
                    return true;
                }
                if (args.length >= 2) {
                    int time = Integer.parseInt(args[1]);
                    gameManager.setElapsedMinutes(time);
                    sender.sendMessage(ChatColor.GREEN + "Vous avez changé le temps à " + time + " minutes.");
                    return true;
                }
                sender.sendMessage(ChatColor.RED + "Usage: /vuhc setTime <TIME_MINUTE>");
                return true;

            case "stop":
                if (!hasAdminPermission(sender)) {
                    sender.sendMessage(ChatColor.RED + "Vous n'avez pas la permission.");
                    return true;
                }
                gameManager.stop();
                sender.sendMessage(ChatColor.GREEN + "Partie arrêtée.");
                return true;

            case "reset":
                if (!hasAdminPermission(sender)) {
                    sender.sendMessage(ChatColor.RED + "Vous n'avez pas la permission.");
                    return true;
                }
                gameManager.resetGame();
                sender.sendMessage(ChatColor.GREEN + "Partie réinitialisée.");
                return true;

            case "spectate":
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(ChatColor.RED + "Cette commande doit être exécutée par un joueur.");
                    return true;
                }
                if (args.length < 2) {
                    player.sendMessage(ChatColor.RED + "Usage: /vuhc spectate <joueur>");
                    return true;
                }
                Player targetSpec = Bukkit.getPlayer(args[1]);
                if (targetSpec == null) {
                    player.sendMessage(ChatColor.RED + "Joueur introuvable ou hors ligne.");
                    return true;
                }
                spectatorManager.follow(player, targetSpec);
                return true;

            case "status":
                if (!hasAdminPermission(sender)) {
                    sender.sendMessage(ChatColor.RED + "Vous n'avez pas la permission.");
                    return true;
                }
                sender.sendMessage(ChatColor.YELLOW + "Phase: " + gameManager.getPhase() + " | Minute: " + gameManager.getElapsedMinutes());
                return true;

            case "who":
                if (!hasAdminPermission(sender)) {
                    sender.sendMessage(ChatColor.RED + "Vous n'avez pas la permission.");
                    return true;
                }
                for (VampireUHCPlayer p : playerManager.getAll()) {
                    sender.sendMessage(p.getLastKnownName() + " -> " + p.getCamp());
                }
                return true;

            case "aura":
                if (!hasAdminPermission(sender)) {
                    sender.sendMessage(ChatColor.RED + "Vous n'avez pas la permission.");
                    return true;
                }
                if (args.length < 2) {
                    sender.sendMessage(ChatColor.RED + "Usage: /vuhc aura <joueur>");
                    return true;
                }
                Player target = sender.getServer().getPlayer(args[1]);
                if (target == null) {
                    sender.sendMessage(ChatColor.RED + "Joueur introuvable ou hors ligne.");
                    return true;
                }
                int score = markerManager.computeAuraScore(target.getUniqueId());
                AuraTier tier = markerManager.computeAuraTier(target.getUniqueId());
                sender.sendMessage(target.getName() + " -> score=" + score + " tier=" + tier);
                return true;

            case "marquer":
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(ChatColor.RED + "Cette commande doit être exécutée par un joueur.");
                    return true;
                }

                if (args.length < 2) {
                    player.sendMessage(ChatColor.RED + "Usage: /vuhc marquer <joueur>");
                    return true;
                }

                Player marquerTarget = Bukkit.getPlayer(args[1]);

                if (marquerTarget == null) {
                    player.sendMessage(ChatColor.RED + "Joueur introuvable ou hors ligne.");
                    return true;
                }

                VampireUHCPlayer targetPlayer = playerManager.get(marquerTarget.getUniqueId());

                if (targetPlayer == null) {
                    player.sendMessage(ChatColor.RED + "Ce joueur n'est pas en partie.");
                    return true;
                }

                VampireUHCPlayer localPlayer = playerManager.get(player.getUniqueId());

                if (localPlayer == null) {
                    player.sendMessage(ChatColor.RED + "Vous n'êtes pas en partie.");
                    return true;
                }

                if (!(localPlayer.getRole() instanceof MasterRole masterRole)) {
                    player.sendMessage(ChatColor.RED + "Vous n'êtes pas le Maître Vampire.");
                    return true;
                }

                masterRole.markPlayer(markerManager, targetPlayer, gameManager.getEpisode());
                return true;

            case "trancher":
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(ChatColor.RED + "Cette commande doit être exécutée par un joueur.");
                    return true;
                }

                VoteResult.Tie pendingTie = voteManager.getPendingTie();
                if (pendingTie == null) {
                    player.sendMessage(ChatColor.RED + "Aucune égalité à trancher en cours.");
                    return true;
                }

                VampireUHCPlayer localTiePlayer = playerManager.get(player.getUniqueId());
                if (localTiePlayer == null) {
                    player.sendMessage(ChatColor.RED + "Vous n'êtes pas en partie.");
                    return true;
                }

                if (!(localTiePlayer.getRole() instanceof MasterRole)) {
                    player.sendMessage(ChatColor.RED + "Seul le Maître Vampire peut trancher une égalité.");
                    return true;
                }

                if (args.length < 2) {
                    player.sendMessage(ChatColor.RED + "Usage: /vuhc trancher <joueur>");
                    return true;
                }

                Player tieTarget = Bukkit.getPlayer(args[1]);
                if (tieTarget == null) {
                    player.sendMessage(ChatColor.RED + "Joueur introuvable ou hors ligne.");
                    return true;
                }

                if (!pendingTie.tiedPlayers().contains(tieTarget.getUniqueId())) {
                    player.sendMessage(ChatColor.RED + "Ce joueur ne fait pas partie de l'égalité à trancher.");
                    return true;
                }

                VampireUHCPlayer tieTargetPlayer = playerManager.get(tieTarget.getUniqueId());
                if (tieTargetPlayer == null) {
                    player.sendMessage(ChatColor.RED + "Ce joueur n'est pas en partie.");
                    return true;
                }

                voteManager.resolveTieWith(tieTargetPlayer.getUuid());
                player.sendMessage(ChatColor.DARK_PURPLE + "Vous avez choisi " + ChatColor.GOLD + tieTargetPlayer.getLastKnownName() + ChatColor.DARK_PURPLE + ".");
                return true;

            case "proteger":
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(ChatColor.RED + "Cette commande doit être exécutée par un joueur.");
                    return true;
                }

                if (args.length < 2) {
                    player.sendMessage(ChatColor.RED + "Usage: /vuhc proteger <joueur>");
                    return true;
                }

                Player protTarget = Bukkit.getPlayer(args[1]);
                if (protTarget == null) {
                    player.sendMessage(ChatColor.RED + "Joueur introuvable ou hors ligne.");
                    return true;
                }

                VampireUHCPlayer protTargetPlayer = playerManager.get(protTarget.getUniqueId());
                if (protTargetPlayer == null) {
                    player.sendMessage(ChatColor.RED + "Ce joueur n'est pas en partie.");
                    return true;
                }

                VampireUHCPlayer localProt = playerManager.get(player.getUniqueId());
                if (localProt == null) {
                    player.sendMessage(ChatColor.RED + "Vous n'êtes pas en partie.");
                    return true;
                }

                if (!(localProt.getRole() instanceof SaviorRole savior)) {
                    player.sendMessage(ChatColor.RED + "Vous n'êtes pas le Salvateur.");
                    return true;
                }

                if (savior.applySalvation(markerManager, protTargetPlayer, gameManager.getEpisode())) {
                    player.sendMessage(ChatColor.GREEN + "Vous avez posé votre marque Salvation sur " + ChatColor.GOLD + protTargetPlayer.getLastKnownName() + ChatColor.GREEN + ".");
                } else {
                    player.sendMessage(ChatColor.RED + "Vous ne pouvez pas protéger ce joueur pour l'instant.");
                }
                return true;

            case "voter":
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(ChatColor.RED + "Cette commande doit être exécutée par un joueur.");
                    return true;
                }

                if (args.length < 2) {
                    player.sendMessage(ChatColor.RED + "Usage: /vuhc voter <joueur>");
                    return true;
                }

                Player voteTarget = Bukkit.getPlayer(args[1]);
                if (voteTarget == null) {
                    player.sendMessage(ChatColor.RED + "Joueur introuvable ou hors ligne.");
                    return true;
                }

                VampireUHCPlayer voteTargetPlayer = playerManager.get(voteTarget.getUniqueId());
                if (voteTargetPlayer == null) {
                    player.sendMessage(ChatColor.RED + "Ce joueur n'est pas en partie.");
                    return true;
                }

                VampireUHCPlayer voter = playerManager.get(player.getUniqueId());
                if (voter == null) {
                    player.sendMessage(ChatColor.RED + "Vous n'êtes pas en partie.");
                    return true;
                }

                if (!(voter.getRole() instanceof VampireMinion) || !voter.canVoteVampireMark()) {
                    player.sendMessage(ChatColor.RED + "Seuls les sbires vampires peuvent voter.");
                    return true;
                }

                if (voteTargetPlayer.getCamp() == Camp.VAMPIRE) {
                    player.sendMessage(ChatColor.RED + "Vous ne pouvez pas voter pour un vampire.");
                    return true;
                }

                if (!voteManager.isVoteOpen()) {
                    player.sendMessage(ChatColor.RED + "Aucun vote n'est en cours.");
                    return true;
                }

                if (voteManager.addVote(player.getUniqueId(), voteTargetPlayer.getUuid())) {
                    player.sendMessage(ChatColor.DARK_PURPLE + "Votre vote pour " + ChatColor.GOLD + voteTargetPlayer.getLastKnownName() + ChatColor.DARK_PURPLE + " a été enregistré.");
                } else {
                    player.sendMessage(ChatColor.RED + "Vous avez déjà voté pour ce tour.");
                }
                return true;

            case "switch":
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(ChatColor.RED + "Cette commande doit être exécutée par un joueur.");
                    return true;
                }

                if (args.length < 3) {
                    player.sendMessage(ChatColor.RED + "Usage: /vuhc switch <joueur1> <joueur2>");
                    return true;
                }

                Player switchTarget1 = Bukkit.getPlayer(args[1]);
                Player switchTarget2 = Bukkit.getPlayer(args[2]);
                if (switchTarget1 == null || switchTarget2 == null) {
                    player.sendMessage(ChatColor.RED + "Joueur introuvable ou hors ligne.");
                    return true;
                }

                VampireUHCPlayer t1 = playerManager.get(switchTarget1.getUniqueId());
                VampireUHCPlayer t2 = playerManager.get(switchTarget2.getUniqueId());
                if (t1 == null || t2 == null) {
                    player.sendMessage(ChatColor.RED + "Un des joueurs n'est pas en partie.");
                    return true;
                }

                VampireUHCPlayer localSwitch = playerManager.get(player.getUniqueId());
                if (localSwitch == null) {
                    player.sendMessage(ChatColor.RED + "Vous n'êtes pas en partie.");
                    return true;
                }

                if (!(localSwitch.getRole() instanceof GremlinRole gremlin)) {
                    player.sendMessage(ChatColor.RED + "Vous n'êtes pas le Gremlin.");
                    return true;
                }

                if (gremlin.SwitchMarkers(markerManager, t1, t2, gameManager.getEpisode())) {
                    player.sendMessage(ChatColor.GREEN + "Vous avez échangé les marques de " + ChatColor.GOLD + t1.getLastKnownName() + ChatColor.GREEN + " et " + ChatColor.GOLD + t2.getLastKnownName() + ChatColor.GREEN + ".");

                    // Si une marque Amour a changé de propriétaire, le Cupidon doit être prévenu.
                    for (VampireUHCPlayer p : playerManager.getAll()) {
                        if (p.getRole() instanceof CupidonRole cupidon) {
                            cupidon.notifyIfLoversMoved(markerManager);
                        }
                    }
                } else {
                    player.sendMessage(ChatColor.RED + "Vous avez déjà utilisé votre switch cet épisode.");
                }
                return true;

            case "role":
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(ChatColor.RED + "Cette commande doit être exécutée par un joueur.");
                    return true;
                }


                var local = playerManager.get(player);
                if (local == null) {
                    sender.sendMessage(ChatColor.RED + "Vous n'êtes pas en partie.");
                    return true;
                }

                // Message d'infection pour les joueurs infectés en cours de partie uniquement
                if (local.getCamp() == Camp.VAMPIRE && local.isInfected()) {
                    sender.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                        ChatColor.RED + "Vous avez été infecté et devez à présent gagner avec les vampires !"));

                    // Liste des pseudos vampires uniquement
                    List<String> vampireNames = new ArrayList<>();
                    for (VampireUHCPlayer v : playerManager.getByCamp(Camp.VAMPIRE)) {
                        if (!local.getUuid().equals(v.getUuid())) {
                            vampireNames.add(v.getLastKnownName());
                        }
                    }
                    sender.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                        ChatColor.YELLOW + "Liste des vampires connus : &f" + String.join(", ", vampireNames)));
                } else {
                    sender.sendMessage(ChatColor.GREEN + "Vous êtes " + (local.getRole() != null ? local.getRole().getName() : "rien. Vous n'avez aucun rôle pour l'instant !"));
                    sender.sendMessage(ChatColor.WHITE + (local.getRole() != null ? local.getRole().getDescription() : ""));
                }
                return true;

            default:
                sender.sendMessage(ChatColor.RED + "Sous-commande inconnue.");
                sendHelp(sender);
                return true;
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> matches = new ArrayList<>();
            for (String sub : SUBCOMMANDS) {
                if (sub.startsWith(args[0].toLowerCase())) {
                    matches.add(sub);
                }
            }
            return matches;
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("trancher")) {
            VoteResult.Tie pendingTie = voteManager.getPendingTie();
            if (pendingTie == null) {
                return new ArrayList<>();
            }
            return pendingTie.tiedPlayers().stream()
                    .map(id -> playerManager.get(id))
                    .filter(p -> p != null)
                    .map(VampireUHCPlayer::getLastKnownName)
                    .filter(name -> name.toLowerCase().startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
        }

        if (args.length == 2 && (args[0].equalsIgnoreCase("voter")
                || args[0].equalsIgnoreCase("proteger")
                || args[0].equalsIgnoreCase("switch")
                || args[0].equalsIgnoreCase("spectate"))) {
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("switch")) {
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(args[2].toLowerCase()))
                    .collect(Collectors.toList());
        }

        return new ArrayList<>();
    }

    // Helpers

    // Autorisation admin : permission vuhc.admin OU pseudo listé dans config.yml (admins.players). La console est toujours autorisée.
    private boolean hasAdminPermission(CommandSender sender) {
        if (sender.hasPermission("vuhc.admin")) {
            return true;
        }
        if (sender instanceof Player player) {
            return fr.vampireuhc.VampireUHC.getInstance().getConfigManager().getAdminPlayers().contains(player.getName());
        }
        return true;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(ChatColor.RED + "Usage: /vuhc <start [sec]|stop|reset|spectate <joueur>|status|who|aura|marquer|trancher|proteger|voter|switch|role|setTime>");
    }
}
