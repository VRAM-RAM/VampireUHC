package fr.vampireuhc.game;

import fr.vampireuhc.VampireUHC;
import fr.vampireuhc.config.ConfigManager;

import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.WorldBorder;
import org.bukkit.WorldCreator;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.world.WorldInitEvent;

/**
 * Crée la map spéciale (1000x1000 centrée sur 0;0, forêt sombre au centre) et
 * gère la téléportation des joueurs ainsi que l'interdiction de la quitter.
 * Le monde est chargé dès le lancement du compte à rebours (/vuhc start) puis
 * les chunks du disque d'éparpillement sont pré-générés en tâche de fond,
 * pour éviter tout freeze au moment du téléport des joueurs.
 */
public class MapManager implements Listener {
    private static final int PREGEN_MARGIN_BLOCKS = 64;

    private final VampireUHC plugin;
    private World world;
    private UhcChunkGenerator generator;
    private AsyncChunkManager asyncChunkManager;

    public MapManager(VampireUHC plugin) {
        this.plugin = plugin;
    }

    public World getWorld() {
        return world;
    }

    // Charge ou crée le monde (rapide : seuls les chunks de spawn sont générés).
    // Appelé dès le début du compte à rebours, et à la reprise d'une partie.
    public World loadWorld() {
        if (world != null) {
            return world;
        }

        ConfigManager config = plugin.getConfigManager();

        WorldCreator creator = new WorldCreator("vuhc_world");
        creator.seed(config.getMapSeed());
        creator.generateStructures(true);
        // L'OrePopulator et les biomes (forêt sombre centrale) passent par le
        // generator : connu avant la génération des chunks de spawn (un
        // populator ajouté après createWorld ne s'y applique jamais).
        generator = new UhcChunkGenerator(new OrePopulator(config), config.getDarkForestRadius());
        creator.generator(generator);

        world = Bukkit.createWorld(creator);
        if (world == null) {
            plugin.getLogger().severe("Impossible de créer la map vuhc_world !");
            return null;
        }

        WorldBorder border = world.getWorldBorder();
        border.setCenter(0, 0);
        border.setSize(config.getMapSize());

        int y = world.getHighestBlockYAt(0, 0);
        world.setSpawnLocation(0, y, 0);
        world.setTime(1000);
        plugin.getLogger().info("Map vuhc_world prête : " + config.getMapSize() + "x" + config.getMapSize()
                + " centrée sur 0;0 (seed " + world.getSeed() + ").");
        return world;
    }

    // Compatibilité : charge simplement le monde (les chunks sont désormais
    // pré-générés séparément via startPregeneration pendant le compte à rebours).
    public void prepareWorld() {
        loadWorld();
    }

    // Pré-génère en tâche de fond les chunks du disque d'éparpillement.
    // Workers (thread pool) calculent le terrain+caves+biomes en parallèle,
    // le thread principal Bukkit applique les résultats sans freeze.
    public void startPregeneration() {
        if (world == null || asyncChunkManager != null) {
            return;
        }

        ConfigManager config = plugin.getConfigManager();
        int scatterRadius = Math.min(config.getSpawnRadius(), Math.max(50, config.getDarkForestRadius() - 50));
        int radius = scatterRadius + PREGEN_MARGIN_BLOCKS;
        int chunkRadius = (radius + 15) / 16;

        List<int[]> coords = new ArrayList<>((2 * chunkRadius + 1) * (2 * chunkRadius + 1));
        for (int x = -chunkRadius; x <= chunkRadius; x++) {
            for (int z = -chunkRadius; z <= chunkRadius; z++) {
                if ((long) x * x + (long) z * z <= (long) chunkRadius * chunkRadius) {
                    coords.add(new int[]{x, z});
                }
            }
        }
        // Du centre vers l'extérieur : les zones utiles en premier si interrompu.
        coords.sort(Comparator.comparingInt(c -> c[0] * c[0] + c[1] * c[1]));

        asyncChunkManager = new AsyncChunkManager(
                plugin, generator, world, world.getSeed(), config.getDarkForestRadius());
        asyncChunkManager.start(coords, config.getPregenChunksPerTick());
    }

    public void cancelPregeneration() {
        if (asyncChunkManager != null) {
            asyncChunkManager.shutdown();
            asyncChunkManager = null;
        }
    }

    // Vrai pendant resetWorld() : les téléports sortants ne doivent pas
    // déclencher onPlayerChangedWorld (qui re-téléporterait les joueurs DANS
    // le monde en cours de déchargement/suppression).
    private boolean resetting;

