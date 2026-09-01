package fr.vampireuhc.roles.usurped;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import fr.vampireuhc.VampireUHC;
import fr.vampireuhc.player.VampireUHCPlayer;

/**
 * Archer copié par le Sosie : passif de marquage lumineux (glow) identique au
 * vrai Archer — 15 secondes de particules rouges visibles uniquement par lui,
 * le timer se réinitialise si la même cible est re-touchée — mais sans
 * l'équipement (arc, livres d'enchantement).
 */
public class UsurpedArcher implements UsurpedPower {

    private VampireUHCPlayer sosie;
    // Tâches actives de glow par UUID cible → permet d'annuler/recommencer.
    private final Map<UUID, BukkitTask> activeGlows = new HashMap<>();

    // --- NMS 1.8.9 (packet particles) -- même mécanique que le vrai Archer. ---
    private static boolean nmsAvailable;
    private static Class<?> enumParticleClass;
    private static Class<?> packetClass;
    private static Object spellMobParticle;
    private static Constructor<?> packetCtor;

    static {
        try {
            final String VERSION = Bukkit.getServer().getClass().getPackage().getName().split("\\.")[3];
            final String NMSPACKET = "net.minecraft.server." + VERSION + ".";

            enumParticleClass = Class.forName(NMSPACKET + "EnumParticle");
            packetClass = Class.forName(NMSPACKET + "PacketPlayOutWorldParticles");

            final Object[] particles = enumParticleClass.getEnumConstants();
            spellMobParticle = particles[15];
            packetCtor = packetClass.getConstructor(enumParticleClass, boolean.class,
                    float.class, float.class, float.class,
                    float.class, float.class, float.class,
                    float.class, int.class, int[].class);

            nmsAvailable = true;
        } catch (Exception e) {
            nmsAvailable = false;
        }
    }

    @Override
    public String getName() {
        return "Archer";
    }

    @Override
    public void onEnter(VampireUHCPlayer doppelganger) {
        this.sosie = doppelganger;
    }

    @Override
    public void onExit() {
        cancelAllGlows();
    }

    @Override
    public void onGameEnd() {
        cancelAllGlows();
    }

    // Pouvoir passif : l'archer-sosie voit les entités qu'il touche avec une
    // traînée de particules rouges visible uniquement par lui.
    public void setGlowOnHit(Entity target, VampireUHC plugin) {
        if (sosie == null || !nmsAvailable) {
            return;
        }

        Player bukkitArcher = Bukkit.getPlayer(sosie.getUuid());
        if (bukkitArcher == null || !bukkitArcher.isOnline()) {
            return;
        }

        UUID targetId = target.getUniqueId();

        // Si un glow est déjà actif sur cette cible, on le reset (nouveau timer de 15s).
        BukkitTask existing = activeGlows.remove(targetId);
        if (existing != null) {
            existing.cancel();
        }

        BukkitTask task = new GlowTask(bukkitArcher, target, plugin).runTaskTimer(plugin, 0L, 1L);
        activeGlows.put(targetId, task);
    }

    private void cancelAllGlows() {
        for (BukkitTask task : activeGlows.values()) {
            task.cancel();
        }
        activeGlows.clear();
    }

    private void removeGlow(UUID targetId) {
        BukkitTask removed = activeGlows.remove(targetId);
        if (removed != null) {
            removed.cancel();
        }
    }

    /**
     * Tâche répétée qui envoie une traînée de particules SPELL_MOB autour de
     * la cible, uniquement visible par l'archer-sosie.
     * 1 tick = 20 fois/seconde, 300 ticks = 15 secondes.
     */
    private class GlowTask extends BukkitRunnable {
        private final Player archerPlayer;
        private final Entity target;
        private final VampireUHC plugin;
        private int elapsed = 0;

        // Connexion NMS de l'archer (cache).
        private final Object nmsConnection;

        GlowTask(Player archerPlayer, Entity target, VampireUHC plugin) {
            this.archerPlayer = archerPlayer;
            this.target = target;
            this.plugin = plugin;
            this.nmsConnection = getNmsConnection(archerPlayer);
        }

        @Override
        public void run() {
            elapsed++;

            // Fin du glow (15 secondes = 300 ticks) ou cible/archer déconnecté.
            if (elapsed > 300 || !archerPlayer.isOnline() || target.isDead()) {
                cancel();
                removeGlow(target.getUniqueId());
                return;
            }

            if (nmsConnection == null) {
                cancel();
                removeGlow(target.getUniqueId());
                return;
            }

            Location loc = target.getLocation();
            sendRedSpellPacket(nmsConnection,
                    (float) loc.getX(), (float) (loc.getY() + 1.0), (float) loc.getZ());
        }
    }

    /**
     * Envoie un paquet SPELL_MOB (offset r=255, g=0, b=0, speed=0)
     * uniquement à l'archer-sosie.
     */
    private void sendRedSpellPacket(Object nmsConnection, float x, float y, float z) {
        try {
            Object packet = packetCtor.newInstance(
                    spellMobParticle, true,
                    x, y, z,
                    255f, 0.0f, 0.0f,
                    0.0f,                 // speed
                    12,                   // count
                    new int[0]);
            Method sendPacket = nmsConnection.getClass().getMethod("sendPacket",
                    Class.forName("net.minecraft.server.v1_8_R3.Packet"));
            sendPacket.invoke(nmsConnection, packet);
        } catch (Exception e) {
            // Silencieux
        }
    }

    private static Object getNmsConnection(Player player) {
        try {
            Method getHandle = player.getClass().getMethod("getHandle");
            Object handle = getHandle.invoke(player);
            return handle.getClass().getField("playerConnection").get(handle);
        } catch (Exception e) {
            return null;
        }
    }
}