package fr.vampireuhc.roles;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import fr.vampireuhc.player.Camp;
import fr.vampireuhc.player.VampireUHCPlayer;
import fr.vampireuhc.config.MessageUtil;

public class CartographerRole implements Role {
    private static final double BEACON_RADIUS = 20.0;
    private static final long SCAN_INTERVAL_TICKS = 20L; // 1 seconde

    private VampireUHCPlayer cartographer;
    // Balise stockée en primitives (monde + coordonnées) : sérialisable tel quel
    // et résoluble même si le monde n'est pas encore chargé au moment du restore.
    private String beaconWorld;
    private double beaconX, beaconY, beaconZ;
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
    public String getDescription() {
        return (
            "<gray>Vous cartographiez discrètement les déplacements des joueurs.</gray>\n\n"
            + "<dark_purple>▸</dark_purple> <gray>Placez une balise : <gold>/vuhc baliser</gold></gray>\n"
            + "<dark_purple>▸</dark_purple> <gray>La balise enregistre le passage des joueurs dans un rayon de <yellow>20 blocs</yellow>.</gray>\n\n"
            + "<bold><dark_purple>Fonctionnement :</dark_purple></bold>\n"
            + "  <gray>• La balise reste active pendant l'épisode en cours et le suivant.</gray>\n"
            + "  <gray>• À la fin de l'épisode, vous recevez la liste des joueurs passés dans le rayon d'action de votre balise.</gray>\n"
            + "  <gray>• Après 2 épisodes au même endroit, la balise disparaît automatiquement.</gray>\n"
            + "  <gray>• Réutilisez <gold>/vuhc baliser</gold> pour la repositionner (disponible à chaque épisode).</gray>"
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
            player.sendMessage(MessageUtil.error("Vous avez déjà posé votre balise cet épisode !"));
            return;
        }

        this.episode = current_episode;
        this.applied_this_episode = true;
        Location loc = player.getLocation();
        this.beaconWorld = loc.getWorld() != null ? loc.getWorld().getName() : null;
        this.beaconX = loc.getX();
        this.beaconY = loc.getY();
        this.beaconZ = loc.getZ();
        this.recordedPlayers.clear();
        player.sendMessage(MessageUtil.successTarget("Balise posée en", loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ()));

        // Annule l'ancien scan avant d'en armer un nouveau (sinon la tâche
        // précédente devient orpheline et immortelle).
        cancelScan();
        startScanTask();
    }

    // Restauration de l'état après un redémarrage, y compris le ré-armement du
    // scan (sinon la balise restaurée n'enregistrerait plus aucun passage).
    public void restoreState(int episode, boolean applied, String worldName, double x, double y, double z, Set<UUID> recorded) {
        this.episode = episode;
        this.applied_this_episode = applied;
        this.beaconWorld = worldName;
        this.beaconX = x;
        this.beaconY = y;
        this.beaconZ = z;
        this.recordedPlayers.clear();
        this.recordedPlayers.addAll(recorded);

        if (applied && worldName != null) {
            cancelScan();
            startScanTask();
        }
    }

    public int getBeaconEpisode() {
        return episode;
    }

    public boolean isBeaconApplied() {
        return applied_this_episode;
    }

    public String getBeaconWorld() {
        return beaconWorld;
    }

    public double getBeaconX() {
        return beaconX;
    }

    public double getBeaconY() {
        return beaconY;
    }

    public double getBeaconZ() {
        return beaconZ;
    }

    public Set<UUID> getRecordedPlayers() {
        return recordedPlayers;
    }

    // Position courante de la balise (null si monde pas encore chargé).
    private Location currentBeaconLocation() {
        if (beaconWorld == null) {
            return null;
        }
        World world = Bukkit.getWorld(beaconWorld);
        return world == null ? null : new Location(world, beaconX, beaconY, beaconZ);
    }

    private void startScanTask() {
        fr.vampireuhc.VampireUHC plugin = fr.vampireuhc.VampireUHC.getInstance();

        // Scan périodique (1 fois/seconde) : enregistre les passages puis révèle
        // la liste en fin de vie. Beaucoup plus léger qu'un PlayerMoveEvent.
        this.scanTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (currentBeaconLocation() == null) {
                cancelScan();
                return;
            }
            int currentEp = plugin.getGameManager().getEpisode();
            // La balise vit l'épisode posé + le suivant : reveal à la FIN de N+1.
            if (currentEp > episode + 1) {
                tryReveal();
                return;
            }
            recordPassingPlayers();
        }, 0L, SCAN_INTERVAL_TICKS);
    }

    // Scan les joueurs vivants autour de la balise, avec des filtres bon marché
    // (monde, chunk, distance au carré) pour ne jamais charger le serveur.
    private void recordPassingPlayers() {
        Location b = currentBeaconLocation();
        if (b == null) {
            return;
        }
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

    // Fin de vie de la balise : communique la liste au Cartographe puis la
    // supprime. Si le cartographe est hors ligne, on ne perd RIEN : les données
    // sont conservées et une nouvelle tentative a lieu à la prochaine seconde.
    private void tryReveal() {
        Player bukkitCartographer = Bukkit.getPlayer(cartographer.getUuid());
        if (bukkitCartographer == null || !bukkitCartographer.isOnline()) {
            return;
        }
        if (recordedPlayers.isEmpty()) {
            bukkitCartographer.sendMessage(MessageUtil.info("Personne n'est passé à proximité de votre balise pendant l'épisode."));
        } else {
            List<String> names = new ArrayList<>();
            for (UUID id : recordedPlayers) {
                VampireUHCPlayer vp = fr.vampireuhc.VampireUHC.getInstance().getPlayerManager().get(id);
                names.add(vp != null ? vp.getLastKnownName() : id.toString());
            }
            bukkitCartographer.sendMessage(MessageUtil.success("Passages enregistrés par votre balise : <gold>" + String.join(", ", names)));
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
        this.beaconWorld = null;
        this.beaconX = 0;
        this.beaconY = 0;
        this.beaconZ = 0;
        this.recordedPlayers.clear();
    }
}
