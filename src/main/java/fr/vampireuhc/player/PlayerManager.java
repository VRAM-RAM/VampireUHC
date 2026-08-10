package fr.vampireuhc.player;

import fr.vampireuhc.VampireUHC;
import org.bukkit.entity.Player;

import java.util.*;

public class PlayerManager {
    private final VampireUHC plugin;

    private final Map<UUID, VampireUHCPlayer> players = new HashMap<>();

    public PlayerManager(VampireUHC plugin) {
        this.plugin = plugin;
    }

    public VampireUHCPlayer register(Player player) {
        return players.computeIfAbsent(player.getUniqueId(),
            id -> new VampireUHCPlayer(id, player.getName()));
    }

    public VampireUHCPlayer get(UUID uuid) {
        return players.get(uuid);
    }

    public VampireUHCPlayer get(Player player) {
        return players.get(player.getUniqueId());
    }

    public Collection<VampireUHCPlayer> getAll() {
        return players.values();
    }

    public List<VampireUHCPlayer> getByCamp(Camp camp) {
        List<VampireUHCPlayer> result = new ArrayList<>();
        for (VampireUHCPlayer player: players.values()) {
            if (player.getCamp() == camp) {
                result.add(player);
            }
        }
        return result;
    }

    public void reset() { players.clear(); }
}

