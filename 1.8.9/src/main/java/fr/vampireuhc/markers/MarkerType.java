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

    public String toLegacy() {
        switch (this) {
            case MARQUE_MAITRE:
            case MARQUE_MAITRE_DOPPELGANGER:
            case MARQUE_VAMPIRE:
                return ChatColor.DARK_RED.toString() + this + "\n";
            case LUMINEUX:
            case LUMINEUX_DOPPELGANGER:
            case SALVATION:
            case SALVATION_DOPPELGANGER:
                return ChatColor.YELLOW.toString() + this + "\n";
            case SABLE_LUMINEUX:
            case SABLE_NEUTRE:
            case SABLE_LUMINEUX_DOPPELGANGER:
            case SABLE_NEUTRE_DOPPELGANGER:
            case FIL:
            case FIL_DOPPELGANGER:
                return ChatColor.GRAY.toString() + this + "\n";
            case AMOUR:
                // purple (moderne) = LIGHT_PURPLE (1.8)
                return ChatColor.LIGHT_PURPLE.toString() + this + "\n";
        }
        return "Marque inconnue\n";
    }
}
