package fr.vampireuhc.game;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

import net.minecraft.server.v1_8_R3.Blocks;
import net.minecraft.server.v1_8_R3.ChunkSnapshot;
import net.minecraft.server.v1_8_R3.IBlockData;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.generator.BlockPopulator;
import org.bukkit.generator.ChunkGenerator;

/**
 * Version 1.8.9 : sur les builds finaux de Spigot 1.8.8 (commit ChunkData,
 * git-Spigot-3c60ece et suivants), le serveur appelle uniquement
 * generateChunkData() et le générateur est responsable de TOUT le terrain :
 * un ChunkData vide donnerait un monde vide (pas de repli vers la génération
 * vanilla, contrairement aux vieux contrats « retourner null »).
 *
 * On génère donc nous-mêmes un heightmap déterministe (value noise seedé sur
 * le seed du monde), les couches classiques (bedrock/pierre/terre/grass/sable
 * sec), les tunnels souterrains et on force les biomes via le BiomeGrid 2D
 * (coordonnées locales). Aucun point d'eau : interdit en UHC PvP, les creux
 * deviennent des cuvettes de sable sec.
 *
 * Les arbres ({@link TreePopulator}), les mineshafts ({@link MineShaftPopulator}),
 * la géologie ({@link GeologyPopulator}) et les minerais ({@link OrePopulator})
 * sont posés en phase standard des populators, après génération du terrain et
 * des caves.
 */
public class UhcChunkGenerator extends ChunkGenerator {

    private static final int MAX_HEIGHT = 120; // monde 0..255 en 1.8, marge de sécurité

    // Caves « spaghetti » : on retire le bloc quand DEUX value noises 3D
    // indépendants sont simultanément proches de 0.5 (l'intersection de deux
    // nappes donne des galeries longues et sinueuses, pas un gruyère).
    private static final int CAVE_MIN_Y = 6;          // jamais sous la bedrock
    private static final int CAVE_SURFACE_MARGIN = 4; // couches de surface préservées

    // Niveau vanilla : l'air creusé en dessous devient lac de lave statique.
    private static final int LAVA_LEVEL = 11;

    private final int forestRadius;
    private final List<BlockPopulator> defaultPopulators;

    // Équivalents 1.8 de la liste moderne (MEADOW n'existe pas encore,
    // DARK_FOREST s'appelle ROOFED_FOREST). Utilisés par le BiomeGrid
    // mais aussi en interne par {@link #biomeAt} via les zones climatiques.
    private static final List<Biome> BIOMES = Arrays.asList(
            Biome.PLAINS,
            Biome.FOREST,
            Biome.BIRCH_FOREST,
            Biome.TAIGA,
            Biome.DESERT,
            Biome.SWAMPLAND,
            Biome.JUNGLE,
            Biome.ROOFED_FOREST
    );

    // NMS IBlockData constants — lectures statiques, thread-safe.
    static final IBlockData NMS_BEDROCK   = Blocks.BEDROCK.getBlockData();
    static final IBlockData NMS_STONE     = Blocks.STONE.getBlockData();
    static final IBlockData NMS_DIRT      = Blocks.DIRT.getBlockData();
    static final IBlockData NMS_GRASS     = Blocks.GRASS.getBlockData();
    static final IBlockData NMS_SAND      = Blocks.SAND.getBlockData();
    static final IBlockData NMS_SANDSTONE = Blocks.SANDSTONE.getBlockData();
    static final IBlockData NMS_AIR       = Blocks.AIR.getBlockData();
    static final IBlockData NMS_LAVA      = Blocks.LAVA.getBlockData();

    public UhcChunkGenerator(BlockPopulator orePopulator, int forestRadius) {
        this.forestRadius = forestRadius;
        this.defaultPopulators = Arrays.asList(
                new MineShaftPopulator(),
                new GeologyPopulator(),
                orePopulator,
                new TreePopulator(forestRadius));
    }

