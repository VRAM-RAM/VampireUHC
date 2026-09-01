package fr.vampireuhc.markers;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;

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
    FIL(Aura.NEUTRE),
    SABLE_LUMINEUX(Aura.LUMINEUSE),
    SABLE_NEUTRE(Aura.NEUTRE),

    // Variantes posées par le Doppelganger (Sosie) lorsqu'il copie un rôle.
    // Même rendu que leur base, mais identification/sémantique distincte.
    MARQUE_MAITRE_DOPPELGANGER(Aura.NEUTRE), // Nerf : neutre, jamais d'infection.
    SALVATION_DOPPELGANGER(Aura.LUMINEUSE),
    LUMINEUX_DOPPELGANGER(Aura.LUMINEUSE),
    FIL_DOPPELGANGER(Aura.NEUTRE),
    SABLE_LUMINEUX_DOPPELGANGER(Aura.LUMINEUSE),
    SABLE_NEUTRE_DOPPELGANGER(Aura.NEUTRE);

    private final Aura aura;

    MarkerType(Aura aura) {
        this.aura = aura;
    }

    public Aura getAura() {
        return aura;
    }

    public String toString() {
        switch (this) {
            case MARQUE_MAITRE:
            case MARQUE_MAITRE_DOPPELGANGER:
                return "Marque maître";
            case MARQUE_VAMPIRE:
                return "Marque vampire";
            case LUMINEUX:
            case LUMINEUX_DOPPELGANGER:
                return "Marque lumineuse.";
            case SABLE_LUMINEUX:
            case SABLE_NEUTRE:
            case SABLE_LUMINEUX_DOPPELGANGER:
            case SABLE_NEUTRE_DOPPELGANGER:
                return "Marque sable";
            case SALVATION:
            case SALVATION_DOPPELGANGER:
                return "Marque de la salvation";
            case AMOUR:
                return "Marque de l'amour";
            case FIL:
            case FIL_DOPPELGANGER:
                return "Marque fil";
            default:
                return "Marque inconnue";
        }
    } 

    private static final MiniMessage mm = MiniMessage.miniMessage();


    public Component toComponent() {
        return switch (this) {
            case MARQUE_MAITRE, MARQUE_MAITRE_DOPPELGANGER, MARQUE_VAMPIRE ->
                mm.deserialize("<dark_red>" + this + "</dark_red>\n");

            case LUMINEUX, LUMINEUX_DOPPELGANGER, SALVATION, SALVATION_DOPPELGANGER ->
                mm.deserialize("<yellow>" + this + "</yellow>\n");

            case SABLE_LUMINEUX, SABLE_NEUTRE, SABLE_LUMINEUX_DOPPELGANGER, SABLE_NEUTRE_DOPPELGANGER, FIL, FIL_DOPPELGANGER ->
                mm.deserialize("<gray>" + this + "</gray>\n");

            case AMOUR ->
                mm.deserialize("<purple>" + this + "</purple>\n");
        };
    }
}
