package fr.vampireuhc.listeners;

import fr.vampireuhc.VampireUHC;
import fr.vampireuhc.config.MessageUtil;
import fr.vampireuhc.game.GameManager;
import fr.vampireuhc.game.GamePhase;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.ChatColor;

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
        GameManager game = plugin.getGameManager();

        boolean inGame = game.isGameStarted() && game.getPhase() != GamePhase.ENDED;
        if (!inGame) {
            return;
        }

        event.setCancelled(true);

        if (sender.getGameMode() == GameMode.SPECTATOR) {
            // Concaténation de chaînes : le message du joueur n'est jamais
            // interprété par le parseur MiniMessage (aucune injection de tags).
            String formatted = ChatColor.GRAY + "[Spectateur] " + sender.getName()
                    + ChatColor.DARK_GRAY + " » " + ChatColor.WHITE + event.getMessage();
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
