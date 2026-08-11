package fr.vampireuhc.roles;

public enum RoleType {
    MASTER, // Le maitre
    VAMPIRE_MINION, // Les sbires vampire
    SAVIOR, // Le salvateur
    SOUL_WEIGHTER,
    PALADIN, // Le paladin
    CUPIDON, // Cupidon
    APPRENTICE_SLAYER, // Apprentie chasseur
    GREMLIN; //Le Gremlin
    
    public static RoleType fromString(String value) {
        return valueOf(value.toUpperCase());
    }
}
