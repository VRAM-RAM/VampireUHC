package fr.vampireuhc.game;

/* Enum pour les différentes phases de jeu. */
public enum GamePhase {
    NOT_STARTED,
    /* Première phase, de 0 à 20 minutes de jeu (préparation du stuff avant l'annonce des rôles).*/
    PRE_ROLES,

    /*Seconde phase : les joueurs ont à présent accès à /vuhc role : les rôles ont été attribués, mais le pvp n'est toujours pas activé. */
    PRE_PVP,

    /*Troisième phase : après 45 minutes de jeu, le PVP est activé, les vampires prennent connaissance de leurs mates. */
    PVP_ACTIVE,

    /* Fin de partie (victoire de l'un des camps) */
    ENDED,
}
