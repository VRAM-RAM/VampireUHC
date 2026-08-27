package fr.vampireuhc.roles;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import fr.vampireuhc.VampireUHC;
import fr.vampireuhc.player.Camp;
import fr.vampireuhc.player.VampireUHCPlayer;

public class ArcherRole implements Role {

    private VampireUHCPlayer archer;

    // Tâches actives de glow par UUID cible → permet d'annuler/recommencer
    private final Map<UUID, BukkitTask> activeGlows = new HashMap<>();

    // Cache NMS (une seule vérification au démarrage)
    private static boolean nmsAvailable;
    private static Class<?> enumParticleClass;
    private static Class<?> packetClass;
    private static Object spellMobParticle;
    private static Constructor<?> packetCtor;

    static {
        try {
            enumParticleClass = Class.forName("net.minecraft.server.v1_8_R3.EnumParticle");
            packetClass = Class.forName("net.minecraft.server.v1_8_R3.PacketPlayOutWorldParticles");
            spellMobParticle = enumParticleClass.getField("SPELL_MOB").get(null);
            packetCtor = packetClass.getConstructor(
                    enumParticleClass, boolean.class,
                    float.class, float.class, float.class,
                    float.class, float.class, float.class,
                    float.class, int.class, int[].class);
            nmsAvailable = true;
        } catch (Exception e) {
            nmsAvailable = false;
        }
    }

    @Override
    public Camp getDefaultCamp() {
        return Camp.VILLAGEOIS;
    }

    @Override
    public String getDescription() {
        return (
            "<gray>Votre objectif est de gagner avec le <green>village</green>.\n\n"
            + "<gray>Pour ce faire, vous disposez, dès l'annonce des rôles, de :</gray>\n"
            + "<dark_purple>• Un livre Infinity.</dark_purple>\n"
            + "<dark_purple>• Un livre Power II.</dark_purple>\n"
            + "<dark_purple>• Un Arc.</dark_purple>\n"
            + "<dark_purple>• Une flèche.</dark_purple>\n\n"
            + "<gray>Vous possédez également un effet passif : lorsque vous touchez une entité d'une de vos flèches, cette entité recevra un effet de <yellow>glowing</yellow> pendant <red>15 secondes</red> que vous seul percevrez."
        );
    }

    @Override
    public boolean isVampire() {
        return false;
    }

    @Override
    public String getName() {
        return "Archer";
    }

    @Override
    public void onAssign(VampireUHCPlayer player) {
        this.archer = player;
    }

    @Override
    public void onAssign(VampireUHCPlayer player, boolean restoring) {
        this.archer = player;

        if (restoring) {
            return;
        }

        Player bukkitArcher = Bukkit.getPlayer(player.getUuid());
        if (bukkitArcher == null || !bukkitArcher.isOnline()) {
            return;
        }

        ItemStack Infinitybook = new ItemStack(Material.ENCHANTED_BOOK);
        ItemStack PowerTwoBook = new ItemStack(Material.ENCHANTED_BOOK);

        EnchantmentStorageMeta infinitymeta = (EnchantmentStorageMeta) Infinitybook.getItemMeta();
        EnchantmentStorageMeta powertwometa = (EnchantmentStorageMeta) PowerTwoBook.getItemMeta();

        infinitymeta.addStoredEnchant(Enchantment.ARROW_INFINITE, 1, true);
        powertwometa.addStoredEnchant(Enchantment.ARROW_DAMAGE, 2, true);

        Infinitybook.setItemMeta(infinitymeta);
        PowerTwoBook.setItemMeta(powertwometa);

        bukkitArcher.getInventory().addItem(
                    new ItemStack(Material.BOW),
                    new ItemStack(Infinitybook),
                    new ItemStack(PowerTwoBook),
                    new ItemStack(Material.ARROW));
    }

    @Override
    public void onGameEnd() {
        cancelAllGlows();
    }

    // Pouvoir passif : l'archer voit les entités qu'il touche avec une traînée
    // de particules rouges visible uniquement par lui. En 1.8.9, le glowing natif
    // n'existe pas. On envoie des particules SPELL_MOB via paquet NMS
    // directement à la connexion de l'archer.
    public void setGlowOnHit(Entity target, VampireUHC plugin) {
        if (archer == null || !nmsAvailable) {
            return;
        }

        Player bukkitArcher = Bukkit.getPlayer(archer.getUuid());
        if (bukkitArcher == null || !bukkitArcher.isOnline()) {
            return;
        }

        UUID targetId = target.getUniqueId();

        // Si un glow est déjà actif sur cette cible, on le reset (nouveau timer de 15s)
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
     * Tâche répétée qui envoie une traînée de particules SPELL_MOB
     * autour de la cible, uniquement visible par l'archer.
     * 1 tick = 20 fois/seconde, 300 ticks = 15 secondes.
     */
    private class GlowTask extends BukkitRunnable {
        private final Player archerPlayer;
        private final Entity target;
        private final VampireUHC plugin;
        private int elapsed = 0;

        // Connexion NMS de l'archer (cache)
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

            // Fin du glow (15 secondes = 300 ticks) ou cible/archer déconnecté
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
     * Envoie un paquet SPELL_MOB (offset r=1, g=0, b=0, speed=0)
     * uniquement à l'archer. offset=0.2 pour un halo compact autour de la cible.
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
