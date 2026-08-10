package fr.vampireuhc.roles;
import fr.vampireuhc.VampireUHC;
import fr.vampireuhc.config.ConfigManager;
import fr.vampireuhc.player.VampireUHCPlayer;
import fr.vampireuhc.markers.MarkerType;
import fr.vampireuhc.markers.MarkerManager;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import fr.vampireuhc.player.Camp;
import fr.vampireuhc.player.PlayerManager;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public class CupidonRole implements Role {
    private VampireUHCPlayer cupidon;
    private boolean marked;
    private final Set<UUID> loverUuids = new HashSet<>();
    private Set<UUID> lastKnownAmourHolders;

    public CupidonRole(VampireUHCPlayer player) {
        this.cupidon = player;
    }
    
    @Override
    public boolean isVampire() {
        return false;
    }

    @Override
    public String getDescription() {
        ConfigManager config = VampireUHC.getInstance().getConfigManager();
        int heartsLost = config.getAmourHeartsLost();
        int penaltyMinutes = config.getAmourPenaltyDurationSeconds() / 60;
        return "Le Cupidon est un rôle villageois. Au début de la partie, il place une marque Amour sur deux joueurs (sans le leur dire). Les deux joueurs sont liés : si l'un meurt, l'autre perd temporairement "
            + ChatColor.DARK_PURPLE + heartsLost + ChatColor.GRAY
            + " coeurs pendant "
            + ChatColor.DARK_PURPLE + penaltyMinutes + ChatColor.GRAY
            + " minutes et connaît l'identité du tueur. Si une marque Amour change de propriétaire (ex : un Gremlin), vous en êtes informé dans un délai aléatoire.";
    }

    @Override
    public String getName() {
        return "Cupidon";
    }

    @Override
    public Camp getDefaultCamp() {
        return Camp.VILLAGEOIS;
    }

    @Override
    public void onAssign(VampireUHCPlayer vampireUHCPlayer) {
        this.cupidon = vampireUHCPlayer;

        // Le Cupidon place ses marques Amour au début de la partie.
        PlayerManager playerManager = VampireUHC.getInstance().getPlayerManager();
        MarkerManager markerManager = VampireUHC.getInstance().getMarkerManager();

        List<VampireUHCPlayer> candidates = playerManager.getAll().stream()
                .filter(p -> !p.getUuid().equals(cupidon.getUuid()))
                .toList();

        if (candidates.size() < 2) {
            return;
        }

        UUID first = candidates.get(ThreadLocalRandom.current().nextInt(candidates.size())).getUuid();
        UUID second = candidates.stream()
                .filter(p -> !p.getUuid().equals(first))
                .findAny()
                .orElseThrow()
                .getUuid();

        markerManager.addMarker(first, MarkerType.AMOUR, cupidon.getUuid());
        markerManager.addMarker(second, MarkerType.AMOUR, cupidon.getUuid());
        this.marked = true;
        this.loverUuids.add(first);
        this.loverUuids.add(second);
        this.lastKnownAmourHolders = new HashSet<>(loverUuids);

        Player bukkitCupidon = Bukkit.getPlayer(cupidon.getUuid());
        if (bukkitCupidon != null) {
            bukkitCupidon.sendMessage(ChatColor.DARK_PURPLE + "Vous avez marqué les joueurs " + ChatColor.GOLD +
                displayName(first) + ChatColor.DARK_PURPLE + " et " + ChatColor.GOLD + displayName(second));
        }
    }

    private static String displayName(UUID uuid) {
        Player p = Bukkit.getPlayer(uuid);
        VampireUHCPlayer vp = VampireUHC.getInstance().getPlayerManager().get(uuid);
        if (p != null) {
            return p.getName();
        }
        return vp != null ? vp.getLastKnownName() : uuid.toString();
    }

    // Marquer deux joueurs en début de partie (fallback si onAssign n'a pas pu).
    public boolean MarkLovers(MarkerManager manager, VampireUHCPlayer target_1, VampireUHCPlayer target_2) {
        if (cupidon == null || marked == true) {
            return false;
        } 
        var uuid_cupidon = cupidon.getUuid();
        manager.addMarker(target_1.getUuid(), MarkerType.AMOUR, uuid_cupidon);
        manager.addMarker(target_2.getUuid(), MarkerType.AMOUR, uuid_cupidon);
        this.marked = true;
        this.loverUuids.add(target_1.getUuid());
        this.loverUuids.add(target_2.getUuid());
        this.lastKnownAmourHolders = new HashSet<>(loverUuids);

        var bukkitCupidon = Bukkit.getPlayer(uuid_cupidon);
        if (bukkitCupidon != null) {
            bukkitCupidon.sendMessage(ChatColor.DARK_PURPLE + "Vous avez marqué les joueurs " + ChatColor.GOLD + target_1.getLastKnownName() + ChatColor.DARK_PURPLE + " et " + target_2.getLastKnownName());
        }
        return true;
    }

    // Quand l'un des deux amoureux meurt, l'autre perd des coeurs et connaît le tueur.
    public void onLoverDeath(MarkerManager manager, VampireUHCPlayer victim, Player killer) {
        if (cupidon == null) {
            return;
        }

        var victimMarkers = manager.getMarkers(victim.getUuid(), MarkerType.AMOUR);
        if (victimMarkers.isEmpty()) {
            return;
        }

        UUID amourSource = victimMarkers.get(0).getSource();
        VampireUHCPlayer partner = VampireUHC.getInstance().getPlayerManager().getAll().stream()
                .filter(p -> !p.getUuid().equals(victim.getUuid()))
                .filter(p -> manager.getMarkers(p.getUuid(), MarkerType.AMOUR).stream()
                        .anyMatch(m -> java.util.Objects.equals(m.getSource(), amourSource)))
                .findFirst()
                .orElse(null);

        if (partner == null) {
            return;
        }

        Player bukkitPartner = Bukkit.getPlayer(partner.getUuid());
        if (bukkitPartner == null) {
            return;
        }

        ConfigManager config = VampireUHC.getInstance().getConfigManager();
        int heartsLost = config.getAmourHeartsLost();
        int durationSeconds = config.getAmourPenaltyDurationSeconds();

        bukkitPartner.setMaxHealth(Math.max(1, bukkitPartner.getMaxHealth() - heartsLost * 2));
        if (bukkitPartner.getHealth() > bukkitPartner.getMaxHealth()) {
            bukkitPartner.setHealth(bukkitPartner.getMaxHealth());
        }

        String killerName = killer != null ? killer.getName() : "inconnu";
        bukkitPartner.sendMessage(ChatColor.RED + "Votre amoureux est mort ! Vous perdez " + heartsLost + " coeurs pendant " + durationSeconds + " secondes. Son tueur était : " + ChatColor.GOLD + killerName);

        new BukkitRunnable() {
            @Override
            public void run() {
                bukkitPartner.setMaxHealth(bukkitPartner.getMaxHealth() + heartsLost * 2);
            }
        }.runTaskLater(VampireUHC.getInstance(), 20L * durationSeconds);
    }

    // Si une marque Amour change de propriétaire (ex : switch du Gremlin), on prévient le Cupidon.
    public void notifyIfLoversMoved(MarkerManager manager) {
        if (cupidon == null) {
            return;
        }

        Set<UUID> currentHolders = new HashSet<>();
        for (UUID id : manager.getAllPlayers()) {
            if (manager.hasMarker(id, MarkerType.AMOUR)) {
                currentHolders.add(id);
            }
        }

        if (lastKnownAmourHolders != null && !currentHolders.equals(lastKnownAmourHolders)) {
            ConfigManager config = VampireUHC.getInstance().getConfigManager();
            int min = config.getCupidonNotifyMinSeconds();
            int max = Math.max(min, config.getCupidonNotifyMaxSeconds());
            long delay = ThreadLocalRandom.current().nextInt(min, max + 1) * 20L;

            new BukkitRunnable() {
                @Override
                public void run() {
                    Player bukkitCupidon = Bukkit.getPlayer(cupidon.getUuid());
                    if (bukkitCupidon != null) {
                        bukkitCupidon.sendMessage(ChatColor.DARK_PURPLE + "Attention ! L'un de vos marqueurs Amour a changé de propriétaire.");
                    }
                }
            }.runTaskLater(VampireUHC.getInstance(), delay);
        }

        lastKnownAmourHolders = currentHolders;
    }
}
