package fr.vampireuhc.roles;

import fr.vampireuhc.player.VampireUHCPlayer;
import net.kyori.adventure.text.Component;

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

    /**
     * Variante avec contexte d'assignation : {@code restoring} vaut true quand le rôle
     * est recréé depuis game-state.json après un redémarrage. Les effets "one-shot"
     * (kits, soins complets...) ne doivent pas être réappliqués dans ce cas.
     */
    default void onAssign(VampireUHCPlayer player, boolean restoring) {
        onAssign(player);
    }

    /** Appelee quand la partie s'arrete ou est reinitialisee : nettoie les taches planifiees. */
    default void onGameEnd() {
    }

    /**
     * Appelé à chaque début d'épisode (à la frontière : épisodes 2+, jamais
     * pendant l'épisode 1). Le numéro passé correspond à l'épisode qui démarre.
     */
    default void onEpisodeStart(int episode) {
    }

    /**
     * Appelé à la mort d'un joueur de la partie. {@code victim} et {@code killer}
     * (peut être null) sont l'état meta-jeu (VampireUHCPlayer).
     */
    default void onPlayerDeath(VampireUHCPlayer victim, VampireUHCPlayer killer) {
    }

    // Retourne la description du role.
    public Component getDescription();
}
