package fr.vampireuhc.player;
import fr.vampireuhc.roles.Role;
import java.util.UUID;

/**
 * Etat "meta-jeu" d'un joueur, distinct du Player Bukkit.
 * On garde ca separe pour pouvoir survivre a une deconnexion/reconnexion
 * et pour ne pas polluer le Player Bukkit avec de la logique custom.
 */
public class VampireUHCPlayer {
    private final UUID uuid;
    private String lastKnownName;
   
    private Camp camp;
    private Role role;
    private boolean alive = true;
    private boolean vampireListRevealed = false; //Passe à true, à l'activation du pvp, pour les vampires.
    private boolean canVoteVampireMark = false; // Changé en true pour les vampires à l'annonce des rôles.
    private boolean infected = false; // true si le joueur a été infecté par les marques du Maître

    public VampireUHCPlayer(UUID uuid, String lastKnownName) {
        this.uuid = uuid;
        this.lastKnownName = lastKnownName;
    }

    public UUID getUuid() {
        return uuid;
    }

    public String getLastKnownName() {
        return lastKnownName;
    }

    public void setLastKnownName(String lastKnownName) {
        this.lastKnownName = lastKnownName;
    }

    public Camp getCamp() {
        return camp;
    }

    public void setCamp(Camp camp) {
        this.camp = camp;
    }

    public boolean isAlive() {
        return alive;
    }

    public void setDead() {
        this.alive = false;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public Role getRole() {
        return role;
    }

    public void setVampireListRevealation() {
        this.vampireListRevealed = true;
    }

    public boolean isVampireListRevealed() {
        return vampireListRevealed;
    }

    public void setVampireVote() {
        this.canVoteVampireMark = true;
    }

    public boolean canVoteVampireMark() {
        return canVoteVampireMark;
    }

     /**
     * Appele lorsque le joueur est infecte (3 marqueurs Maitre).
     * L'infection est permanente mais l'infecté ne peut pas voter pour marquer
     */
    public void infect() {
        this.camp = Camp.VAMPIRE;
        this.infected = true;
        this.canVoteVampireMark = false; // Le joueur infecté ne peut pas voter
    }

    // true si le joueur a rejoint les vampires en cours de partie (via l'infection).
    public boolean isInfected() {
        return infected;
    }
}
