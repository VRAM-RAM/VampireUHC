package fr.vampireuhc.roles;

import com.google.gson.*;
import fr.vampireuhc.VampireUHC;
import fr.vampireuhc.game.GamePhase;
import fr.vampireuhc.markers.Marker;
import fr.vampireuhc.markers.MarkerType;
import fr.vampireuhc.player.Camp;
import fr.vampireuhc.player.PlayerManager;
import fr.vampireuhc.player.VampireUHCPlayer;
import fr.vampireuhc.vampire_vote.VoteResult;
import java.io.*;
import java.util.*;

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
        RoleType.SAVIOR,
        RoleType.SOUL_WEIGHTER,
        RoleType.WEAVER,
        RoleType.CARTOGRAPHER,
        RoleType.SAND_MERCHANT,
        RoleType.ARCHER
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

    // pour le debug seulement :

    public void setRoleFromType(VampireUHCPlayer player, RoleType type) {
        Role role = createRoleFromType(type, player);
        player.setRole(role);
        if (role != null) {
            role.onAssign(player);
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

    // Crée une instance d'un rôle à partir d'un type de Rôle :

    public Role createRoleFromType(RoleType type, VampireUHCPlayer player) {
        switch (type) {
            case RoleType.MASTER:
                return new MasterRole();

            case RoleType.WEAVER:
                return new WeaverRole(player);

            case RoleType.ARCHER:
                return new ArcherRole();

            case RoleType.CARTOGRAPHER:
                return new CartographerRole(player);

            case RoleType.SOUL_WEIGHTER:
                return new SoulweigherRole();

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

            case RoleType.SAND_MERCHANT:
                return new SandMerchantRole(player);

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

        // État de la partie (phase, temps écoulé) pour la reprise après redémarrage.
        gameState.addProperty("phase", plugin.getGameManager().getPhase().name());
        gameState.addProperty("gameStarted", plugin.getGameManager().isGameStarted());
        gameState.addProperty("elapsedMinutes", plugin.getGameManager().getElapsedMinutes());
        
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

    // Résultat d'une restauration : phase et minute à reprendre.
    public record LoadedGameState(GamePhase phase, int elapsedMinutes) {}

    /**
     * Relit game-state.json et restaure joueurs, rôles, marqueurs et votes.
     * Retourne la phase et la minute de jeu à reprendre, ou null si rien à restaurer.
     */
    public LoadedGameState loadGameFromJson(String filePath) {
        File file = new File(filePath);
        if (!file.exists()) {
            return null;
        }

        try (Reader reader = new FileReader(file)) {
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();

            GamePhase phase = GamePhase.valueOf(root.get("phase").getAsString());
            int elapsedMinutes = root.has("elapsedMinutes") ? root.get("elapsedMinutes").getAsInt() : 0;
            boolean gameStarted = root.has("gameStarted") && root.get("gameStarted").getAsBoolean();

            if (phase == GamePhase.ENDED || !gameStarted) {
                plugin.getLogger().info("Partie terminée ou jamais lancée, restauration ignorée.");
                file.delete();
                return null;
            }

            playerManager.reset();
            plugin.getMarkerManager().clearMarkersOnAll();

            JsonArray playersArray = root.getAsJsonArray("players");
            for (JsonElement element : playersArray) {
                JsonObject obj = element.getAsJsonObject();
                UUID uuid = UUID.fromString(obj.get("uuid").getAsString());
                String name = obj.get("name").getAsString();
                VampireUHCPlayer player = new VampireUHCPlayer(uuid, name);
                playerManager.add(player);

                String campName = obj.has("camp") ? obj.get("camp").getAsString() : "NONE";
                if (!campName.equals("NONE")) {
                    player.setCamp(Camp.valueOf(campName));
                }

                String roleName = obj.has("role") ? obj.get("role").getAsString() : "NONE";
                if (!roleName.equals("NONE")) {
                    RoleType type = roleTypeFromName(roleName);
                    if (type != null) {
                        Role role = createRoleFromType(type, player);
                        if (role != null) {
                            player.setRole(role);
                            role.onAssign(player);
                        }
                    }
                }

                player.setVampireListRevealed(obj.get("vampireListRevealed").getAsBoolean());
                if (obj.get("canVoteVampireMark").getAsBoolean()) {
                    player.setVampireVote();
                }
                if (obj.get("infected").getAsBoolean()) {
                    player.infect();
                }
                if (!obj.get("alive").getAsBoolean()) {
                    player.setDead();
                }

                JsonArray markersArray = obj.getAsJsonArray("markers");
                for (JsonElement markerEl : markersArray) {
                    JsonObject m = markerEl.getAsJsonObject();
                    MarkerType type = MarkerType.valueOf(m.get("type").getAsString());
                    UUID source = m.get("source").isJsonNull() ? null : UUID.fromString(m.get("source").getAsString());
                    long placedAt = m.has("placedAt") ? m.get("placedAt").getAsLong() : System.currentTimeMillis();
                    plugin.getMarkerManager().addMarker(uuid, type, source, placedAt);
                }
            }

            if (root.has("vote")) {
                JsonObject voteObj = root.getAsJsonObject("vote");
                boolean open = voteObj.get("open").getAsBoolean();
                int markedPlayerCount = voteObj.get("markedPlayerCount").getAsInt();

                Map<UUID, Integer> votes = new HashMap<>();
                JsonObject votesObj = voteObj.getAsJsonObject("votes");
                for (String id : votesObj.keySet()) {
                    votes.put(UUID.fromString(id), votesObj.get(id).getAsInt());
                }

                Set<UUID> marked = new HashSet<>();
                for (JsonElement idEl : voteObj.getAsJsonArray("markedPlayers")) {
                    marked.add(UUID.fromString(idEl.getAsString()));
                }

                VoteResult.Tie pendingTie = null;
                if (voteObj.has("pendingTie") && !voteObj.get("pendingTie").isJsonNull()) {
                    List<UUID> tied = new ArrayList<>();
                    for (JsonElement idEl : voteObj.getAsJsonArray("pendingTie")) {
                        tied.add(UUID.fromString(idEl.getAsString()));
                    }
                    pendingTie = new VoteResult.Tie(tied);
                }

                plugin.getVoteManager().restore(open, votes, marked, markedPlayerCount, pendingTie);
            }

            plugin.getLogger().info("Partie restaurée depuis " + filePath + " (phase " + phase + ", minute " + elapsedMinutes + ").");
            return new LoadedGameState(phase, elapsedMinutes);
        } catch (Exception e) {
            plugin.getLogger().severe("Impossible de restaurer la partie : " + e.getMessage());
            return null;
        }
    }

    private RoleType roleTypeFromName(String name) {
        switch (name) {
            case "Maître": return RoleType.MASTER;
            case "Sbire": return RoleType.VAMPIRE_MINION;
            case "Salvateur": return RoleType.SAVIOR;
            case "Paladin": return RoleType.PALADIN;
            case "Cupidon": return RoleType.CUPIDON;
            case "Apprenti Chasseur": return RoleType.APPRENTICE_SLAYER;
            case "Gremlin": return RoleType.GREMLIN;
            case "Peseuse d'âmes": return RoleType.SOUL_WEIGHTER;
            case "Tisseur": return RoleType.WEAVER;
            case "Cartographe": return RoleType.CARTOGRAPHER;
            case "Marchand de Sable": return RoleType.SAND_MERCHANT;
            case "Archer": return RoleType.ARCHER;
            default: return null;
        }
    }
    
}
