package fr.vampireuhc.game;

import org.bukkit.block.Biome;
import org.bukkit.generator.BiomeParameterPoint;
import org.bukkit.generator.BiomeProvider;
import org.bukkit.generator.WorldInfo;

import java.util.List;

/**
 * Force une forêt sombre (roofed forest) autour de 0;0, et laisse un choix de
 * biomes plausibles ailleurs (basé sur température/humidité). Le terrain reste
 * généré par le générateur vanilla (on ne fournit qu'un BiomeProvider).
 */
public class UhcBiomeProvider extends BiomeProvider {
    private final int forestRadius;

    private static final List<Biome> BIOMES = List.of(
            Biome.PLAINS,
            Biome.FOREST,
            Biome.BIRCH_FOREST,
            Biome.TAIGA,
            Biome.DESERT,
            Biome.SWAMP,
            Biome.MEADOW,
            Biome.JUNGLE,
            Biome.DARK_FOREST
    );

    public UhcBiomeProvider(int forestRadius) {
        this.forestRadius = forestRadius;
    }

    private boolean inDarkForest(int x, int z) {
        return (long) x * x + (long) z * z <= (long) forestRadius * forestRadius;
    }

    @Override
    public Biome getBiome(WorldInfo worldInfo, int x, int y, int z) {
        if (inDarkForest(x, z)) {
            return Biome.DARK_FOREST;
        }
        // Fallback déterministe si la variante avec paramètres n'est pas utilisée.
        long h = (long) x * 0x9E3779B97F4A7C15L ^ (long) z * 0xBF58476D1CE4E5B9L ^ 0x5DEECE66DL;
        h ^= h >>> 16;
        return BIOMES.get((int) (Math.abs(h) % BIOMES.size()));
    }

    @Override
    public Biome getBiome(WorldInfo worldInfo, int x, int y, int z, BiomeParameterPoint point) {
        if (inDarkForest(x, z)) {
            return Biome.DARK_FOREST;
        }
        double temperature = point.getTemperature();
        double humidity = point.getHumidity();
        if (temperature < 0.3) {
            return humidity > 0.5 ? Biome.TAIGA : Biome.PLAINS;
        }
        if (temperature > 0.8 && humidity > 0.4) {
            return Biome.JUNGLE;
        }
        if (temperature > 0.7) {
            return Biome.DESERT;
        }
        if (humidity > 0.6) {
            return Biome.FOREST;
        }
        return Biome.PLAINS;
    }

    @Override
    public List<Biome> getBiomes(WorldInfo worldInfo) {
        return BIOMES;
    }
}
