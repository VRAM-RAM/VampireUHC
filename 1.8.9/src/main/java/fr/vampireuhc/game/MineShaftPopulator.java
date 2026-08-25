package fr.vampireuhc.game;

import java.util.Random;

import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.generator.BlockPopulator;
import org.bukkit.inventory.ItemStack;

/**
 * Mineshafts abandonnés, déterministes et chunk-locaux : régions 96×96 blocs
 * (~60 % dotées), 2 galeries se croisant (axes X et Z) de largeur 5, hauteur
 * 4, avec salles élargies tous les ~20 blocs, poteaux FENCE sur les bords,
 * rails au centre, toiles d'araignée en abondance, gravier ponctuel au sol
 * et coffres lootés en salles. Le sol peut monter/descendre de ±1 bloc par
 * section de 8 blocs pour rompre la monotonie.
 */
public class MineShaftPopulator extends BlockPopulator {

    private static final int REGION_CHUNKS = 6;
    private static final int REGION_BLOCKS = REGION_CHUNKS * 16;
    private static final int HALF_WIDTH = 2;     // galerie = 5 (center ±2)
    private static final int ROOM_EXTRA = 1;      // salle = 7 (center ±3)
    private static final int CORRIDOR_AIR = 3;    // hauteur d'air (y+1 à y+3)
    private static final int MIN_Y = 13;
    private static final int MAX_Y = 26;
    private static final int PRESENCE_PERCENT = 60;
    private static final int ROOM_INTERVAL = 20;  // salle tous les ~20 blocs
    private static final int ROOM_SPAN = 3;       // une salle fait 3 blocs de long

    @Override
    public void populate(org.bukkit.World world, Random random, Chunk chunk) {
        long seed = world.getSeed();
        int baseX = chunk.getX() * 16;
        int baseZ = chunk.getZ() * 16;
        int regX = Math.floorDiv(chunk.getX(), REGION_CHUNKS);
        int regZ = Math.floorDiv(chunk.getZ(), REGION_CHUNKS);

        long presence = mix(regX, regZ, seed ^ 0x51633E2DCL);
        if (Math.floorMod(presence, 100) >= PRESENCE_PERCENT) {
            return;
        }

        long h2 = mix(regX, regZ, seed ^ 0xA24BAED49L);
        long h3 = mix(regX, regZ, seed ^ 0x9E3779B97F4A7C15L);

        int lineZ = regZ * REGION_BLOCKS + 16
                + (int) Math.floorMod(h2 >> 8, REGION_BLOCKS - 32);
        int lineX = regX * REGION_BLOCKS + 16
                + (int) Math.floorMod(h2 >> 40, REGION_BLOCKS - 32);
        int yAlongX = MIN_Y + (int) Math.floorMod(h3, MAX_Y - MIN_Y + 1);
        int yAlongZ = MIN_Y + (int) Math.floorMod(h3 >> 32, MAX_Y - MIN_Y + 1);

        if (lineZ >= baseZ && lineZ < baseZ + 16) {
            carveCorridor(chunk, random, seed, baseX, baseZ, lineZ - baseZ, yAlongX, true);
        }
        if (lineX >= baseX && lineX < baseX + 16) {
            carveCorridor(chunk, random, seed, baseX, baseZ, lineX - baseX, yAlongZ, false);
        }
    }

