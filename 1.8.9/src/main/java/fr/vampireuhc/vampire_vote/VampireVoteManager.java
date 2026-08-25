package fr.vampireuhc.vampire_vote;
import fr.vampireuhc.VampireUHC;
import fr.vampireuhc.config.MessageUtil;
import fr.vampireuhc.markers.MarkerManager;
import fr.vampireuhc.markers.MarkerType;
import fr.vampireuhc.player.Camp;
import fr.vampireuhc.player.VampireUHCPlayer;
import fr.vampireuhc.roles.VampireMinion;


import java.util.*;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.TextComponent;

public class VampireVoteManager {
    private final VampireUHC plugin;
    private final Map<UUID, Integer> voteByPlayer = new HashMap<>();
    private final Set<UUID> voters = new HashSet<>();
    private VoteResult.Tie pendingTie;
    private boolean open = false;

    public VampireVoteManager(VampireUHC plugin) {
        this.plugin = plugin;
    }

    public void openVote() {
        voteByPlayer.clear();
        voters.clear();
        // Un tie en attente SURVIT à l'ouverture du cycle suivant : l'écraser
        // abandonnerait la marque sans prévenir personne. On rappelle le Maître.
        if (pendingTie != null) {
            notifyMasterOfTie(pendingTie);
        }
        open = true;
        broadcastToVampires(MessageUtil.serialize("<dark_purple>Le vote pour la marque vampire est ouvert ! <gray>/vuhc voter <joueur></gray></dark_purple>"));
    }

    public void closeAndResolve() {
        if (!open) {
            return;
        }
        open = false;

        if (voteByPlayer.isEmpty()) {
            return;
        }

        VoteResult result = resolveVote();
        if (result instanceof VoteResult.Winner) {
            // Un vainqueur supersède un éventuel tie resté pendant.
            VoteResult.Winner w = (VoteResult.Winner) result;
            pendingTie = null;
            applyVampireMark(w.playerId());
            sendVoteFeedback(w.playerId());
        } else if (result instanceof VoteResult.Tie) {
            VoteResult.Tie t = (VoteResult.Tie) result;
            masterResolves(t);
            sendTieFeedback(t);
        }
        voteByPlayer.clear();
        voters.clear();
    }

    public boolean isVoteOpen() {
        return open;
    }

    public boolean addVote(UUID voter, UUID target) {
        if (!open || !voters.add(voter)) {
            return false;
        }
        voteByPlayer.merge(target, 1, Integer::sum);
        return true;
    }

    public void clearVotes() {
        voteByPlayer.clear();
        voters.clear();
    }

    public VoteResult resolveVote() {
        return voteByPlayer.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(max -> {
                    List<UUID> tied = voteByPlayer.entrySet().stream()
                            .filter(e -> e.getValue().equals(max.getValue()))
                            .map(Map.Entry::getKey)
                            .collect(java.util.stream.Collectors.toList());
                    return tied.size() == 1
                            ? new VoteResult.Winner(tied.get(0))
                            : new VoteResult.Tie(tied);
                })
                .orElseThrow(() -> new IllegalStateException("Aucun vote enregistré."));
    }

    // Applique la marque vampire sur le gagnant du vote (la Salvation peut la bloquer silencieusement).
    // La cible est revérifiée au moment de la résolution : elle peut être morte ou infectée
    // entre l'ouverture du vote et la fermeture.
    private void applyVampireMark(UUID targetId) {
        VampireUHCPlayer target = plugin.getPlayerManager().get(targetId);
        if (target == null || !target.isAlive() || target.getCamp() == Camp.VAMPIRE) {
            return;
        }
        plugin.getMarkerManager().tryApplyMark(targetId, MarkerType.MARQUE_VAMPIRE, null);
    }

    // Les vampires ne reçoivent que le résultat de leur vote, même si la marque a été bloquée.
    private void sendVoteFeedback(UUID targetId) {
        String targetName = nameOf(targetId);
        for (UUID voterId : voters) {
            Player p = Bukkit.getPlayer(voterId);
            if (p != null) {
                p.sendMessage(MessageUtil.serialize("<dark_purple>Votre cible est <gold>" + targetName + "</gold> !</dark_purple>"));
            }
        }
    }

    private void sendTieFeedback(VoteResult.Tie tie) {
        for (UUID voterId : voters) {
            Player p = Bukkit.getPlayer(voterId);
            if (p != null) {
                p.sendMessage(MessageUtil.serialize("<red>Égalité entre plusieurs joueurs ! Le Maître va trancher...</red>"));
            }
        }
    }

