package fr.vampireuhc.markers;

import org.bukkit.ChatColor;

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

    public String toLegacy() {
        switch (this) {
            case MARQUE_MAITRE:
            case MARQUE_VAMPIRE:
                return ChatColor.DARK_RED.toString() + this + "\n";
            case LUMINEUX:
            case SALVATION:
                return ChatColor.YELLOW.toString() + this + "\n";
            case SABLE_LUMINEUX:
            case SABLE_NEUTRE:
            case FIL:
                return ChatColor.GRAY.toString() + this + "\n";
            case AMOUR:
                // purple (moderne) = LIGHT_PURPLE (1.8)
                return ChatColor.LIGHT_PURPLE.toString() + this + "\n";
        }
        return "Marque inconnue\n";
    }
}
