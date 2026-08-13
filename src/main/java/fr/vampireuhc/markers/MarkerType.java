package fr.vampireuhc.markers;

/* Enum des types de marqueurs */
public enum MarkerType {
    
    // Vampires :
    MARQUE_VAMPIRE(Aura.OBSCURE),
    MARQUE_MAITRE(Aura.OBSCURE),

    //Village 
    SALVATION(Aura.LUMINEUSE),
    LUMINEUX(Aura.LUMINEUSE),
    
    // Le reste :
    AMOUR(Aura.NEUTRE),
    FIL(Aura.NEUTRE);

    private final Aura aura;

    MarkerType(Aura aura) {
        this.aura = aura;
    }

    public Aura getAura() {
        return aura;
    }
}
