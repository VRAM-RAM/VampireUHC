package fr.vampireuhc.roles.usurped;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import fr.vampireuhc.VampireUHC;
import fr.vampireuhc.config.MessageUtil;
import fr.vampireuhc.player.VampireUHCPlayer;

/**
 * Cartographe copié par le Sosie : copie exacte de la balise (rayon 20 blocs,
 * scan 1 fois/seconde, révélation à la fin de l'épisode N+1) avec les limites
 * du vrai rôle. Seules les clés JSON sont différentiées (préfixe
 * usurpedCarto*), pour éviter tout conflit de restauration avec un vrai
 * Cartographe.
 */
public class UsurpedCartographer implements UsurpedPower {

    private static final double BEACON_RADIUS = 20.0;
    private static final long SCAN_INTERVAL_TICKS = 20L;

    private VampireUHCPlayer sosie;
    private String beaconWorld;
    private double beaconX, beaconY, beaconZ;
    private boolean applied_this_episode;
    private int episode;
    private BukkitTask scanTask;
    private final Set<UUID> recordedPlayers = new LinkedHashSet<>();

    @Override
    public String getName() {
        return "Cartographe";
    }

    @Override
    public void onEnter(VampireUHCPlayer doppelganger) {
        this.sosie = doppelganger;
    }

    @Override
    public void onExit() {
        suppressBeacon();
    }

    @Override
    public void onGameEnd() {
        suppressBeacon();
    }

    public void placeBeacon(Player player, int current_episode) {
        if (sosie == null) {
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

        cancelScan();
        startScanTask();
    }

    private void startScanTask() {
        VampireUHC plugin = VampireUHC.getInstance();
        this.scanTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (currentBeaconLocation() == null) {
                cancelScan();
                return;
            }
            int currentEp = plugin.getGameManager().getEpisode();
            if (currentEp > episode + 1) {
                tryReveal();
                return;
            }
            recordPassingPlayers();
        }, 0L, SCAN_INTERVAL_TICKS);
    }

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
            if (p.getUniqueId().equals(sosie.getUuid())) {
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

    private void tryReveal() {
        Player bukkitCartographer = Bukkit.getPlayer(sosie.getUuid());
        if (bukkitCartographer == null || !bukkitCartographer.isOnline()) {
            return;
        }
        if (recordedPlayers.isEmpty()) {
            bukkitCartographer.sendMessage(MessageUtil.info("Personne n'est passé à proximité de votre balise pendant l'épisode."));
        } else {
            StringBuilder names = new StringBuilder();
            boolean first = true;
            for (UUID id : recordedPlayers) {
                if (!first) {
                    names.append(", ");
                }
                first = false;
                VampireUHCPlayer vp = VampireUHC.getInstance().getPlayerManager().get(id);
                names.append(vp != null ? vp.getLastKnownName() : id.toString());
            }
            bukkitCartographer.sendMessage(MessageUtil.success("Passages enregistrés par votre balise : <gold>" + names));
        }
        suppressBeacon();
    }

    private Location currentBeaconLocation() {
        if (beaconWorld == null) {
            return null;
        }
        var world = Bukkit.getWorld(beaconWorld);
        return world == null ? null : new Location(world, beaconX, beaconY, beaconZ);
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

    @Override
    public void saveState(JsonObject obj) {
        obj.addProperty("usurpedCartoBeaconEpisode", episode);
        obj.addProperty("usurpedCartoBeaconApplied", applied_this_episode);
        if (beaconWorld != null) {
            obj.addProperty("usurpedCartoBeaconWorld", beaconWorld);
        }
        obj.addProperty("usurpedCartoBeaconX", beaconX);
        obj.addProperty("usurpedCartoBeaconY", beaconY);
        obj.addProperty("usurpedCartoBeaconZ", beaconZ);
        JsonArray recorded = new JsonArray();
        for (UUID id : recordedPlayers) {
            recorded.add(new JsonPrimitive(id.toString()));
        }
        obj.add("usurpedCartoRecorded", recorded);
    }

    @Override
    public void restoreState(JsonObject obj) {
        this.episode = obj.has("usurpedCartoBeaconEpisode") ? obj.get("usurpedCartoBeaconEpisode").getAsInt() : 0;
        this.applied_this_episode = obj.has("usurpedCartoBeaconApplied")
                ? obj.get("usurpedCartoBeaconApplied").getAsBoolean() : false;
        this.beaconWorld = obj.has("usurpedCartoBeaconWorld")
                ? obj.get("usurpedCartoBeaconWorld").getAsString() : null;
        this.beaconX = obj.has("usurpedCartoBeaconX") ? obj.get("usurpedCartoBeaconX").getAsDouble() : 0;
        this.beaconY = obj.has("usurpedCartoBeaconY") ? obj.get("usurpedCartoBeaconY").getAsDouble() : 0;
        this.beaconZ = obj.has("usurpedCartoBeaconZ") ? obj.get("usurpedCartoBeaconZ").getAsDouble() : 0;
        this.recordedPlayers.clear();
        if (obj.has("usurpedCartoRecorded")) {
            for (var element : obj.getAsJsonArray("usurpedCartoRecorded")) {
                recordedPlayers.add(UUID.fromString(element.getAsString()));
            }
        }

        if (applied_this_episode && beaconWorld != null) {
            cancelScan();
            startScanTask();
        }
    }
}