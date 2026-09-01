package fr.vampireuhc.roles.usurped;

import com.google.gson.JsonObject;
import fr.vampireuhc.player.VampireUHCPlayer;

/**
 * Un pouvoir copié par le Doppelganger .
 *
 * Chaque adaptateur encapsule un pouvoir copié avec SON PROPRE état et ses
 * propres compteurs : il n'hérite jamais de l'état, des informations déjà
 * accumulées ni du pool d'usages du joueur usurpé.
 *
 * Les nerfs de chaque usurpation (vs. le pouvoir original) sont décrits dans
 * doc/Doppelganger.md et sont donc propres à chaque adaptateur.
 */
public interface UsurpedPower {
    /** Nom du rôle dont le pouvoir est copié (affichage seulement). */
    String getName();

    /** Appelé quand le Doppelganger usurpe la cible. {@code doppelganger} est le Doppelgangers. */
    void onEnter(VampireUHCPlayer doppelganger);

    /** Appelé quand les pouvoirs copiés disparaissent (mort de l'usurpé, fin de partie...). */
    void onExit();

    default void onEpisodeStart(int episode) {
    }

    default void onGameEnd() {
    }

    /** Sauvegarde de l'état propre du pouvoir copié dans {@code obj}. */
    default void saveState(JsonObject obj) {
    }

    /** Restauration de l'état propre du pouvoir copié depuis {@code obj}. */
    default void restoreState(JsonObject obj) {
    }
}