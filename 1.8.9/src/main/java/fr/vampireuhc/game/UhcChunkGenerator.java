package fr.vampireuhc.game;

import java.util.List;

import org.bukkit.World;
import org.bukkit.generator.BlockPopulator;
import org.bukkit.generator.ChunkGenerator;

/**
 * Générateur passthrough 100% vanilla (toutes les étapes déléguées au serveur)
 * avec l'OrePopulator en populator PAR DÉFAUT : les chunks de spawn sont générés
 * en synchrone dès createWorld, donc un populator ajouté après coup ne s'y
 * applique jamais. En le déclarant ici, il est connu du monde avant la moindre
 * génération.
 */
public class UhcChunkGenerator extends ChunkGenerator {
    private final List<BlockPopulator> defaultPopulators;

    public UhcChunkGenerator(BlockPopulator orePopulator) {
        this.defaultPopulators = List.of(orePopulator);
    }

    @Override
    public boolean shouldGenerateNoise() {
        return true;
    }

    @Override
    public boolean shouldGenerateSurface() {
        return true;
    }

    @Override
    public boolean shouldGenerateBedrock() {
        return true;
    }

    @Override
    public boolean shouldGenerateCaves() {
        return true;
    }

    @Override
    public boolean shouldGenerateDecorations() {
        return true;
    }

    @Override
    public boolean shouldGenerateMobs() {
        return true;
    }

    @Override
    public boolean shouldGenerateStructures() {
        return true;
    }

    @Override
    public List<BlockPopulator> getDefaultPopulators(World world) {
        return defaultPopulators;
    }
}
