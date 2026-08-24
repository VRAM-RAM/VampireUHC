package fr.vampireuhc.roles;
import fr.vampireuhc.VampireUHC;
import fr.vampireuhc.config.ConfigManager;
import fr.vampireuhc.config.MessageUtil;
import fr.vampireuhc.game.RoleBuffManager;
import fr.vampireuhc.player.VampireUHCPlayer;
import fr.vampireuhc.markers.Marker;
import fr.vampireuhc.markers.MarkerType;
import fr.vampireuhc.markers.MarkerManager;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import fr.vampireuhc.player.Camp;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public class CupidonRole implements Role {
    private VampireUHCPlayer cupidon;
    private boolean marked;
    private final Set<UUID> loverUuids = new HashSet<>();
    private Set<UUID> lastKnownAmourHolders;
    private BukkitTask linkFallbackTask;

    // Restaurations de cœurs en attente, par joueur (annulées en fin de partie).
    private final Map<UUID, BukkitTask> pendingRestores = new HashMap<>();

    // Notification « marque déplacée » à délai aléatoire (trackée pour être
    // annulée : sinon elle peut tirer après un stop/reset, message stale).
    private BukkitTask movedNotifyTask;

    public CupidonRole(VampireUHCPlayer player) {
        this.cupidon = player;
    }
    
    @Override
    public boolean isVampire() {
        return false;
    }

    @Override
    public Component getDescription() {
        ConfigManager config = VampireUHC.getInstance().getConfigManager();
        int heartsLost = config.getAmourHeartsLost();
        int penaltyMinutes = config.getAmourPenaltyDurationSeconds() / 60;
        MiniMessage mm = MiniMessage.miniMessage();
        return mm.deserialize(
            "<gray>Vous liez deux joueurs par un lien d'amour invisible.</gray>\n\n"
            + "<dark_purple>▸</dark_purple> <gray>Au début de la partie, liez deux joueurs : <gold>/vuhc lier <j1> <j2></gold></gray>\n"
            + "<dark_purple>▸</dark_purple> <gray>Les joueurs liés ne le savent pas.</gray>\n\n"
            + "<bold><dark_purple>Effet du lien :</dark_purple></bold>\n"
            + "  <gray>• Si l'un meurt, l'autre perd <red>" + heartsLost + " coeurs</red> pendant <yellow>" + penaltyMinutes + " minutes</yellow>.</gray>\n"
            + "  <gray>• Il connaît l'identité du tueur.</gray>\n\n"
            + "<bold><dark_purple>Surveillance :</dark_purple></bold>\n"
            + "  <gray>Si une marque Amour change de propriétaire (ex : Gremlin), vous l'apprenez dans un délai aléatoire, et connaissez l'identité du nouveau propriétaire — mais pas celle de celui qui l'a perdue.</gray>"
        );
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

        MarkerManager markerManager = VampireUHC.getInstance().getMarkerManager();

        // Cas restauration : le cupidon a déjà posé ses marques Amour, on re-dérive son état.
        Set<UUID> existing = holdersOfAmourFrom(markerManager);
        if (!existing.isEmpty()) {
            this.marked = true;
            this.loverUuids.addAll(existing);
            this.lastKnownAmourHolders = new HashSet<>(existing);
            return;
        }

        // Partie neuve : le cupidon choisit lui-même les amoureux.
        Player bukkitCupidon = Bukkit.getPlayer(cupidon.getUuid());
        if (bukkitCupidon != null) {
            bukkitCupidon.sendMessage(MessageUtil.warn("Choisissez vos deux amoureux : /vuhc lier <Joueur1> <Joueur2>"));
        }

        // Fallback : auto-lien aléatoire après une minute s'il n'a pas choisi.
        linkFallbackTask = Bukkit.getScheduler().runTaskLater(VampireUHC.getInstance(), () -> {
            if (marked) {
                return;
            }
            autoLinkRandom(markerManager);
        }, 20L * 60);
    }

    @Override
    public void onGameEnd() {
        if (linkFallbackTask != null) {
            linkFallbackTask.cancel();
            linkFallbackTask = null;
        }
        if (movedNotifyTask != null) {
            movedNotifyTask.cancel();
            movedNotifyTask = null;
        }
        for (BukkitTask task : pendingRestores.values()) {
            task.cancel();
        }
        pendingRestores.clear();
        VampireUHC.getInstance().getBuffManager().clearHeartPenalties();
    }

    // Joueurs portant une marque Amour posée par le cupidon.
    private Set<UUID> holdersOfAmourFrom(MarkerManager manager) {
        Set<UUID> holders = new HashSet<>();
        for (UUID id : manager.getAllPlayers()) {
            if (manager.hasMarker(id, MarkerType.AMOUR)) {
                List<Marker> markers = manager.getMarkers(id, MarkerType.AMOUR);
                if (!markers.isEmpty() && markers.get(0).getSource().equals(cupidon.getUuid())) {
                    holders.add(id);
                }
            }
        }
        return holders;
    }

    // Auto-lien aléatoire si le cupidon n'a pas choisi à temps.
    private void autoLinkRandom(MarkerManager manager) {
        List<VampireUHCPlayer> candidates = VampireUHC.getInstance().getPlayerManager().getAll().stream()
                .filter(p -> !p.getUuid().equals(cupidon.getUuid()))
                .filter(VampireUHCPlayer::isAlive) // jamais de lien avec un mort/spectateur
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

        applyLoveMarks(manager, first, second);

        Player bukkitCupidon = Bukkit.getPlayer(cupidon.getUuid());
        if (bukkitCupidon != null) {
            bukkitCupidon.sendMessage(MessageUtil.warn("Vous n'avez pas choisi vos amoureux : <gold>" +
                displayName(first) + "</gold> et <gold>" + displayName(second) +
                "</gold> ont été liés au hasard."));
        }
    }

    // Pose les marques Amour et mémorise l'état (partagé par /vuhc lier et l'auto-lien).
    private void applyLoveMarks(MarkerManager manager, UUID first, UUID second) {
        manager.addMarker(first, MarkerType.AMOUR, cupidon.getUuid());
        manager.addMarker(second, MarkerType.AMOUR, cupidon.getUuid());
        this.marked = true;
        this.loverUuids.add(first);
        this.loverUuids.add(second);
        this.lastKnownAmourHolders = new HashSet<>(loverUuids);
        if (linkFallbackTask != null) {
            linkFallbackTask.cancel();
            linkFallbackTask = null;
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

    // Marquer deux joueurs en début de partie (via /vuhc lier).
    public boolean MarkLovers(MarkerManager manager, VampireUHCPlayer target_1, VampireUHCPlayer target_2) {
        if (cupidon == null || marked) {
            return false;
        }
        applyLoveMarks(manager, target_1.getUuid(), target_2.getUuid());

        var bukkitCupidon = Bukkit.getPlayer(cupidon.getUuid());
        if (bukkitCupidon != null) {
            bukkitCupidon.sendMessage(MessageUtil.successTwoTargets("Joueurs marqués :", target_1.getLastKnownName(), target_2.getLastKnownName()));
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

        // La pénalité s'applique par UUID, partenaire en ligne ou pas (le buff
        // manager retombe sur le handle courant à la reconnexion).
        ConfigManager config = VampireUHC.getInstance().getConfigManager();
        int heartsLost = config.getAmourHeartsLost();
        int durationSeconds = config.getAmourPenaltyDurationSeconds();

        // La perte de cœurs est déléguée à l'autorité centrale du max health
        // (RoleBuffManager), qui combine base + aura Paladin − pénalités actives.
        RoleBuffManager buffs = VampireUHC.getInstance().getBuffManager();
        buffs.registerHeartPenalty(partner.getUuid(), heartsLost);

        String killerName = killer != null ? killer.getName() : "inconnu";
        MiniMessage mm = MiniMessage.miniMessage();
        Player bukkitPartner = Bukkit.getPlayer(partner.getUuid());
        if (bukkitPartner != null) {
            bukkitPartner.sendMessage(mm.deserialize(
                "<red>Votre amoureux est mort ! Vous perdez <gold>" + heartsLost + " coeurs</gold> pendant <gold>" + durationSeconds + " secondes</gold>. Son tueur était : <gold>" + killerName + "</gold></red>"
            ));
        }

        UUID partnerId = partner.getUuid();
        BukkitTask previous = pendingRestores.remove(partnerId);
        if (previous != null) {
            previous.cancel();
        }
        BukkitTask restoreTask = new BukkitRunnable() {
            @Override
            public void run() {
                pendingRestores.remove(partnerId);
                buffs.clearHeartPenalty(partnerId);
            }
        }.runTaskLater(VampireUHC.getInstance(), 20L * durationSeconds);
        pendingRestores.put(partnerId, restoreTask);
    }

    // Si une marque Amour change de propriétaire (ex : switch du Gremlin), le Cupidon
    // apprend qui la détient désormais, mais pas qui l'a perdue.
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

        if (lastKnownAmourHolders == null) {
            lastKnownAmourHolders = new HashSet<>(currentHolders);
            return;
        }

        Set<UUID> newHolders = new HashSet<>(currentHolders);
        newHolders.removeAll(lastKnownAmourHolders);

        if (newHolders.isEmpty()) {
            return;
        }

        lastKnownAmourHolders.addAll(newHolders);

        List<String> names = new ArrayList<>();
        for (UUID id : newHolders) {
            names.add(displayName(id));
        }

        ConfigManager config = VampireUHC.getInstance().getConfigManager();
        int min = config.getCupidonNotifyMinSeconds();
        int max = Math.max(min, config.getCupidonNotifyMaxSeconds());
        long delay = ThreadLocalRandom.current().nextInt(min, max + 1) * 20L;

        // Un seul rappel à la fois : un nouveau switch remplace le précédent.
        if (movedNotifyTask != null) {
            movedNotifyTask.cancel();
        }
        movedNotifyTask = new BukkitRunnable() {
            @Override
            public void run() {
                movedNotifyTask = null;
                Player bukkitCupidon = Bukkit.getPlayer(cupidon.getUuid());
                if (bukkitCupidon != null) {
                    bukkitCupidon.sendMessage(MessageUtil.warn("Une marque Amour a changé de propriétaire : <gold>" +
                        String.join(", ", names) + "</gold> la porte désormais."));
                }
            }
        }.runTaskLater(VampireUHC.getInstance(), delay);
    }
}
