package fr.vampireuhc.listeners;

import fr.vampireuhc.VampireUHC;
import fr.vampireuhc.config.MessageUtil;
import fr.vampireuhc.game.GamePhase;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

/**
 * Chat pendant la partie :
 * - les joueurs vivants n'ont pas de chat du tout (aucune communication) ;
 * - les spectateurs disposent d'un canal privé "[Spectateur]", invisible des vivants.
 * Hors partie (lobby), le chat reste normal.
 */
public class ChatListener implements Listener {
    private final VampireUHC plugin;

    public ChatListener(VampireUHC plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent event) {
        Player sender = event.getPlayer();
        var game = plugin.getGameManager();

        boolean inGame = game.isGameStarted() && game.getPhase() != GamePhase.ENDED;
        if (!inGame) {
            return;
        }

        event.setCancelled(true);

        if (sender.getGameMode() == GameMode.SPECTATOR) {
            // Composition de Components : le message du joueur n'est jamais interprété
            // par MiniMessage (aucune injection de tags possible).
            Component formatted = MessageUtil.serialize("<gray>[Spectateur]</gray>")
                    .append(Component.space())
                    .append(Component.text(sender.getName(), NamedTextColor.GRAY))
                    .append(Component.text(" » ", NamedTextColor.DARK_GRAY))
                    .append(Component.text(event.getMessage(), NamedTextColor.WHITE));
            for (Player online : Bukkit.getOnlinePlayers()) {
                if (online.getGameMode() == GameMode.SPECTATOR) {
                    online.sendMessage(formatted);
                }
            }
        } else {
            sender.sendMessage(MessageUtil.error("Le chat est désactivé pendant la partie."));
        }
    }
}
