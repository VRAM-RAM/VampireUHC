package fr.vampireuhc.markers;

/**
 * Tiers d'aura
 */
public enum AuraTier {
    TRES_OBSCURE,
    OBSCURE,
    NEUTRE,
    LUMINEUSE,
    TRES_LUMINEUSE; 

    public String toString() {
        switch (this) {
            case TRES_LUMINEUSE:
                return "très lumineuse";
            case LUMINEUSE:
                return "lumineuse";

            case NEUTRE:
                return "neutre";

            case TRES_OBSCURE:
                return "très obscure";

            case OBSCURE:
                return "obscure";
            
            default:
                return "inconnue";
        }
    }
    
    public TightAuraTier getTight() {
        switch (this) {
            case TRES_LUMINEUSE:
                return TightAuraTier.LUMINEUSE;
            case LUMINEUSE:
                return TightAuraTier.LUMINEUSE;

            case NEUTRE:
                return TightAuraTier.NEUTRE;

            case TRES_OBSCURE:
                return TightAuraTier.OBSCURE;

            case OBSCURE:
                return TightAuraTier.OBSCURE;
            
            default:
                return TightAuraTier.NEUTRE;
        }
    }
}

