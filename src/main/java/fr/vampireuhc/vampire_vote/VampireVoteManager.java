package fr.vampireuhc.vampire_vote;
import fr.vampireuhc.VampireUHC;
import fr.vampireuhc.markers.MarkerType;
import fr.vampireuhc.player.VampireUHCPlayer;
import fr.vampireuhc.roles.VampireMinion;


import java.util.*;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.TextColor;

public class VampireVoteManager {
    private final VampireUHC plugin;
    private final Map<UUID, Integer> voteByPlayer = new HashMap<>();
    private final Set<UUID> voters = new HashSet<>();
    private final Set<UUID> markedPlayers = new HashSet<>();
    private VoteResult.Tie pendingTie;
    private boolean open = false;
    private int markedPlayerCount = 0;

    public VampireVoteManager(VampireUHC plugin) {
        this.plugin = plugin;
    }

    public void openVote() {
        voteByPlayer.clear();
        voters.clear();
        pendingTie = null;
        open = true;
        broadcastToVampires(ChatColor.DARK_PURPLE + "Le vote pour la marque vampire est ouvert ! /vuhc voter <joueur>");
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
        switch (result) {
            case VoteResult.Winner w -> {
                applyVampireMark(w.playerId());
                sendVoteFeedback(w.playerId());
            }
            case VoteResult.Tie t -> {
                masterResolves(t);
                sendTieFeedback(t);
            }
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
                            .toList();
                    return tied.size() == 1
                            ? new VoteResult.Winner(tied.getFirst())
                            : new VoteResult.Tie(tied);
                })
                .orElseThrow(() -> new IllegalStateException("Aucun vote enregistré."));
    }

    // Applique la marque vampire sur le gagnant du vote (la Salvation peut la bloquer silencieusement).
    private void applyVampireMark(UUID targetId) {
        boolean applied = plugin.getMarkerManager().tryApplyMark(targetId, MarkerType.MARQUE_VAMPIRE, null);
        if (applied && markedPlayers.add(targetId)) {
            markedPlayerCount++;
        }
    }

    // Les vampires ne reçoivent que le résultat de leur vote, même si la marque a été bloquée.
    private void sendVoteFeedback(UUID targetId) {
        String targetName = nameOf(targetId);
        for (UUID voterId : voters) {
            Player p = Bukkit.getPlayer(voterId);
            if (p != null) {
                p.sendMessage(ChatColor.DARK_PURPLE + "Votre cible est " + ChatColor.GOLD + targetName + ChatColor.DARK_PURPLE + " !");
            }
        }
    }

    private void sendTieFeedback(VoteResult.Tie tie) {
        for (UUID voterId : voters) {
            Player p = Bukkit.getPlayer(voterId);
            if (p != null) {
                p.sendMessage(ChatColor.RED + "Égalité entre plusieurs joueurs ! Le Maître va trancher...");
            }
        }
    }

    // Le Maitre tranche une égalité
    public void masterResolves(VoteResult.Tie tie) {
        this.pendingTie = tie;

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

        master.sendMessage(ChatColor.DARK_PURPLE + "Égalité détectée ! Choisissez le joueur à marquer en cliquant sur son nom :");

        for (UUID id : tie.tiedPlayers()) {
            Player bukkit = Bukkit.getPlayer(id);
            String name = nameOf(id);

            Component line = Component.text(" » ", TextColor.color(0xAAAAAA))
                    .append(Component.text(name, TextColor.color(0xFFFF55))
                            .clickEvent(ClickEvent.runCommand("/vuhc trancher " + (bukkit != null ? bukkit.getName() : name))));
            master.sendMessage(line);
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

    public int getMarkedPlayerCount() {
        return markedPlayerCount;
    }

    // Remet entièrement le système de vote à zéro (nouvelle partie).
    public void reset() {
        voteByPlayer.clear();
        voters.clear();
        markedPlayers.clear();
        markedPlayerCount = 0;
        pendingTie = null;
        open = false;
    }

    // Restaure l'état des votes après un redémarrage du serveur.
    public void restore(boolean open, Map<UUID, Integer> votes, Set<UUID> markedPlayers, int markedPlayerCount, VoteResult.Tie pendingTie) {
        this.open = open;
        this.voteByPlayer.clear();
        this.voteByPlayer.putAll(votes);
        this.voters.clear();
        this.markedPlayers.clear();
        this.markedPlayers.addAll(markedPlayers);
        this.markedPlayerCount = markedPlayerCount;
        this.pendingTie = pendingTie;
    }

    public int getVotesFor(UUID target) {
        return voteByPlayer.getOrDefault(target, 0);
    }

    public Map<UUID, Integer> getVotesCopy() {
        return new HashMap<>(voteByPlayer);
    }

    public Set<UUID> getMarkedPlayersCopy() {
        return new HashSet<>(markedPlayers);
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
