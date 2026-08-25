package fr.vampireuhc.game;

import net.minecraft.server.v1_8_R3.ChunkSnapshot;

/**
 * Données pré-générées pour un chunk (calculé sur un worker thread,
 * appliqué sur le thread principal Bukkit).
 */
class PreGenChunk {
    final int chunkX;
    final int chunkZ;
    final ChunkSnapshot snapshot;
    final byte[] biomeIds; // NMS biome IDs, 256 octets (z * 16 + x)

    PreGenChunk(int chunkX, int chunkZ, ChunkSnapshot snapshot, byte[] biomeIds) {
        this.chunkX = chunkX;
        this.chunkZ = chunkZ;
        this.snapshot = snapshot;
        this.biomeIds = biomeIds;
    }
}
