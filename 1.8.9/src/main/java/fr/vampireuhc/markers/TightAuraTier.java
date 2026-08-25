package fr.vampireuhc.markers;
import fr.vampireuhc.markers.Aura;

public enum TightAuraTier {
    OBSCURE,
    LUMINEUSE,
    NEUTRE;

    public Aura toAura() {
        switch (this) {
            case OBSCURE:
                return Aura.OBSCURE;
            
            case LUMINEUSE:
                return Aura.LUMINEUSE;
            
            case NEUTRE:
                return Aura.NEUTRE;
            default:
                return Aura.NEUTRE;
        }
    }
}
