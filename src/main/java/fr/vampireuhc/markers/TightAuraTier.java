package fr.vampireuhc.markers;
import fr.vampireuhc.markers.Aura;

public enum TightAuraTier {
    OBSCURE,
    LUMINEUSE,
    NEUTRE;

    public Aura toAura() {
        switch (this) {
            case TightAuraTier.OBSCURE:
                return Aura.OBSCURE;
            
            case TightAuraTier.LUMINEUSE:
                return Aura.LUMINEUSE;
            
            case TightAuraTier.NEUTRE:
                return Aura.NEUTRE;
            default:
                return Aura.NEUTRE;
        }
    }
}
