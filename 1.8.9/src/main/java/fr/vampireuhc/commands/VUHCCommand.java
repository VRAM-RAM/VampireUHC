package fr.vampireuhc.commands;

import fr.vampireuhc.VampireUHC;
import fr.vampireuhc.game.GameManager;
import fr.vampireuhc.game.SpectatorManager;
import fr.vampireuhc.markers.MarkerManager;
import fr.vampireuhc.player.PlayerManager;
import fr.vampireuhc.player.Camp;
import fr.vampireuhc.player.VampireUHCPlayer;
import fr.vampireuhc.roles.RoleManager;
import fr.vampireuhc.roles.RoleType;
import fr.vampireuhc.roles.SandMerchantRole;
import fr.vampireuhc.roles.WatchmanRole;
import fr.vampireuhc.vampire_vote.VampireVoteManager;
import fr.vampireuhc.vampire_vote.VoteResult;
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
import fr.vampireuhc.roles.BabaYagaRole;
import fr.vampireuhc.roles.PriestRole;
import fr.vampireuhc.roles.ComteRole;
import fr.vampireuhc.roles.DoppelgangerRole;
import fr.vampireuhc.roles.WatchmanRole;
import fr.vampireuhc.roles.usurped.UsurpedPower;
import fr.vampireuhc.roles.usurped.UsurpedPriest;
import fr.vampireuhc.roles.usurped.UsurpedSoulweigher;
import fr.vampireuhc.roles.usurped.UsurpedMaster;
import fr.vampireuhc.roles.usurped.UsurpedVampire;
import fr.vampireuhc.roles.usurped.UsurpedExorcist;
import fr.vampireuhc.roles.usurped.UsurpedGravedigger;
import fr.vampireuhc.roles.usurped.UsurpedGremlin;
import fr.vampireuhc.roles.usurped.UsurpedWeaver;
import fr.vampireuhc.roles.usurped.UsurpedCartographer;
import fr.vampireuhc.roles.usurped.UsurpedWatchman;
import fr.vampireuhc.roles.usurped.UsurpedBabaYaga;
import fr.vampireuhc.roles.usurped.UsurpedSandMerchant;
import fr.vampireuhc.roles.usurped.UsurpedSavior;
import fr.vampireuhc.roles.usurped.UsurpedCupidon;



