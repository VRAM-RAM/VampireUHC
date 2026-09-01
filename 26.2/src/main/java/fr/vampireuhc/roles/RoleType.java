package fr.vampireuhc.roles;

public enum RoleType {
    MASTER, // Le maitre
    VAMPIRE_MINION, // Les sbires vampire
    SAVIOR, // Le salvateur
    SOUL_WEIGHTER, // La peseuse d'ame
    WEAVER, // Le Tisseur
    CARTOGRAPHER, // Le Cartographe
    PALADIN, // Le paladin
    CUPIDON, // Cupidon
    APPRENTICE_SLAYER, // Apprentie assassin
    SAND_MERCHANT, // Marchand de sable
    ARCHER, // Archer
    GRAVE_DIGGER, // Fossoyeur
    WHITE_LADY, // Dame Blanche
    BABA_YAGA, // Baba Yaga
    BANSHEE, // Banshee
    EXORCIST, // L'Exorciste
    WATCHMAN, // Le Veilleur
    GREMLIN, //Le Gremlin
    BOURREAU, // Le Bourreau
    PRIEST, // Le Prêtre
    COMTE, // Le Comte
    DOPPELGANGER, // Le Doppelganger (Sosie)
    GHOST_HUNTER; // Le Chasseur de Fantômes
    
    public static RoleType fromString(String value) {
        return valueOf(value.toUpperCase());
    }
}
