package fr.vampireuhc.game;

import fr.vampireuhc.VampireUHC;

import java.util.List;
import java.util.Random;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;

import net.minecraft.server.v1_8_R3.ChunkProviderServer;
import net.minecraft.server.v1_8_R3.WorldServer;

import org.bukkit.World;
import org.bukkit.craftbukkit.v1_8_R3.CraftWorld;
import org.bukkit.scheduler.BukkitTask;

/**
 * Pré-génération asynchrone des chunks : un pool de workers génère les données
 * de terrain (bruit pur, caves, biomes) sous forme de NMS ChunkSnapshot, et
 * le thread principal Bukkit applique les résultats (création NMS Chunk +
 * populators + injection dans le ChunkProviderServer).
 *
 * Architecture :
 *
 *   Worker 1 --                    --> new Chunk(World, snap) --> populators
 *   Worker 2 ---- Queue;PreGenChunk;
 *   Worker 3 --                    --> injection ChunkProviderServer
 *   Worker 4 --
 * 
 * (oui bon le schéma est naze mais je peux pas faire de graphe mermaid)
 */
class AsyncChunkManager {
    private final VampireUHC plugin;
    private final UhcChunkGenerator generator;
    private final World world;
    private final long seed;
    private final int forestRadius;

    private ExecutorService pool;
    private BukkitTask applyTask;
    private final ConcurrentLinkedQueue<PreGenChunk> queue = new ConcurrentLinkedQueue<>();
    private final Semaphore queueSlots = new Semaphore(64); // max 64 chunks en attente
    private final AtomicInteger generatedCount = new AtomicInteger(0);
    private volatile int totalChunks;
    private volatile int appliedTotal;

    AsyncChunkManager(VampireUHC plugin, UhcChunkGenerator generator, World world,
                      long seed, int forestRadius) {
        this.plugin = plugin;
        this.generator = generator;
        this.world = world;
        this.seed = seed;
        this.forestRadius = forestRadius;
    }

    /**
     * Lance les workers et la tâche d'injection sur le thread principal.
     * @param coords     liste des coords chunk (chunkX, chunkZ) à générer
     * @param applyPerTick nombre de chunks injectés par tick (main thread)
     */
    void start(List<int[]> coords, int applyPerTick) {
        totalChunks = coords.size();
        appliedTotal = 0;

        // 4 workers daemon : le serveur peut s'arrêter sans attendre
        pool = Executors.newFixedThreadPool(4, r -> {
            Thread t = new Thread(r, "UHC-Worker");
            t.setDaemon(true);
            return t;
        });

        for (int[] c : coords) {
            final int cx = c[0], cz = c[1];
            pool.submit(() -> {
                try {
                    PreGenChunk pgc = UhcChunkGenerator.generateForWorker(
                            cx, cz, seed, forestRadius);
                    queueSlots.acquire(); // attend si la queue déborde
                    queue.add(pgc);
                    int count = generatedCount.incrementAndGet();
                    if (count % 200 == 0) {
                        plugin.getLogger().info(
                                "Workers: " + count + "/" + totalChunks + " chunks.");
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (Exception e) {
                    plugin.getLogger().warning(
                            "Worker error chunk " + cx + "," + cz + ": " + e.getMessage());
                }
            });
        }
        pool.shutdown();

        // Tâche principale : injecte applyPerTick chunks par tick
        final int perTick = Math.max(1, applyPerTick);
        applyTask = org.bukkit.Bukkit.getScheduler()
                .runTaskTimer(plugin, () -> applyBatch(perTick), 1L, 1L);

        plugin.getLogger().info("Pré-génération async lancée (" + totalChunks
                + " chunks, 4 workers, " + perTick + " injections/tick).");
    }

    private void applyBatch(int perTick) {
        int applied = 0;
        PreGenChunk pgc;
        while (applied < perTick && (pgc = queue.poll()) != null) {
            queueSlots.release(); // libère un slot pour les workers
            if (!world.isChunkLoaded(pgc.chunkX, pgc.chunkZ)) {
                applyChunk(pgc);
            }
            applied++;
        }
        appliedTotal += applied;

        if (appliedTotal % 500 == 0 && appliedTotal < totalChunks) {
            plugin.getLogger().info("Injection: " + appliedTotal + "/" + totalChunks);
        }

        // Terminé ?
        if (queue.isEmpty() && pool.isTerminated()) {
            applyTask.cancel();
            applyTask = null;
            plugin.getLogger().info("Pré-génération async terminée ("
                    + appliedTotal + " chunks).");
        }
    }

    /**
     * Applique un chunk pré-généré sur le thread principal :
     * 1. Crée un NMS Chunk à partir du snapshot
     * 2. Définit les biomes
     * 3. Injecte dans le ChunkProviderServer (AVANT les populators)
     * 4. Initialise l'éclairage
     * 5. Exécute les populators (ores, arbres, mineshafts, géologie)
     *
     * L'ordre 3→5 est CRITIQUE : les populators écrivent via le World API
     * (block.setType → World.getChunkAt → ChunkProviderServer.getChunkAt).
     * Sans injection préalable, le provider génère un chunk vierge de secours,
     * les populators le remplissent, puis notre injection l'écrase → tout est perdu.
     */
    private void applyChunk(PreGenChunk pgc) {
        WorldServer ws = ((CraftWorld) world).getHandle();
        ChunkProviderServer cps = ws.chunkProviderServer;

        // 1. NMS Chunk depuis le snapshot (copie les blocs)
        net.minecraft.server.v1_8_R3.Chunk nmsChunk =
                new net.minecraft.server.v1_8_R3.Chunk(ws, pgc.snapshot, pgc.chunkX, pgc.chunkZ);

        // 2. Biomes
        byte[] biomes = nmsChunk.getBiomeIndex();
        System.arraycopy(pgc.biomeIds, 0, biomes, 0, 256);

        // 3. Injection dans le cache du serveur — AVANT les populators
        long key = ((long) pgc.chunkX << 32)
                 | ((long) pgc.chunkZ & 0xFFFFFFFFL);
        cps.chunks.put(key, nmsChunk);

        // 4. Éclairage (heightmap + sky light)
        nmsChunk.initLighting();

        // 5. Populateurs — le World.getChunkAt() les trouve dans le provider
        long popSeed = seed ^ ((long) pgc.chunkX * 73856093L)
                           ^ ((long) pgc.chunkZ * 19349669L);
        Random random = new Random(popSeed);
        org.bukkit.Chunk bukkitChunk = new org.bukkit.craftbukkit.v1_8_R3.CraftChunk(nmsChunk);
        for (org.bukkit.generator.BlockPopulator pop
                : generator.getDefaultPopulators(world)) {
            pop.populate(world, random, bukkitChunk);
        }
    }

    void shutdown() {
        if (pool != null) {
            pool.shutdownNow();
            pool = null;
        }
        if (applyTask != null) {
            applyTask.cancel();
            applyTask = null;
        }
    }
}
