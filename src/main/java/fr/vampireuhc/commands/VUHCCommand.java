package fr.vampireuhc.commands;

import fr.vampireuhc.game.GameManager;
import fr.vampireuhc.markers.MarkerManager;
import fr.vampireuhc.markers.MarkerType;
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
import fr.vampireuhc.roles.MasterRole;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * /vuhc start            -> lance la partie
 * /vuhc status           -> affiche la phase et le temps ecoule
 * /vuhc who              -> (debug) affiche camp de chaque joueur connecte
 * /vuhc aura <joueur>    -> (debug) affiche le score/tier d'aura d'un joueur
 * /vuhc marquer <nom>   -> Maître Vampire marque un joueur
 * /vuhc trancher <nom>  -> Maître Vampire tranche une égalité de vote
 * /vuhc role            -> Affiche son rôle ou la liste des vampires si infecté
 */
public class VUHCCommand implements CommandExecutor, TabCompleter {
    private final GameManager gameManager;
    private final PlayerManager playerManager;
    private final MarkerManager markerManager;
    private final RoleManager roleManager;
    private final VampireVoteManager voteManager;

    private static final List<String> SUBCOMMANDS = Arrays.asList("start", "status", "who", "aura", "marquer", "trancher", "role");

    public VUHCCommand(GameManager gameManager, PlayerManager playerManager, MarkerManager markerManager, RoleManager roleManager, VampireVoteManager voteManager) {
        this.gameManager = gameManager;
        this.playerManager = playerManager;
        this.markerManager = markerManager;
        this.roleManager = roleManager;
        this.voteManager = voteManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }
       
        switch (args[0].toLowerCase()) {
            case "start":
                gameManager.start();
                sender.sendMessage(ChatColor.GREEN + "Partie VampireUHC lancee.");
                return true;

            case "status":
                sender.sendMessage(ChatColor.YELLOW + "Phase: " + gameManager.getPhase() + " | Minute: " + gameManager.getElapsedMinutes());
                return true;

            case "who":
                for (VampireUHCPlayer p : playerManager.getAll()) {
                    sender.sendMessage(p.getLastKnownName() + " -> " + p.getCamp());
                }
                return true;

            case "aura":
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

                masterRole.markPlayer(markerManager, targetPlayer);
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

                voteManager.clearPendingTie();
                MasterRole master = (MasterRole) localTiePlayer.getRole();
                master.markPlayer(markerManager, tieTargetPlayer);
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

                // Message d'infection si vampire non-listé
                if (local.getCamp() == Camp.VAMPIRE && !local.isVampireListRevealed()) {
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

        return new ArrayList<>();
    }

    // Helpers
    private void sendHelp(CommandSender sender) {
        sender.sendMessage(ChatColor.RED + "Usage: /vuhc <start|status|who|aura|marquer|trancher|role>");
    }
}
