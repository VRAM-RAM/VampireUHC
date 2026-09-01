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
import fr.vampireuhc.roles.WatchmanRole;
import fr.vampireuhc.roles.WeaverRole;
import fr.vampireuhc.roles.ExorcistRole;
import fr.vampireuhc.roles.BabaYagaRole;
import fr.vampireuhc.roles.ComteRole;
import fr.vampireuhc.roles.PriestRole;
import fr.vampireuhc.roles.DoppelgangerRole;
import fr.vampireuhc.roles.usurped.UsurpedPower;
import fr.vampireuhc.roles.usurped.UsurpedPriest;
import fr.vampireuhc.roles.usurped.UsurpedSoulweigher;
import fr.vampireuhc.roles.usurped.UsurpedMaster;
import fr.vampireuhc.roles.usurped.UsurpedVampire;
import fr.vampireuhc.roles.usurped.UsurpedGravedigger;
import fr.vampireuhc.roles.usurped.UsurpedExorcist;
import fr.vampireuhc.roles.usurped.UsurpedGremlin;
import fr.vampireuhc.roles.usurped.UsurpedSavior;
import fr.vampireuhc.roles.usurped.UsurpedBourreau;
import fr.vampireuhc.roles.usurped.UsurpedCartographer;
import fr.vampireuhc.roles.usurped.UsurpedCupidon;
import fr.vampireuhc.roles.usurped.UsurpedWeaver;
import fr.vampireuhc.roles.usurped.UsurpedSandMerchant;
import fr.vampireuhc.roles.usurped.UsurpedBabaYaga;
import fr.vampireuhc.roles.usurped.UsurpedWatchman;

