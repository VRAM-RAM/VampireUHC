package fr.vampireuhc.roles;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import fr.vampireuhc.player.Camp;
import fr.vampireuhc.player.VampireUHCPlayer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;

public class CartographerRole implements Role {
    private static final double BEACON_RADIUS = 20.0;
    private static final long SCAN_INTERVAL_TICKS = 20L; // 1 seconde

    private VampireUHCPlayer cartographer;
    private Location beacon_location;
    private boolean applied_this_episode;
    private int episode;
    private BukkitTask scanTask;
    private final Set<UUID> recordedPlayers = new LinkedHashSet<>();

    public CartographerRole(VampireUHCPlayer player) {
        this.cartographer = player;
    }

    @Override
    public boolean isVampire() {
        return false;
    }

    @Override
    public Component getDescription() {
        MiniMessage mm = MiniMessage.miniMessage();
        return mm.deserialize(
            "<gray>Vous cartographiez discrètement les déplacements des joueurs.</gray>\n\n"
            + "<dark_purple>▸</dark_purple> <gray>Placez une balise : <gold>/vuhc baliser</gold></gray>\n"
            + "<dark_purple>▸</dark_purple> <gray>La balise enregistre les joueurs dans un rayon de <yellow>20 blocs</yellow>.</gray>\n\n"
            + "<bold><dark_purple>Fonctionnement :</dark_purple></bold>\n"
            + "  <gray>• La balise reste active pendant l'épisode en cours et le suivant.</gray>\n"
            + "  <gray>• Au début de l'épisode suivant, vous recevez la liste des joueurs passés.</gray>\n"
            + "  <gray>• Après 2 épisodes au même endroit, la balise disparaît.</gray>\n"
            + "  <gray>• Réutilisez <gold>/vuhc baliser</gold> pour la repositionner.</gray>"
        );
    }

    @Override
    public String getName() {
        return "Cartographe";
    }

    @Override
    public Camp getDefaultCamp() {
        return Camp.VILLAGEOIS;
    }

    @Override
    public void onAssign(VampireUHCPlayer vampireUHCPlayer) {
        this.cartographer = vampireUHCPlayer;
    }

    @Override
    public void onGameEnd() {
        suppressBeacon();
    }

    // Pouvoirs spécifiques au rôle :

    public void placeBeacon(Player player, int current_episode) {
        if (cartographer == null) {
            return;
        }

        if (episode == current_episode && applied_this_episode) {
            player.sendMessage(ChatColor.RED + "Vous avez déjà posé votre balise cet épisode !");
            return;
        }

        this.episode = current_episode;
        this.applied_this_episode = true;
        this.beacon_location = player.getLocation().clone();
        this.recordedPlayers.clear();
        player.sendMessage(ChatColor.DARK_BLUE + "Balise posée en " + ChatColor.GREEN + beacon_location);

        var plugin = fr.vampireuhc.VampireUHC.getInstance();

        // Scan périodique (1 fois/seconde) : enregistre les passages puis révèle
        // la liste à la fin de l'épisode. Beaucoup plus léger qu'un PlayerMoveEvent.
        this.scanTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (beacon_location == null) {
                cancelScan();
                return;
            }
            if (plugin.getGameManager().getEpisode() != episode) {
                revealAndReset();
                return;
            }
            recordPassingPlayers();
        }, 0L, SCAN_INTERVAL_TICKS);
    }

    // Scan les joueurs vivants autour de la balise, avec des filtres bon marché
    // (monde, chunk, distance au carré) pour ne jamais charger le serveur.
    private void recordPassingPlayers() {
        if (beacon_location == null) {
            return;
        }
        Location b = beacon_location;
        int beaconChunkX = b.getBlockX() >> 4;
        int beaconChunkZ = b.getBlockZ() >> 4;
        double radiusSquared = BEACON_RADIUS * BEACON_RADIUS;

        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.getGameMode() == GameMode.SPECTATOR) {
                continue;
            }
            if (p.getUniqueId().equals(cartographer.getUuid())) {
                continue;
            }
            if (!p.getWorld().equals(b.getWorld())) {
                continue;
            }
            int chunkX = p.getLocation().getBlockX() >> 4;
            int chunkZ = p.getLocation().getBlockZ() >> 4;
            if (Math.abs(chunkX - beaconChunkX) > 2 || Math.abs(chunkZ - beaconChunkZ) > 2) {
                continue;
            }
            if (p.getLocation().distanceSquared(b) <= radiusSquared) {
                recordedPlayers.add(p.getUniqueId());
            }
        }
    }

    // Fin d'épisode : communique la liste au Cartographe puis réinitialise la balise.
    private void revealAndReset() {
        Player bukkitCartographer = Bukkit.getPlayer(cartographer.getUuid());
        if (bukkitCartographer != null) {
            if (recordedPlayers.isEmpty()) {
                bukkitCartographer.sendMessage(ChatColor.DARK_BLUE + "Personne n'est passé à proximité de votre balise pendant l'épisode.");
            } else {
                List<String> names = new ArrayList<>();
                for (UUID id : recordedPlayers) {
                    VampireUHCPlayer vp = fr.vampireuhc.VampireUHC.getInstance().getPlayerManager().get(id);
                    names.add(vp != null ? vp.getLastKnownName() : id.toString());
                }
                bukkitCartographer.sendMessage(ChatColor.DARK_BLUE + "Passages enregistrés par votre balise : " + ChatColor.GREEN + String.join(", ", names));
            }
        }
        suppressBeacon();
    }

    private void cancelScan() {
        if (scanTask != null) {
            scanTask.cancel();
            scanTask = null;
        }
    }

    private void suppressBeacon() {
        cancelScan();
        this.applied_this_episode = false;
        this.beacon_location = null;
        this.recordedPlayers.clear();
    }
}
