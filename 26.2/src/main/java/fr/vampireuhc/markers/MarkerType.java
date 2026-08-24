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
    SABLE_NEUTRE(Aura.NEUTRE);

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
                return "Marque maître";
            case MARQUE_VAMPIRE:
                return "Marque vampire";
            case LUMINEUX:
                return "Marque lumineuse.";
            case SABLE_LUMINEUX:
                return "Marque sable";
            case SABLE_NEUTRE:
                return "Marque sable";
            case SALVATION:
                return "Marque de la salvation";
            case AMOUR:
                return "Marque de l'amour";
            case FIL:
                return "Marque fil";
            default:
                return "Marque inconnue";
        }
    } 

    private static final MiniMessage mm = MiniMessage.miniMessage();


    public Component toComponent() {
        return switch (this) {
            case MARQUE_MAITRE, MARQUE_VAMPIRE ->
                mm.deserialize("<dark_red>" + this + "</dark_red>\n");

            case LUMINEUX, SALVATION ->
                mm.deserialize("<yellow>" + this + "</yellow>\n");

            case SABLE_LUMINEUX, SABLE_NEUTRE, FIL ->
                mm.deserialize("<gray>" + this + "</gray>\n");

            case AMOUR ->
                mm.deserialize("<purple>" + this + "</purple>\n");
        };
    }
}
