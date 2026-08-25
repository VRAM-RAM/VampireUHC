package fr.vampireuhc.game;

import java.util.Random;

import net.minecraft.server.v1_8_R3.BlockPosition;
import net.minecraft.server.v1_8_R3.Blocks;
import net.minecraft.server.v1_8_R3.WorldGenForest;
import net.minecraft.server.v1_8_R3.WorldGenForestTree;
import net.minecraft.server.v1_8_R3.WorldGenTaiga2;
import net.minecraft.server.v1_8_R3.WorldGenTrees;

import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;
import org.bukkit.craftbukkit.v1_8_R3.CraftWorld;
import org.bukkit.generator.BlockPopulator;

/**
 * Plante les arbres avec les VRAIS générateurs vanilla du serveur
 * (net.minecraft.server.v1_8_R3, présent sur le classpath via spigot
 * « provided ») : silhouettes exactes, y compris le dark oak à double
 * tronc de la forêt sombre centrale.
 *
 * Le biome et la hauteur restent recalculés localement avec les fonctions
 * statiques de {@link UhcChunkGenerator} : aucun accès monde fragile en
 * pleine population de chunk. La porte de plantation combine la proba du
 * biome et un bruit de densité ({@link UhcChunkGenerator#forestDensity})
 * qui dessine des massifs denses séparés de clairières.
 *
 * Les générateurs vanilla peuvent déborder sur le chunk voisin : c'est le
 * comportement standard des décorateurs Minecraft, géré par CraftBukkit.
 */
public class TreePopulator extends BlockPopulator {

    private static final int TREE_ATTEMPTS = 16;
    private static final int PLANT_ATTEMPTS = 12;

    private final int forestRadius;

    public TreePopulator(int forestRadius) {
        this.forestRadius = forestRadius;
    }

    @Override
    public void populate(World world, Random random, Chunk chunk) {
        int baseX = chunk.getX() * 16;
        int baseZ = chunk.getZ() * 16;
        long seed = world.getSeed();

        for (int attempt = 0; attempt < TREE_ATTEMPTS; attempt++) {
            // [3,12] : marge suffisante pour l'empreinte 2x2 des dark oaks.
            int lx = 3 + random.nextInt(10);
            int lz = 3 + random.nextInt(10);
            int wx = baseX + lx;
            int wz = baseZ + lz;

            Biome biome = UhcChunkGenerator.biomeAt(wx, wz, forestRadius, seed);
            double weight = treeWeight(biome, UhcChunkGenerator.forestDensity(wx, wz, seed));
            if (weight <= 0 || random.nextDouble() >= weight) {
                continue;
            }

            // Hauteur recalculee avec la meme fonction que le generateur :
            // le bloc de surface est forcement celui que nous avons pose.
            int h = UhcChunkGenerator.heightAt(wx, wz, seed);
            Block ground = chunk.getBlock(lx, h, lz);
            if (ground.getType() != Material.GRASS || !headroomClear(chunk, lx, h, lz)) {
                continue;
            }

            plant(world, random, biome, lx, h, lz, wx, wz);
        }

        growUndergrowth(chunk, baseX, baseZ, seed, random);
    }

    /**
     * Proba effective de plantation : proba du biome pondérée par le bruit
     * de densité². Le plancher évite les forêts trop clairsemées hors massifs
     * (la forêt sombre reste dense partout, comme son homologue vanilla).
     */
    private double treeWeight(Biome biome, double density) {
        double chance;
        double floor;
        switch (biome) {
            case ROOFED_FOREST: chance = 0.95; floor = 0.45; break;
            case BIRCH_FOREST:  chance = 0.60; floor = 0.15; break;
            case FOREST:        chance = 0.50; floor = 0.15; break;
            case TAIGA:         chance = 0.45; floor = 0.15; break;
            case JUNGLE:        chance = 0.35; floor = 0.15; break;
            case SWAMPLAND:     chance = 0.20; floor = 0.10; break;
            case DESERT:        return 0.0;
            default:            chance = 0.05; floor = 0.02; break; // chênes isolés en plaine
        }
        return Math.min(1.0, chance * (floor + (1.0 - floor) * density * density));
    }

