package fr.vampireuhc.player;

public enum Camp {
    VILLAGEOIS,
    SOLO,
    VAMPIRE;

    // Libellé français pour les messages.
    public String getDisplayName() {
        switch (this) {
            case VILLAGEOIS:
                return "Villageois";
            case VAMPIRE:
                return "Vampire";
            case SOLO:
                return "Solitaire";
            default:
                return name();
        }
    }
}
