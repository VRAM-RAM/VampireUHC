package fr.vampireuhc.markers;

/**
 * Aura portee par un marqueur individuel.
 * L'aura globale d'un joueur (voir MarkerManager#computeAura) se deduit
 * du ratio marqueurs lumineux / obscurs, PAS de cette enum directement.
 */
public enum Aura {
    OBSCURE(-1),
    NEUTRE(0),
    LUMINEUSE(1);

    private final int weight;

    Aura(int weight) {
        this.weight = weight;
    }

    /** Poids utilisé pour calculer l'aura globale d'un joueur. */
    public int getWeight() {
        return weight;
    }
}
