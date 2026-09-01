package fr.vampireuhc.roles.usurped;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import com.google.gson.JsonObject;

import fr.vampireuhc.VampireUHC;
import fr.vampireuhc.config.ConfigManager;
import fr.vampireuhc.config.MessageUtil;
import fr.vampireuhc.markers.MarkerManager;
import fr.vampireuhc.markers.MarkerType;
import fr.vampireuhc.player.VampireUHCPlayer;

/**
 * Cupidon copié par le Sosie : il observe les marques Amour mais ne révèle
 * pas le couple initial. Seuls les CHANGEMENTS futurs de propriétaire (ex :
 * switch du Gremlin) lui sont notifiés, avec la même temporisation que le
 * vrai Cupidon.
 */
public class UsurpedCupidon implements UsurpedPower {

    private VampireUHCPlayer sosie;
    private Set<UUID> lastKnownAmourHolders;
    private BukkitTask movedNotifyTask;

    @Override
    public String getName() {
        return "Cupidon";
    }

    @Override
    public void onEnter(VampireUHCPlayer doppelganger) {
        this.sosie = doppelganger;
        // Le Sosie ne connaît pas les amoureux initiaux : on sème l'état
        // courant au moment de l'usurpation pour ne notifier que les changements.
        MarkerManager manager = VampireUHC.getInstance().getMarkerManager();
        lastKnownAmourHolders = new HashSet<>(holdersOfAmour(manager));
    }

    @Override
    public void onExit() {
        if (movedNotifyTask != null) {
            movedNotifyTask.cancel();
            movedNotifyTask = null;
        }
    }

    // Appelé après chaque échange (switch) : si une marque Amour a changé de
    // propriétaire, notifie le Sosie (qui ne connaît pas qui l'a perdue).
    public void notifyIfLoversMoved(MarkerManager manager) {
        if (sosie == null) {
            return;
        }

        Set<UUID> currentHolders = holdersOfAmour(manager);

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

        if (movedNotifyTask != null) {
            movedNotifyTask.cancel();
        }

        VampireUHC plugin = VampireUHC.getInstance();
        Player bukkitSosie = Bukkit.getPlayer(sosie.getUuid());
        movedNotifyTask = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (bukkitSosie != null && bukkitSosie.isOnline()) {
                bukkitSosie.sendMessage(MessageUtil.warn("Une marque Amour a changé de propriétaire : <gold>"
                        + String.join(", ", names) + "</gold> la porte désormais."));
            }
        }, delay);
    }

    private Set<UUID> holdersOfAmour(MarkerManager manager) {
        Set<UUID> holders = new HashSet<>();
        for (UUID id : manager.getAllPlayers()) {
            if (manager.hasMarker(id, MarkerType.AMOUR)) {
                holders.add(id);
            }
        }
        return holders;
    }

    private static String displayName(UUID uuid) {
        Player p = Bukkit.getPlayer(uuid);
        VampireUHCPlayer vp = VampireUHC.getInstance().getPlayerManager().get(uuid);
        if (p != null) {
            return p.getName();
        }
        return vp != null ? vp.getLastKnownName() : uuid.toString();
    }

    @Override
    public void saveState(JsonObject obj) {
        // Aucun état persistant : le seed du couple initial est rejoué à la
        // restauration (restoreState), conformément au comportement du vrai Cupidon.
    }

    @Override
    public void restoreState(JsonObject obj) {
        MarkerManager manager = VampireUHC.getInstance().getMarkerManager();
        lastKnownAmourHolders = new HashSet<>(holdersOfAmour(manager));
    }
}