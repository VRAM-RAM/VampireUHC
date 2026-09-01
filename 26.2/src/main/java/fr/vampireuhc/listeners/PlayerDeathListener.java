package fr.vampireuhc.listeners;

import fr.vampireuhc.VampireUHC;
import fr.vampireuhc.markers.MarkerManager;
import fr.vampireuhc.markers.MarkerType;
import fr.vampireuhc.player.VampireUHCPlayer;
import fr.vampireuhc.roles.ApprenticeSlayer;
import fr.vampireuhc.roles.BabaYagaRole;
import fr.vampireuhc.roles.CupidonRole;
import fr.vampireuhc.roles.DoppelgangerRole;
import fr.vampireuhc.roles.GravediggerRole;
import fr.vampireuhc.roles.PaladinRole;
import fr.vampireuhc.roles.SandMerchantRole;
import fr.vampireuhc.roles.WeaverRole;
import fr.vampireuhc.roles.WhiteLadyRole;
import fr.vampireuhc.roles.usurped.UsurpedGravedigger;
import fr.vampireuhc.roles.usurped.UsurpedPaladin;
import fr.vampireuhc.roles.usurped.UsurpedPower;
import fr.vampireuhc.roles.usurped.UsurpedSlayer;
import fr.vampireuhc.roles.usurped.UsurpedWeaver;
import fr.vampireuhc.roles.usurped.UsurpedWhiteLady;
import fr.vampireuhc.roles.usurped.UsurpedBabaYaga;
import fr.vampireuhc.roles.usurped.UsurpedSandMerchant;

