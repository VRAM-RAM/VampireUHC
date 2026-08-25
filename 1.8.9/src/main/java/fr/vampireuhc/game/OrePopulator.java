package fr.vampireuhc.game;

import fr.vampireuhc.config.ConfigManager;

import java.util.Random;

import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.generator.BlockPopulator;

/**
 * Génère de VRAIS filons de minerais (marche aléatoire biaisée vers le bas,
 * comme le générateur vanilla) qui remplacent uniquement la pierre. Avec les
 * caves creusées par {@link UhcChunkGenerator}, les filons affleurent dans
 * les parois des galeries.
 *
 * Densité par chunk (nombres configurables dans map.ores.*) :
 *   charbon 2 filons de 12-20 blocs (y <= 80), fer 2 de 8-15 (y <= 60),
 *   or 1 de 5-8 (y <= 32), redstone 1 de 6-10 (y <= 16), diamant 1 de 3-6 (y <= 16).
 */
public class OrePopulator extends BlockPopulator {

    private static final int MIN_ANCHOR_Y = 6; // jamais dans la bedrock

    private final ConfigManager config;

    public OrePopulator(ConfigManager config) {
        this.config = config;
    }

    @Override
    public void populate(org.bukkit.World world, Random random, Chunk chunk) {
        placeVeins(chunk, random, Material.COAL_ORE, config.getOreCoalPerChunk(), 80, 12, 20);
        placeVeins(chunk, random, Material.IRON_ORE, config.getOreIronPerChunk(), 60, 8, 15);
        placeVeins(chunk, random, Material.GOLD_ORE, config.getOreGoldPerChunk(), 32, 5, 8);
        placeVeins(chunk, random, Material.REDSTONE_ORE, config.getOreRedstonePerChunk(), 16, 6, 10);
        placeVeins(chunk, random, Material.DIAMOND_ORE, config.getOreDiamondPerChunk(), 16, 3, 6);
    }

    /**
     * Pose des filons d'un minerai : ancre aléatoire puis marche aléatoire
     * (horizontal libre, vertical à 75 % vers le bas, les filons s'enfoncent).
     * Seule la pierre est remplacée ; un filon sortant du chunk s'arrête.
     */
    private void placeVeins(Chunk chunk, Random random, Material ore, int veins, int maxY,
                            int minSize, int maxSize) {
        for (int v = 0; v < veins; v++) {
            int x = random.nextInt(16);
            int z = random.nextInt(16);
            int y = MIN_ANCHOR_Y + random.nextInt(maxY - MIN_ANCHOR_Y);

            int size = minSize + random.nextInt(maxSize - minSize + 1);
            for (int i = 0; i < size && x >= 0 && x < 16 && z >= 0 && z < 16 && y > 0; i++) {
                Block block = chunk.getBlock(x, y, z);
                if (block.getType() == Material.STONE) {
                    block.setType(ore);
                }
                x += random.nextInt(3) - 1;
                y += random.nextInt(4) == 0 ? 1 : -1;
                z += random.nextInt(3) - 1;
            }
        }
    }
}
