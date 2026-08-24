package fr.vampireuhc.game;
 
import java.util.ArrayList;
import java.util.List;
import fr.vampireuhc.player.VampireUHCPlayer;

public class GameState {
    private long timestamp;
    private List<VampireUHCPlayer> players = new ArrayList<>();

    public long getTimestamp() { return timestamp; }
    
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public List<VampireUHCPlayer> getPlayers() { return players; }

    public void setPlayers(List<VampireUHCPlayer> players) { this.players = players; }
}
    