    // Supprime la map de la partie pour préparer la prochaine partie.
    public void resetWorld() {
        cancelPregeneration();

        if (world == null) {
            return;
        }

        resetting = true;
        try {
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (player.getWorld().equals(world)) {
                    player.teleport(Bukkit.getWorlds().get(0).getSpawnLocation());
                }
            }

            Bukkit.unloadWorld(world, false);

            // Rename instantané pour libérer le nom vuhc_world, puis suppression
            // asynchrone du dossier renommé (évite un freeze du thread principal
            // et une course avec la création du prochain monde).
            File folder = world.getWorldFolder();
            File trash = new File(folder.getParentFile(), folder.getName() + "_trash_" + System.currentTimeMillis());
            File toDelete = folder.renameTo(trash) ? trash : folder;
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> deleteRecursively(toDelete));

            world = null;
        } finally {
            resetting = false;
        }
        plugin.getLogger().info("Map vuhc_world supprimée.");
    }

    private void deleteRecursively(File file) {
        if (file == null || !file.exists()) {
            return;
        }
        File[] children = file.listFiles();
        if (children != null) {
            for (File child : children) {
                deleteRecursively(child);
            }
        }
        file.delete();
    }

    // Éparpille les joueurs à des positions aléatoires au début de la partie.
    public void teleportPlayersToSpawn() {
        if (world == null) {
            return;
        }
        ConfigManager config = plugin.getConfigManager();
        int radius = Math.min(config.getSpawnRadius(), Math.max(50, config.getDarkForestRadius() - 50));
        int minDist = Math.max(10, Math.min(config.getMinSpawnDistance(), radius / 2));

        Random random = new Random();
        List<Location> taken = new ArrayList<>();
        for (Player player : Bukkit.getOnlinePlayers()) {
            Location loc = randomSpawn(radius, minDist, taken, random);
            player.teleport(loc);
            taken.add(loc);
        }
        plugin.getLogger().info("Joueurs éparpillés aléatoirement (rayon " + radius + ", distance min " + minDist + ").");
    }

    public void teleportPlayerRandomly(Player player) {
        if (world == null) {
            return;
        }
        ConfigManager config = plugin.getConfigManager();
        int radius = Math.min(config.getSpawnRadius(), Math.max(50, config.getDarkForestRadius() - 50));
        int minDist = Math.max(10, Math.min(config.getMinSpawnDistance(), radius / 2));
        Random random = new Random();
        Location loc = randomSpawn(radius, minDist, new ArrayList<>(), random);
        player.teleport(loc);
    }

    private Location randomSpawn(int radius, int minDist, List<Location> taken, Random random) {
        Location fallback = null;
        for (int attempt = 0; attempt < 50; attempt++) {
            double angle = random.nextDouble() * Math.PI * 2;
            double dist = minDist + random.nextDouble() * (radius - minDist);
            int x = (int) Math.round(Math.cos(angle) * dist);
            int z = (int) Math.round(Math.sin(angle) * dist);
            int y = world.getHighestBlockYAt(x, z) + 1;
            Location loc = new Location(world, x + 0.5, y, z + 0.5, random.nextFloat() * 360f, 0f);
            if (isFarEnough(loc, taken, minDist)) {
                return loc;
            }
            fallback = loc;
        }
        return fallback;
    }

    private boolean isFarEnough(Location loc, List<Location> taken, int minDist) {
        for (Location other : taken) {
            double dx = loc.getX() - other.getX();
            double dz = loc.getZ() - other.getZ();
            if (dx * dx + dz * dz < (double) minDist * minDist) {
                return false;
            }
        }
        return true;
    }

    // Empeche les joueurs de quitter la map de la partie.
    @EventHandler
    public void onPlayerChangedWorld(PlayerChangedWorldEvent event) {
        if (resetting || world == null || plugin.getGameManager().getPhase() == GamePhase.ENDED) {
            return;
        }
        if (!event.getPlayer().getWorld().equals(world)) {
            event.getPlayer().teleport(world.getSpawnLocation());
        }
    }

    // Désactive keepSpawnInMemory avant que Bukkit.createWorld() n'exécute
    // sa boucle de 625 chunks synchrones (skip total de la zone de spawn).
    @EventHandler
    public void onWorldInit(WorldInitEvent event) {
        if ("vuhc_world".equals(event.getWorld().getName())) {
            event.getWorld().setKeepSpawnInMemory(false);
        }
    }
}
