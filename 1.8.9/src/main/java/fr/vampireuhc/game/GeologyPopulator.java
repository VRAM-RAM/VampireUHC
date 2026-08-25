package fr.vampireuhc.game;

import java.util.Random;

import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.generator.BlockPopulator;

/**
 * Ajoute des blobs de blocs géologiques (granite, diorite, andésite, gravier)
 * sous la surface, en remplaçant la pierre de façon regroupée. Les blobs
 * donnent du texture et de la variété au souterrain sans modifier la
 * topographie. Les blocs variant avec les data values 1.8 :
 *   STONE:data1 = granite, data3 = diorite, data5 = andésite.
 *
 * Le gravier apparaît surtout en plafond de cave (hauteur 15-50) et en
 * petites poches disséminées. Les blobs de pierre variante sont placés en
 * profondeur (hauteur 15-55) et font 15-30 blocs.
 */
public class GeologyPopulator extends BlockPopulator {

    private static final int MIN_Y = 12;
    private static final int MAX_Y = 55;

    // Data values 1.8 pour les variantes de pierre (Material.STONE).
    private static final byte GRANITE    = 1;
    private static final byte DIORITE    = 3;
    private static final byte ANDESITE   = 5;

    @Override
    public void populate(org.bukkit.World world, Random random, Chunk chunk) {
        long seed = world.getSeed();
        int baseX = chunk.getX() * 16;
        int baseZ = chunk.getZ() * 16;

        // 3 blobs de pierre variante par chunk (granite/diorite/andésite).
        placeStoneBlob(chunk, random, seed, baseX, baseZ, GRANITE,  18);
        placeStoneBlob(chunk, random, seed, baseX, baseZ, DIORITE,  22);
        placeStoneBlob(chunk, random, seed, baseX, baseZ, ANDESITE, 16);

        // 2-3 poches de gravier en profondeur.
        int gravelPockets = 2 + random.nextInt(2);
        for (int i = 0; i < gravelPockets; i++) {
            placeGravelPocket(chunk, random, seed, baseX, baseZ);
        }
    }

    /**
     * Place un blob de blocs variant en marche aléatoire dans la pierre,
     * semblable aux filons de minerais mais plus large.
     */
    private void placeStoneBlob(Chunk chunk, Random random, long seed,
                                int baseX, int baseZ, byte variant, int maxSize) {
        int x = random.nextInt(16);
        int z = random.nextInt(16);
        int y = MIN_Y + random.nextInt(MAX_Y - MIN_Y);
        int size = maxSize / 2 + random.nextInt(maxSize / 2);

        for (int i = 0; i < size; i++) {
            if (x >= 0 && x < 16 && z >= 0 && z < 16 && y > 0 && y < 256) {
                Block block = chunk.getBlock(x, y, z);
                if (block.getType() == Material.STONE) {
                    block.setTypeIdAndData(Material.STONE.getId(), variant, false);
                }
            }
            x += random.nextInt(3) - 1;
            y += random.nextInt(3) - 2;
            z += random.nextInt(3) - 1;
        }
    }

    /**
     * Poche de gravier : ancre aléatoire puis marche aléatoire biaisée vers
     * le bas, remplace uniquement la pierre.
     */
    private void placeGravelPocket(Chunk chunk, Random random, long seed,
                                   int baseX, int baseZ) {
        int x = random.nextInt(16);
        int z = random.nextInt(16);
        int y = MIN_Y + random.nextInt(MAX_Y - MIN_Y);
        int size = 5 + random.nextInt(8);

        for (int i = 0; i < size; i++) {
            if (x >= 0 && x < 16 && z >= 0 && z < 16 && y > 0 && y < 256) {
                Block block = chunk.getBlock(x, y, z);
                if (block.getType() == Material.STONE) {
                    block.setType(Material.GRAVEL, false);
                }
            }
            x += random.nextInt(3) - 1;
            y += random.nextInt(4) == 0 ? 1 : -1;
            z += random.nextInt(3) - 1;
        }
    }
}
