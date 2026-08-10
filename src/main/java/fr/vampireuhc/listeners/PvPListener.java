package fr.vampireuhc.listeners;

import fr.vampireuhc.game.GameManager;
import fr.vampireuhc.game.GamePhase;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.entity.Arrow;

/**
 * Invincibilité totale avant l'annonce des rôles (phase PRE_ROLES) et blocage
 * de tout dégât joueur -> joueur tant que le pvp n'est pas actif.
 */
public class PvPListener implements Listener {

    private final GameManager gameManager;

    public PvPListener(GameManager gameManager) {
        this.gameManager = gameManager;
    }

    // Invincibilité avant les 20 premières minutes : aucun dégât.
    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        if (gameManager.getPhase() == GamePhase.PRE_ROLES && event.getEntity() instanceof Player) {
            event.setCancelled(true);
        }
    }

    // Invincibilité explicite à la lave et au feu pendant la phase PRE_ROLES.
    @EventHandler
    public void onLavaOrFireDamage(EntityDamageEvent event) {
        if (gameManager.getPhase() != GamePhase.PRE_ROLES || !(event.getEntity() instanceof Player)) {
            return;
        }
        EntityDamageEvent.DamageCause cause = event.getCause();
        if (cause == EntityDamageEvent.DamageCause.LAVA
                || cause == EntityDamageEvent.DamageCause.FIRE
                || cause == EntityDamageEvent.DamageCause.FIRE_TICK
                || cause == EntityDamageEvent.DamageCause.HOT_FLOOR) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (gameManager.isPvPActive()) {
            // Le pvp est actif, pas besoin de vérifier
            return;
        }
        // On vérifie que l'entité visée est bien un joueur.
        if (!(event.getEntity() instanceof Player)) {
            return;
        }

        // Attaque directe d'un joueur.
        if (event.getDamager() instanceof Player) {
            event.setCancelled(true);

        // Attaque par une flèche tirée par un joueur.
        } else if (event.getDamager() instanceof Arrow arrow
                && arrow.getShooter() instanceof Player) {
            event.setCancelled(true);
        }
    }
}
