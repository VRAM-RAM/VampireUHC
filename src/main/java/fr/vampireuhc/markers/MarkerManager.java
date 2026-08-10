package fr.vampireuhc.markers;

import fr.vampireuhc.VampireUHC;

import java.util.*;

public class MarkerManager {
    
    private final VampireUHC plugin;
    private final Map<UUID, List<Marker>> markersByPlayer = new HashMap<>();

    public MarkerManager(VampireUHC plugin) {
        this.plugin = plugin;
    }

    public Marker addMarker(UUID target, MarkerType type, UUID source) {
        return addMarker(target, type, source, System.currentTimeMillis());
    }

    public Marker addMarker(UUID target, MarkerType type, UUID source, long placedAtMillis) {
        Marker marker = new Marker(type, source, placedAtMillis);
        markersByPlayer.computeIfAbsent(target, k -> new ArrayList<>()).add(marker);
        return marker;
    }

    public boolean hasMarker(UUID target, MarkerType type) {
        return getMarkers(target, type).size() > 0;
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
        // On récupère les marqueurs du premier joueur
        List<Marker> markers_of_target_1 = markersByPlayer.get(target_1);
        // On récupère les marqueurs du second joueur
        List<Marker> markers_of_target_2 = markersByPlayer.get(target_2);

        // Puis on clear leurs marqueurs :
        markersByPlayer.remove(target_1);
        markersByPlayer.remove(target_2);

        //Et on ajoute les marqueurs de l'autre (on intervertit) :

        markersByPlayer.putIfAbsent(target_1, markers_of_target_2);
        markersByPlayer.putIfAbsent(target_2, markers_of_target_1);
    }

    public int countMarkers(UUID target, MarkerType type) {
        return getMarkers(target, type).size();
    }

    // Retire le marker si possible
    public boolean removeMarker(UUID target, Marker marker) {
        List<Marker> list = markersByPlayer.get(target);
        if (list == null) {
            return false;
        }
        return list.remove(marker);
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
