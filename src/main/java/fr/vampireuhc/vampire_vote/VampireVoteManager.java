package fr.vampireuhc.vampire_vote;
import fr.vampireuhc.VampireUHC;
import fr.vampireuhc.roles.MasterRole;
import fr.vampireuhc.player.VampireUHCPlayer;


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
    private VoteResult.Tie pendingTie;

    public VampireVoteManager(VampireUHC plugin) {
        this.plugin = plugin;
    }

    public void addVote(UUID target) {
        voteByPlayer.merge(target, 1, Integer::sum);
    }

    public void clearVotes() {
        voteByPlayer.clear();
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
            VampireUHCPlayer p = plugin.getPlayerManager().get(id);
            String name = p != null ? p.getLastKnownName() : id.toString();

            Component line = Component.text(" » ", TextColor.color(0xAAAAAA))
                    .append(Component.text(name, TextColor.color(0xFFFF55))
                            .clickEvent(ClickEvent.runCommand("/vuhc trancher " + name)));
            master.sendMessage(line);
        }
    }

    public VoteResult.Tie getPendingTie() {
        return pendingTie;
    }

    public void clearPendingTie() {
        this.pendingTie = null;
    }

    private UUID findMaster() {
        for (VampireUHCPlayer player : plugin.getPlayerManager().getAll()) {
            if (player.getRole() instanceof MasterRole) {
                return player.getUuid();
            }
        }
        return null;
    }
}
