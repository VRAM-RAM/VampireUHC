package fr.vampireuhc.listeners;

import fr.vampireuhc.VampireUHC;
import fr.vampireuhc.markers.MarkerManager;
import fr.vampireuhc.markers.MarkerType;
import fr.vampireuhc.player.VampireUHCPlayer;
import fr.vampireuhc.roles.ApprenticeSlayer;
import fr.vampireuhc.roles.CupidonRole;
import fr.vampireuhc.roles.GravediggerRole;
import fr.vampireuhc.roles.PaladinRole;
import fr.vampireuhc.roles.SandMerchantRole;
import fr.vampireuhc.roles.WeaverRole;
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
        Location location = victim.getLastDeathLocation();

        VampireUHCPlayer vp = plugin.getPlayerManager().get(victim.getUniqueId());
        if (vp == null) {
            return;
        }
        vp.setDead();
        
        MarkerManager markerManager = plugin.getMarkerManager();

        // Le joueur mort devient spectateur jusqu'à la fin de la partie (pas de respawn).
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            if (victim.isOnline()) {
                victim.setGameMode(GameMode.SPECTATOR);
            }
        });

        Player killer = victim.getKiller();
        VampireUHCPlayer killerVp = killer != null ? plugin.getPlayerManager().get(killer.getUniqueId()) : null;

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
        }

        // Le Cupidon est notifié si l'un des amoureux meurt (penalty + identité du tueur).
        // Le Tisseur est notifié si l'un des membres de sa toile / reseau meurt ou tue.
        // Si le Fossoyeur est encore en vie, on fait pop des particules à l'endroit de la mort du joueur, et on ajoute
        // ses marqueurs (ceux qu'il avait) dans la `HashMap<>` du fossoyeur.
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
            if (p.getRole() instanceof GravediggerRole graveDigger) {
                List<MarkerType> markers = markerManager.getMarkerTypesByPlayer(vp.getUuid());
                graveDigger.spawnParticlesAtLocation(location, markers);
            }
        }

        // à la mort du marchand de sable, tous les joueurs marqués par un marqueur sable deviennent ensommeillés.
        if (vp.getRole() instanceof SandMerchantRole sandMerchant) {
            sandMerchant.makePlayersSleepOnMarchantDeath(markerManager);
        }
        
        // L'Apprentie assassin récupère les marques (sauf Maître) du tué.
        if (killerVp != null && killerVp.getRole() instanceof ApprenticeSlayer slayer) {
            slayer.CopyMarkersOnKill(plugin.getMarkerManager(), vp);
        }

        plugin.getGameManager().checkWinCondition();
    }
}
