package fr.vampireuhc.game;

import fr.vampireuhc.config.ConfigManager;

import java.util.Random;

import org.bukkit.Material;
import org.bukkit.generator.BlockPopulator;
import org.bukkit.generator.LimitedRegion;
import org.bukkit.generator.WorldInfo;

/**
 * Ajoute des minerais supplémentaires (fer, or, diamant) dans les zones
 * souterraines, en plus de la génération vanilla. Ne place que dans la pierre,
 * aux profondeurs classiques de chaque minerai.
 */
public class OrePopulator extends BlockPopulator {
    private final ConfigManager config;

    public OrePopulator(ConfigManager config) {
        this.config = config;
    }

    @Override
    public void populate(WorldInfo worldInfo, Random random, int chunkX, int chunkZ, LimitedRegion region) {
        placeOre(region, random, chunkX, chunkZ, Material.IRON_ORE, config.getOreIronPerChunk(), 0, 64);
        placeOre(region, random, chunkX, chunkZ, Material.GOLD_ORE, config.getOreGoldPerChunk(), 0, 32);
        placeOre(region, random, chunkX, chunkZ, Material.DIAMOND_ORE, config.getOreDiamondPerChunk(), 0, 16);
    }

    private void placeOre(LimitedRegion region, Random random, int chunkX, int chunkZ,
                          Material ore, int count, int minY, int maxY) {
        for (int i = 0; i < count; i++) {
            int x = chunkX * 16 + random.nextInt(16);
            int y = minY + random.nextInt(maxY - minY);
            int z = chunkZ * 16 + random.nextInt(16);
            if (region.getType(x, y, z) == Material.STONE) {
                region.setType(x, y, z, ore);
            }
        }
    }
}
