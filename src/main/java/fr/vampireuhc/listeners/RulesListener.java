package fr.vampireuhc.listeners;

import fr.vampireuhc.VampireUHC;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.player.PlayerBedEnterEvent;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.world.PortalCreateEvent;

/**
 * Règles UHC : pas de régénération naturelle, lits interdits,
 * Nether/End désactivés.
 */
public class RulesListener implements Listener {
    private final VampireUHC plugin;

    public RulesListener(VampireUHC plugin) {
        this.plugin = plugin;
    }

    // Pas de régénération naturelle : la nourriture ne soigne plus.
    @EventHandler
    public void onRegainHealth(EntityRegainHealthEvent event) {
        if (!plugin.getConfigManager().isNoNaturalRegenEnabled()) {
            return;
        }
        if (!(event.getEntity() instanceof Player)) {
            return;
        }
        EntityRegainHealthEvent.RegainReason reason = event.getRegainReason();
        if (reason == EntityRegainHealthEvent.RegainReason.REGEN
                || reason == EntityRegainHealthEvent.RegainReason.SATIATED) {
            event.setCancelled(true);
        }
    }

    // Les lits sont interdits (impossible de dormir pour passer la nuit).
    @EventHandler
    public void onBedEnter(PlayerBedEnterEvent event) {
        if (!plugin.getConfigManager().areBedsBlocked()) {
            return;
        }
        event.setCancelled(true);
        event.getPlayer().sendMessage(plugin.getConfigManager().translate("&cLes lits sont interdits dans cet UHC !"));
    }

    // Empêche la création de tout portail (Nether ou End).
    @EventHandler
    public void onPortalCreate(PortalCreateEvent event) {
        if (plugin.getConfigManager().isNetherEndBlocked()) {
            event.setCancelled(true);
        }
    }

    // Empêche l'entrée dans le Nether et l'End.
    @EventHandler
    public void onPlayerPortal(PlayerPortalEvent event) {
        if (!plugin.getConfigManager().isNetherEndBlocked()) {
            return;
        }
        event.setCancelled(true);
        event.getPlayer().sendMessage(plugin.getConfigManager().translate("&cLe Nether et l'End sont désactivés dans cet UHC !"));
    }
}