    // Le Maitre tranche une égalité
    public void masterResolves(VoteResult.Tie tie) {
        this.pendingTie = tie;
        notifyMasterOfTie(tie);
    }

    // Envoie au Maître (s'il est en ligne) la liste cliquable des joueurs à égalité.
    private void notifyMasterOfTie(VoteResult.Tie tie) {
        UUID masterId = findMaster();
        if (masterId == null) {
            plugin.getLogger().warning("Impossible de trouver le Maître pour trancher l'égalité.");
            return;
        }

        Player master = Bukkit.getPlayer(masterId);
        if (master == null) {
            plugin.getLogger().warning("Le Maître est hors ligne, impossible de trancher l'égalité.");
            return;
        }

        master.sendMessage(MessageUtil.serialize("<dark_purple>Égalité détectée ! Choisissez le joueur à marquer en cliquant sur son nom :</dark_purple>"));

        for (UUID id : tie.tiedPlayers()) {
            Player bukkit = Bukkit.getPlayer(id);
            String name = nameOf(id);

            TextComponent line = new TextComponent(" » ");
            line.setColor(net.md_5.bungee.api.ChatColor.GRAY);
            TextComponent nameComp = new TextComponent(name);
            nameComp.setColor(net.md_5.bungee.api.ChatColor.YELLOW);
            nameComp.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND,
                    "/vuhc trancher " + (bukkit != null ? bukkit.getName() : name)));
            line.addExtra(nameComp);
            master.spigot().sendMessage(line);
        }
    }

    // Appelé quand le Maître tranche l'égalité via /vuhc trancher.
    public void resolveTieWith(UUID chosenId) {
        applyVampireMark(chosenId);
        sendVoteFeedback(chosenId);
        pendingTie = null;
    }

    public VoteResult.Tie getPendingTie() {
        return pendingTie;
    }

    public void clearPendingTie() {
        this.pendingTie = null;
    }

    // Porteurs actuels d'une marque vampire, dérivés des marqueurs.
    private Set<UUID> markedHolders() {
        MarkerManager markers = plugin.getMarkerManager();
        Set<UUID> holders = new HashSet<>();
        for (UUID id : markers.getAllPlayers()) {
            if (markers.hasMarker(id, MarkerType.MARQUE_VAMPIRE)) {
                holders.add(id);
            }
        }
        return holders;
    }

    // Nombre de joueurs marqués, recompté à la demande depuis les marqueurs
    // (aucun compteur incrémental à désynchroniser).
    public int getMarkedPlayerCount() {
        return markedHolders().size();
    }

    // Remet entièrement le système de vote à zéro (nouvelle partie).
    public void reset() {
        voteByPlayer.clear();
        voters.clear();
        pendingTie = null;
        open = false;
    }

    // Restaure l'état des votes après un redémarrage du serveur (voters inclus,
    // sinon les Sbires pourraient revoter dans le même round après un restart).
    public void restore(boolean open, Map<UUID, Integer> votes, Set<UUID> voters, VoteResult.Tie pendingTie) {
        this.open = open;
        this.voteByPlayer.clear();
        this.voteByPlayer.putAll(votes);
        this.voters.clear();
        this.voters.addAll(voters);
        this.pendingTie = pendingTie;
    }

    public int getVotesFor(UUID target) {
        return voteByPlayer.getOrDefault(target, 0);
    }

    public Map<UUID, Integer> getVotesCopy() {
        return new HashMap<>(voteByPlayer);
    }

    public Set<UUID> getVotersCopy() {
        return new HashSet<>(voters);
    }

    public Set<UUID> getMarkedPlayersCopy() {
        return markedHolders();
    }

    private void broadcastToVampires(String message) {
        for (VampireUHCPlayer player : plugin.getPlayerManager().getAll()) {
            if (player.getRole() instanceof VampireMinion && player.isAlive()) {
                Player p = Bukkit.getPlayer(player.getUuid());
                if (p != null) {
                    p.sendMessage(message);
                }
            }
        }
    }

    private String nameOf(UUID id) {
        Player bukkit = Bukkit.getPlayer(id);
        if (bukkit != null) {
            return bukkit.getName();
        }
        VampireUHCPlayer p = plugin.getPlayerManager().get(id);
        return p != null ? p.getLastKnownName() : id.toString();
    }

    private UUID findMaster() {
        for (VampireUHCPlayer player : plugin.getPlayerManager().getAll()) {
            if (player.getRole() instanceof fr.vampireuhc.roles.MasterRole) {
                return player.getUuid();
            }
        }
        return null;
    }
}
