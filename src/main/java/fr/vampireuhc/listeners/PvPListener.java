package fr.vampireuhc.listeners;

import fr.vampireuhc.game.GameManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.entity.Arrow;

/**
 * Empeche tout degat joueur -> joueur tant que le pvp n'est pas actif.
 */
public class PvPListener implements Listener {

    private final GameManager gameManager;

    public PvPListener(GameManager gameManager) {
        this.gameManager = gameManager;
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
