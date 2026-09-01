package fr.vampireuhc.listeners;

import fr.vampireuhc.VampireUHC;
import fr.vampireuhc.game.GameManager;
import fr.vampireuhc.game.GamePhase;
import fr.vampireuhc.roles.ArcherRole;
import fr.vampireuhc.roles.BourreauRole;
import fr.vampireuhc.roles.DoppelgangerRole;
import fr.vampireuhc.roles.GremlinRole;
import fr.vampireuhc.roles.usurped.UsurpedGremlin;
import fr.vampireuhc.roles.usurped.UsurpedArcher;
import fr.vampireuhc.roles.usurped.UsurpedBourreau;

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
        if (cause == EntityDamageEvent.DamageCause.LAVA
                || cause == EntityDamageEvent.DamageCause.FIRE
                || cause == EntityDamageEvent.DamageCause.FIRE_TICK
                || cause == EntityDamageEvent.DamageCause.HOT_FLOOR) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {

        var plugin = VampireUHC.getInstance();
        var roleManager = plugin.getRoleManager();

        
        // Pouvoir de l'Archer.
        //Cette partie est volontairement exécutée avant la gestion du PvP. (car glow dans tous les cas)
        if (event.getDamager() instanceof Arrow arrow
                && arrow.getShooter() instanceof Player shooter) {
            var archerRole = roleManager.getPlayerRole(shooter.getUniqueId());
            if (archerRole instanceof ArcherRole archer) {
                archer.setGlowOnHit(event.getEntity(), plugin);
            } else if (archerRole instanceof DoppelgangerRole doppelgangerA
                    && doppelgangerA.getActivePower() instanceof UsurpedArcher usurpedArcher) {
                // Archer copié par le Sosie : glow identique, sans équipement.
                usurpedArcher.setGlowOnHit(event.getEntity(), plugin);
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
                    && event.getDamager() instanceof Arrow arrow
                    && arrow.getShooter() instanceof Player) {

                event.setCancelled(true);
            }

            return;
        }

        // Si pvp actif, gestion des pouvoirs
        if (event.getEntity() instanceof Player victim
                && event.getDamager() instanceof Player attacker) {

            // Le premier coup du Bourreau de l'épisode inflige 50% de dégâts en plus.
            var attackerRole = roleManager.getPlayerRole(attacker.getUniqueId());
            if (attackerRole instanceof BourreauRole bourreau) {
                if (bourreau.tryApplyFirstHitBonus(plugin.getGameManager().getEpisode())) {
                    event.setDamage(event.getDamage() * 1.5);
                }
            } else if (attackerRole instanceof DoppelgangerRole doppelgangerB
                    && doppelgangerB.getActivePower() instanceof UsurpedBourreau usurpedBourreau) {
                // Bourreau copié par le Sosie : premier coup +50%, compteurs propres.
                if (usurpedBourreau.tryApplyFirstHitBonus(plugin.getGameManager().getEpisode())) {
                    event.setDamage(event.getDamage() * 1.5);
                }
            }

            if (roleManager.getPlayerRole(attacker.getUniqueId()) instanceof DoppelgangerRole doppelganger) {
                // Le Sosie inflige 50% de dégâts en plus au tueur de sa cible usurpée.
                if (doppelganger.getDamageBonusAgainst(victim.getUniqueId())) {
                    event.setDamage(event.getDamage() * 1.5);
                }
            }

            if (roleManager.getPlayerRole(attacker.getUniqueId()) instanceof GremlinRole gremlin) {
                gremlin.applyDrainEffect(attacker, victim);
            }

            // Drain du Sosie : identique à celui du Gremlin, mais activé via le
            // pouvoir usurpé (UsurpedGremlin.activateDrain) au lieu du rôle.
            if (roleManager.getPlayerRole(attacker.getUniqueId()) instanceof DoppelgangerRole doppelganger
                    && doppelganger.getActivePower() instanceof UsurpedGremlin usurpedGremlin) {
                usurpedGremlin.applyDrainEffect(attacker, victim);
            }
        }
    }

}
