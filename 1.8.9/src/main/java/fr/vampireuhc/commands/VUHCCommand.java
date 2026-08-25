package fr.vampireuhc.commands;

import fr.vampireuhc.game.GameManager;
import fr.vampireuhc.game.SpectatorManager;
import fr.vampireuhc.markers.MarkerManager;
import fr.vampireuhc.player.PlayerManager;
import fr.vampireuhc.player.Camp;
import fr.vampireuhc.player.VampireUHCPlayer;
import fr.vampireuhc.roles.RoleManager;
import fr.vampireuhc.roles.RoleType;
import fr.vampireuhc.roles.SandMerchantRole;
import fr.vampireuhc.vampire_vote.VampireVoteManager;
import fr.vampireuhc.vampire_vote.VoteResult;
import fr.vampireuhc.config.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import fr.vampireuhc.markers.AuraTier;
import fr.vampireuhc.roles.CartographerRole;
import fr.vampireuhc.roles.CupidonRole;
import fr.vampireuhc.roles.GravediggerRole;
import fr.vampireuhc.roles.GremlinRole;
import fr.vampireuhc.roles.MasterRole;
import fr.vampireuhc.roles.Role;
import fr.vampireuhc.roles.SaviorRole;
import fr.vampireuhc.roles.SoulweigherRole;
import fr.vampireuhc.roles.VampireMinion;
import fr.vampireuhc.roles.WeaverRole;
import fr.vampireuhc.roles.ExorcistRole;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Commandes joueur (visibles uniquement par ceux qui peuvent les utiliser) :
 *  * /vuhc role              -> Affiche son rôle (accessible à tous)
 *  * /vuhc spectate <joueur> -> le spectateur suit un joueur vivant
 *  * /vuhc marquer <nom>     -> Maître Vampire marque un joueur
 *  * /vuhc trancher <nom>    -> Maître Vampire tranche une égalité de vote
 *  * /vuhc proteger <nom>    -> Salvateur protège un joueur (marque Salvation)
 *  * /vuhc voter <nom>       -> Sbire vampire vote pour la marque vampire
 *  * /vuhc switch <j1> <j2>  -> Gremlin échange les marques de deux joueurs
 *  * /vuhc drain             -> pouvoir secondaire du gremlin
 *  * /vuhc lier <j1> <j2>    -> Cupidon lie deux joueurs
 *  * /vuhc peser <j1> <j2>   -> Peseuse d'âmes pèse l'aura de deux joueurs
 *  * /vuhc tisser <joueur>   -> Le Tisseur pose un fil sur un joueur.
 *  * /vuhc ensabler <joueur> -> Le Marchand de sable pose un marqueur sable sur un joueur
 *  * /vuhc exhumer           -> Le Fossoyeur exhume un cadavre.
 * 
 * Commandes admin (perm vuhc.admin) :
 *  * /vuhc admin <start [sec]|stop|reset|status>
 *
 * Commandes dev (perm vuhc.admin) :
 *  * /vuhc dev <setTime <min>|setRole <type>|who|aura <joueur>>
 *
 * Une commande n'est affichée (tab-complétion + aide) que si l'expéditeur
 * peut l'utiliser. Toute commande inaccessible répond "Sous-commande inconnue."
 */
public class VUHCCommand implements CommandExecutor, TabCompleter {
    private final GameManager gameManager;
    private final PlayerManager playerManager;
    private final MarkerManager markerManager;
    private final RoleManager roleManager;
    private final VampireVoteManager voteManager;
    private final SpectatorManager spectatorManager;

    private static final List<String> PLAYER_SUBCOMMANDS = Arrays.asList(
            "role", "spectate", "marquer", "trancher", "proteger", "voter",
            "switch", "drain", "lier", "peser", "tisser", "baliser", "ensabler", "exhumer", "exorciser");
    private static final List<String> ADMIN_SUBCOMMANDS = Arrays.asList("start", "stop", "reset", "status");
    private static final List<String> DEV_SUBCOMMANDS = Arrays.asList("setTime", "setRole", "who", "aura");

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

        String sub = args[0].toLowerCase();

        if (sub.equals("admin")) {
            handleAdmin(sender, Arrays.copyOfRange(args, 1, args.length));
            return true;
        }

        if (sub.equals("dev")) {
            handleDev(sender, Arrays.copyOfRange(args, 1, args.length));
            return true;
        }

        if (PLAYER_SUBCOMMANDS.contains(sub)) {
            if (!hasAccess(sender, sub)) {
                unknown(sender);
                return true;
            }
            dispatchPlayerCommand(sender, sub, args);
            return true;
        }

