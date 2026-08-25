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
import fr.vampireuhc.vampire_vote.VampireVoteManager;

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
        RoleType.GRAVE_DIGGER,
        RoleType.WHITE_LADY,
        RoleType.BANSHEE,
        RoleType.EXORCIST,
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
            case VILLAGEOIS:
                return villagerRoles;
            case VAMPIRE:
                return vampiresRoles;
            case SOLO:
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
            role.onAssign(player, false);
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

            // Appel de onAssign pour initialiser le rôle (assignation fraîche, pas une restauration).
            if (role != null) {
                role.onAssign(player, false);
            }
        }
    }

    // Crée une instance d'un rôle à partir d'un type de Rôle :

    public Role createRoleFromType(RoleType type, VampireUHCPlayer player) {
        switch (type) {
            case MASTER:
                return new MasterRole();

            case GRAVE_DIGGER:
                return new GravediggerRole();

            case WEAVER:
                return new WeaverRole(player);

            case WHITE_LADY:
                return new WhiteLadyRole();

            case ARCHER:
                return new ArcherRole();

            case EXORCIST:
                return new ExorcistRole();

            case CARTOGRAPHER:
                return new CartographerRole(player);

            case SOUL_WEIGHTER:
                return new SoulweigherRole();

            case VAMPIRE_MINION:
                return new VampireMinion(player);
            
            case SAVIOR:
                return new SaviorRole(player);

            case CUPIDON:
                return new CupidonRole(player);

            case PALADIN:
                return new PaladinRole(player);

            case APPRENTICE_SLAYER:
                return new ApprenticeSlayer(player);

            case GREMLIN:
                return new GremlinRole(player);

            case SAND_MERCHANT:
                return new SandMerchantRole(player);

            case BANSHEE:
                return new BansheeRole();

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

            // État spécifique au rôle (gates "une fois par épisode").
            saveRoleState(playerObj, player.getRole());

            playersArray.add(playerObj);
        }
        
        gameState.add("players", playersArray);

        // État des votes vampires (sauvegarde pour reprise en cas de reload).
        // Les joueurs marqués ne sont pas persistés : ils se dérivent des marqueurs.
        JsonObject voteObj = new JsonObject();
        VampireVoteManager voteManager = fr.vampireuhc.VampireUHC.getInstance().getVoteManager();
        voteObj.addProperty("open", voteManager.isVoteOpen());

        JsonObject votesObj = new JsonObject();
        voteManager.getVotesCopy().forEach((id, count) -> votesObj.addProperty(id.toString(), count));
        voteObj.add("votes", votesObj);

        JsonArray votersArray = new JsonArray();
        voteManager.getVotersCopy().forEach(id -> votersArray.add(new com.google.gson.JsonPrimitive(id.toString())));
        voteObj.add("voters", votersArray);

        if (voteManager.getPendingTie() != null) {
            JsonArray tieArray = new JsonArray();
            voteManager.getPendingTie().tiedPlayers().forEach(id -> tieArray.add(new com.google.gson.JsonPrimitive(id.toString())));
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
    public static final class LoadedGameState {
        private final GamePhase phase;
        private final int elapsedMinutes;

        public LoadedGameState(GamePhase phase, int elapsedMinutes) {
            this.phase = phase;
            this.elapsedMinutes = elapsedMinutes;
        }

        public GamePhase phase() {
            return phase;
        }

        public int elapsedMinutes() {
            return elapsedMinutes;
        }
    }

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
            JsonObject root = new JsonParser().parse(reader).getAsJsonObject();

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

            if (!root.has("players") || !root.get("players").isJsonArray()) {
                plugin.getLogger().warning("Sauvegarde sans liste de joueurs, restauration ignorée.");
                return null;
            }

            JsonArray playersArray = root.getAsJsonArray("players");
            for (JsonElement element : playersArray) {
                JsonObject obj = element.getAsJsonObject();
                UUID uuid = UUID.fromString(obj.get("uuid").getAsString());
                String name = obj.has("name") && !obj.get("name").isJsonNull()
                        ? obj.get("name").getAsString()
                        : uuid.toString();
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
                            // onAssign est différé après le chargement de TOUS les
                            // marqueurs (voir boucle ci-dessous).
                        }
                    }
                }

                // Lecture blindée : une clé absente (save legacy/éditée) retombe sur
                // une valeur par défaut au lieu d'avorter toute la restauration.
                player.setVampireListRevealed(getBool(obj, "vampireListRevealed", false));
                if (getBool(obj, "canVoteVampireMark", false)) {
                    player.setVampireVote();
                }
                if (getBool(obj, "infected", false)) {
                    player.infect();
                }
                if (!getBool(obj, "alive", true)) {
                    player.setDead();
                }

                JsonArray markersArray = obj.has("markers") && obj.get("markers").isJsonArray()
                        ? obj.getAsJsonArray("markers")
                        : new JsonArray();
                for (JsonElement markerEl : markersArray) {
                    JsonObject m = markerEl.getAsJsonObject();
                    MarkerType type = MarkerType.valueOf(m.get("type").getAsString());
                    UUID source = m.get("source").isJsonNull() ? null : UUID.fromString(m.get("source").getAsString());
                    long placedAt = m.has("placedAt") ? m.get("placedAt").getAsLong() : System.currentTimeMillis();
                    plugin.getMarkerManager().addMarker(uuid, type, source, placedAt);
                }

                restoreRoleState(obj, player.getRole());
            }

            // Tous les marqueurs sont chargés : on peut maintenant initialiser les
            // rôles. Sinon Cupidon (assigné avant ses cibles dans l'ordre JSON) ne
            // voit pas les marques Amour restaurées et ré-arme son fallback 60s,
            // qui crée une 2e paire d'amoureux à l'expiration. restoring=true :
            // les effets one-shot (kits...) ne sont pas réappliqués.
            for (VampireUHCPlayer p : playerManager.getAll()) {
                if (p.getRole() != null) {
                    p.getRole().onAssign(p, true);
                }
                // La Dame Blanche attendait peut-être un tueur restauré après elle.
                if (p.getRole() instanceof WhiteLadyRole) {
                    ((WhiteLadyRole) p.getRole()).resolvePendingReferences();
                }
            }

            if (root.has("vote") && root.get("vote").isJsonObject()) {
                JsonObject voteObj = root.getAsJsonObject("vote");
                boolean open = voteObj.has("open") && voteObj.get("open").getAsBoolean();

                Map<UUID, Integer> votes = new HashMap<>();
                if (voteObj.has("votes") && voteObj.get("votes").isJsonObject()) {
                    JsonObject votesObj = voteObj.getAsJsonObject("votes");
                    for (Map.Entry<String, JsonElement> entry : votesObj.entrySet()) {
                        String id = entry.getKey();
                        votes.put(UUID.fromString(id), votesObj.get(id).getAsInt());
                    }
                }

                Set<UUID> voters = new HashSet<>();
                if (voteObj.has("voters") && voteObj.get("voters").isJsonArray()) {
                    for (JsonElement idEl : voteObj.getAsJsonArray("voters")) {
                        voters.add(UUID.fromString(idEl.getAsString()));
                    }
                }

                VoteResult.Tie pendingTie = null;
                if (voteObj.has("pendingTie") && !voteObj.get("pendingTie").isJsonNull()) {
                    List<UUID> tied = new ArrayList<>();
                    for (JsonElement idEl : voteObj.getAsJsonArray("pendingTie")) {
                        tied.add(UUID.fromString(idEl.getAsString()));
                    }
                    pendingTie = new VoteResult.Tie(tied);
                }

                plugin.getVoteManager().restore(open, votes, voters, pendingTie);
            }

            plugin.getLogger().info("Partie restaurée depuis " + filePath + " (phase " + phase + ", minute " + elapsedMinutes + ").");
            return new LoadedGameState(phase, elapsedMinutes);
        } catch (Exception e) {
            plugin.getLogger().severe("Impossible de restaurer la partie : " + e.getMessage());
            return null;
        }
    }

    // --- État spécifique aux rôles : gates "une fois par épisode" ---

    private void saveRoleState(JsonObject obj, Role role) {
        if (role instanceof MasterRole) {
            MasterRole masterRole = (MasterRole) role;
            obj.addProperty("masterLastMarkedEpisode", masterRole.getLastMarkedEpisode());
        } else if (role instanceof SaviorRole) {
            SaviorRole savior = (SaviorRole) role;
            obj.addProperty("saviorLastEpisode", savior.getLastAppliedEpisode());
            UUID lastTarget = savior.getLastAppliedUuid();
            if (lastTarget != null) {
                obj.addProperty("saviorLastTarget", lastTarget.toString());
            }
        } else if (role instanceof GremlinRole) {
            GremlinRole gremlin = (GremlinRole) role;
            obj.addProperty("gremlinSwitchEpisode", gremlin.getLastSwitchEpisode());
            obj.addProperty("gremlinDrainEpisode", gremlin.getLastDrainEpisode());
        } else if (role instanceof SoulweigherRole) {
            SoulweigherRole soulweigher = (SoulweigherRole) role;
            obj.addProperty("soulweigherLastEpisode", soulweigher.getLastWeightEpisode());
        } else if (role instanceof WhiteLadyRole) {
            WhiteLadyRole whiteLady = (WhiteLadyRole) role;
            obj.addProperty("wlIsSolo", whiteLady.isSoloState());
            obj.addProperty("wlKilledByVampire", whiteLady.wasKilledByVampire());
            obj.addProperty("wlKilledKiller", whiteLady.hasKilledKiller());
            UUID killer = whiteLady.getKillerUuid();
            if (killer != null) {
                obj.addProperty("wlKillerUuid", killer.toString());
            }
        } else if (role instanceof CartographerRole && ((CartographerRole) role).isBeaconApplied()) {
            CartographerRole cartographer = (CartographerRole) role;
            obj.addProperty("cartoBeaconApplied", true);
            obj.addProperty("cartoBeaconEpisode", cartographer.getBeaconEpisode());
            obj.addProperty("cartoBeaconWorld", cartographer.getBeaconWorld());
            obj.addProperty("cartoBeaconX", cartographer.getBeaconX());
            obj.addProperty("cartoBeaconY", cartographer.getBeaconY());
            obj.addProperty("cartoBeaconZ", cartographer.getBeaconZ());
            JsonArray recorded = new JsonArray();
            for (UUID id : cartographer.getRecordedPlayers()) {
                recorded.add(new com.google.gson.JsonPrimitive(id.toString()));
            }
            obj.add("cartoRecorded", recorded);
        }
    }

    private void restoreRoleState(JsonObject obj, Role role) {
        if (role instanceof MasterRole) {
            ((MasterRole) role).restoreState(getInt(obj, "masterLastMarkedEpisode", -1));
        } else if (role instanceof SaviorRole) {
            SaviorRole savior = (SaviorRole) role;
            UUID lastTarget = null;
            if (obj.has("saviorLastTarget") && !obj.get("saviorLastTarget").isJsonNull()) {
                try {
                    lastTarget = UUID.fromString(obj.get("saviorLastTarget").getAsString());
                } catch (IllegalArgumentException ignored) {
                    // UUID invalide dans la sauvegarde : on repart sans cible.
                }
            }
            savior.restoreState(getInt(obj, "saviorLastEpisode", -1), lastTarget);
        } else if (role instanceof GremlinRole) {
            ((GremlinRole) role).restoreState(
                    getInt(obj, "gremlinSwitchEpisode", -1),
                    getInt(obj, "gremlinDrainEpisode", -1));
        } else if (role instanceof SoulweigherRole) {
            ((SoulweigherRole) role).restoreState(getInt(obj, "soulweigherLastEpisode", -1));
        } else if (role instanceof WhiteLadyRole) {
            WhiteLadyRole whiteLady = (WhiteLadyRole) role;
            UUID killer = null;
            if (obj.has("wlKillerUuid") && !obj.get("wlKillerUuid").isJsonNull()) {
                try {
                    killer = UUID.fromString(obj.get("wlKillerUuid").getAsString());
                } catch (IllegalArgumentException ignored) {
                    // UUID invalide : pas de tueur restauré.
                }
            }
            whiteLady.restoreState(
                    getBool(obj, "wlIsSolo", false),
                    getBool(obj, "wlKilledByVampire", false),
                    getBool(obj, "wlKilledKiller", false),
                    killer);
        } else if (role instanceof CartographerRole && getBool(obj, "cartoBeaconApplied", false)) {
            CartographerRole cartographer = (CartographerRole) role;
            Set<UUID> recorded = new HashSet<>();
            if (obj.has("cartoRecorded") && obj.get("cartoRecorded").isJsonArray()) {
                for (JsonElement idEl : obj.getAsJsonArray("cartoRecorded")) {
                    try {
                        recorded.add(UUID.fromString(idEl.getAsString()));
                    } catch (IllegalArgumentException ignored) {
                        // Entrée invalide : on saute ce joueur.
                    }
                }
            }
            String worldName = obj.has("cartoBeaconWorld") && !obj.get("cartoBeaconWorld").isJsonNull()
                    ? obj.get("cartoBeaconWorld").getAsString()
                    : null;
            cartographer.restoreState(
                    getInt(obj, "cartoBeaconEpisode", 0),
                    true,
                    worldName,
                    obj.has("cartoBeaconX") ? obj.get("cartoBeaconX").getAsDouble() : 0,
                    obj.has("cartoBeaconY") ? obj.get("cartoBeaconY").getAsDouble() : 0,
                    obj.has("cartoBeaconZ") ? obj.get("cartoBeaconZ").getAsDouble() : 0,
                    recorded);
        }
    }

    private static boolean getBool(JsonObject obj, String key, boolean def) {
        return obj.has(key) && !obj.get(key).isJsonNull() ? obj.get(key).getAsBoolean() : def;
    }

    private static int getInt(JsonObject obj, String key, int def) {
        return obj.has(key) && !obj.get(key).isJsonNull() ? obj.get(key).getAsInt() : def;
    }

    private RoleType roleTypeFromName(String name) {
        switch (name) {
            case "Maître": return RoleType.MASTER;
            case "Sbire": return RoleType.VAMPIRE_MINION;
            case "Salvateur": return RoleType.SAVIOR;
            case "Paladin": return RoleType.PALADIN;
            case "Cupidon": return RoleType.CUPIDON;
            case "Apprenti Assassin": return RoleType.APPRENTICE_SLAYER;
            case "Gremlin": return RoleType.GREMLIN;
            case "Peseuse d'âmes": return RoleType.SOUL_WEIGHTER;
            case "Tisseur": return RoleType.WEAVER;
            case "Cartographe": return RoleType.CARTOGRAPHER;
            case "Marchand de Sable": return RoleType.SAND_MERCHANT;
            case "Archer": return RoleType.ARCHER;
            case "Fossoyeur": return RoleType.GRAVE_DIGGER;
            case "Dame Blanche": return RoleType.WHITE_LADY;
            case "Banshee": return RoleType.BANSHEE;
            case "Exorciste": return RoleType.EXORCIST;
            default: return null;
        }
    }
    
}