    private void plant(World world, Random random, Biome biome,
                       int lx, int groundY, int lz, int wx, int wz) {
        net.minecraft.server.v1_8_R3.World nms = ((CraftWorld) world).getHandle();
        BlockPosition pos = new BlockPosition(wx, groundY + 1, wz);

        switch (biome) {
            case ROOFED_FOREST:
                // Dark oak authentique : tronc 2x2, canopée large.
                new WorldGenForestTree(false).generate(nms, random, pos);
                return;
            case BIRCH_FOREST:
                if (random.nextInt(100) < 85) {
                    new WorldGenForest(false, false).generate(nms, random, pos);
                } else {
                    new WorldGenTrees(false).generate(nms, random, pos);
                }
                return;
            case FOREST:
                if (random.nextInt(100) < 20) {
                    new WorldGenForest(false, false).generate(nms, random, pos);
                } else {
                    new WorldGenTrees(false).generate(nms, random, pos);
                }
                return;
            case TAIGA:
                new WorldGenTaiga2(false).generate(nms, random, pos);
                return;
            case JUNGLE:
                // Jungle vanilla : haut tronc et lianes (bois/feuilles data 3).
                new WorldGenTrees(false, 6 + random.nextInt(5),
                        Blocks.LOG.fromLegacyData(3), Blocks.LEAVES.fromLegacyData(3), true)
                        .generate(nms, random, pos);
                return;
            default:
                // SWAMPLAND et plaines : chêne vanilla classique.
                new WorldGenTrees(false).generate(nms, random, pos);
        }
    }

    /** Colonne centrale libre au-dessus du sol (évite de percer un surplomb voisin). */
    private boolean headroomClear(Chunk chunk, int lx, int groundY, int lz) {
        for (int y = groundY + 1; y <= groundY + 12; y++) {
            if (chunk.getBlock(lx, y, lz).getType() != Material.AIR) {
                return false;
            }
        }
        return true;
    }

    /**
     * Sous-bois : hautes herbes et fleurs dans les clairières (la canopée
     * dense les étouffe), buissons morts sur le sable du désert.
     */
    private void growUndergrowth(Chunk chunk, int baseX, int baseZ, long seed, Random random) {
        for (int attempt = 0; attempt < PLANT_ATTEMPTS; attempt++) {
            int lx = 1 + random.nextInt(14);
            int lz = 1 + random.nextInt(14);
            int wx = baseX + lx;
            int wz = baseZ + lz;

            int h = UhcChunkGenerator.heightAt(wx, wz, seed);
            Block above = chunk.getBlock(lx, h + 1, lz);
            if (above.getType() != Material.AIR) {
                continue;
            }

            if (UhcChunkGenerator.biomeAt(wx, wz, forestRadius, seed) == Biome.DESERT) {
                if (chunk.getBlock(lx, h, lz).getType() == Material.SAND && random.nextInt(100) < 8) {
                    above.setTypeIdAndData(Material.DEAD_BUSH.getId(), (byte) 0, false);
                }
                continue;
            }
            if (chunk.getBlock(lx, h, lz).getType() != Material.GRASS) {
                continue;
            }

            double density = UhcChunkGenerator.forestDensity(wx, wz, seed);
            if (random.nextDouble() > 0.85 - 0.9 * density) {
                continue; // sous canopée dense, rien ne pousse
            }
            if (random.nextInt(100) < 80) {
                above.setTypeIdAndData(Material.LONG_GRASS.getId(),
                        (byte) (random.nextBoolean() ? 1 : 2), false);
            } else if (random.nextBoolean()) {
                above.setType(Material.YELLOW_FLOWER);
            } else {
                above.setType(Material.RED_ROSE);
            }
        }
    }
}