    @Override
    public ChunkData generateChunkData(World world, Random random, int chunkX, int chunkZ, BiomeGrid biomes) {
        ChunkData data = createChunkData(world);
        long seed = world.getSeed();
        int[] heights = new int[256];

        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                long wx = (long) chunkX * 16 + x;
                long wz = (long) chunkZ * 16 + z;
                Biome biome = biomeAt(wx, wz, forestRadius, seed);
                biomes.setBiome(x, z, biome);
                int h = heightAt(wx, wz, seed);
                heights[(x << 4) | z] = h;
                fillColumn(data, x, z, h, biome);
            }
        }

        carveCaves(data, heights, chunkX, chunkZ, seed);
        return data;
    }

    @Override
    public List<BlockPopulator> getDefaultPopulators(World world) {
        return defaultPopulators;
    }

    /**
     * Hauteur du sol (Y du bloc de surface) pour une colonne monde.
     * Trois octaves de value noise : grandes collines, reliefs moyens, détails.
     * Statique et déterministe : {@link TreePopulator} recalcule exactement
     * la même hauteur sans dépendre d'accesseurs monde.
     */
    static int heightAt(long wx, long wz, long seed) {
        double e = 0.6 * valueNoise(wx / 64.0, wz / 64.0, seed ^ 0x1B873593L)
                 + 0.3 * valueNoise(wx / 18.0, wz / 18.0, seed ^ 0xCC9E2D51L)
                 + 0.1 * valueNoise(wx / 7.0, wz / 7.0, seed ^ 0x1B56C4CBL);
        int h = 54 + (int) Math.round(e * 28.0); // ~54..82, moyenne ~68 : moins de terrain sous la mer
        return Math.max(8, Math.min(MAX_HEIGHT, h));
    }

    /**
     * Densité de forêt en [0,1] à grande échelle (~96 blocs de période) :
     * dessine des massifs denses séparés par des clairières, indépendamment
     * du biome. {@link TreePopulator} module ses plantations avec ce bruit.
     */
    static double forestDensity(long wx, long wz, long seed) {
        return valueNoise(wx / 96.0, wz / 96.0, seed ^ 0x27D4EB2FL);
    }

    /** Value noise 2D interpolé (smoothstep), renvoie environ [0,1]. */
    private static double valueNoise(double x, double z, long salt) {
        int x0 = (int) Math.floor(x);
        int z0 = (int) Math.floor(z);
        double fx = smooth(x - x0);
        double fz = smooth(z - z0);
        double a = lattice(x0, z0, salt);
        double b = lattice(x0 + 1, z0, salt);
        double c = lattice(x0, z0 + 1, salt);
        double d = lattice(x0 + 1, z0 + 1, salt);
        return lerp(lerp(a, b, fx), lerp(c, d, fx), fz);
    }

    /** Hash déterministe d'une cellule de bruit (splitmix64 réduit à [0,1)). */
    private static double lattice(int x, int z, long salt) {
        long h = (x * 0x9E3779B97F4A7C15L) ^ (z * 0xBF58476D1CE4E5B9L) ^ salt;
        h ^= h >>> 30;
        h *= 0xBF58476D1CE4E5B9L;
        h ^= h >>> 27;
        h *= 0x94D049BB133111EBL;
        h ^= h >>> 31;
        return (h & 0xFFFFFFL) / (double) 0x1000000L;
    }

    private static double smooth(double t) {
        return t * t * (3.0 - 2.0 * t);
    }

    private static double lerp(double a, double b, double t) {
        return a + (b - a) * t;
    }

    private static boolean inDarkForest(long worldX, long worldZ, int forestRadius) {
        return worldX * worldX + worldZ * worldZ <= (long) forestRadius * forestRadius;
    }

    /**
     * Biome d'une colonne monde : zones climatiques lisses (température,
     * humidité) à ~200 blocs de diamètre via value noise, au lieu du
     * hash par-colonne qui créait des biomes isolés (trous de sable).
     * Statique : {@link TreePopulator} recalcule localement sans accès monde.
     */
    static Biome biomeAt(long worldX, long worldZ, int forestRadius, long seed) {
        if (inDarkForest(worldX, worldZ, forestRadius)) {
            return Biome.ROOFED_FOREST;
        }
        double temp = valueNoise(worldX / 192.0, worldZ / 192.0, seed ^ 0x123456789ABCDEFL);
        double humid = valueNoise(worldX / 192.0, worldZ / 192.0, seed ^ 0xFEDCBA9876543210L);

        if (temp < 0.40) {
            // Froid : taïga humide, plaines sèches.
            return humid > 0.60 ? Biome.TAIGA : Biome.PLAINS;
        } else if (temp < 0.60) {
            // Tempéré : marécage très humide, bouleaux/forges modérément humides.
            if (humid > 0.65) return Biome.SWAMPLAND;
            if (humid > 0.45) {
                double birch = valueNoise(worldX / 96.0, worldZ / 96.0, seed ^ 0xB17C6A5E4D3C2B1AL);
                return birch > 0.65 ? Biome.BIRCH_FOREST : Biome.FOREST;
            }
            return Biome.FOREST;
        } else {
            // Chaud : jungle humide, désert sec.
            return humid > 0.55 ? Biome.JUNGLE : Biome.DESERT;
        }
    }

    /**
     * Remplit une colonne : bedrock, pierre, puis couche de surface selon le
     * biome. Le terrain suit simplement le relief — aucun point d'eau, aucune
     * cuvette artificielle ; le sable est réservé aux déserts.
     */
    private void fillColumn(ChunkData data, int x, int z, int h, Biome biome) {
        data.setBlock(x, 0, z, Material.BEDROCK);

        if (biome == Biome.DESERT) {
            int stoneTop = Math.max(1, h - 6);
            data.setRegion(x, 1, z, x + 1, stoneTop, z + 1, Material.STONE);
            data.setRegion(x, stoneTop, z, x + 1, Math.max(stoneTop + 1, h - 3), z + 1, Material.SANDSTONE);
            data.setRegion(x, Math.max(1, h - 3), z, x + 1, h, z + 1, Material.SAND);
        } else {
            data.setRegion(x, 1, z, x + 1, Math.max(1, h - 3), z + 1, Material.STONE);
            data.setRegion(x, Math.max(1, h - 3), z, x + 1, h, z + 1, Material.DIRT);
            data.setBlock(x, h, z, Material.GRASS);
        }
    }

    /**
     * Passe de découpe des caves sur toute la hauteur utile du chunk :
     * bedrock intacte (min y=6) et couches de surface préservées (h-4).
     * Les entrées se créent naturellement sur les flancs où les colonnes
     * voisines sont plus basses que la galerie.
     */
    private void carveCaves(ChunkData data, int[] heights, int chunkX, int chunkZ, long seed) {
        long baseX = (long) chunkX * 16;
        long baseZ = (long) chunkZ * 16;
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                int top = heights[(x << 4) | z] - CAVE_SURFACE_MARGIN;
                if (top < CAVE_MIN_Y) {
                    continue;
                }
                long wx = baseX + x;
                long wz = baseZ + z;
                for (int y = CAVE_MIN_Y; y <= top; y++) {
                    // Seuil variable : étroit près de la surface/bedrock (0.025),
                    // large à mi-profondeur (0.07) via sin(π·depth).
                    double depthNorm = (double)(y - CAVE_MIN_Y) / Math.max(1, top - CAVE_MIN_Y);
                    double threshold = 0.025 + 0.045 * Math.sin(depthNorm * Math.PI);
                    if (isCave(wx, y, wz, seed, threshold)) {
                        // Règle vanilla : l'air souterrain sous le niveau de
                        // lave devient un lac de lave statique.
                        data.setBlock(x, y, z,
                                y < LAVA_LEVEL ? Material.STATIONARY_LAVA : Material.AIR);
                    }
                }
            }
        }
    }

    /**
     * Vrai si le bloc monde appartient à une galerie.
     * Deux modes : tunnels « spaghetti » dont la largeur varie avec la
     * profondeur (threshold), et cavernes ouvertes (~5-10 blocs rayon)
     * là où les deux bruits sont très proches du centre (da, db < 0.025).
     */
    private static boolean isCave(long wx, int wy, long wz, long seed, double threshold) {
        double a = 0.65 * noise3(wx / 48.0, wy / 24.0, wz / 48.0, seed ^ 0x2545F491L)
                 + 0.35 * noise3(wx / 15.0, wy / 8.0, wz / 15.0, seed ^ 0x9E3779B9L);
        double b = 0.65 * noise3(wx / 48.0, wy / 24.0, wz / 48.0, seed ^ 0x85EBCA6BL)
                 + 0.35 * noise3(wx / 15.0, wy / 8.0, wz / 15.0, seed ^ 0xC2B2AE35L);
        double da = Math.abs(a - 0.5);
        double db = Math.abs(b - 0.5);
        // Spaghetti : tunnels sinueux, largeur variable selon la profondeur.
        if (da < threshold && db < threshold) {
            return true;
        }
        // Cavernes : salles ouvertes là où les deux bruits sont très centraux.
        return da < 0.025 && db < 0.025;
    }

    /** Value noise 3D interpolé (smoothstep trilinéaire), renvoie environ [0,1]. */
    private static double noise3(double x, double y, double z, long salt) {
        int x0 = (int) Math.floor(x);
        int y0 = (int) Math.floor(y);
        int z0 = (int) Math.floor(z);
        double fx = smooth(x - x0);
        double fy = smooth(y - y0);
        double fz = smooth(z - z0);
        double c00 = lerp(lattice3(x0, y0, z0, salt), lattice3(x0 + 1, y0, z0, salt), fx);
        double c10 = lerp(lattice3(x0, y0 + 1, z0, salt), lattice3(x0 + 1, y0 + 1, z0, salt), fx);
        double c01 = lerp(lattice3(x0, y0, z0 + 1, salt), lattice3(x0 + 1, y0, z0 + 1, salt), fx);
        double c11 = lerp(lattice3(x0, y0 + 1, z0 + 1, salt), lattice3(x0 + 1, y0 + 1, z0 + 1, salt), fx);
        return lerp(lerp(c00, c10, fy), lerp(c01, c11, fy), fz);
    }

    /** Hash déterministe d'une cellule de bruit 3D (splitmix64 réduit à [0,1)). */
    private static double lattice3(int x, int y, int z, long salt) {
        long h = (x * 0x9E3779B97F4A7C15L) ^ (y * 0xBF58476D1CE4E5B9L)
               ^ (z * 0x94D049BB133111EBL) ^ salt;
        h ^= h >>> 30;
        h *= 0xBF58476D1CE4E5B9L;
        h ^= h >>> 27;
        h *= 0x94D049BB133111EBL;
        h ^= h >>> 31;
        return (h & 0xFFFFFFL) / (double) 0x1000000L;
    }

    // ---- Worker thread methods ------------------------------------------------
    // Même logique que generateChunkData / fillColumn / carveCaves, mais écrit
    // dans un NMS ChunkSnapshot (pas de ChunkData Bukkit, pas d'accès World).
    // Appelé depuis les workers d'AsyncChunkManager.

    /**
     * Génère terrain + caves + biomes pour un chunk sur un worker thread.
     * Aucun accès World — pur calcul retournant un NMS ChunkSnapshot.
     */
    static PreGenChunk generateForWorker(int chunkX, int chunkZ,
                                         long seed, int forestRadius) {
        ChunkSnapshot snapshot = new ChunkSnapshot();
        long baseX = (long) chunkX * 16;
        long baseZ = (long) chunkZ * 16;
        byte[] biomeIds = new byte[256];
        int[] heights = new int[256];

        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                long wx = baseX + x;
                long wz = baseZ + z;
                Biome biome = biomeAt(wx, wz, forestRadius, seed);
                biomeIds[z * 16 + x] = biomeToNmsId(biome);
                int h = heightAt(wx, wz, seed);
                heights[(x << 4) | z] = h;
                fillColumnWorker(snapshot, x, z, h, biome);
            }
        }

        carveCavesWorker(snapshot, heights, chunkX, chunkZ, seed);
        return new PreGenChunk(chunkX, chunkZ, snapshot, biomeIds);
    }

    /** Même logique que fillColumn, mais écrit dans un ChunkSnapshot NMS. */
    private static void fillColumnWorker(ChunkSnapshot snap, int x, int z,
                                         int h, Biome biome) {
        snap.a(x, 0, z, NMS_BEDROCK);

        if (biome == Biome.DESERT) {
            int stoneTop = Math.max(1, h - 6);
            for (int y = 1; y < stoneTop; y++) {
                snap.a(x, y, z, NMS_STONE);
            }
            for (int y = stoneTop; y < Math.max(stoneTop + 1, h - 3); y++) {
                snap.a(x, y, z, NMS_SANDSTONE);
            }
            for (int y = Math.max(1, h - 3); y < h; y++) {
                snap.a(x, y, z, NMS_SAND);
            }
        } else {
            int dirtBottom = Math.max(1, h - 3);
            for (int y = 1; y < dirtBottom; y++) {
                snap.a(x, y, z, NMS_STONE);
            }
            for (int y = dirtBottom; y < h; y++) {
                snap.a(x, y, z, NMS_DIRT);
            }
            snap.a(x, h, z, NMS_GRASS);
        }
    }

    /** Même logique que carveCaves, mais écrit dans un ChunkSnapshot NMS. */
    private static void carveCavesWorker(ChunkSnapshot snap, int[] heights,
                                         int chunkX, int chunkZ, long seed) {
        long baseX = (long) chunkX * 16;
        long baseZ = (long) chunkZ * 16;
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                int top = heights[(x << 4) | z] - CAVE_SURFACE_MARGIN;
                if (top < CAVE_MIN_Y) {
                    continue;
                }
                long wx = baseX + x;
                long wz = baseZ + z;
                for (int y = CAVE_MIN_Y; y <= top; y++) {
                    double depthNorm = (double) (y - CAVE_MIN_Y)
                            / Math.max(1, top - CAVE_MIN_Y);
                    double threshold = 0.025 + 0.045 * Math.sin(depthNorm * Math.PI);
                    if (isCave(wx, y, wz, seed, threshold)) {
                        snap.a(x, y, z,
                                y < LAVA_LEVEL ? NMS_LAVA : NMS_AIR);
                    }
                }
            }
        }
    }

    /** Conversion Biome Bukkit → NMS biome ID (vanilla 1.8). */
    static byte biomeToNmsId(Biome biome) {
        switch (biome) {
            case PLAINS:        return 1;
            case DESERT:        return 2;
            case FOREST:        return 4;
            case TAIGA:         return 5;
            case SWAMPLAND:     return 6;
            case JUNGLE:        return 21;
            case BIRCH_FOREST:  return 27;
            case ROOFED_FOREST: return 29;
            default:            return 1;
        }
    }
}