import java.util.List;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
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

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();

        VampireUHCPlayer vp = plugin.getPlayerManager().get(victim.getUniqueId());
        if (vp == null) {
            return;
        }

        Player killer = victim.getKiller();
        VampireUHCPlayer killerVp = killer != null ? plugin.getPlayerManager().get(killer.getUniqueId()) : null;

        if (vp.getRole() instanceof WhiteLadyRole whiteLady) {
            if (whiteLady.onDeath(killerVp)) {
                event.setCancelled(true);
                plugin.getMapManager().teleportPlayerRandomly(victim);
                return;
            }
        }
        // Dame Blanche copiée par le Sosie : résurrection unique, qualifiée par
        // le camp du tueur (villageois → faiblesse de nuit, vampire → de jour).
        if (vp.getRole() instanceof DoppelgangerRole doppelgangerLady
                && doppelgangerLady.getActivePower() instanceof UsurpedWhiteLady usurpedWhiteLady) {
            if (usurpedWhiteLady.onDeath(killerVp)) {
                event.setCancelled(true);
                plugin.getMapManager().teleportPlayerRandomly(victim);
                return;
            }
        }
        vp.setDead();

        Location location = victim.getLastDeathLocation();
        
        MarkerManager markerManager = plugin.getMarkerManager();

        // Le joueur mort devient spectateur jusqu'à la fin de la partie (pas de respawn).
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            if (victim.isOnline()) {
                victim.setGameMode(GameMode.SPECTATOR);
            }
        });


        Component message = Component.text(victim.getName(), NamedTextColor.RED)
                .append(Component.text(" est mort", NamedTextColor.GRAY));
        if (killer != null) {
            message = message.append(Component.text(", tué par ", NamedTextColor.GRAY))
                    .append(Component.text(killer.getName(), NamedTextColor.GOLD));
        }
        event.deathMessage(message);

        if (killerVp != null) {
            // Le Paladin gagne une marque lumineuse en tuant un vampire.
            if (killerVp.getRole() instanceof PaladinRole paladin) {
                paladin.gainLuminousMarkerOnKill(plugin.getMarkerManager(), vp);
            }
            // Paladin usurpé : 50% de marque lumineuse en tuant, camp peu importe.
            if (killerVp.getRole() instanceof DoppelgangerRole doppelganger
                    && doppelganger.getActivePower() instanceof UsurpedPaladin usurpedPaladin) {
                usurpedPaladin.gainLuminousMarkerOnKill(plugin.getMarkerManager(), vp);
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
            if (p.getRole() instanceof CupidonRole cupidon) {
                cupidon.onLoverDeath(plugin.getMarkerManager(), vp, killer);
            }
            if (p.getRole() instanceof WeaverRole weaver) {
                if (p.getUuid().equals(vp.getUuid())) {
                    // Le Tisseur meurt : sa toile s'effondre avec lui.
                    weaver.collapseWeb(markerManager);
                } else {
                    // Sinon, on teste si le tué était membre du réseau ou si le tueur est membre.
                    weaver.tryInformDeathOfNodeAndDestroyWeb(markerManager, vp);
                    weaver.tryInformMurderByNodeOfWeb(markerManager, killerVp);
                }
            }
            // Tisseur copié par le Sosie : réseau FIL_DOPPELGANGER, même effondrement.
            if (p.getRole() instanceof DoppelgangerRole doppelgangerWeaver
                    && doppelgangerWeaver.getActivePower() instanceof UsurpedWeaver usurpedWeaver) {
                if (p.getUuid().equals(vp.getUuid())) {
                    // Le Sosie-Tisseur meurt : sa toile s'effondre avec lui.
                    usurpedWeaver.collapseWeb(markerManager);
                } else {
                    usurpedWeaver.tryInformDeathOfNodeAndDestroyWeb(markerManager, vp);
                    usurpedWeaver.tryInformMurderByNodeOfWeb(markerManager, killerVp);
                }
            }
            if (p.getRole() instanceof GravediggerRole graveDigger) {
                List<MarkerType> markers = markerManager.getMarkerTypesByPlayer(vp.getUuid());
                graveDigger.spawnParticlesAtLocation(location, markers);
            }
            // Sosie Fossoyeur : particules sur le cadavre (exhumé via son propre registre).
            if (p.getRole() instanceof DoppelgangerRole doppelganger
                    && doppelganger.getActivePower() instanceof UsurpedGravedigger usurpedGraveDigger) {
                List<MarkerType> markers = markerManager.getMarkerTypesByPlayer(vp.getUuid());
                usurpedGraveDigger.spawnParticlesAtLocation(location, markers);
            }

            // La Baba Yaga vivante reçoit la proposition de résurrection du défunt.
            if (p.getRole() instanceof BabaYagaRole babaYaga
                    && p.isAlive()
                    && !p.getUuid().equals(vp.getUuid())) {
                babaYaga.offerResurrection(vp);
            }

            // Baba Yaga copiée par le Sosie : information seule, sans résurrection.
            if (p.getRole() instanceof DoppelgangerRole doppelgangerBaba
                    && doppelgangerBaba.getActivePower() instanceof UsurpedBabaYaga usurpedBabaYaga
                    && p.isAlive()
                    && !p.getUuid().equals(vp.getUuid())) {
                usurpedBabaYaga.notifyDeath(vp);
            }

            if (p.getRole() instanceof WhiteLadyRole whiteLady) {
                whiteLady.killedKiller(victim);
            }

            // Mort de la vraie Dame Blanche : purge le malus de faiblesse du Sosie qui la copie.
            if (vp.getRole() instanceof WhiteLadyRole
                    && p.getRole() instanceof DoppelgangerRole doppelgangerLadyPurge
                    && doppelgangerLadyPurge.getActivePower() instanceof UsurpedWhiteLady usurpedWhiteLadyPurge) {
                usurpedWhiteLadyPurge.onRealWhiteLadyDeath();
            }

            // Hook de mort généralisé : chaque rôle observateur (ex. Doppelganger
            // qui suit la mort de sa cible ou de son tueur) est notifié.
            if (p.getRole() != null) {
                p.getRole().onPlayerDeath(vp, killerVp);
            }
        }

        // La mort de la Baba Yaga active le lien de mort de son ressuscité.
        if (vp.getRole() instanceof BabaYagaRole babaYaga) {
            babaYaga.onBabaYagaDeath(killer != null ? killer.getUniqueId() : null);
        }

        // à la mort du marchand de sable, tous les joueurs marqués par un marqueur sable deviennent ensommeillés.
        if (vp.getRole() instanceof SandMerchantRole sandMerchant) {
            sandMerchant.makePlayersSleepOnMarchantDeath(markerManager);
        }
        // Marchand de Sable copié par le Sosie : mêmes ensommeillements (lenteur 1 min).
        if (vp.getRole() instanceof DoppelgangerRole doppelgangerSable
                && doppelgangerSable.getActivePower() instanceof UsurpedSandMerchant usurpedSandMerchant) {
            usurpedSandMerchant.makePlayersSleepOnDeath(markerManager);
        }
        
        // L'Apprentie assassin récupère les marques (sauf Maître) du tué.
        if (killerVp != null && killerVp.getRole() instanceof ApprenticeSlayer slayer) {
            slayer.CopyMarkersOnKill(plugin.getMarkerManager(), vp);
        }
        // Sosie Apprentie assassin : même vol de marqueurs (sauf Maître/Amour/Fil).
        if (killerVp != null && killerVp.getRole() instanceof DoppelgangerRole doppelganger
                && doppelganger.getActivePower() instanceof UsurpedSlayer usurpedSlayer) {
            usurpedSlayer.copyMarkersOnKill(plugin.getMarkerManager(), vp);
        }

        plugin.getGameManager().checkWinCondition();
    }
}
