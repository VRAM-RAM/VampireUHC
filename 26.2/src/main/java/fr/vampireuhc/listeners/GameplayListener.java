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
import org.bukkit.block.Block;
import org.bukkit.entity.Cow;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.world.LootGenerateEvent;
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

    // Timer actif de clamp d'absorption par joueur : on ne crée jamais deux
    // timers simultanés pour le même joueur (sinon ils s'empilent à chaque pomme).
    private final Map<UUID, BukkitTask> absorptionClampTasks = new HashMap<>();

    private static final Map<Material, ItemStack> AUTO_SMELT = Map.of(
            Material.IRON_ORE, new ItemStack(Material.IRON_INGOT, 1),
            Material.DEEPSLATE_IRON_ORE, new ItemStack(Material.IRON_INGOT, 1),
            Material.GOLD_ORE, new ItemStack(Material.GOLD_INGOT, 2),
            Material.DEEPSLATE_GOLD_ORE, new ItemStack(Material.GOLD_INGOT, 2),
            Material.NETHER_GOLD_ORE, new ItemStack(Material.GOLD_INGOT, 2),
            Material.ANCIENT_DEBRIS, new ItemStack(Material.NETHERITE_SCRAP, 1),
            Material.SAND, new ItemStack(Material.GLASS, 1),
            Material.COBBLESTONE, new ItemStack(Material.STONE, 1)
    );

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
                event.setDropItems(false);
                event.setExpToDrop(3);
                block.getWorld().dropItemNaturally(block.getLocation(), smelted.clone());
                return;
            }
        }

        if (config.isBetterLootEnabled() && block.getType().name().endsWith("_LEAVES")
                && random.nextDouble() < config.getAppleDropChance()) {
            block.getWorld().dropItemNaturally(block.getLocation(), new ItemStack(Material.APPLE, 1));
        }
    }

    // Plus d'or, de cuir et de pommes dans les coffres générés.
    @EventHandler
    public void onLootGenerate(LootGenerateEvent event) {
        if (!plugin.getConfigManager().isBetterLootEnabled() || event.isPlugin()) {
            return;
        }

        List<ItemStack> loot = new ArrayList<>(event.getLoot());
        if (random.nextDouble() < 0.5) {
            loot.add(new ItemStack(Material.GOLD_INGOT, 1 + random.nextInt(2)));
        }
        if (random.nextDouble() < 0.4) {
            loot.add(new ItemStack(Material.LEATHER, 1 + random.nextInt(3)));
        }
        if (random.nextDouble() < 0.3) {
            loot.add(new ItemStack(Material.APPLE, 1));
        }
        event.setLoot(loot);
    }

    // Plus de cuir par vache tuée par un joueur.
    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        if (!plugin.getConfigManager().isBetterLootEnabled()) {
            return;
        }
        if (event.getEntity() instanceof Cow cow && cow.getKiller() instanceof Player) {
            event.getDrops().add(new ItemStack(Material.LEATHER, plugin.getConfigManager().getLeatherBonus()));
        }
    }

    // Le joueur portant 2 marques Maître ne gagne qu'un coeur d'absorption
    // avec les pommes d'or.
    @EventHandler
    public void onConsumeGoldenApple(PlayerItemConsumeEvent event) {
        Material item = event.getItem().getType();
        if (item != Material.GOLDEN_APPLE && item != Material.ENCHANTED_GOLDEN_APPLE) {
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
            Bukkit.getScheduler().runTask(plugin, () -> player.setAbsorptionAmount(0));
            return;
        }

        if (plugin.getMarkerManager().countMarkers(vp.getUuid(), MarkerType.MARQUE_MAITRE) == 2) {
            clampAbsorption(player);
        }

        // Même clamp pour le Sosie lorsque sa copie du Maître a posé 3 marques
        // Maître Doppelganger (les clamps se cumulent sans ambiguité : c'est un
        // simple maintenir à 1 coeur, donc aucune cascade à gérer).
        if (plugin.getMarkerManager().countMarkers(vp.getUuid(), MarkerType.MARQUE_MAITRE_DOPPELGANGER) == 3) {
            clampAbsorption(player);
        }
    }

    // Un joueur est maudit si un Baba Yaga en partie a une malédiction active
    // sur lui.
    private boolean isCursedByBabaYaga(UUID uuid) {
        for (VampireUHCPlayer p : plugin.getPlayerManager().getAll()) {
            if (p.getRole() instanceof BabaYagaRole babaYaga && babaYaga.isCurseActive(uuid)) {
                return true;
            }
            // Baba Yaga copiée par le Sosie : malédiction seule, même effet.
            if (p.getRole() instanceof DoppelgangerRole doppelgangerBaba
                    && doppelgangerBaba.getActivePower() instanceof UsurpedBabaYaga usurpedBabaYaga
                    && usurpedBabaYaga.isCurseActive(uuid)) {
                return true;
            }
        }
        return false;
    }

    // Maintient l'absorption à 1 coeur (2 demi-coeurs) pendant 2 minutes.
    private void clampAbsorption(Player player) {
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline()) {
                player.setAbsorptionAmount(2);
            }
        }, 1L);

        UUID uuid = player.getUniqueId();
        BukkitTask previous = absorptionClampTasks.remove(uuid);
        if (previous != null) {
            previous.cancel();
        }

        AtomicInteger ticks = new AtomicInteger(0);
        absorptionClampTasks.put(uuid, new BukkitRunnable() {
            @Override
            public void run() {
                int elapsed = ticks.addAndGet(10);
                if (elapsed >= 20 * 120 || !player.isOnline()) {
                    cancel();
                    absorptionClampTasks.remove(uuid, this);
                    return;
                }
                if (player.getAbsorptionAmount() > 2) {
                    player.setAbsorptionAmount(2);
                }
            }
        }.runTaskTimer(plugin, 10L, 10L));
    }
}