import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import fr.vampireuhc.config.MessageUtil;

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
 *  * /vuhc usurper <joueur>  -> Doppelganger copie les pouvoirs d'un joueur.
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
                if (localPlayer == null || localPlayer.getRole() == null) {
                    return;
                }

                VampireUHCPlayer targetPlayer = playerManager.get(target.getUniqueId());

                if (targetPlayer == null) {
                    player.sendMessage(MessageUtil.error("Ce joueur n'est pas en partie."));
                    return;
                }

                int current_episode = gameManager.getEpisode();

                // Pouvoir copié : l'exorcisme du Sosie est imparfait (une seule
                // marque obscure retirée, seul le nombre est révélé).
                if (localPlayer.getRole() instanceof DoppelgangerRole) {
                    DoppelgangerRole doppelganger = (DoppelgangerRole) localPlayer.getRole();
                    if (doppelganger.getActivePower() instanceof UsurpedExorcist) {
                        ((UsurpedExorcist) doppelganger.getActivePower()).exorcisePlayer(targetPlayer, markerManager, current_episode);
                    }
                    return;
                }

                if (!(localPlayer.getRole() instanceof ExorcistRole)) {
                    return;
                }
                ExorcistRole exorcist = (ExorcistRole) localPlayer.getRole();

                exorcist.exorcisePlayer(targetPlayer, markerManager, current_episode);
                return;
            }

            case "exhumer": {
                if (!(sender instanceof Player)) {
                    sender.sendMessage(MessageUtil.error("Cette commande doit être exécutée par un joueur."));
                    return;
                }
                Player player = (Player) sender;

                VampireUHCPlayer localPlayer = playerManager.get(player.getUniqueId());
                if (localPlayer == null || localPlayer.getRole() == null) {
                    return;
                }

                Location location = player.getLocation();

                // Pouvoir copié : le Sosie exhume depuis son registre propre, mais
                // un cadavre ne peut être exhumé qu'une seule fois au total.
                if (localPlayer.getRole() instanceof DoppelgangerRole) {
                    DoppelgangerRole doppelganger = (DoppelgangerRole) localPlayer.getRole();
                    if (doppelganger.getActivePower() instanceof UsurpedGravedigger) {
                        ((UsurpedGravedigger) doppelganger.getActivePower()).exhum(location);
                    }
                    return;
                }

                if (!(localPlayer.getRole() instanceof GravediggerRole)) {
                    return;
                }
                GravediggerRole graveDigger = (GravediggerRole) localPlayer.getRole();

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
                if (localPlayer == null || localPlayer.getRole() == null) {
                    return;
                }

                int current_episode = gameManager.getEpisode();

                // Pouvoir copié : le Sosie ensable avec ses propres marqueurs
                // (SABLE_*_DOPPELGANGER), lenteur réduite à 1 minute.
                if (localPlayer.getRole() instanceof DoppelgangerRole) {
                    DoppelgangerRole doppelganger = (DoppelgangerRole) localPlayer.getRole();
                    if (doppelganger.getActivePower() instanceof UsurpedSandMerchant) {
                        ((UsurpedSandMerchant) doppelganger.getActivePower()).sandPlayer(markerManager, targetPlayer, current_episode);
                    }
                    return;
                }

                if (!(localPlayer.getRole() instanceof SandMerchantRole)) {
                    return;
                }
                SandMerchantRole sandMerchant = (SandMerchantRole) localPlayer.getRole();

                sandMerchant.sandPlayer(markerManager, targetPlayer, current_episode);
                return;
            }

            case "maudire": {
                if (!(sender instanceof Player)) {
                    sender.sendMessage(MessageUtil.error("Cette commande doit être exécutée par un joueur."));
                    return;
                }
                Player player = (Player) sender;

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
                if (localPlayer == null || localPlayer.getRole() == null) {
                    return;
                }

                // Pouvoir copié : le Sosie ne sait que maudire (pas ressusciter).
                if (localPlayer.getRole() instanceof DoppelgangerRole) {
                    DoppelgangerRole doppelganger = (DoppelgangerRole) localPlayer.getRole();
                    if (doppelganger.getActivePower() instanceof UsurpedBabaYaga
                            && !((UsurpedBabaYaga) doppelganger.getActivePower()).cursePlayer(targetPlayer)) {
                        player.sendMessage(MessageUtil.error("Vous avez déjà utilisé votre malédiction."));
                    }
                    return;
                }

                if (!(localPlayer.getRole() instanceof BabaYagaRole)) {
                    return;
                }
                BabaYagaRole babaYaga = (BabaYagaRole) localPlayer.getRole();

                if (!babaYaga.cursePlayer(targetPlayer)) {
                    player.sendMessage(MessageUtil.error("Vous avez déjà utilisé votre malédiction."));
                }
                return;
            }

            case "ressusciter": {
                if (!(sender instanceof Player)) {
                    sender.sendMessage(MessageUtil.error("Cette commande doit être exécutée par un joueur."));
                    return;
                }
                Player player = (Player) sender;

                VampireUHCPlayer localPlayer = playerManager.get(player.getUniqueId());
                if (!(localPlayer != null && localPlayer.getRole() instanceof BabaYagaRole)) {
                    return;
                }
                BabaYagaRole babaYaga = (BabaYagaRole) localPlayer.getRole();

                babaYaga.resurrect();
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
                if (localPlayer == null || localPlayer.getRole() == null) {
                    return;
                }

                // Pouvoir copié : le Sosie tisse son propre réseau
                // (FIL_DOPPELGANGER, rayon réduit à 5 blocs).
                if (localPlayer.getRole() instanceof DoppelgangerRole) {
                    DoppelgangerRole doppelganger = (DoppelgangerRole) localPlayer.getRole();
                    if (doppelganger.getActivePower() instanceof UsurpedWeaver) {
                        ((UsurpedWeaver) doppelganger.getActivePower()).weavePlayer(markerManager, targetPlayer);
                    }
                    return;
                }

                if (!(localPlayer.getRole() instanceof WeaverRole)) {
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

                if (localPlayer == null || localPlayer.getRole() == null) {
                    return;
                }

                // Pouvoir copié : le Sosie balise comme le Cartographe.
                if (localPlayer.getRole() instanceof DoppelgangerRole) {
                    DoppelgangerRole doppelganger = (DoppelgangerRole) localPlayer.getRole();
                    if (doppelganger.getActivePower() instanceof UsurpedCartographer) {
                        ((UsurpedCartographer) doppelganger.getActivePower()).placeBeacon(player, gameManager.getEpisode());
                    }
                    return;
                }

                if (!(localPlayer.getRole() instanceof CartographerRole)) {
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
                if (localPlayer.getRole() instanceof DoppelgangerRole) {
                    DoppelgangerRole doppelganger = (DoppelgangerRole) localPlayer.getRole();
                    if (doppelganger.getActivePower() instanceof UsurpedMaster) {
                        ((UsurpedMaster) doppelganger.getActivePower()).markPlayer(markerManager, targetPlayer, gameManager.getEpisode());
                    }
                    return;
                }

                if (!(localPlayer.getRole() instanceof MasterRole)) {
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

            case "usurper": {
                if (!(sender instanceof Player)) {
                    sender.sendMessage(MessageUtil.error("Cette commande doit être exécutée par un joueur."));
                    return;
                }
                Player player = (Player) sender;

                if (args.length < 2) {
                    player.sendMessage(MessageUtil.error("Usage: /vuhc usurper <joueur>"));
                    return;
                }

                VampireUHCPlayer local = playerManager.get(player.getUniqueId());
                if (!(local != null && local.getRole() instanceof DoppelgangerRole)) {
                    return;
                }
                DoppelgangerRole doppelganger = (DoppelgangerRole) local.getRole();

                if (!doppelganger.canUsurp(gameManager.getElapsedSeconds())) {
                    player.sendMessage(MessageUtil.error(
                            "L'usurpation n'est possible qu'entre 20 et 60 minutes de jeu, et une seule fois."));
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
                if (targetPlayer.getUuid().equals(local.getUuid())) {
                    player.sendMessage(MessageUtil.error("Vous ne pouvez pas vous copier vous-même."));
                    return;
                }
                if (!targetPlayer.isAlive()) {
                    player.sendMessage(MessageUtil.error("Ce joueur est mort, vous ne pouvez pas copier ses pouvoirs."));
                    return;
                }
                if (targetPlayer.getRole() instanceof DoppelgangerRole) {
                    player.sendMessage(MessageUtil.error("Vous ne pouvez pas copier les pouvoirs d'un autre Sosie."));
                    return;
                }

                UsurpedPower power = doppelganger.usurp(targetPlayer);
                if (power == null) {
                    player.sendMessage(MessageUtil.error("Vous n'êtes pas parvenu à copier les pouvoirs de ce joueur."));
                    return;
                }
                player.sendMessage(MessageUtil.successTarget("Vous copiez les pouvoirs de", targetPlayer.getLastKnownName()));
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
                if (localProt == null || localProt.getRole() == null) {
                    return;
                }

                // Pouvoir copié : salvation imparfaite (50% de blocage).
                if (localProt.getRole() instanceof DoppelgangerRole) {
                    DoppelgangerRole doppelganger = (DoppelgangerRole) localProt.getRole();
                    if (doppelganger.getActivePower() instanceof UsurpedSavior) {
                        if (((UsurpedSavior) doppelganger.getActivePower()).applySalvation(markerManager, protTargetPlayer, gameManager.getEpisode())) {
                            player.sendMessage(MessageUtil.successTarget("Marque Salvation posée sur", protTargetPlayer.getLastKnownName()));
                        } else {
                            player.sendMessage(MessageUtil.error("Vous ne pouvez pas protéger ce joueur pour l'instant."));
                        }
                    }
                    return;
                }

                if (!(localProt.getRole() instanceof SaviorRole)) {
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

            case "percevoir": {
                if (!(sender instanceof Player)) {
                    sender.sendMessage(MessageUtil.error("Cette commande doit être exécutée par un joueur."));
                    return;
                }

                Player player = (Player) sender;

                if (args.length < 2) {
                    player.sendMessage(MessageUtil.error("Usage: /vuhc percevoir <joueur>"));
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

                VampireUHCPlayer localPlayer = playerManager.get(player.getUniqueId());
                if (localPlayer == null || localPlayer.getRole() == null) {
                    return;
                }

                // Pouvoir copié : le Sosie qui a usurpé un Prêtre perçoit comme lui,
                // mais avec chaque probabilité d'erreur augmentée de 10 points.
                if (localPlayer.getRole() instanceof DoppelgangerRole) {
                    DoppelgangerRole doppelganger = (DoppelgangerRole) localPlayer.getRole();
                    if (doppelganger.getActivePower() instanceof UsurpedPriest) {
                        UsurpedPriest usurpedPriest = (UsurpedPriest) doppelganger.getActivePower();
                        int code = usurpedPriest.perceive(markerManager, targetPlayer, gameManager.getEpisode());
                        if (code == 1) {
                            player.sendMessage(MessageUtil.error("Vous avez déjà perçu une aura cet épisode."));
                        } else if (code == 2) {
                            player.sendMessage(MessageUtil.error("Vous ne pouvez pas percevoir deux épisodes de suite le même joueur."));
                        }
                        return;
                    }
                }

                if (!(localPlayer.getRole() instanceof PriestRole)) {
                    return;
                }

                PriestRole priest = (PriestRole) localPlayer.getRole();
                int code = priest.perceive(markerManager, targetPlayer, gameManager.getEpisode());
                if (code == 1) {
                    player.sendMessage(MessageUtil.error("Vous avez déjà perçu une aura cet épisode."));
                } else if (code == 2) {
                    player.sendMessage(MessageUtil.error("Vous ne pouvez pas percevoir deux épisodes de suite le même joueur."));
                }
                return;
            }

            case "veiller": {
                if (!(sender instanceof Player)) {
                    sender.sendMessage(MessageUtil.error("Cette commande doit être exécutée par un joueur."));
                    return;
                }

                Player player = (Player) sender;


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
                VampireUHCPlayer targetPlayer = playerManager.get(target.getUniqueId());

                if (targetPlayer == null) {
                    player.sendMessage(MessageUtil.error("Ce joueur n'est pas en partie."));
                    return;
                }

                int current_episode = gameManager.getEpisode();

                if (localPlayer == null || localPlayer.getRole() == null) {
                    return;
                }

                // Pouvoir copié : le Sosie veille comme le Veilleur.
                if (localPlayer.getRole() instanceof DoppelgangerRole) {
                    DoppelgangerRole doppelganger = (DoppelgangerRole) localPlayer.getRole();
                    if (doppelganger.getActivePower() instanceof UsurpedWatchman) {
                        ((UsurpedWatchman) doppelganger.getActivePower()).watchPlayer(targetPlayer, markerManager, current_episode);
                    }
                    return;
                }

                if (!(localPlayer.getRole() instanceof WatchmanRole)) {
                    return;
                }

                WatchmanRole watchman = (WatchmanRole) localPlayer.getRole();

                watchman.watchPlayer(targetPlayer, markerManager, current_episode);
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
                if (localSwitch == null || localSwitch.getRole() == null) {
                    return;
                }

                // Pouvoir copié : le switch du Sosie est imparfait (un seul marqueur
                // aléatoire de chaque joueur, pas l'ensemble).
                DoppelgangerRole doppelganger = localSwitch.getRole() instanceof DoppelgangerRole
                        ? (DoppelgangerRole) localSwitch.getRole()
                        : null;
                UsurpedGremlin usurpedGremlin = doppelganger != null
                        && doppelganger.getActivePower() instanceof UsurpedGremlin
                        ? (UsurpedGremlin) doppelganger.getActivePower()
                        : null;
                GremlinRole gremlin = usurpedGremlin == null && localSwitch.getRole() instanceof GremlinRole
                        ? (GremlinRole) localSwitch.getRole()
                        : null;
                if (usurpedGremlin == null && gremlin == null) {
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

                boolean switched = usurpedGremlin != null
                        ? usurpedGremlin.switchMarkers(markerManager, t1, t2, gameManager.getEpisode())
                        : gremlin.SwitchMarkers(markerManager, t1, t2, gameManager.getEpisode());
                if (switched) {
                    player.sendMessage(MessageUtil.successTwoTargets("Marques échangées entre", t1.getLastKnownName(), t2.getLastKnownName()));

                    // Si une marque Amour a changé de propriétaire, le Cupidon doit être prévenu.
                    for (VampireUHCPlayer p : playerManager.getAll()) {
                        if (p.getRole() instanceof CupidonRole) {
                    CupidonRole cupidon = (CupidonRole) p.getRole();
                            cupidon.notifyIfLoversMoved(markerManager);
                        }
                        // Le Sosie ayant copié un Cupidon est aussi prévenu
                        // (mais ne connaît jamais le couple initial).
                        if (p.getRole() instanceof DoppelgangerRole) {
                            DoppelgangerRole usurpedCupidonHolder = (DoppelgangerRole) p.getRole();
                            if (usurpedCupidonHolder.getActivePower() instanceof UsurpedCupidon) {
                                ((UsurpedCupidon) usurpedCupidonHolder.getActivePower()).notifyIfLoversMoved(markerManager);
                            }
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
                if (localPlayer == null || localPlayer.getRole() == null) {
                    return;
                }

                boolean activated;
                if (localPlayer.getRole() instanceof DoppelgangerRole) {
                    DoppelgangerRole doppelganger = (DoppelgangerRole) localPlayer.getRole();
                    if (!(doppelganger.getActivePower() instanceof UsurpedGremlin)) {
                        return;
                    }
                    activated = ((UsurpedGremlin) doppelganger.getActivePower()).activateDrain(gameManager.getEpisode());
                } else {
                    if (!(localPlayer.getRole() instanceof GremlinRole)) {
                        return;
                    }
                    activated = ((GremlinRole) localPlayer.getRole()).activateDrain(gameManager.getEpisode());
                }

                if (activated) {
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
                if (localPlayer == null || localPlayer.getRole() == null) {
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

                // Pouvoir copié : une Peseuse usurpée pèse exactement comme l'originale,
                // avec son propre pool d'usages.
                if (localPlayer.getRole() instanceof DoppelgangerRole) {
                    DoppelgangerRole doppelganger = (DoppelgangerRole) localPlayer.getRole();
                    if (doppelganger.getActivePower() instanceof UsurpedSoulweigher) {
                        if (!((UsurpedSoulweigher) doppelganger.getActivePower())
                                .weightAura(markerManager, t1, t2, gameManager.getEpisode())) {
                            MessageUtil.sendActionBar(player, "<red>Erreur interne.");
                        }
                        return;
                    }
                }

                if (!(localPlayer.getRole() instanceof SoulweigherRole)) {
                    return;
                }
                SoulweigherRole soulweigherRole = (SoulweigherRole) localPlayer.getRole();

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
            sender.sendMessage(MessageUtil.error("Usage: /vuhc admin <start [sec]|stop|reset|status|roster <list|add|remove|clear|fill>>"));
            return;
        }

        switch (args[0].toLowerCase()) {
            case "start":
                String rosterError = gameManager.preStartValidation();
                if (rosterError != null) {
                    sender.sendMessage(MessageUtil.error(rosterError));
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
                handleRoster(sender, args);
                return;

            default:
                sender.sendMessage(MessageUtil.error("Sous-commande inconnue."));
                sender.sendMessage(MessageUtil.error("Usage: /vuhc admin <start [sec]|stop|reset|status|roster <list|add|remove|clear|fill>>"));
        }
    }

    // Sélection des joueurs pour la partie à venir (composition config.yml).
    private void handleRoster(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(MessageUtil.error("Usage: /vuhc admin roster <list|add <pseudo>|remove <pseudo>|clear|fill>"));
            return;
        }
        if (gameManager.isGameStarted()) {
            sender.sendMessage(MessageUtil.error("La partie a déjà commencé, le roster est verrouillé."));
            return;
        }

        switch (args[1].toLowerCase()) {
            case "list": {
                int required = plugin.getConfigManager().getComposition().getRequiredPlayers();
                sender.sendMessage(MessageUtil.info("Roster : " + gameManager.getRosterSize() + "/" + required + " joueurs"));
                if (gameManager.getRoster().isEmpty()) {
                    sender.sendMessage(MessageUtil.warn("Aucun joueur sélectionné."));
                    return;
                }
                for (Map.Entry<UUID, String> entry : gameManager.getRoster().entrySet()) {
                    Player online = Bukkit.getPlayer(entry.getKey());
                    String state = online != null ? "<green>en ligne</green>" : "<red>hors ligne</red>";
                    sender.sendMessage(MessageUtil.info("- <white>" + entry.getValue() + " <gray>(" + state + ")</gray>"));
                }
                return;
            }
            case "add":
                if (args.length < 3) {
                    sender.sendMessage(MessageUtil.error("Usage: /vuhc admin roster add <pseudo>"));
                    return;
                }
                addToRoster(sender, args[2]);
                return;
            case "remove":
                if (args.length < 3) {
                    sender.sendMessage(MessageUtil.error("Usage: /vuhc admin roster remove <pseudo>"));
                    return;
                }
                removeFromRoster(sender, args[2]);
                return;
            case "clear":
                gameManager.clearRoster();
                sender.sendMessage(MessageUtil.success("Roster vidé."));
                return;
            case "fill": {
                int added = gameManager.fillRoster();
                int required = plugin.getConfigManager().getComposition().getRequiredPlayers();
                if (added == 0) {
                    if (gameManager.getRosterSize() >= required) {
                        sender.sendMessage(MessageUtil.error("Roster déjà complet (" + required + " joueurs)."));
                    } else {
                        sender.sendMessage(MessageUtil.error("Aucun joueur en ligne supplémentaire à sélectionner (roster " + gameManager.getRosterSize() + "/" + required + ")."));
                    }
                } else {
                    sender.sendMessage(MessageUtil.success(added + " joueur(s) ajouté(s) au roster (" + gameManager.getRosterSize() + "/" + required + ")."));
                }
                return;
            }
            default:
                sender.sendMessage(MessageUtil.error("Usage: /vuhc admin roster <list|add <pseudo>|remove <pseudo>|clear|fill>"));
        }
    }

    private void addToRoster(CommandSender sender, String name) {
        Player target = Bukkit.getPlayerExact(name);
        if (target == null) {
            sender.sendMessage(MessageUtil.error("Joueur introuvable ou hors ligne : " + name));
            return;
        }
        if (gameManager.addToRoster(target)) {
            sender.sendMessage(MessageUtil.success(target.getName() + " ajouté au roster."));
        } else {
            sender.sendMessage(MessageUtil.error(target.getName() + " est déjà dans le roster."));
        }
    }

    private void removeFromRoster(CommandSender sender, String name) {
        Player target = Bukkit.getPlayerExact(name);
        if (target == null) {
            sender.sendMessage(MessageUtil.error("Joueur introuvable ou hors ligne : " + name));
            return;
        }
        if (gameManager.removeFromRoster(target)) {
            sender.sendMessage(MessageUtil.success(target.getName() + " retiré du roster."));
        } else {
            sender.sendMessage(MessageUtil.error(target.getName() + " n'est pas dans le roster."));
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

        if (sub.equals("admin") && args.length >= 3 && args[1].equalsIgnoreCase("roster")
                && hasAccess(sender, "admin")) {
            if (args.length == 3) {
                return filterStartsWith(ROSTER_SUBCOMMANDS, args[2]);
            }
            if (args.length == 4 && (args[2].equalsIgnoreCase("add") || args[2].equalsIgnoreCase("remove"))) {
                return playersStartingWith(args[3]);
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
                return hasRole(sender, WeaverRole.class)
                        || hasActiveUsurpedPower(sender, UsurpedWeaver.class);
            case "baliser":
                return hasRole(sender, CartographerRole.class)
                        || hasActiveUsurpedPower(sender, UsurpedCartographer.class);
            case "role":
                return true;
            case "veiller":
                return hasRole(sender, WatchmanRole.class)
                        || hasActiveUsurpedPower(sender, UsurpedWatchman.class);
            case "exorciser":
                return hasRole(sender, ExorcistRole.class)
                        || hasActiveUsurpedPower(sender, UsurpedExorcist.class);
            case "maudire":
                return hasRole(sender, BabaYagaRole.class)
                        || hasActiveUsurpedPower(sender, UsurpedBabaYaga.class);
            case "ressusciter":
                return hasRole(sender, BabaYagaRole.class);
            case "exhumer":
                return hasRole(sender, GravediggerRole.class)
                        || hasActiveUsurpedPower(sender, UsurpedGravedigger.class);
            case "ensabler":
                return hasRole(sender, SandMerchantRole.class)
                        || hasActiveUsurpedPower(sender, UsurpedSandMerchant.class);
            case "spectate":
                return sender instanceof Player && ((Player) sender).getGameMode() == GameMode.SPECTATOR;
            case "marquer":
                return hasRole(sender, MasterRole.class)
                        || hasActiveUsurpedPower(sender, UsurpedMaster.class);
            case "trancher":
                return hasRole(sender, MasterRole.class);
            case "proteger":
                return hasRole(sender, SaviorRole.class)
                        || hasActiveUsurpedPower(sender, UsurpedSavior.class);
            case "voter": {
                VampireUHCPlayer vp = playerOf(sender);
                return vp != null
                        && (vp.getRole() instanceof VampireMinion || vp.getRole() instanceof ComteRole)
                        && vp.canVoteVampireMark()
                        // Le Sosie ayant usurpé un Sbire ou un Comte vote aussi
                        // (sans jamais connaître le résultat).
                        || hasActiveUsurpedPower(sender, UsurpedVampire.class);
            }
            case "switch":
            case "drain":
                return hasRole(sender, GremlinRole.class)
                        || hasActiveUsurpedPower(sender, UsurpedGremlin.class);
            case "lier":
                return hasRole(sender, CupidonRole.class);
            case "peser":
                return hasRole(sender, SoulweigherRole.class)
                        || hasActiveUsurpedPower(sender, UsurpedSoulweigher.class);
            case "percevoir":
                return hasRole(sender, PriestRole.class)
                        || hasActiveUsurpedPower(sender, UsurpedPriest.class);
            case "usurper":
                return hasRole(sender, DoppelgangerRole.class);
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

    // Accès via un pouvoir copié par le Doppelganger (ex. percevoir/peser usurpés).
    private boolean hasActiveUsurpedPower(CommandSender sender, Class<? extends UsurpedPower> powerClass) {
        VampireUHCPlayer vp = playerOf(sender);
        if (vp == null || !vp.isAlive() || !(vp.getRole() instanceof DoppelgangerRole)) {
            return false;
        }
        DoppelgangerRole doppelganger = (DoppelgangerRole) vp.getRole();
        UsurpedPower power = doppelganger.getActivePower();
        return power != null && powerClass.isInstance(power);
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
            sender.sendMessage(MessageUtil.helpEntry("/vuhc admin", "<start|stop|reset|status>"));
            sender.sendMessage(MessageUtil.helpSection("Dev"));
            sender.sendMessage(MessageUtil.helpEntry("/vuhc dev", "<setTime|setRole|who|aura>"));
        }
    }
}
