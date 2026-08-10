package fr.vampireuhc.markers;

import java.util.UUID;

/**
 * Instance concrete d'un marqueur pose sur un joueur.
 * Plusieurs marqueurs du meme type peuvent coexister sur un joueur
 * (ex: plusieurs marques Maitre qui s'accumulent avant l'infection).
 */
public class Marker {
    
    private final MarkerType type;
    private final UUID source; // Qui a posé le marqueur
    private final long placedAtMillis;

    public Marker(MarkerType type, UUID source) {
        this(type, source, System.currentTimeMillis());
    }

    public Marker(MarkerType type, UUID source, long placedAtMillis) {
        this.type = type;
        this.source = source;
        this.placedAtMillis = placedAtMillis;
    }

    public MarkerType getType() {
        return type;
    }

    public Aura getAura() {
        return type.getAura();
    }

    public UUID getSource() {
        return source;
    }

    public long getPlacedAtMillis() {
        return placedAtMillis;
    }
}
