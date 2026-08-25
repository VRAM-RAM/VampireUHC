package fr.vampireuhc.listeners;

import fr.vampireuhc.VampireUHC;
import fr.vampireuhc.game.GameManager;
import fr.vampireuhc.game.GamePhase;
import fr.vampireuhc.roles.ArcherRole;
import fr.vampireuhc.roles.GremlinRole;
import fr.vampireuhc.roles.RoleManager;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.entity.Arrow;

/**
 * Invincibilité totale avant l'annonce des rôles (phase PRE_ROLES) et blocage
 * de tout dégât joueur -> joueur tant que le pvp n'est pas actif.
 * Intercept les degats joueur --> joueur pour le gremlin (drain)
 */
public class PvPListener implements Listener {

    private final GameManager gameManager;

    public PvPListener(GameManager gameManager) {
        this.gameManager = gameManager;
    }

    // Invincibilité avant les 20 premières minutes, MAIS seulement en partie :
    // la phase initiale vaut déjà PRE_ROLES, sans ce gate tout le lobby serait
    // invincible (chute/feu/mob) jusqu'au premier resetGame.
    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        if (gameManager.isGameStarted()
                && gameManager.getPhase() == GamePhase.PRE_ROLES
                && event.getEntity() instanceof Player) {
            event.setCancelled(true);
        }
    }

    // Invincibilité explicite à la lave et au feu pendant la phase PRE_ROLES (en partie).
    @EventHandler
    public void onLavaOrFireDamage(EntityDamageEvent event) {
        if (!gameManager.isGameStarted()
                || gameManager.getPhase() != GamePhase.PRE_ROLES
                || !(event.getEntity() instanceof Player)) {
            return;
        }
        EntityDamageEvent.DamageCause cause = event.getCause();
        // HOT_FLOOR n'existe pas en 1.8 (bloc ajouté en 1.16).
        if (cause == EntityDamageEvent.DamageCause.LAVA
                || cause == EntityDamageEvent.DamageCause.FIRE
                || cause == EntityDamageEvent.DamageCause.FIRE_TICK) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {

        VampireUHC plugin = VampireUHC.getInstance();
        RoleManager roleManager = plugin.getRoleManager();


        // Pouvoir de l'Archer.
        //Cette partie est volontairement exécutée avant la gestion du PvP. (car glow dans tous les cas)
        if (event.getDamager() instanceof Arrow
                && ((Arrow) event.getDamager()).getShooter() instanceof Player) {

            Player shooter = (Player) ((Arrow) event.getDamager()).getShooter();
            if (roleManager.getPlayerRole(shooter.getUniqueId()) instanceof ArcherRole) {
                ArcherRole archer = (ArcherRole) roleManager.getPlayerRole(shooter.getUniqueId());
                archer.setGlowOnHit(event.getEntity(), plugin);
            }
        }

        
        //Gestion du PvP.
        if (!gameManager.isPvPActive()) {

            // Dégâts directs entre joueurs annulés si pvp non actid
            if (event.getEntity() instanceof Player
                    && event.getDamager() instanceof Player) {

                event.setCancelled(true);
                return;
            }

            // Dégâts par flèche entre joueurs annulés si pvp non actif
            if (event.getEntity() instanceof Player
                    && event.getDamager() instanceof Arrow
                    && ((Arrow) event.getDamager()).getShooter() instanceof Player) {

                event.setCancelled(true);
            }

            return;
        }

        // Si pvp actif, gestion des pouvoirs
        if (event.getEntity() instanceof Player
                && event.getDamager() instanceof Player
                && roleManager.getPlayerRole(((Player) event.getDamager()).getUniqueId()) instanceof GremlinRole) {

            GremlinRole gremlin = (GremlinRole) roleManager.getPlayerRole(((Player) event.getDamager()).getUniqueId());
            gremlin.applyDrainEffect((Player) event.getDamager(), (Player) event.getEntity());
        }
    }

}
