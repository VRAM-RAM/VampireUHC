package fr.vampireuhc.listeners;

import fr.vampireuhc.VampireUHC;
import fr.vampireuhc.config.ConfigManager;
import fr.vampireuhc.markers.MarkerType;
import fr.vampireuhc.player.VampireUHCPlayer;
import fr.vampireuhc.roles.BabaYagaRole;
import fr.vampireuhc.roles.DoppelgangerRole;
import fr.vampireuhc.roles.usurped.UsurpedBabaYaga;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.block.Block;
import org.bukkit.entity.Cow;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

/**
 * Ajustements de gameplay : fonte automatique des minerais, meilleurs loots
 * (plus d'or, de cuir et de pommes), et limitation de l'absorption des pommes
 * d'or pour les joueurs portant 2 marques Maître.
 */
public class GameplayListener implements Listener {
    private final VampireUHC plugin;
    private final Random random = new Random();

    private static final Map<Material, ItemStack> createAutoSmeltMap() {
        Map<Material, ItemStack> m = new HashMap<Material, ItemStack>();
        // Les variantes deepslate/nether/netherite n'existent pas en 1.8.
        m.put(Material.IRON_ORE, new ItemStack(Material.IRON_INGOT, 1));
        m.put(Material.GOLD_ORE, new ItemStack(Material.GOLD_INGOT, 2));
        m.put(Material.SAND, new ItemStack(Material.GLASS, 1));
        m.put(Material.COBBLESTONE, new ItemStack(Material.STONE, 1));
        return m;
    }

    private static final Map<Material, ItemStack> AUTO_SMELT = createAutoSmeltMap();

    public GameplayListener(VampireUHC plugin) {
        this.plugin = plugin;
    }

    // Fonte automatique + plus de pommes dans les feuilles.
    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        ConfigManager config = plugin.getConfigManager();
        Block block = event.getBlock();

        if (config.isAutoSmeltEnabled()) {
            ItemStack smelted = AUTO_SMELT.get(block.getType());
            if (smelted != null) {
                // Pas de setDropItems en 1.8 : on annule, retire le bloc et
                // droppe nous-mêmes le résultat fondu.
                event.setCancelled(true);
                block.setType(Material.AIR);
                block.getWorld().dropItemNaturally(block.getLocation(), smelted.clone());
                event.getPlayer().giveExp(3);
                return;
            }
        }

        if (config.isBetterLootEnabled() && block.getType() == Material.LEAVES
                && random.nextDouble() < config.getAppleDropChance()) {
            block.getWorld().dropItemNaturally(block.getLocation(), new ItemStack(Material.APPLE, 1));
        }
    }

    // TODO(1.8.9) : LootGenerateEvent n'existe qu'à partir de 1.16 — l'enrichissement
    // des coffres naturels (or/cuir/pommes) est absent de cette version.

    // Plus de cuir par vache tuée par un joueur.
    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        if (!plugin.getConfigManager().isBetterLootEnabled()) {
            return;
        }
        if (event.getEntity() instanceof Cow) {
            Cow cow = (Cow) event.getEntity();
            if (cow.getKiller() instanceof Player) {
                event.getDrops().add(new ItemStack(Material.LEATHER, plugin.getConfigManager().getLeatherBonus()));
            }
        }
    }

    // Le joueur portant 2 marques Maître ne gagne qu'un coeur d'absorption
    // avec les pommes d'or.
    @EventHandler
    public void onConsumeGoldenApple(PlayerItemConsumeEvent event) {
        Material item = event.getItem().getType();
        // En 1.8, pomme normale et pomme dorée enchantée sont toutes deux des
        // GOLDEN_APPLE (durabilité 0/1) : on traite les deux variantes.
        if (item != Material.GOLDEN_APPLE) {
            return;
        }

        Player player = event.getPlayer();
        VampireUHCPlayer vp = plugin.getPlayerManager().get(player.getUniqueId());
        if (vp == null) {
            return;
        }

        // La malédiction de la Baba Yaga retire toute absorption, même si le
        // joueur porte par ailleurs 2 marques Maître (curse > clamp).
        if (isCursedByBabaYaga(player.getUniqueId())) {
            removeAbsorption(player);
            return;
        }

        if (plugin.getMarkerManager().countMarkers(vp.getUuid(), MarkerType.MARQUE_MAITRE) == 2) {
            clampAbsorption(player);
        }

        // Trois marques Maître du Sosie (variante DOPPELGANGER) : le joueur ne
        // doit jamais bénéficier d'absorption pleine (pomme gold). Le Sosie
        // marque déjà comme le Maître : le plafond est donc le même.
        if (plugin.getMarkerManager().countMarkers(vp.getUuid(), MarkerType.MARQUE_MAITRE_DOPPELGANGER) == 3) {
            clampAbsorption(player);
        }
    }

    // Un joueur est maudit si un Baba Yaga en partie a une malédiction active
    // sur lui.
    private boolean isCursedByBabaYaga(UUID uuid) {
        for (VampireUHCPlayer p : plugin.getPlayerManager().getAll()) {
            if (p.getRole() instanceof BabaYagaRole
                    && ((BabaYagaRole) p.getRole()).isCurseActive(uuid)) {
                return true;
            }
            // Le Sosie qui a copié la Baba Yaga maudit aussi (sans ressusciter).
            if (p.getRole() instanceof DoppelgangerRole) {
                fr.vampireuhc.roles.usurped.UsurpedPower power = ((DoppelgangerRole) p.getRole()).getActivePower();
                if (power instanceof UsurpedBabaYaga
                        && ((UsurpedBabaYaga) power).isCurseActive(uuid)) {
                    return true;
                }
            }
        }
        return false;
    }

    private void removeAbsorption(Player player) {
        Bukkit.getScheduler().runTaskLater(plugin, new Runnable() {
            @Override
            public void run() {
                if (player.isOnline()) {
                    player.removePotionEffect(PotionEffectType.ABSORPTION);
                }
            }
        }, 1L);
    }

    // Pas de get/setAbsorptionAmount en 1.8 : approximation du clamp via une
    // potion d'absorption niveau 1 (2 cœurs, durée vanilla d'une pomme). Le
    // double-marqué plafonne ainsi au bonus d'une pomme simple.
    private void clampAbsorption(Player player) {
        Bukkit.getScheduler().runTaskLater(plugin, new Runnable() {
            @Override
            public void run() {
                if (player.isOnline()) {
                    player.removePotionEffect(PotionEffectType.ABSORPTION);
                    player.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, 20 * 120, 0, true));
                }
            }
        }, 1L);
    }
}
