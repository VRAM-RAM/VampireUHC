package fr.vampireuhc.roles.usurped;

import com.google.gson.JsonObject;
import fr.vampireuhc.VampireUHC;
import fr.vampireuhc.config.MessageUtil;
import fr.vampireuhc.markers.Marker;
import fr.vampireuhc.markers.MarkerManager;
import fr.vampireuhc.player.VampireUHCPlayer;

import java.util.List;
import java.util.Random;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;

/**
 * Gremlin usurpé : le switch est imparfait — un seul marqueur aléatoire de
 * chaque joueur est échangé (pas l'ensemble). Le drain est copié exactement
 * (30% de chance de voler un demi-cœur par coup pendant 5 minutes, puis un
 * poison léger). Gates propres au Sosie.
 */
public class UsurpedGremlin implements UsurpedPower {

    private VampireUHCPlayer sosie;

    private int lastSwitchEpisode = -1;
    private int drainEpisode = -1;

    private BukkitTask drainTask;
    private boolean drainActive;
    private final Random random = new Random();

    @Override
    public String getName() {
        return "Gremlin";
    }

    @Override
    public void onEnter(VampireUHCPlayer doppelganger) {
        this.sosie = doppelganger;
    }

    @Override
    public void onExit() {
        stopDrain();
    }

    @Override
    public void onGameEnd() {
        stopDrain();
    }

    private void stopDrain() {
        drainActive = false;
        if (drainTask != null) {
            drainTask.cancel();
            drainTask = null;
        }
    }

    // Switch imparfait : on échange un seul marqueur de chaque joueur.
    public boolean switchMarkers(MarkerManager manager, VampireUHCPlayer targetOne, VampireUHCPlayer targetTwo, int currentEpisode) {
        if (sosie == null) {
            return false;
        }
        if (lastSwitchEpisode == currentEpisode) {
            return false;
        }

        switchOne(manager, targetOne.getUuid(), targetTwo.getUuid());
        switchOne(manager, targetTwo.getUuid(), targetOne.getUuid());

        this.lastSwitchEpisode = currentEpisode;
        return true;
    }

    private void switchOne(MarkerManager manager, UUID from, UUID to) {
        List<Marker> fromMarkers = manager.getMarkers(from);
        if (fromMarkers.isEmpty()) {
            return;
        }
        Marker marker = fromMarkers.get(random.nextInt(fromMarkers.size()));
        manager.removeMarker(from, marker);
        manager.addMarker(to, marker.getType(), marker.getSource());
    }

    // Drain copié exactement du Gremlin.
    public boolean activateDrain(int currentEpisode) {
        if (sosie == null) {
            return false;
        }
        if (drainActive) {
            return false;
        }
        if (drainEpisode == currentEpisode) {
            return false;
        }

        this.drainEpisode = currentEpisode;

        Player player = Bukkit.getPlayer(sosie.getUuid());

        this.drainActive = true;
        this.drainTask = Bukkit.getScheduler().runTaskLater(VampireUHC.getInstance(),
                new Runnable() {
                    @Override
                    public void run() {
                        UsurpedGremlin.this.drainActive = false;
                        if (player != null && player.isOnline() && player.isValid()) {
                            MessageUtil.sendActionBar(player,
                                    "<dark_red>Votre pouvoir de <dark_purple>drain</dark_purple> est épuisé !"
                                    + " Vous ressentez une <green>faiblesse</green>.</dark_red>");
                            player.addPotionEffect(poisonEffect(1));
                        }
                    }
                }, 20L * 60 * 5);
        return true;
    }

    public void applyDrainEffect(Player player, Player victim) {
        if (!drainActive) {
            return;
        }

        int chance = random.nextInt(101);
        if (chance > 30) {
            return;
        }

        // Le vol s'applique au tick suivant : on est en plein dans l'event de
        // dégâts du coup déclencheur.
        VampireUHC plugin = VampireUHC.getInstance();
        Bukkit.getScheduler().runTask(plugin, new Runnable() {
            @Override
            public void run() {
                if (!victim.isOnline() || victim.isDead()) {
                    return;
                }
                double stolen = Math.min(1.0, victim.getHealth());
                if (stolen <= 0) {
                    return;
                }
                victim.setHealth(victim.getHealth() - stolen);
                if (player.isOnline() && !player.isDead()) {
                    double maxHealth = player.getMaxHealth();
                    player.setHealth(Math.min(player.getHealth() + stolen, maxHealth));
                }
            }
        });
    }

    private PotionEffect poisonEffect(int amplifier) {
        // 10 secondes d'effet
        return new PotionEffect(PotionEffectType.POISON, 20 * 10, amplifier, true, false);
    }

    @Override
    public void saveState(JsonObject obj) {
        obj.addProperty("usurpedGremlinLastSwitch", lastSwitchEpisode);
        obj.addProperty("usurpedGremlinLastDrain", drainEpisode);
    }

    @Override
    public void restoreState(JsonObject obj) {
        if (obj.has("usurpedGremlinLastSwitch")) {
            lastSwitchEpisode = obj.get("usurpedGremlinLastSwitch").getAsInt();
        }
        if (obj.has("usurpedGremlinLastDrain")) {
            drainEpisode = obj.get("usurpedGremlinLastDrain").getAsInt();
        }
    }

    public int getLastSwitchEpisode() {
        return lastSwitchEpisode;
    }

    public int getLastDrainEpisode() {
        return drainEpisode;
    }
}