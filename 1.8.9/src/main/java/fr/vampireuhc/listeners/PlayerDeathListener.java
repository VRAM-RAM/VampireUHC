package fr.vampireuhc.listeners;

import fr.vampireuhc.VampireUHC;
import fr.vampireuhc.markers.MarkerManager;
import fr.vampireuhc.markers.MarkerType;
import fr.vampireuhc.player.VampireUHCPlayer;
import fr.vampireuhc.roles.ApprenticeSlayer;
import fr.vampireuhc.roles.BabaYagaRole;
import fr.vampireuhc.roles.CupidonRole;
import fr.vampireuhc.roles.GravediggerRole;
import fr.vampireuhc.roles.PaladinRole;
import fr.vampireuhc.roles.SandMerchantRole;
import fr.vampireuhc.roles.WeaverRole;
import fr.vampireuhc.roles.WhiteLadyRole;

import java.util.List;

import org.bukkit.ChatColor;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;

/**
 * Gère les morts : marque le joueur comme mort, le passe en spectateur, envoie
 * un message de mort personnalisé, et déclenche les hooks de rôles (Paladin,
 * Apprentie assassin, Cupidon). Vérifie aussi les conditions de victoire.
 */
public class PlayerDeathListener implements Listener {
    private final VampireUHC plugin;

    public PlayerDeathListener(VampireUHC plugin) {
        this.plugin = plugin;
    }

    // Résurrection de la Dame Blanche : en 1.8, PlayerDeathEvent n'est pas
    // annulable. On intercepte donc le coup fatal (EntityDamageEvent) juste
    // avant la mort et on l'annule si la Dame doit se relever.
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onLethalDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }
        Player victim = (Player) event.getEntity();

        VampireUHCPlayer vp = plugin.getPlayerManager().get(victim.getUniqueId());
        if (vp == null || !vp.isAlive()
                || !(vp.getRole() instanceof WhiteLadyRole)) {
            return;
        }

        double finalHealth = victim.getHealth() - event.getFinalDamage();
        if (finalHealth > 0) {
            return; // coup non létal
        }

        Player killer = victim.getKiller();
        VampireUHCPlayer killerVp = killer != null ? plugin.getPlayerManager().get(killer.getUniqueId()) : null;

        WhiteLadyRole whiteLady = (WhiteLadyRole) vp.getRole();
        if (whiteLady.onDeath(killerVp)) {
            event.setCancelled(true);
            victim.setHealth(victim.getMaxHealth());
            plugin.getMapManager().teleportPlayerRandomly(victim);
        }
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();

        VampireUHCPlayer vp = plugin.getPlayerManager().get(victim.getUniqueId());
        if (vp == null) {
            return;
        }

        Player killer = victim.getKiller();
        VampireUHCPlayer killerVp = killer != null ? plugin.getPlayerManager().get(killer.getUniqueId()) : null;

        // NB : la résurrection de la Dame Blanche est gérée en amont dans
        // onLethalDamage() — PlayerDeathEvent n'est pas annulable en 1.8.
        vp.setDead();

        Location location = victim.getLocation(); // pas de getLastDeathLocation en 1.8 : la mort EST la position courante
        
        MarkerManager markerManager = plugin.getMarkerManager();

        // Le joueur mort devient spectateur jusqu'à la fin de la partie (pas de respawn).
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            if (victim.isOnline()) {
                victim.setGameMode(GameMode.SPECTATOR);
            }
        });


        String message = ChatColor.RED + victim.getName()
                + ChatColor.GRAY + " est mort";
        if (killer != null) {
            message += ChatColor.GRAY + ", tué par "
                    + ChatColor.GOLD + killer.getName();
        }
        event.setDeathMessage(message);

        if (killerVp != null) {
            // Le Paladin gagne une marque lumineuse en tuant un vampire.
            if (killerVp.getRole() instanceof PaladinRole) {
                PaladinRole paladin = (PaladinRole) killerVp.getRole();
                paladin.gainLuminousMarkerOnKill(plugin.getMarkerManager(), vp);
            }
        }

        // Le Cupidon est notifié si l'un des amoureux meurt (penalty + identité du tueur).
        // Le Tisseur est notifié si l'un des membres de sa toile / reseau meurt ou tue.
        // Si le Fossoyeur est encore en vie, on fait pop des particules à l'endroit de la mort du joueur, et on ajoute
        // ses marqueurs (ceux qu'il avait) dans la `HashMap<>` du fossoyeur.
        // Si la dame Blanche tue son tueur, le booléen `killedKiller` doit être update.
        // Ces hooks lisent l'état des marqueurs du défunt : ils doivent tourner AVANT
        // que l'Apprentie assassin ne récupère les marques, sinon ils voient un état tronqué.
        for (VampireUHCPlayer p : plugin.getPlayerManager().getAll()) {
            // En cas de mort d'un membre du couple
            if (p.getRole() instanceof CupidonRole) {
                CupidonRole cupidon = (CupidonRole) p.getRole();
                cupidon.onLoverDeath(plugin.getMarkerManager(), vp, killer);
            }
            if (p.getRole() instanceof WeaverRole) {
                WeaverRole weaver = (WeaverRole) p.getRole();
                if (p.getUuid().equals(vp.getUuid())) {
                    // Le Tisseur meurt : sa toile s'effondre avec lui.
                    weaver.collapseWeb(markerManager);
                } else {
                    // Sinon, on teste si le tué était membre du réseau ou si le tueur est membre.
                    weaver.tryInformDeathOfNodeAndDestroyWeb(markerManager, vp);
                    weaver.tryInformMurderByNodeOfWeb(markerManager, killerVp);
                }
            }
            if (p.getRole() instanceof GravediggerRole) {
                GravediggerRole graveDigger = (GravediggerRole) p.getRole();
                List<MarkerType> markers = markerManager.getMarkerTypesByPlayer(vp.getUuid());
                graveDigger.spawnParticlesAtLocation(location, markers);
            }

            // La Baba Yaga vivante reçoit la proposition de résurrection du défunt.
            if (p.getRole() instanceof BabaYagaRole
                    && p.isAlive()
                    && !p.getUuid().equals(vp.getUuid())) {
                ((BabaYagaRole) p.getRole()).offerResurrection(vp);
            }

            if (p.getRole() instanceof WhiteLadyRole) {
                ((WhiteLadyRole) p.getRole()).killedKiller(victim);
            }
        }

        // La mort de la Baba Yaga active le lien de mort de son ressuscité.
        if (vp.getRole() instanceof BabaYagaRole) {
            ((BabaYagaRole) vp.getRole()).onBabaYagaDeath(killer != null ? killer.getUniqueId() : null);
        }

        // à la mort du marchand de sable, tous les joueurs marqués par un marqueur sable deviennent ensommeillés.
        if (vp.getRole() instanceof SandMerchantRole) {
            ((SandMerchantRole) vp.getRole()).makePlayersSleepOnMarchantDeath(markerManager);
        }

        // L'Apprentie assassin récupère les marques (sauf Maître) du tué.
        if (killerVp != null && killerVp.getRole() instanceof ApprenticeSlayer) {
            ((ApprenticeSlayer) killerVp.getRole()).CopyMarkersOnKill(plugin.getMarkerManager(), vp);
        }

        plugin.getGameManager().checkWinCondition();
    }
}
