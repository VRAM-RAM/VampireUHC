package fr.vampireuhc.roles;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import fr.vampireuhc.VampireUHC;
import fr.vampireuhc.game.GamePhase;
import fr.vampireuhc.markers.Marker;
import fr.vampireuhc.player.Camp;
import fr.vampireuhc.player.PlayerManager;
import fr.vampireuhc.player.VampireUHCPlayer;
import fr.vampireuhc.roles.RoleType;
import java.io.*;
import java.lang.reflect.Type;
import java.util.*;
import java.util.stream.Collectors;

public class RoleManager {  
    private final VampireUHC plugin;
    private final PlayerManager playerManager;

    // Les trois camps (et les rôles leur appartenant)
    private List<RoleType> vampiresRoles = Arrays.asList(
        RoleType.MASTER,
        RoleType.VAMPIRE_MINION
    );

    private List<RoleType> villagerRoles = Arrays.asList(
        RoleType.PALADIN,
        RoleType.CUPIDON,
        RoleType.SAVIOR
    );

    private List<RoleType> soloroles = Arrays.asList(
        RoleType.APPRENTICE_SLAYER,
        RoleType.GREMLIN
    );

    // RoleManager :

    public RoleManager(VampireUHC plugin, PlayerManager manager) {
        this.plugin = plugin;
        this.playerManager = manager;
        
    }

    // Helpers publics :


    // Retourne la liste des rôles appartenant à un camp
    public List<RoleType> getPlayableRolesByCamp(Camp camp) {
        switch (camp) {
            case Camp.VILLAGEOIS:
                return villagerRoles;
            case Camp.VAMPIRE:
                return vampiresRoles;
            case Camp.SOLO:
                return soloroles;
            default:
                return new ArrayList<>();
        }
    }

    // Récupère et retourne le rôle d'un joueur
    public Role getPlayerRole(UUID id) {
        VampireUHCPlayer player = playerManager.get(id);
        return player != null ? player.getRole() : null;
    }

    /**
     * Attribue un rôle précis à chaque joueur après que les camps ont été répartis.
     */
    public void assignRolesToPlayers() {
        List<VampireUHCPlayer> allPlayers = new ArrayList<>(playerManager.getAll());
        
        // Séparer par camp
        List<VampireUHCPlayer> vampires = playerManager.getByCamp(Camp.VAMPIRE);
        List<VampireUHCPlayer> villagers = playerManager.getByCamp(Camp.VILLAGEOIS);
        List<VampireUHCPlayer> solitaires = playerManager.getByCamp(Camp.SOLO);

        // Assigner les rôles pour chaque camp
        assignRolesToGroup(villagers, villagerRoles);
        assignRolesToGroup(vampires, vampiresRoles);
        assignRolesToGroup(solitaires, soloroles);
        
        plugin.getLogger().info("Rôles attribués : " + vampires.size() 
            + " vampires, " + villagers.size() + " villageois, " 
            + solitaires.size() + " solitaires");
    }

    // Helpers internes :

    /**
     * Attribue des rôles aléatoires à un groupe de joueurs.
     */
    private void assignRolesToGroup(List<VampireUHCPlayer> players, List<RoleType> availableRoles) {
        if (players.isEmpty()) return;
        
        // Créer une liste avec chaque rôle répété pour correspondre au nombre de joueurs
        List<RoleType> rolePool = new ArrayList<>();
        for (int i = 0; i < players.size() / availableRoles.size(); i++) {
            rolePool.addAll(availableRoles);
        }
        
        // Si le pool est trop petit, ajouter des rôles aléatoires
        while (rolePool.size() < players.size()) {
            rolePool.add(availableRoles.get(new Random().nextInt(availableRoles.size())));
        }
        
        // Mélanger et attribuer
        Collections.shuffle(rolePool);
        
        for (int i = 0; i < players.size(); i++) {
            VampireUHCPlayer player = players.get(i);
            RoleType roleType = rolePool.get(i);
            
            Role role = createRoleFromType(roleType, player);
            player.setRole(role);
            
            // Appel de onAssign pour initialiser le rôle
            if (role != null) {
                role.onAssign(player);
            }
        }
    }

    private void loadRolesConfig() {
        // TODO: Charger depuis config.yml quand elle sera mise à jour
        plugin.getLogger().info("Configuration des rôles chargée par défaut");
    }

    // Crée une instance d'un rôle à partir d'un type de Rôle :

