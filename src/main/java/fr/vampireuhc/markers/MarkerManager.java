package fr.vampireuhc.markers;

import fr.vampireuhc.VampireUHC;

import java.util.*;

import org.bukkit.entity.Player;

public class MarkerManager {
    
    private final VampireUHC plugin;
    private final Map<UUID, List<Marker>> markersByPlayer = new HashMap<>();

    public MarkerManager(VampireUHC plugin) {
        this.plugin = plugin;
    }

    public Marker addMarker(UUID target, MarkerType type, UUID source) {
        return addMarker(target, type, source, System.currentTimeMillis());
    }

    public List<MarkerType> getMarkerTypesByPlayer(UUID target) {
        List<MarkerType> result = new ArrayList<>();
        List<Marker> markers = getMarkers(target);
        for (Marker m: markers) {
            result.add(m.getType());
        }
        return result;
    }


    public Marker addMarker(UUID target, MarkerType type, UUID source, long placedAtMillis) {
        Marker marker = new Marker(type, source, placedAtMillis);
        markersByPlayer.computeIfAbsent(target, k -> new ArrayList<>()).add(marker);
        return marker;
    }

    // Sans allocation : hot path (événements de dégâts, de mort, etc.).
    public boolean hasMarker(UUID target, MarkerType type) {
        for (Marker marker : markersByPlayer.getOrDefault(target, Collections.emptyList())) {
            if (marker.getType() == type) {
                return true;
            }
        }
        return false;
    }

    /**
     * Tente d'appliquer une marque en respectant la protection Salvation.
     * Si la cible porte une marque SALVATION et que le type est une marque
     * vampire ou maitre, la marque Salvation est consommée et la marque n'est
     * PAS appliquée (l'appelant peut tout de même annoncer un succès).
     */
    public boolean tryApplyMark(UUID target, MarkerType type, UUID source) {
        boolean hostile = type == MarkerType.MARQUE_VAMPIRE || type == MarkerType.MARQUE_MAITRE;
        if (hostile && hasMarker(target, MarkerType.SALVATION)) {
            clearMarkersOfType(target, MarkerType.SALVATION);
            return false;
        }
        addMarker(target, type, source);
        return true;
    }

    public void addMarkers(UUID target, List<Marker> markers) {
        markersByPlayer.computeIfAbsent(target, k -> new ArrayList<>()).addAll(markers);
    }

    public List<Marker> getMarkers(UUID target) {
        return markersByPlayer.getOrDefault(target, Collections.emptyList());
    }

    public List<Marker> getMarkers(UUID target, MarkerType type) {
        return getMarkers(target).stream()
                .filter(marker -> marker.getType() == type)
                .toList();
    }

    public void clearMarkersOfType(UUID target, MarkerType type) {
        List<Marker> list = markersByPlayer.get(target);

        if (list != null) {
            list.removeIf(marker -> marker.getType() == type);
        }
    }

    

    public void clearMarkersOfTypeOnAllPlayers(MarkerType type) {
        // Pour chaque joueur (id), on clear les marqueurs du type donné
        markersByPlayer.forEach((id, _) -> clearMarkersOfType(id, type));
    }

    public void SwitchMarkers(UUID target_1, UUID target_2) {
        // Normalisation systématique : un joueur sans marqueur reçoit une liste
        // vide, jamais null (sinon NPE en cascade dans tous les lecteurs, et une
        // sauvegarde corrompue au disable).
        List<Marker> markers_of_target_1 = markersByPlayer.getOrDefault(target_1, new ArrayList<>());
        List<Marker> markers_of_target_2 = markersByPlayer.getOrDefault(target_2, new ArrayList<>());

        // On intervertit les marqueurs :
        markersByPlayer.put(target_1, markers_of_target_2);
        markersByPlayer.put(target_2, markers_of_target_1);
    }

    // Sans allocation : hot path (événements de dégâts, de mort, etc.).
    public int countMarkers(UUID target, MarkerType type) {
        int count = 0;
        for (Marker marker : markersByPlayer.getOrDefault(target, Collections.emptyList())) {
            if (marker.getType() == type) {
                count++;
            }
        }
        return count;
    }

    // Retire le marker si possible
    public boolean removeMarker(UUID target, Marker marker) {
        List<Marker> list = markersByPlayer.get(target);
        if (list == null) {
            return false;
        }
        return list.remove(marker);
    }

    public ArrayList<Player> getPlayersThatHaveMarkerType(MarkerType type) {
        ArrayList<Player> players = new ArrayList<>();
        for (Map.Entry<UUID, List<Marker>> entry : markersByPlayer.entrySet()) {
            for (Marker marker : entry.getValue()) {
                if (marker.getType() == type) {
                    Player player = plugin.getServer().getPlayer(entry.getKey());
                    if (player != null) {
                        players.add(player);
                    }
                    break;
                }
            }
        }
        return players;
    }

    // Retire tous les marqueurs d'un type donné
    public void clearMarkersofType(UUID target, MarkerType type) {
        clearMarkersOfType(target, type);
    }

    public void clearMarkers(UUID target) {
        markersByPlayer.remove(target);
    }

    // Vide toutes les marques de tous les joueurs (nouvelle partie).
    public void clearMarkersOnAll() {
        markersByPlayer.clear();
    }

    public int computeAuraScore(UUID target) {
        int score = 0;
        for (Marker marker: getMarkers(target)) {
            score += marker.getAura().getWeight();
        }
        return score;
    }

    /* Renvoie le score d'aura */
    public AuraTier computeAuraTier(UUID target) {
        int score = computeAuraScore(target);
        if (score <= -3) return AuraTier.TRES_OBSCURE;
        if (score < 0) return AuraTier.OBSCURE;
        if (score == 0) return AuraTier.NEUTRE;
        if (score < 3) return AuraTier.LUMINEUSE;
        return AuraTier.TRES_LUMINEUSE;
    }

    public Collection<UUID> getAllPlayers() {
        return markersByPlayer.keySet();
    }
}