import fr.vampireuhc.VampireUHC;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
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
 *  * /vuhc veiller <joueur>  -> Le Veilleur veille sur un joueur
 *  * /vuhc exorciser <j>     -> L'Exorciste exorcise un joueur
 *  * /vuhc usurper <joueur>  -> Le Doppelganger (Sosie) copie les pouvoirs d'un joueur
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
    private final VampireUHC plugin;
    private final GameManager gameManager;
    private final PlayerManager playerManager;
    private final MarkerManager markerManager;
    private final RoleManager roleManager;
    private final VampireVoteManager voteManager;
    private final SpectatorManager spectatorManager;

    private static final List<String> PLAYER_SUBCOMMANDS = Arrays.asList(
            "role", "spectate", "marquer", "trancher", "proteger", "voter",
            "switch", "drain", "lier", "peser", "tisser", "baliser", "ensabler", "exhumer", "exorciser", "veiller", "maudire", "ressusciter", "percevoir", "usurper");
    private static final List<String> ADMIN_SUBCOMMANDS = Arrays.asList("start", "stop", "reset", "status", "roster");
    private static final List<String> ROSTER_SUBCOMMANDS = Arrays.asList("list", "add", "remove", "clear", "fill");
    private static final List<String> DEV_SUBCOMMANDS = Arrays.asList("setTime", "setRole", "who", "aura");

    public VUHCCommand(GameManager gameManager, PlayerManager playerManager, MarkerManager markerManager, RoleManager roleManager, VampireVoteManager voteManager, SpectatorManager spectatorManager) {
        this.plugin = VampireUHC.getInstance();
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
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(MessageUtil.error("Cette commande doit être exécutée par un joueur."));
                    return;
                }

                var local = playerManager.get(player);
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

            case "veiller": {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(MessageUtil.error("Cette commande doit être exécutée par un joueur."));
                    return;
                }

                if (args.length < 2) {
                    player.sendMessage(MessageUtil.error("Usage: /vuhc veiller <joueur>"));
                    return;
                }

                Player target = Bukkit.getPlayer(args[1]);
                if (target == null) {
                    player.sendMessage(MessageUtil.error("Joueur introuvable ou hors ligne."));
                    return;                
                } 

                VampireUHCPlayer localPlayer = playerManager.get(player.getUniqueId());
                if (!(localPlayer != null && localPlayer.getRole() instanceof WatchmanRole watchman)) {
                    if (localPlayer != null && localPlayer.getRole() instanceof DoppelgangerRole doppelgangerW
                            && doppelgangerW.getActivePower() instanceof UsurpedWatchman usurpedWatchman) {
                        // Veilleur copié par le Sosie : pouvoir exact, compteurs propres.
                        VampireUHCPlayer watchTargetPlayer = playerManager.get(target.getUniqueId());
                        if (watchTargetPlayer == null) {
                            player.sendMessage(MessageUtil.error("Ce joueur n'est pas en partie."));
                            return;
                        }
                        usurpedWatchman.watchPlayer(watchTargetPlayer, markerManager, gameManager.getEpisode());
                        return;
                    }
                    return;
                }

                VampireUHCPlayer targetPlayer = playerManager.get(target.getUniqueId());

                if (targetPlayer == null) {
                    player.sendMessage(MessageUtil.error("Ce joueur n'est pas en partie."));
                    return;
                }

                int current_episode = gameManager.getEpisode();

                watchman.watchPlayer(targetPlayer, markerManager, current_episode);
                return;
            }

            case "exorciser": {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(MessageUtil.error("Cette commande doit être exécutée par un joueur."));
                    return;
                }

                if (args.length < 2) {
                    player.sendMessage(MessageUtil.error("Usage: /vuhc exorciser <joueur>"));
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
                if (localPlayer != null && localPlayer.getRole() instanceof ExorcistRole exorcist) {
                    int current_episode = gameManager.getEpisode();
                    exorcist.exorcisePlayer(targetPlayer, markerManager, current_episode);
                    return;
                }
                if (localPlayer != null && localPlayer.getRole() instanceof DoppelgangerRole doppelganger1
                        && doppelganger1.getActivePower() instanceof UsurpedExorcist usurpedExorcist) {
                    // Exorciste copié par le Sosie : un seul marqueur obscur retiré,
                    // seul le nombre de marqueurs obscurs est révélé.
                    usurpedExorcist.exorcisePlayer(targetPlayer, markerManager, gameManager.getEpisode());
                    return;
                }
                return;
            }

            case "exhumer": {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(MessageUtil.error("Cette commande doit être exécutée par un joueur."));
                    return;
                }

                VampireUHCPlayer localPlayer = playerManager.get(player.getUniqueId());
                if (!(localPlayer != null && localPlayer.getRole() instanceof GravediggerRole graveDigger)) {
                    if (localPlayer != null && localPlayer.getRole() instanceof DoppelgangerRole doppelgangerD
                            && doppelgangerD.getActivePower() instanceof UsurpedGravedigger usurpedGraveDigger) {
                        // Fossoyeur copié par le Sosie : exhumation partagée (un cadavre
                        // ne se donne qu'une seule fois, vrai Fossoyeur ou sosie).
                        usurpedGraveDigger.exhum(player.getLocation());
                        return;
                    }
                    return;
                }

                Location location = player.getLocation();

                graveDigger.exhum(location);
                return;
            }

            case "ensabler": {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(MessageUtil.error("Cette commande doit être exécutée par un joueur."));
                    return;
                }

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
                if (!(localPlayer != null && localPlayer.getRole() instanceof SandMerchantRole sandMerchant)) {
                    if (localPlayer != null && localPlayer.getRole() instanceof DoppelgangerRole doppelgangerSM
                            && doppelgangerSM.getActivePower() instanceof UsurpedSandMerchant usurpedSandMerchant) {
                        // Marchand de Sable copié par le Sosie : lenteur réduite (1 min).
                        usurpedSandMerchant.sandPlayer(markerManager, targetPlayer, gameManager.getEpisode());
                        return;
                    }
                    return;
                }

                int current_episode = gameManager.getEpisode();

                sandMerchant.sandPlayer(markerManager, targetPlayer, current_episode);
                return;
            }

            case "maudire": {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(MessageUtil.error("Cette commande doit être exécutée par un joueur."));
                    return;
                }

                if (args.length < 2) {
                    player.sendMessage(MessageUtil.error("Usage: /vuhc maudire <joueur>"));
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
                if (!(localPlayer != null && localPlayer.getRole() instanceof BabaYagaRole babaYaga)) {
                    if (localPlayer != null && localPlayer.getRole() instanceof DoppelgangerRole doppelgangerBY
                            && doppelgangerBY.getActivePower() instanceof UsurpedBabaYaga usurpedBabaYaga) {
                        // Baba Yaga copiée par le Sosie : malédiction seule, sans résurrection.
                        if (!usurpedBabaYaga.cursePlayer(targetPlayer)) {
                            player.sendMessage(MessageUtil.error("Vous avez déjà utilisé votre malédiction."));
                        }
                        return;
                    }
                    return;
                }

                if (!babaYaga.cursePlayer(targetPlayer)) {
                    player.sendMessage(MessageUtil.error("Vous avez déjà utilisé votre malédiction."));
                }
                return;
            }

            case "ressusciter": {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(MessageUtil.error("Cette commande doit être exécutée par un joueur."));
                    return;
                }

                VampireUHCPlayer localPlayer = playerManager.get(player.getUniqueId());
                if (!(localPlayer != null && localPlayer.getRole() instanceof BabaYagaRole babaYaga)) {
                    return;
                }

                babaYaga.resurrect();
                return;
            }

            case "spectate": {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(MessageUtil.error("Cette commande doit être exécutée par un joueur."));
                    return;
                }
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
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(MessageUtil.error("Cette commande doit être exécutée par un joueur."));
                    return;
                }

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
                if (!(localPlayer != null && localPlayer.getRole() instanceof WeaverRole weaverRole)) {
                    if (localPlayer != null && localPlayer.getRole() instanceof DoppelgangerRole doppelgangerWeaver
                            && doppelgangerWeaver.getActivePower() instanceof UsurpedWeaver usurpedWeaver) {
                        // Tisseur copié par le Sosie : réseau FIL_DOPPELGANGER, rayon 5 blocs.
                        usurpedWeaver.weavePlayer(markerManager, targetPlayer);
                        return;
                    }
                    return;
                }

                weaverRole.weavePlayer(markerManager, targetPlayer);
                return;
            }

            case "baliser": {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(MessageUtil.error("Cette commande doit être exécutée par un joueur."));
                    return;
                } 
                
                if (args.length > 1) {
                    player.sendMessage(MessageUtil.error("Usage: /vuhc baliser"));
                    return;
                }

                VampireUHCPlayer localPlayer = playerManager.get(player.getUniqueId());

                if (!(localPlayer != null && localPlayer.getRole() instanceof CartographerRole cartographerRole)) {
                    if (localPlayer != null && localPlayer.getRole() instanceof DoppelgangerRole doppelgangerCarto
                            && doppelgangerCarto.getActivePower() instanceof UsurpedCartographer usurpedCartographer) {
                        // Cartographe copié par le Sosie : balise exacte (clés JSON propres).
                        usurpedCartographer.placeBeacon(player, gameManager.getEpisode());
                        return;
                    }
                    return;
                }

                cartographerRole.placeBeacon(player, gameManager.getEpisode());
                return;
            }

            case "marquer": {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(MessageUtil.error("Cette commande doit être exécutée par un joueur."));
                    return;
                }
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
                if (localPlayer == null) {
                    return;
                }

                // Le Maître (comme le Sosie qui le copie) ne peut marquer qu'un
                // joueur croisé durant l'épisode précédent.
                if (!plugin.getCrossTracker().hasCrossed(player.getUniqueId(), targetPlayer.getUuid())) {
                    player.sendMessage(MessageUtil.error("Vous n'avez pas croisé ce joueur durant l'épisode précédent."));
                    return;
                }

                // Pouvoir copié : le Sosie marque comme le Maître, mais pose sa
                // propre marque neutre (MARQUE_MAITRE_DOPPELGANGER).
                if (localPlayer.getRole() instanceof DoppelgangerRole doppelgangerM
                        && doppelgangerM.getActivePower() instanceof UsurpedMaster usurpedMaster) {
                    if (!usurpedMaster.markPlayer(markerManager, targetPlayer, gameManager.getEpisode())) {
                        player.sendMessage(MessageUtil.error("Vous avez déjà marqué ce joueur avec votre marque."));
                    }
                    return;
                }

                if (localPlayer.getRole() instanceof MasterRole masterRole) {
                    masterRole.markPlayer(markerManager, targetPlayer, gameManager.getEpisode());
                }
                return;
            }

            case "trancher": {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(MessageUtil.error("Cette commande doit être exécutée par un joueur."));
                    return;
                }

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
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(MessageUtil.error("Cette commande doit être exécutée par un joueur."));
                    return;
                }

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
                if (!(localProt != null && localProt.getRole() instanceof SaviorRole savior)) {
                    if (localProt != null && localProt.getRole() instanceof DoppelgangerRole doppelgangerSav
                            && doppelgangerSav.getActivePower() instanceof UsurpedSavior usurpedSavior) {
                        // Salvateur copié par le Sosie : blocage 50% (MarkerManager).
                        if (usurpedSavior.applySalvation(markerManager, protTargetPlayer, gameManager.getEpisode())) {
                            player.sendMessage(MessageUtil.successTarget("Marque Salvation posée sur", protTargetPlayer.getLastKnownName()));
                        } else {
                            player.sendMessage(MessageUtil.error("Vous ne pouvez pas protéger ce joueur pour l'instant."));
                        }
                        return;
                    }
                    return;
                }

                if (savior.applySalvation(markerManager, protTargetPlayer, gameManager.getEpisode())) {
                    player.sendMessage(MessageUtil.successTarget("Marque Salvation posée sur", protTargetPlayer.getLastKnownName()));
                } else {
                    player.sendMessage(MessageUtil.error("Vous ne pouvez pas protéger ce joueur pour l'instant."));
                }
                return;
            }

            case "usurper": {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(MessageUtil.error("Cette commande doit être exécutée par un joueur."));
                    return;
                }

                if (args.length < 2) {
                    player.sendMessage(MessageUtil.error("Usage: /vuhc usurper <joueur>"));
                    return;
                }

                Player susTarget = Bukkit.getPlayer(args[1]);
                if (susTarget == null) {
                    player.sendMessage(MessageUtil.error("Joueur introuvable ou hors ligne."));
                    return;
                }

                VampireUHCPlayer localUsurp = playerManager.get(player.getUniqueId());
                if (!(localUsurp != null && localUsurp.getRole() instanceof DoppelgangerRole doppelganger)) {
                    return;
                }

                VampireUHCPlayer targetUsurp = playerManager.get(susTarget.getUniqueId());
                if (targetUsurp == null) {
                    player.sendMessage(MessageUtil.error("Ce joueur n'est pas en partie."));
                    return;
                }
                if (targetUsurp.getUuid().equals(localUsurp.getUuid())) {
                    player.sendMessage(MessageUtil.error("Vous ne pouvez pas vous copier vous-même."));
                    return;
                }
                if (!targetUsurp.isAlive()) {
                    player.sendMessage(MessageUtil.error("Ce joueur est mort, vous ne pouvez pas copier ses pouvoirs."));
                    return;
                }
                if (targetUsurp.getRole() instanceof DoppelgangerRole) {
                    player.sendMessage(MessageUtil.error("Vous ne pouvez pas copier les pouvoirs d'un autre Sosie."));
                    return;
                }
                if (!doppelganger.canUsurp(gameManager.getElapsedSeconds())) {
                    if (doppelganger.hasUsed()) {
                        player.sendMessage(MessageUtil.error("Vous avez déjà copié les pouvoirs d'un joueur, une seule fois."));
                    } else {
                        player.sendMessage(MessageUtil.error("L'usurpation n'est possible qu'entre "
                                + VampireUHC.getInstance().getConfigManager().getUsurpWindowStartMin()
                                + " et "
                                + VampireUHC.getInstance().getConfigManager().getUsurpWindowEndMin()
                                + " minutes de jeu."));
                    }
                    return;
                }

                var copied = doppelganger.usurp(targetUsurp);
                if (copied == null) {
                    player.sendMessage(MessageUtil.error("Vous n'êtes pas parvenu à copier les pouvoirs de ce joueur."));
                    return;
                }
                player.sendMessage(MessageUtil.successTarget("Vous copiez les pouvoirs de", targetUsurp.getLastKnownName())
                        .append(MessageUtil.info(" (\"" + copied.getName() + "\").")));
                return;
            }

            case "voter": {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(MessageUtil.error("Cette commande doit être exécutée par un joueur."));
                    return;
                }

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

                // Un vampire ne peut voter qu'un joueur croisé durant l'épisode précédent.
                if (!plugin.getCrossTracker().hasCrossed(player.getUniqueId(), voteTargetPlayer.getUuid())) {
                    player.sendMessage(MessageUtil.error("Vous n'avez pas croisé ce joueur durant l'épisode précédent."));
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
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(MessageUtil.error("Cette commande doit être exécutée par un joueur."));
                    return;
                }

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
                if (!(localSwitch != null && localSwitch.getRole() instanceof GremlinRole gremlin)) {
                    if (localSwitch != null && localSwitch.getRole() instanceof DoppelgangerRole doppelgangerS
                            && doppelgangerS.getActivePower() instanceof UsurpedGremlin usurpedGremlin) {
                        // Gremlin copié par le Sosie : switch imparfait (un seul marqueur
                        // aléatoire de chaque joueur), draineur à distance nulle inclus.
                        boolean nearS1 = sameWorld(player, switchTarget1)
                                && player.getLocation().distanceSquared(switchTarget1.getLocation()) <= 20 * 20;
                        boolean nearS2 = sameWorld(player, switchTarget2)
                                && player.getLocation().distanceSquared(switchTarget2.getLocation()) <= 20 * 20;
                        if (!nearS1 && !nearS2) {
                            player.sendMessage(MessageUtil.error("L'une des deux cibles doit se trouver à moins de 20 blocs de vous."));
                            return;
                        }

                        if (usurpedGremlin.switchMarkers(markerManager, t1, t2, gameManager.getEpisode())) {
                            player.sendMessage(MessageUtil.successTwoTargets("Marques échangées entre", t1.getLastKnownName(), t2.getLastKnownName()));

                            for (VampireUHCPlayer p : playerManager.getAll()) {
                                if (p.getRole() instanceof CupidonRole cupidon) {
                                    cupidon.notifyIfLoversMoved(markerManager);
                                } else if (p.getRole() instanceof DoppelgangerRole doppelgangerNotify2
                                        && doppelgangerNotify2.getActivePower() instanceof UsurpedCupidon usurpedCupidon2) {
                                    // Cupidon copié par le Sosie : même surveillance des swaps.
                                    usurpedCupidon2.notifyIfLoversMoved(markerManager);
                                }
                            }
                        } else {
                            player.sendMessage(MessageUtil.error("Vous avez déjà utilisé votre switch cet épisode."));
                        }
                        return;
                    }
                    return;
                }

                // Au moins une des deux cibles doit se trouver à moins de 20 blocs
                // du Gremlin (auto-switch : distance nulle, donc toujours valide).
                boolean near1 = sameWorld(player, switchTarget1)
                        && player.getLocation().distanceSquared(switchTarget1.getLocation()) <= 20 * 20;
                boolean near2 = sameWorld(player, switchTarget2)
                        && player.getLocation().distanceSquared(switchTarget2.getLocation()) <= 20 * 20;
                if (!near1 && !near2) {
                    player.sendMessage(MessageUtil.error("L'une des deux cibles doit se trouver à moins de 20 blocs de vous."));
                    return;
                }

                if (gremlin.SwitchMarkers(markerManager, t1, t2, gameManager.getEpisode())) {
                    player.sendMessage(MessageUtil.successTwoTargets("Marques échangées entre", t1.getLastKnownName(), t2.getLastKnownName()));

                    // Si une marque Amour a changé de propriétaire, le Cupidon doit être prévenu.
                    for (VampireUHCPlayer p : playerManager.getAll()) {
                        if (p.getRole() instanceof CupidonRole cupidon) {
                            cupidon.notifyIfLoversMoved(markerManager);
                        } else if (p.getRole() instanceof DoppelgangerRole doppelgangerNotify
                                && doppelgangerNotify.getActivePower() instanceof UsurpedCupidon usurpedCupidon) {
                            // Cupidon copié par le Sosie : même surveillance des swaps.
                            usurpedCupidon.notifyIfLoversMoved(markerManager);
                        }
                    }
                } else {
                    player.sendMessage(MessageUtil.error("Vous avez déjà utilisé votre switch cet épisode."));
                }
                return;
            }

            case "drain": {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(MessageUtil.error("Cette commande doit être exécutée par un joueur."));
                    return;
                }

                VampireUHCPlayer localPlayer = playerManager.get(player.getUniqueId());
                if (!(localPlayer != null && localPlayer.getRole() instanceof GremlinRole gremlinRole)) {
                    if (localPlayer != null && localPlayer.getRole() instanceof DoppelgangerRole doppelgangerDrain
                            && doppelgangerDrain.getActivePower() instanceof UsurpedGremlin usurpedGremlin) {
                        // Drain du Sosie : même activation, compteurs propres.
                        if (usurpedGremlin.activateDrain(gameManager.getEpisode())) {
                            player.sendActionBar(MessageUtil.actionBar("<dark_purple>Vous avez activé le <dark_green>drain</dark_green> !"));
                        } else {
                            player.sendMessage(MessageUtil.error("Vous ne pouvez pas activer votre drain pour l'instant."));
                        }
                        return;
                    }
                    return;
                }

                if (gremlinRole.activateDrain(gameManager.getEpisode())) {
                    player.sendActionBar(MessageUtil.actionBar("<dark_purple>Vous avez activé le <dark_green>drain</dark_green> !"));
                } else {
                    player.sendMessage(MessageUtil.error("Vous ne pouvez pas activer votre drain pour l'instant."));
                }
                return;
            }

            case "lier": {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(MessageUtil.error("Cette commande doit être exécutée par un joueur."));
                    return;
                }
                if (args.length < 3) {
                    player.sendMessage(MessageUtil.error("Usage: /vuhc lier <Joueur1> <Joueur2>"));
                    return;
                }

                VampireUHCPlayer cupidon = playerManager.get(player.getUniqueId());
                if (!(cupidon != null && cupidon.getRole() instanceof CupidonRole cupidonRole)) {
                    return;
                }

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
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(MessageUtil.error("Cette commande doit être exécutée par un joueur."));
                    return;
                }
                if (args.length < 3) {
                    player.sendMessage(MessageUtil.error("Usage: /vuhc peser <Joueur1> <Joueur2>"));
                    return;
                }

                VampireUHCPlayer localPlayer = playerManager.get(player.getUniqueId());
                if (localPlayer == null) {
                    return;
                }

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

                if (localPlayer.getRole() instanceof SoulweigherRole soulweigherRole) {
                    if (!soulweigherRole.weightAura(markerManager, t1, t2, gameManager.getEpisode())) {
                        player.sendActionBar(MessageUtil.actionBar("<red>Erreur interne."));
                    }
                } else if (localPlayer.getRole() instanceof DoppelgangerRole doppelganger2
                        && doppelganger2.getActivePower() instanceof UsurpedSoulweigher usurpedSoulweigher) {
                    // Peseuse d'âmes copiée par le Sosie : pouvoir exact, compteurs propres.
                    if (!usurpedSoulweigher.weightAura(markerManager, t1, t2, gameManager.getEpisode())) {
                        player.sendMessage(MessageUtil.error("Erreur interne."));
                    }
                }
                return;
            }

            case "percevoir": {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(MessageUtil.error("Cette commande doit être exécutée par un joueur."));
                    return;
                }
                if (args.length < 2) {
                    player.sendMessage(MessageUtil.error("Usage: /vuhc percevoir <joueur>"));
                    return;
                }

                VampireUHCPlayer localPlayer = playerManager.get(player.getUniqueId());
                if (localPlayer == null) {
                    return;
                }

                Player perceiveTarget = Bukkit.getPlayer(args[1]);
                if (perceiveTarget == null) {
                    player.sendMessage(MessageUtil.error("Joueur introuvable ou hors ligne."));
                    return;
                }

                VampireUHCPlayer targetPlayer = playerManager.get(perceiveTarget.getUniqueId());
                if (targetPlayer == null) {
                    player.sendMessage(MessageUtil.error("Ce joueur n'est pas en partie."));
                    return;
                }

                if (localPlayer.getRole() instanceof PriestRole priest) {
                    int code = priest.perceive(markerManager, targetPlayer, gameManager.getEpisode());
                    if (code == 1) {
                        player.sendMessage(MessageUtil.error("Vous avez déjà perçu une aura cet épisode."));
                    } else if (code == 2) {
                        player.sendMessage(MessageUtil.error("Vous ne pouvez pas percevoir deux épisodes de suite le même joueur."));
                    }
                } else if (localPlayer.getRole() instanceof DoppelgangerRole doppelganger3
                        && doppelganger3.getActivePower() instanceof UsurpedPriest usurpedPriest) {
                    // Prêtre copié par le Sosie : perception imparfaite, compteurs propres.
                    int code = usurpedPriest.perceive(markerManager, targetPlayer, gameManager.getEpisode());
                    if (code == 1) {
                        player.sendMessage(MessageUtil.error("Vous avez déjà perçu une aura cet épisode."));
                    } else if (code == 2) {
                        player.sendMessage(MessageUtil.error("Vous ne pouvez pas percevoir deux épisodes de suite le même joueur."));
                    }
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
            sender.sendMessage(MessageUtil.error("Usage: /vuhc admin <start [sec]|stop|reset|status|roster <list|add|remove|clear|fill>>"));
            return;
        }

        switch (args[0].toLowerCase()) {
            case "start":
                String validationError = gameManager.preStartValidation();
                if (validationError != null) {
                    sender.sendMessage(MessageUtil.error(validationError));
                    return;
                }
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

            case "roster":
                if (args.length < 2) {
                    sender.sendMessage(MessageUtil.error("Usage: /vuhc admin roster <list|add <pseudo>|remove <pseudo>|clear|fill>"));
                    return;
                }
                handleRoster(sender, Arrays.copyOfRange(args, 1, args.length));
                return;

            default:
                sender.sendMessage(MessageUtil.error("Sous-commande inconnue."));
                sender.sendMessage(MessageUtil.error("Usage: /vuhc admin <start [sec]|stop|reset|status|roster <list|add|remove|clear|fill>>"));
        }
    }

    private void handleRoster(CommandSender sender, String[] args) {
        switch (args[0].toLowerCase()) {
            case "list":
                sender.sendMessage(MessageUtil.warn("Roster : " + gameManager.getRosterSize() + "/" + plugin.getConfigManager().getComposition().getRequiredPlayers() + " joueurs"));
                if (gameManager.getRoster().isEmpty()) {
                    sender.sendMessage(MessageUtil.warn("Aucun joueur sélectionné."));
                    return;
                }
                for (Map.Entry<UUID, String> entry : gameManager.getRoster().entrySet()) {
                    boolean online = Bukkit.getPlayer(entry.getKey()) != null;
                    sender.sendMessage(MessageUtil.info("- " + entry.getValue() + (online ? "" : " (hors ligne)")));
                }
                return;

            case "add":
                if (args.length < 2) {
                    sender.sendMessage(MessageUtil.error("Usage: /vuhc admin roster add <pseudo>"));
                    return;
                }
                Player toAdd = Bukkit.getPlayerExact(args[1]);
                if (toAdd == null) {
                    sender.sendMessage(MessageUtil.error("Joueur introuvable ou hors ligne : " + args[1]));
                    return;
                }
                if (gameManager.isGameStarted()) {
                    sender.sendMessage(MessageUtil.error("Impossible de modifier le roster, la partie a commencé."));
                    return;
                }
                if (gameManager.addToRoster(toAdd)) {
                    sender.sendMessage(MessageUtil.success(toAdd.getName() + " sélectionné. Roster : " + gameManager.getRosterSize() + "/" + plugin.getConfigManager().getComposition().getRequiredPlayers()));
                } else {
                    sender.sendMessage(MessageUtil.error(toAdd.getName() + " est déjà sélectionné."));
                }
                return;

            case "remove":
                if (args.length < 2) {
                    sender.sendMessage(MessageUtil.error("Usage: /vuhc admin roster remove <pseudo>"));
                    return;
                }
                Player toRemove = Bukkit.getPlayerExact(args[1]);
                if (gameManager.removeFromRoster(toRemove)) {
                    sender.sendMessage(MessageUtil.success(toRemove.getName() + " retiré de la sélection. Roster : " + gameManager.getRosterSize() + "/" + plugin.getConfigManager().getComposition().getRequiredPlayers()));
                } else {
                    sender.sendMessage(MessageUtil.error(toRemove.getName() + " n'est pas sélectionné."));
                }
                return;

            case "clear":
                gameManager.clearRoster();
                sender.sendMessage(MessageUtil.success("Roster vidé (0/" + plugin.getConfigManager().getComposition().getRequiredPlayers() + ")."));
                return;

            case "fill":
                int added = gameManager.fillRoster();
                if (added == 0) {
                    sender.sendMessage(MessageUtil.warn("Aucun joueur en ligne supplémentaire à sélectionner (roster " + gameManager.getRosterSize() + "/" + plugin.getConfigManager().getComposition().getRequiredPlayers() + ")."));
                } else {
                    sender.sendMessage(MessageUtil.success(added + " joueur(s) sélectionné(s). Roster : " + gameManager.getRosterSize() + "/" + plugin.getConfigManager().getComposition().getRequiredPlayers()));
                }
                return;

            default:
                sender.sendMessage(MessageUtil.error("Sous-commande inconnue."));
                sender.sendMessage(MessageUtil.error("Usage: /vuhc admin roster <list|add <pseudo>|remove <pseudo>|clear|fill>"));
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
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(MessageUtil.error("Cette commande doit être exécutée par un joueur."));
                    return;
                }
                if (args.length >= 2) {
                    try {
                        var type = RoleType.fromString(args[1]);
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
            if (args.length == 2) {
                return filterStartsWith(ADMIN_SUBCOMMANDS, args[1]);
            }
            if (args.length >= 3 && args[1].equalsIgnoreCase("roster")) {
                if (args.length == 3) {
                    return filterStartsWith(ROSTER_SUBCOMMANDS, args[2]);
                }
                if (args.length == 4 && (args[2].equalsIgnoreCase("add") || args[2].equalsIgnoreCase("remove"))) {
                    return playersStartingWith(args[3]);
                }
            }
            return new ArrayList<>();
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
                    || sub.equals("tisser") || sub.equals("spectate") || sub.equals("ensabler") || sub.equals("veiller")) || sub.equals("exorciser") || sub.equals("maudire") || sub.equals("percevoir") || sub.equals("usurper") && hasAccess(sender, sub)) {
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
                return hasRole(sender, WeaverRole.class) || hasActiveUsurpedPower(sender, UsurpedWeaver.class);
            case "baliser":
                return hasRole(sender, CartographerRole.class) || hasActiveUsurpedPower(sender, UsurpedCartographer.class);
            case "role":
                return true;
            case "exorciser":
                return hasRole(sender, ExorcistRole.class) || hasActiveUsurpedPower(sender, UsurpedExorcist.class);
            case "maudire":
                return hasRole(sender, BabaYagaRole.class) || hasActiveUsurpedPower(sender, UsurpedBabaYaga.class);
            case "ressusciter":
                return hasRole(sender, BabaYagaRole.class);
            case "exhumer":
                return hasRole(sender, GravediggerRole.class) || hasActiveUsurpedPower(sender, UsurpedGravedigger.class);
            case "ensabler":
                return hasRole(sender, SandMerchantRole.class) || hasActiveUsurpedPower(sender, UsurpedSandMerchant.class);
            case "spectate":
                return sender instanceof Player player && player.getGameMode() == GameMode.SPECTATOR;
            case "marquer":
            case "trancher":
                return hasRole(sender, MasterRole.class) || hasActiveUsurpedPower(sender, UsurpedMaster.class);
            case "proteger":
                return hasRole(sender, SaviorRole.class) || hasActiveUsurpedPower(sender, UsurpedSavior.class);
            case "voter": {
                VampireUHCPlayer vp = playerOf(sender);
                return vp != null && (vp.getRole() instanceof VampireMinion || vp.getRole() instanceof ComteRole) && vp.canVoteVampireMark()
                        // Le Sosie vote lui aussi s'il a copié un vampire (Sbire ou Comte).
                        || hasActiveUsurpedPower(sender, UsurpedVampire.class);
            }
            case "switch":
            case "drain":
                return hasRole(sender, GremlinRole.class) || hasActiveUsurpedPower(sender, UsurpedGremlin.class);
            case "lier":
                return hasRole(sender, CupidonRole.class);
            case "peser":
                return hasRole(sender, SoulweigherRole.class) || hasActiveUsurpedPower(sender, UsurpedSoulweigher.class);
            case "percevoir":
                return hasRole(sender, PriestRole.class) || hasActiveUsurpedPower(sender, UsurpedPriest.class);
            case "usurper":
                return hasRole(sender, DoppelgangerRole.class);
            case "veiller":
                return hasRole(sender, WatchmanRole.class) || hasActiveUsurpedPower(sender, UsurpedWatchman.class);
            case "admin":
            case "dev":
            case "start":
            case "stop":
            case "reset":
            case "status":
            case "roster":
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

    // Le Sosie garde accès aux commandes du rôle copié tant que son pouvoir
    // copié est actif (tant que la cible usurpée est vivante).
    private boolean hasActiveUsurpedPower(CommandSender sender, Class<? extends UsurpedPower> powerClass) {
        VampireUHCPlayer vp = playerOf(sender);
        if (vp == null || !vp.isAlive() || !(vp.getRole() instanceof DoppelgangerRole doppelganger)) {
            return false;
        }
        return doppelganger.getActivePower() != null && powerClass.isInstance(doppelganger.getActivePower());
    }

    private VampireUHCPlayer playerOf(CommandSender sender) {
        if (sender instanceof Player player) {
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
        if (sender instanceof Player player) {
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

    // Même monde ?
    private boolean sameWorld(Player a, Player b) {
        org.bukkit.World wa = a.getLocation().getWorld();
        org.bukkit.World wb = b.getLocation().getWorld();
        return wa != null && wa.equals(wb);
    }

    // Aide filtrée : seules les commandes accessibles à l'expéditeur sont affichées.
    private void sendHelp(CommandSender sender) {
        sender.sendMessage(MessageUtil.helpBanner());
        if (hasAccess(sender, "role")) {
            sender.sendMessage(MessageUtil.helpEntry("/vuhc role", "Affiche votre rôle"));
        }

        if (hasAccess(sender, "veiller")) {
            sender.sendMessage(MessageUtil.helpEntry("/vuhc veiller <joueur>", "Veiller sur un joueur (Veilleur)"));
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
        if (hasAccess(sender, "maudire")) {
            sender.sendMessage(MessageUtil.helpEntry("/vuhc maudire <joueur>", "Maudire un joueur (Baba Yaga)"));
        }
        if (hasAccess(sender, "ressusciter")) {
            sender.sendMessage(MessageUtil.helpEntry("/vuhc ressusciter", "Ressusciter le joueur proposé (Baba Yaga)"));
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
        if (hasAccess(sender, "percevoir")) {
            sender.sendMessage(MessageUtil.helpEntry("/vuhc percevoir <joueur>", "Percevoir l'aura d'un joueur (Prêtre)"));
        }
        if (hasAccess(sender, "usurper")) {
            sender.sendMessage(MessageUtil.helpEntry("/vuhc usurper <joueur>", "Copier les pouvoirs d'un joueur (Doppelganger)"));
        }
        if (hasAccess(sender, "tisser")) {
            sender.sendMessage(MessageUtil.helpEntry("/vuhc tisser <joueur>", "Tisser un fil sur un joueur"));
        }
        if (hasAccess(sender, "baliser")) {
            sender.sendMessage(MessageUtil.helpEntry("/vuhc baliser", "Poser une balise (Cartographe)"));
        }
        if (hasAdminPermission(sender)) {
            sender.sendMessage(MessageUtil.helpSection("Admin"));
            sender.sendMessage(MessageUtil.helpEntry("/vuhc admin", "<start|stop|reset|status|roster>"));
            sender.sendMessage(MessageUtil.helpSection("Dev"));
            sender.sendMessage(MessageUtil.helpEntry("/vuhc dev", "<setTime|setRole|who|aura>"));
        }
    }
}