    private Role createRoleFromType(RoleType type, VampireUHCPlayer player) {
        switch (type) {
            case RoleType.MASTER:
                return new MasterRole();

            case RoleType.VAMPIRE_MINION:
                return new VampireMinion(player);
            
            case RoleType.SAVIOR:
                return new SaviorRole(player);

            case RoleType.CUPIDON:
                return new CupidonRole(player);

            case RoleType.PALADIN:
                return new PaladinRole(player);

            case RoleType.APPRENTICE_SLAYER:
                return new ApprenticeSlayer(player);

            case RoleType.GREMLIN:
                return new GremlinRole(player);

            default:
                plugin.getLogger().warning("Rôle inconnu : " + type);
                return null;
        }
    }

    // Sérialization :

    // Sauvegarde l'état complet de la partie dans un .json
    public void saveGameToJson(String filePath) {
        try (FileWriter writer = new FileWriter(filePath)) {
            JsonObject gameState = buildGameStateObject();
            
            Gson gson = new GsonBuilder()
                .setPrettyPrinting()
                .disableHtmlEscaping()
                .create();
            
            gson.toJson(gameState, writer);
            plugin.getLogger().info("Partie sauvegardée : " + filePath);
        } catch (IOException e) {
            plugin.getLogger().severe("Erreur lors de la sauvegarde JSON : " + e.getMessage());
        }
    }

    private JsonObject buildGameStateObject() {
        JsonObject gameState = new JsonObject();
        
        // Métadonnées
        gameState.addProperty("pluginVersion", plugin.getDescription().getVersion());
        gameState.addProperty("timestamp", System.currentTimeMillis());
        
        // État des joueurs
        JsonArray playersArray = new JsonArray();
        for (VampireUHCPlayer player : playerManager.getAll()) {
            JsonObject playerObj = new JsonObject();
            
            playerObj.addProperty("uuid", player.getUuid().toString());
            playerObj.addProperty("name", player.getLastKnownName());
            playerObj.addProperty("camp", player.getCamp() != null ? player.getCamp().name() : "NONE");
            
            // Rôle (nom seulement, pas l'objet complet)
            if (player.getRole() != null) {
                playerObj.addProperty("role", player.getRole().getName());
            } else {
                playerObj.addProperty("role", "NONE");
            }
            
            playerObj.addProperty("vampireListRevealed", player.isVampireListRevealed());
            playerObj.addProperty("canVoteVampireMark", player.canVoteVampireMark());
            playerObj.addProperty("infected", player.isInfected());
            playerObj.addProperty("alive", player.isAlive());
            
            // Marqueurs du joueur
            JsonArray markersArray = new JsonArray();
            for (Marker marker : plugin.getMarkerManager().getMarkers(player.getUuid())) {
                JsonObject markerObj = new JsonObject();
                
                if (marker.getType() != null) {
                    markerObj.addProperty("type", marker.getType().name());
                } else {
                    markerObj.addProperty("type", "UNKNOWN");
                }
                
                if (marker.getSource() != null) {
                    markerObj.addProperty("source", marker.getSource().toString());
                } else {
                    markerObj.add("source", JsonNull.INSTANCE);
                }
                
                markerObj.addProperty("placedAt", marker.getPlacedAtMillis());
                
                markersArray.add(markerObj);
            }
            
            playerObj.add("markers", markersArray);
            playersArray.add(playerObj);
        }
        
        gameState.add("players", playersArray);

        // État des votes vampires (sauvegarde pour reprise en cas de reload)
        JsonObject voteObj = new JsonObject();
        var voteManager = fr.vampireuhc.VampireUHC.getInstance().getVoteManager();
        voteObj.addProperty("open", voteManager.isVoteOpen());
        voteObj.addProperty("markedPlayerCount", voteManager.getMarkedPlayerCount());

        JsonObject votesObj = new JsonObject();
        voteManager.getVotesCopy().forEach((id, count) -> votesObj.addProperty(id.toString(), count));
        voteObj.add("votes", votesObj);

        JsonArray markedPlayersArray = new JsonArray();
        voteManager.getMarkedPlayersCopy().forEach(id -> markedPlayersArray.add(id.toString()));
        voteObj.add("markedPlayers", markedPlayersArray);

        if (voteManager.getPendingTie() != null) {
            JsonArray tieArray = new JsonArray();
            voteManager.getPendingTie().tiedPlayers().forEach(id -> tieArray.add(id.toString()));
            voteObj.add("pendingTie", tieArray);
        }
        gameState.add("vote", voteObj);
        
        // Écrire dans le fichier
        Gson gson = new GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create();
        
        plugin.getLogger().info("État du jeu prêt pour sauvegarde JSON");
        
        return gameState;
    }
    
}
