package fr.vampireuhc.roles;

import fr.vampireuhc.player.VampireUHCPlayer;

/**
 * Contrat minimal pour un role (Maitre, Salvateur, Cupidon, Paladin, ...).
 * Volontairement tres leger pour l'instant : on branchera les hooks
 * (onEpisodeStart, onPlayerDeath, onAuraChange...) au fur et a mesure
 * qu'on implementera chaque role, plutot que de deviner une interface
 * complete a l'avance.
 */
public interface Role {

    String getName();
    boolean isVampire();

    /** Le camp auquel ce role appartient par defaut. */
    fr.vampireuhc.player.Camp getDefaultCamp();

    /** Appele une fois, au moment ou le role est attribue a un joueur. */
    void onAssign(VampireUHCPlayer player);

    // Retourne la description du role.
    public String getDescription();
}