    /**
     * Rend la tranche de galerie traversant ce chunk. alongX = galerie
     * courant vers l'est (fixedLocal est alors lz), sinon vers le sud.
     * Le sol monte/descend de ±1 bloc par section de 8 pour rompre la
     * monotonie. Les salles (isRoom) élargissent à7 de large.
     */
    private void carveCorridor(Chunk chunk, Random random, long seed,
                               int baseX, int baseZ, int fixedLocal,
                               int baseFloorY, boolean alongX) {
        for (int t = 0; t < 16; t++) {
            long g = (alongX ? baseX : baseZ) + t;

            // Variation de hauteur : ±1 tous les 8 blocs.
            long segHash = mix(g / 8, 0xABC, seed ^ 0x5555);
            int yShift = (segHash % 3 == 0) ? (int) Math.floorMod(segHash >> 4, 3) - 1 : 0;
            int floorY = baseFloorY + yShift;

            // Salle ou galerie standard ?
            long roomCycle = Math.floorMod(g, ROOM_INTERVAL);
            boolean isRoom = roomCycle < ROOM_SPAN;
            int halfW = isRoom ? HALF_WIDTH + ROOM_EXTRA : HALF_WIDTH;

            for (int w = -halfW; w <= halfW; w++) {
                int lx = alongX ? t : fixedLocal + w;
                int lz = alongX ? fixedLocal + w : t;
                if (lx < 0 || lx > 15 || lz < 0 || lz > 15) {
                    continue;
                }

                // Sol : planches de chêne, gravier ponctuel.
                Material floor = (Math.floorMod(mix(g, w, seed), 100) < 12)
                        ? Material.GRAVEL : Material.WOOD;
                set(chunk, lx, floorY, lz, floor);
                set(chunk, lx, floorY + 4, lz, Material.WOOD);

                // Déçagement 3 de haut.
                for (int dy = 1; dy <= CORRIDOR_AIR; dy++) {
                    set(chunk, lx, floorY + dy, lz, Material.AIR);
                }

                // Rail au centre.
                if (w == 0) {
                    set(chunk, lx, floorY + 1, lz, Material.RAILS);
                }

                // Toiles d'araignée sur les bords (fréquence 15 %).
                if (Math.abs(w) == halfW && Math.floorMod(mix(g, 0xC0B + w, seed), 100) < 15) {
                    Block web = chunk.getBlock(lx, floorY + 1 + (int) Math.floorMod(mix(g, w, seed), 2), lz);
                    if (web.getType() == Material.AIR) {
                        web.setType(Material.WEB, false);
                    }
                }
            }

            // Poteaux de soutènement tous les 3 blocs, sur les bords ±HALF_WIDTH.
            if (g % 3 == 0) {
                placePosts(chunk, t, floorY, fixedLocal, alongX, HALF_WIDTH);
                // Torche au sommet d'un poteau.
                if (Math.floorMod(mix(g, 0x70E4, seed), 100) < 25) {
                    int torchX = alongX ? t : fixedLocal + HALF_WIDTH;
                    int torchZ = alongX ? fixedLocal + HALF_WIDTH : t;
                    set(chunk, torchX, floorY + 4, torchZ, Material.TORCH);
                }
            }

            // Coffre looté en salles (~1 par salle, position déterministe).
            if (isRoom && roomCycle == 1 && Math.floorMod(mix(g, 0xCFFA, seed), 4) == 0) {
                int chestLx = alongX ? t : fixedLocal + HALF_WIDTH;
                int chestLz = alongX ? fixedLocal + HALF_WIDTH : t;
                placeLootedChest(chunk, random, chestLx, floorY + 1, chestLz);
            }
        }
    }

    /** Poteaux de clôtures aux deux bords de la galerie. */
    private void placePosts(Chunk chunk, int t, int floorY, int fixedLocal,
                            boolean alongX, int halfW) {
        for (int w : new int[]{-halfW, halfW}) {
            int lx = alongX ? t : fixedLocal + w;
            int lz = alongX ? fixedLocal + w : t;
            for (int dy = 1; dy <= CORRIDOR_AIR; dy++) {
                set(chunk, lx, floorY + dy, lz, Material.FENCE);
            }
        }
    }

    /** Pose un coffre si la place est libre puis le remplit aléatoirement. */
    private void placeLootedChest(Chunk chunk, Random random, int x, int y, int z) {
        Block block = chunk.getBlock(x, y, z);
        if (block.getType() != Material.AIR) {
            return;
        }
        block.setType(Material.CHEST, false);
        if (!(block.getState() instanceof Chest)) {
            return;
        }
        Chest chest = (Chest) block.getState();
        int stacks = 3 + random.nextInt(4);
        for (int i = 0; i < stacks; i++) {
            LootEntry entry = rollLoot(random);
            int amount = entry.min + random.nextInt(entry.max - entry.min + 1);
            chest.getInventory().setItem(random.nextInt(27),
                    new ItemStack(entry.material, amount));
        }
        chest.update();
    }

    private static LootEntry rollLoot(Random random) {
        int total = 0;
        for (LootEntry entry : LOOT) {
            total += entry.weight;
        }
        int roll = random.nextInt(total);
        for (LootEntry entry : LOOT) {
            roll -= entry.weight;
            if (roll < 0) {
                return entry;
            }
        }
        return LOOT[0];
    }

    private static class LootEntry {
        private final Material material;
        private final int min, max, weight;
        private LootEntry(Material material, int min, int max, int weight) {
            this.material = material;
            this.min = min;
            this.max = max;
            this.weight = weight;
        }
    }

    private static final LootEntry[] LOOT = {
            new LootEntry(Material.BREAD, 1, 3, 20),
            new LootEntry(Material.IRON_INGOT, 1, 3, 14),
            new LootEntry(Material.COAL, 2, 5, 14),
            new LootEntry(Material.APPLE, 1, 2, 12),
            new LootEntry(Material.STRING, 1, 3, 10),
            new LootEntry(Material.RAILS, 4, 8, 8),
            new LootEntry(Material.GOLD_INGOT, 1, 2, 8),
            new LootEntry(Material.DIAMOND, 1, 1, 5),
            new LootEntry(Material.IRON_PICKAXE, 1, 1, 4),
            new LootEntry(Material.GOLDEN_APPLE, 1, 1, 2),
    };

    private static void set(Chunk chunk, int x, int y, int z, Material material) {
        Block block = chunk.getBlock(x, y, z);
        if (block.getType() != material) {
            block.setType(material, false);
        }
    }

    private static long mix(long a, long b, long salt) {
        long h = a * 0x9E3779B97F4A7C15L ^ b * 0xBF58476D1CE4E5B9L ^ salt;
        h ^= h >>> 30;
        h *= 0xBF58476D1CE4E5B9L;
        h ^= h >>> 27;
        h *= 0x94D049BB133111EBL;
        h ^= h >>> 31;
        return h;
    }
}