        unknown(sender);
        return true;
    }

    private void dispatchPlayerCommand(CommandSender sender, String sub, String[] args) {
        switch (sub) {
            case "role": {
                if (!(sender instanceof Player)) {
                    sender.sendMessage(MessageUtil.error("Cette commande doit être exécutée par un joueur."));
                    return;
                }
                Player player = (Player) sender;

                VampireUHCPlayer local = playerManager.get(player);
                if (local == null) {
                    player.sendMessage(MessageUtil.error("Vous n'êtes pas en partie."));
                    return;
                }

                // Message d'infection pour les joueurs infectés en cours de partie uniquement
                if (local.getCamp() == Camp.VAMPIRE && local.isInfected()) {
                    List<String> vampireNames = new ArrayList<>();
                    for (VampireUHCPlayer v : playerManager.getByCamp(Camp.VAMPIRE)) {
                        if (!local.getUuid().equals(v.getUuid())) {
                            vampireNames.add(v.getLastKnownName());
                        }
                    }
                    player.sendMessage(MessageUtil.infectionBanner(vampireNames));
                } else if (local.getRole() != null) {
                    player.sendMessage(MessageUtil.roleBanner(local.getRole()));
                } else {
                    player.sendMessage(MessageUtil.info("Vous n'avez aucun rôle pour l'instant."));
                }
                return;
            }

            case "exorciser": {
                if (!(sender instanceof Player)) {
                    sender.sendMessage(MessageUtil.error("Cette commande doit être exécutée par un joueur."));
                    return;
                }
                Player player = (Player) sender;

                if (args.length < 2) {
                    player.sendMessage(MessageUtil.error("Usage: /vuhc exorciser <joueur>"));
                    return;
                }

                Player target = Bukkit.getPlayer(args[1]);
                if (target == null) {
                    player.sendMessage(MessageUtil.error("Joueur introuvable ou hors ligne."));
                    return;                
                } 

                VampireUHCPlayer localPlayer = playerManager.get(player.getUniqueId());
                if (!(localPlayer != null && localPlayer.getRole() instanceof ExorcistRole)) {
                    return;
                }
                ExorcistRole exorcist = (ExorcistRole) localPlayer.getRole();

                VampireUHCPlayer targetPlayer = playerManager.get(target.getUniqueId());

                if (targetPlayer == null) {
                    player.sendMessage(MessageUtil.error("Ce joueur n'est pas en partie."));
                    return;
                }

                exorcist.exorcisePlayer(targetPlayer, markerManager);
                return;
            }

            case "exhumer": {
                if (!(sender instanceof Player)) {
                    sender.sendMessage(MessageUtil.error("Cette commande doit être exécutée par un joueur."));
                    return;
                }
                Player player = (Player) sender;

                VampireUHCPlayer localPlayer = playerManager.get(player.getUniqueId());
                if (!(localPlayer != null && localPlayer.getRole() instanceof GravediggerRole)) {
                    return;
                }
                GravediggerRole graveDigger = (GravediggerRole) localPlayer.getRole();

                Location location = player.getLocation();

                graveDigger.exhum(location);
                return;
            }

            case "ensabler": {
                if (!(sender instanceof Player)) {
                    sender.sendMessage(MessageUtil.error("Cette commande doit être exécutée par un joueur."));
                    return;
                }
                    Player player = (Player) sender;

                if (args.length < 2) {
                    player.sendMessage(MessageUtil.error("Usage: /vuhc ensabler <joueur>"));
                    return;
                }

                Player target = Bukkit.getPlayer(args[1]);
                if (target == null) {
                    player.sendMessage(MessageUtil.error("Joueur introuvable ou hors ligne."));
                    return;                
                } 

                VampireUHCPlayer targetPlayer = playerManager.get(target.getUniqueId());


                if (targetPlayer == null) {
                    player.sendMessage(MessageUtil.error("Ce joueur n'est pas en partie."));
                    return;
                }

                VampireUHCPlayer localPlayer = playerManager.get(player.getUniqueId());
                if (!(localPlayer != null && localPlayer.getRole() instanceof SandMerchantRole)) {

                    return;
                
                }
                SandMerchantRole sandMerchant = (SandMerchantRole) localPlayer.getRole();

                sandMerchant.sandPlayer(markerManager, targetPlayer);
                return;
            }

            case "spectate": {
                if (!(sender instanceof Player)) {
                    sender.sendMessage(MessageUtil.error("Cette commande doit être exécutée par un joueur."));
                    return;
                }
                    Player player = (Player) sender;
                if (args.length < 2) {
                    player.sendMessage(MessageUtil.error("Usage: /vuhc spectate <joueur>"));
                    return;
                }
                Player targetSpec = Bukkit.getPlayer(args[1]);
                if (targetSpec == null) {
                    player.sendMessage(MessageUtil.error("Joueur introuvable ou hors ligne."));
                    return;
                }
                spectatorManager.follow(player, targetSpec);
                return;
            }

            case "tisser": {
                if (!(sender instanceof Player)) {
                    sender.sendMessage(MessageUtil.error("Cette commande doit être exécutée par un joueur."));
                    return;
                }
                    Player player = (Player) sender;

                if (args.length < 2) {
                    player.sendMessage(MessageUtil.error("Usage: /vuhc tisser <joueur>"));
                    return;
                }

                Player target = Bukkit.getPlayer(args[1]);
                if (target == null) {
                    player.sendMessage(MessageUtil.error("Joueur introuvable ou hors ligne."));
                    return;                
                } 

                VampireUHCPlayer targetPlayer = playerManager.get(target.getUniqueId());

                if (targetPlayer == null) {
                    player.sendMessage(MessageUtil.error("Ce joueur n'est pas en partie."));
                    return;
                }

                VampireUHCPlayer localPlayer = playerManager.get(player.getUniqueId());
                if (!(localPlayer != null && localPlayer.getRole() instanceof WeaverRole)) {

                    return;
                
                }
                WeaverRole weaverRole = (WeaverRole) localPlayer.getRole();

                weaverRole.weavePlayer(markerManager, targetPlayer);
                return;
            }

            case "baliser": {
                if (!(sender instanceof Player)) {
                    sender.sendMessage(MessageUtil.error("Cette commande doit être exécutée par un joueur."));
                    return;
                }
                    Player player = (Player) sender; 
                
                if (args.length > 1) {
                    player.sendMessage(MessageUtil.error("Usage: /vuhc baliser"));
                    return;
                }

                VampireUHCPlayer localPlayer = playerManager.get(player.getUniqueId());

                if (!(localPlayer != null && localPlayer.getRole() instanceof CartographerRole)) {

                    return;
                
                }
                CartographerRole cartographerRole = (CartographerRole) localPlayer.getRole();

                cartographerRole.placeBeacon(player, gameManager.getEpisode());
                return;
            }

            case "marquer": {
                if (!(sender instanceof Player)) {
                    sender.sendMessage(MessageUtil.error("Cette commande doit être exécutée par un joueur."));
                    return;
                }
                    Player player = (Player) sender;
                if (args.length < 2) {
                    player.sendMessage(MessageUtil.error("Usage: /vuhc marquer <joueur>"));
                    return;
                }

                Player marquerTarget = Bukkit.getPlayer(args[1]);
                if (marquerTarget == null) {
                    player.sendMessage(MessageUtil.error("Joueur introuvable ou hors ligne."));
                    return;
                }

                VampireUHCPlayer targetPlayer = playerManager.get(marquerTarget.getUniqueId());
                if (targetPlayer == null) {
                    player.sendMessage(MessageUtil.error("Ce joueur n'est pas en partie."));
                    return;
                }

                VampireUHCPlayer localPlayer = playerManager.get(player.getUniqueId());
                if (!(localPlayer != null && localPlayer.getRole() instanceof MasterRole)) {

                    return;
                
                }
                MasterRole masterRole = (MasterRole) localPlayer.getRole();

                masterRole.markPlayer(markerManager, targetPlayer, gameManager.getEpisode());
                return;
            }

            case "trancher": {
                if (!(sender instanceof Player)) {
                    sender.sendMessage(MessageUtil.error("Cette commande doit être exécutée par un joueur."));
                    return;
                }
                    Player player = (Player) sender;

                VoteResult.Tie pendingTie = voteManager.getPendingTie();
                if (pendingTie == null) {
                    player.sendMessage(MessageUtil.error("Aucune égalité à trancher en cours."));
                    return;
                }

                if (args.length < 2) {
                    player.sendMessage(MessageUtil.error("Usage: /vuhc trancher <joueur>"));
                    return;
                }

                Player tieTarget = Bukkit.getPlayer(args[1]);
                if (tieTarget == null) {
                    player.sendMessage(MessageUtil.error("Joueur introuvable ou hors ligne."));
                    return;
                }

                if (!pendingTie.tiedPlayers().contains(tieTarget.getUniqueId())) {
                    player.sendMessage(MessageUtil.error("Ce joueur ne fait pas partie de l'égalité à trancher."));
                    return;
                }

                VampireUHCPlayer tieTargetPlayer = playerManager.get(tieTarget.getUniqueId());
                if (tieTargetPlayer == null) {
                    player.sendMessage(MessageUtil.error("Ce joueur n'est pas en partie."));
                    return;
                }

                voteManager.resolveTieWith(tieTargetPlayer.getUuid());
                player.sendMessage(MessageUtil.successTarget("Vous avez tranché en faveur de", tieTargetPlayer.getLastKnownName()));
                return;
            }

            case "proteger": {
                if (!(sender instanceof Player)) {
                    sender.sendMessage(MessageUtil.error("Cette commande doit être exécutée par un joueur."));
                    return;
                }
                    Player player = (Player) sender;

                if (args.length < 2) {
                    player.sendMessage(MessageUtil.error("Usage: /vuhc proteger <joueur>"));
                    return;
                }

                Player protTarget = Bukkit.getPlayer(args[1]);
                if (protTarget == null) {
                    player.sendMessage(MessageUtil.error("Joueur introuvable ou hors ligne."));
                    return;
                }

                VampireUHCPlayer protTargetPlayer = playerManager.get(protTarget.getUniqueId());
                if (protTargetPlayer == null) {
                    player.sendMessage(MessageUtil.error("Ce joueur n'est pas en partie."));
                    return;
                }

                VampireUHCPlayer localProt = playerManager.get(player.getUniqueId());
                if (!(localProt != null && localProt.getRole() instanceof SaviorRole)) {

                    return;
                
                }
                SaviorRole savior = (SaviorRole) localProt.getRole();

                if (savior.applySalvation(markerManager, protTargetPlayer, gameManager.getEpisode())) {
                    player.sendMessage(MessageUtil.successTarget("Marque Salvation posée sur", protTargetPlayer.getLastKnownName()));
                } else {
                    player.sendMessage(MessageUtil.error("Vous ne pouvez pas protéger ce joueur pour l'instant."));
                }
                return;
            }

            case "voter": {
                if (!(sender instanceof Player)) {
                    sender.sendMessage(MessageUtil.error("Cette commande doit être exécutée par un joueur."));
                    return;
                }
                    Player player = (Player) sender;

                if (args.length < 2) {
                    player.sendMessage(MessageUtil.error("Usage: /vuhc voter <joueur>"));
                    return;
                }

                Player voteTarget = Bukkit.getPlayer(args[1]);
                if (voteTarget == null) {
                    player.sendMessage(MessageUtil.error("Joueur introuvable ou hors ligne."));
                    return;
                }

                VampireUHCPlayer voteTargetPlayer = playerManager.get(voteTarget.getUniqueId());
                if (voteTargetPlayer == null) {
                    player.sendMessage(MessageUtil.error("Ce joueur n'est pas en partie."));
                    return;
                }

                if (voteTargetPlayer.getCamp() == Camp.VAMPIRE) {
                    player.sendMessage(MessageUtil.error("Vous ne pouvez pas voter pour un vampire."));
                    return;
                }

                if (!voteManager.isVoteOpen()) {
                    player.sendMessage(MessageUtil.error("Aucun vote n'est en cours."));
                    return;
                }

                if (voteManager.addVote(player.getUniqueId(), voteTargetPlayer.getUuid())) {
                    player.sendMessage(MessageUtil.successTarget("Vote enregistré pour", voteTargetPlayer.getLastKnownName()));
                } else {
                    player.sendMessage(MessageUtil.error("Vous avez déjà voté pour ce tour."));
                }
                return;
            }

            case "switch": {
                if (!(sender instanceof Player)) {
                    sender.sendMessage(MessageUtil.error("Cette commande doit être exécutée par un joueur."));
                    return;
                }
                    Player player = (Player) sender;

                if (args.length < 3) {
                    player.sendMessage(MessageUtil.error("Usage: /vuhc switch <joueur1> <joueur2>"));
                    return;
                }

                Player switchTarget1 = Bukkit.getPlayer(args[1]);
                Player switchTarget2 = Bukkit.getPlayer(args[2]);
                if (switchTarget1 == null || switchTarget2 == null) {
                    player.sendMessage(MessageUtil.error("Joueur introuvable ou hors ligne."));
                    return;
                }

                VampireUHCPlayer t1 = playerManager.get(switchTarget1.getUniqueId());
                VampireUHCPlayer t2 = playerManager.get(switchTarget2.getUniqueId());
                if (t1 == null || t2 == null) {
                    player.sendMessage(MessageUtil.error("Un des joueurs n'est pas en partie."));
                    return;
                }
                // Sinon SwitchMarkers stocke la MÊME instance de liste sous deux
                // UUIDs : les mutations de l'un corrompraient les marqueurs de l'autre.
                if (t1.getUuid().equals(t2.getUuid())) {
                    player.sendMessage(MessageUtil.error("Les deux cibles doivent être différentes."));
                    return;
                }

                VampireUHCPlayer localSwitch = playerManager.get(player.getUniqueId());
                if (!(localSwitch != null && localSwitch.getRole() instanceof GremlinRole)) {

                    return;
                
                }
                GremlinRole gremlin = (GremlinRole) localSwitch.getRole();

                if (gremlin.SwitchMarkers(markerManager, t1, t2, gameManager.getEpisode())) {
                    player.sendMessage(MessageUtil.successTwoTargets("Marques échangées entre", t1.getLastKnownName(), t2.getLastKnownName()));

                    // Si une marque Amour a changé de propriétaire, le Cupidon doit être prévenu.
                    for (VampireUHCPlayer p : playerManager.getAll()) {
                        if (p.getRole() instanceof CupidonRole) {
                    CupidonRole cupidon = (CupidonRole) p.getRole();
                            cupidon.notifyIfLoversMoved(markerManager);
                        }
                    }
                } else {
                    player.sendMessage(MessageUtil.error("Vous avez déjà utilisé votre switch cet épisode."));
                }
                return;
            }

            case "drain": {
                if (!(sender instanceof Player)) {
                    sender.sendMessage(MessageUtil.error("Cette commande doit être exécutée par un joueur."));
                    return;
                }
                    Player player = (Player) sender;

                VampireUHCPlayer localPlayer = playerManager.get(player.getUniqueId());
                if (!(localPlayer != null && localPlayer.getRole() instanceof GremlinRole)) {

                    return;
                
                }
                GremlinRole gremlinRole = (GremlinRole) localPlayer.getRole();

                if (gremlinRole.activateDrain(gameManager.getEpisode())) {
                    MessageUtil.sendActionBar(player, "<dark_purple>Vous avez activé le <dark_green>drain</dark_green> !");
                } else {
                    player.sendMessage(MessageUtil.error("Vous ne pouvez pas activer votre drain pour l'instant."));
                }
                return;
            }

            case "lier": {
                if (!(sender instanceof Player)) {
                    sender.sendMessage(MessageUtil.error("Cette commande doit être exécutée par un joueur."));
                    return;
                }
                    Player player = (Player) sender;
                if (args.length < 3) {
                    player.sendMessage(MessageUtil.error("Usage: /vuhc lier <Joueur1> <Joueur2>"));
                    return;
                }

                VampireUHCPlayer cupidon = playerManager.get(player.getUniqueId());
                if (!(cupidon != null && cupidon.getRole() instanceof CupidonRole)) {

                    return;
                
                }
                CupidonRole cupidonRole = (CupidonRole) cupidon.getRole();

                Player target1 = Bukkit.getPlayer(args[1]);
                Player target2 = Bukkit.getPlayer(args[2]);
                if (target1 == null || target2 == null) {
                    player.sendMessage(MessageUtil.error("Un des joueurs est introuvable ou hors ligne."));
                    return;
                }

                VampireUHCPlayer t1 = playerManager.get(target1.getUniqueId());
                VampireUHCPlayer t2 = playerManager.get(target2.getUniqueId());
                if (t1 == null || t2 == null) {
                    player.sendMessage(MessageUtil.error("Un des joueurs n'est pas en partie."));
                    return;
                }
                if (t1.getUuid().equals(t2.getUuid())) {
                    player.sendMessage(MessageUtil.error("Les deux joueurs doivent être différents."));
                    return;
                }
                // Pas de lien avec un mort/spectateur (pénalités futures sur des corpses).
                if (!t1.isAlive() || !t2.isAlive()) {
                    player.sendMessage(MessageUtil.error("Les deux amoureux doivent être vivants."));
                    return;
                }
                if (t1.getUuid().equals(player.getUniqueId()) || t2.getUuid().equals(player.getUniqueId())) {
                    player.sendMessage(MessageUtil.error("Vous ne pouvez pas vous lier vous-même."));
                    return;
                }

                if (cupidonRole.MarkLovers(markerManager, t1, t2)) {
                    player.sendMessage(MessageUtil.successTwoTargets("Joueurs liés :", t1.getLastKnownName(), t2.getLastKnownName()));
                } else {
                    player.sendMessage(MessageUtil.error("Vous avez déjà choisi vos amoureux."));
                }
                return;
            }

            case "peser": {
                if (!(sender instanceof Player)) {
                    sender.sendMessage(MessageUtil.error("Cette commande doit être exécutée par un joueur."));
                    return;
                }
                    Player player = (Player) sender;
                if (args.length < 3) {
                    player.sendMessage(MessageUtil.error("Usage: /vuhc peser <Joueur1> <Joueur2>"));
                    return;
                }

                VampireUHCPlayer localPlayer = playerManager.get(player.getUniqueId());
                if (!(localPlayer != null && localPlayer.getRole() instanceof SoulweigherRole)) {

                    return;
                
                }
                SoulweigherRole soulweigherRole = (SoulweigherRole) localPlayer.getRole();

                Player target1 = Bukkit.getPlayer(args[1]);
                Player target2 = Bukkit.getPlayer(args[2]);
                if (target1 == null || target2 == null) {
                    player.sendMessage(MessageUtil.error("Un des joueurs est introuvable ou hors ligne."));
                    return;
                }

                VampireUHCPlayer t1 = playerManager.get(target1.getUniqueId());
                VampireUHCPlayer t2 = playerManager.get(target2.getUniqueId());
                if (t1 == null || t2 == null) {
                    player.sendMessage(MessageUtil.error("Un des joueurs n'est pas en partie."));
                    return;
                }
                if (t1.getUuid().equals(t2.getUuid())) {
                    player.sendMessage(MessageUtil.error("Les deux joueurs doivent être différents."));
                    return;
                }

                if (!soulweigherRole.weightAura(markerManager, t1, t2, gameManager.getEpisode())) {
                    MessageUtil.sendActionBar(player, "<red>Erreur interne.");
                }
                return;
            }

            default:
                return;
        }
    }

    private void handleAdmin(CommandSender sender, String[] args) {
        if (!hasAccess(sender, "admin")) {
            unknown(sender);
            return;
        }

        if (args.length == 0) {
            sender.sendMessage(MessageUtil.error("Usage: /vuhc admin <start [sec]|stop|reset|status>"));
            return;
        }

        switch (args[0].toLowerCase()) {
            case "start":
                int seconds = gameManager.getDefaultCountdownSeconds();
                if (args.length >= 2) {
                    try {
                        seconds = Math.max(0, Integer.parseInt(args[1]));
                    } catch (NumberFormatException e) {
                        sender.sendMessage(MessageUtil.error("Durée invalide : " + args[1]));
                        return;
                    }
                }
                if (gameManager.startCountdown(seconds)) {
                    sender.sendMessage(MessageUtil.success("Partie VampireUHC lancée (démarrage dans " + seconds + "s)."));
                } else {
                    sender.sendMessage(MessageUtil.error("Une partie est déjà en cours ou en cours de lancement."));
                }
                return;

            case "stop":
                gameManager.stop();
                sender.sendMessage(MessageUtil.success("Partie arrêtée."));
                return;

            case "reset":
                gameManager.resetGame();
                sender.sendMessage(MessageUtil.success("Partie réinitialisée."));
                return;

            case "status":
                sender.sendMessage(MessageUtil.warn("Phase: " + gameManager.getPhase() + " | Minute: " + gameManager.getElapsedMinutes()));
                return;

            default:
                sender.sendMessage(MessageUtil.error("Sous-commande inconnue."));
                sender.sendMessage(MessageUtil.error("Usage: /vuhc admin <start [sec]|stop|reset|status>"));
        }
    }

    private void handleDev(CommandSender sender, String[] args) {
        if (!hasAccess(sender, "dev")) {
            unknown(sender);
            return;
        }

        if (args.length == 0) {
            sender.sendMessage(MessageUtil.error("Usage: /vuhc dev <setTime <min>|setRole <type>|who|aura <joueur>>"));
            return;
        }

        switch (args[0].toLowerCase()) {
            case "settime":
                if (args.length >= 2) {
                    try {
                        int time = Integer.parseInt(args[1]);
                        gameManager.setElapsedMinutes(time);
                        sender.sendMessage(MessageUtil.success("Vous avez changé le temps à " + time + " minutes."));
                    } catch (NumberFormatException e) {
                        sender.sendMessage(MessageUtil.error("Temps invalide : " + args[1]));
                    }
                    return;
                }
                sender.sendMessage(MessageUtil.error("Usage: /vuhc dev setTime <TIME_MINUTE>"));
                return;

            case "setrole":
                if (!(sender instanceof Player)) {
                    sender.sendMessage(MessageUtil.error("Cette commande doit être exécutée par un joueur."));
                    return;
                }
                    Player player = (Player) sender;
                if (args.length >= 2) {
                    try {
                        RoleType type = RoleType.fromString(args[1]);
                        gameManager.setRole(player, type);
                    } catch (IllegalArgumentException e) {
                        player.sendMessage(MessageUtil.error("Type de rôle invalide."));
                    }
                    return;
                }
                sender.sendMessage(MessageUtil.error("Usage: /vuhc dev setRole <RoleType>"));
                return;

            case "who":
                for (VampireUHCPlayer p : playerManager.getAll()) {
                    sender.sendMessage(p.getLastKnownName() + " -> " + p.getCamp());
                }
                return;

            case "aura":
                if (args.length < 2) {
                    sender.sendMessage(MessageUtil.error("Usage: /vuhc dev aura <joueur>"));
                    return;
                }
                Player target = sender.getServer().getPlayer(args[1]);
                if (target == null) {
                    sender.sendMessage(MessageUtil.error("Joueur introuvable ou hors ligne."));
                    return;
                }
                int score = markerManager.computeAuraScore(target.getUniqueId());
                AuraTier tier = markerManager.computeAuraTier(target.getUniqueId());
                sender.sendMessage(target.getName() + " -> score=" + score + " tier=" + tier);
                return;

            default:
                sender.sendMessage(MessageUtil.error("Sous-commande inconnue."));
                sender.sendMessage(MessageUtil.error("Usage: /vuhc dev <setTime <min>|setRole <type>|who|aura <joueur>>"));
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            String prefix = args[0].toLowerCase();
            return visibleTopLevelSubcommands(sender).stream()
                    .filter(sub -> sub.startsWith(prefix))
                    .collect(Collectors.toList());
        }

        String sub = args[0].toLowerCase();

        if (sub.equals("admin") && hasAccess(sender, "admin")) {
            return filterStartsWith(ADMIN_SUBCOMMANDS, args[1]);
        }

        if (sub.equals("dev") && hasAccess(sender, "dev")) {
            return filterStartsWith(DEV_SUBCOMMANDS, args[1]);
        }

        if (args.length == 2) {
            if (sub.equals("trancher") && hasAccess(sender, "trancher")) {
                return tiedPlayersStartingWith(args[1]);
            }
            if ((sub.equals("marquer") || sub.equals("proteger") || sub.equals("voter")
                    || sub.equals("switch") || sub.equals("lier") || sub.equals("peser")
                    || sub.equals("tisser") || sub.equals("spectate") || sub.equals("ensabler")) || sub.equals("exorciser") && hasAccess(sender, sub)) {
                return playersStartingWith(args[1]);
            }
            return new ArrayList<>();
        }

        if (args.length == 3 && (sub.equals("switch") || sub.equals("lier") || sub.equals("peser"))
                && hasAccess(sender, sub)) {
            return playersStartingWith(args[2]);
        }

        return new ArrayList<>();
    }

    // Helpers

    // Accès à une sous-commande selon le rôle/statut de l'expéditeur.
    private boolean hasAccess(CommandSender sender, String sub) {
        switch (sub) {
            case "tisser":
                return hasRole(sender, WeaverRole.class);
            case "baliser":
                return hasRole(sender, CartographerRole.class);
            case "role":
                return true;
            case "exorciser":
                return hasRole(sender, ExorcistRole.class);
            case "exhumer":
                return hasRole(sender, GravediggerRole.class);
            case "ensabler":
                return hasRole(sender, SandMerchantRole.class);
            case "spectate":
                return sender instanceof Player && ((Player) sender).getGameMode() == GameMode.SPECTATOR;
            case "marquer":
            case "trancher":
                return hasRole(sender, MasterRole.class);
            case "proteger":
                return hasRole(sender, SaviorRole.class);
            case "voter": {
                VampireUHCPlayer vp = playerOf(sender);
                return vp != null && vp.getRole() instanceof VampireMinion && vp.canVoteVampireMark();
            }
            case "switch":
            case "drain":
                return hasRole(sender, GremlinRole.class);
            case "lier":
                return hasRole(sender, CupidonRole.class);
            case "peser":
                return hasRole(sender, SoulweigherRole.class);
            case "admin":
            case "dev":
            case "start":
            case "stop":
            case "reset":
            case "status":
            case "settime":
            case "setrole":
            case "who":
            case "aura":
                return hasAdminPermission(sender);
            default:
                return false;
        }
    }

    private boolean hasRole(CommandSender sender, Class<? extends Role> roleClass) {
        VampireUHCPlayer vp = playerOf(sender);
        // Un joueur mort perd l'usage des pouvoirs de son rôle.
        return vp != null && vp.isAlive() && roleClass.isInstance(vp.getRole());
    }

    private VampireUHCPlayer playerOf(CommandSender sender) {
        if (sender instanceof Player) {
            Player player = (Player) sender;
            return playerManager.get(player.getUniqueId());
        }
        return null;
    }

    private List<String> visibleTopLevelSubcommands(CommandSender sender) {
        List<String> result = new ArrayList<>();
        for (String sub : PLAYER_SUBCOMMANDS) {
            if (hasAccess(sender, sub)) {
                result.add(sub);
            }
        }
        if (hasAdminPermission(sender)) {
            result.add("admin");
            result.add("dev");
        }
        return result;
    }

    // Autorisation admin : permission vuhc.admin OU pseudo listé dans config.yml (admins.players). La console est toujours autorisée.
    private boolean hasAdminPermission(CommandSender sender) {
        if (sender.hasPermission("vuhc.admin")) {
            return true;
        }
        if (sender instanceof Player) {
            Player player = (Player) sender;
            return fr.vampireuhc.VampireUHC.getInstance().getConfigManager().getAdminPlayers().contains(player.getName());
        }
        return true;
    }

    private void unknown(CommandSender sender) {
        sender.sendMessage(MessageUtil.error("Sous-commande inconnue."));
    }

    private List<String> filterStartsWith(List<String> options, String prefix) {
        return options.stream()
                .filter(o -> o.toLowerCase().startsWith(prefix.toLowerCase()))
                .collect(Collectors.toList());
    }

    private List<String> playersStartingWith(String prefix) {
        return Bukkit.getOnlinePlayers().stream()
                .map(Player::getName)
                .filter(name -> name.toLowerCase().startsWith(prefix.toLowerCase()))
                .collect(Collectors.toList());
    }

    private List<String> tiedPlayersStartingWith(String prefix) {
        VoteResult.Tie pendingTie = voteManager.getPendingTie();
        if (pendingTie == null) {
            return new ArrayList<>();
        }
        return pendingTie.tiedPlayers().stream()
                .map(id -> playerManager.get(id))
                .filter(p -> p != null)
                .map(VampireUHCPlayer::getLastKnownName)
                .filter(name -> name.toLowerCase().startsWith(prefix.toLowerCase()))
                .collect(Collectors.toList());
    }

    // Aide filtrée : seules les commandes accessibles à l'expéditeur sont affichées.
    private void sendHelp(CommandSender sender) {
        sender.sendMessage(MessageUtil.helpBanner());
        if (hasAccess(sender, "role")) {
            sender.sendMessage(MessageUtil.helpEntry("/vuhc role", "Affiche votre rôle"));
        }
        if (hasAccess(sender, "ensabler")) {
            sender.sendMessage(MessageUtil.helpEntry("/vuhc ensabler <joueur>", "Ensabler un joueur (Mar. de Sable)"));
        }
        if (hasAccess(sender, "exorciser")) {
            sender.sendMessage(MessageUtil.helpEntry("/vuhc exorciser <joueur>", "Exorciser un joueur (Une seule fois par joueur)"));
        }
        if (hasAccess(sender, "exhumer")) {
            sender.sendMessage(MessageUtil.helpEntry("/vuhc exhumer", "Tenter d'exhumer le cadavre d'un défunt à votre position."));
        }
        if (hasAccess(sender, "spectate")) {
            sender.sendMessage(MessageUtil.helpEntry("/vuhc spectate <joueur>", "Suivre un joueur vivant"));
        }
        if (hasAccess(sender, "marquer")) {
            sender.sendMessage(MessageUtil.helpEntry("/vuhc marquer <joueur>", "Poser un marqueur Maître"));
        }
        if (hasAccess(sender, "trancher")) {
            sender.sendMessage(MessageUtil.helpEntry("/vuhc trancher <joueur>", "Trancher une égalité de vote"));
        }
        if (hasAccess(sender, "proteger")) {
            sender.sendMessage(MessageUtil.helpEntry("/vuhc proteger <joueur>", "Poser une marque Salvation"));
        }
        if (hasAccess(sender, "voter")) {
            sender.sendMessage(MessageUtil.helpEntry("/vuhc voter <joueur>", "Voter pour une marque vampire"));
        }
        if (hasAccess(sender, "switch")) {
            sender.sendMessage(MessageUtil.helpEntry("/vuhc switch <j1> <j2>", "Échanger les marques de deux joueurs"));
        }
        if (hasAccess(sender, "drain")) {
            sender.sendMessage(MessageUtil.helpEntry("/vuhc drain", "Activer le drain (Gremlin)"));
        }
        if (hasAccess(sender, "lier")) {
            sender.sendMessage(MessageUtil.helpEntry("/vuhc lier <j1> <j2>", "Lier deux joueurs (Cupidon)"));
        }
        if (hasAccess(sender, "peser")) {
            sender.sendMessage(MessageUtil.helpEntry("/vuhc peser <j1> <j2>", "Peser l'aura de deux joueurs"));
        }
        if (hasAccess(sender, "tisser")) {
            sender.sendMessage(MessageUtil.helpEntry("/vuhc tisser <joueur>", "Tisser un fil sur un joueur"));
        }
        if (hasAccess(sender, "baliser")) {
            sender.sendMessage(MessageUtil.helpEntry("/vuhc baliser", "Poser une balise (Cartographe)"));
        }
        if (hasAdminPermission(sender)) {
            sender.sendMessage(MessageUtil.helpSection("Admin"));
            sender.sendMessage(MessageUtil.helpEntry("/vuhc admin", "<start|stop|reset|status>"));
            sender.sendMessage(MessageUtil.helpSection("Dev"));
            sender.sendMessage(MessageUtil.helpEntry("/vuhc dev", "<setTime|setRole|who|aura>"));
        }
    }
}
