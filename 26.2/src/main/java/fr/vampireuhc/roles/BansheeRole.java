package fr.vampireuhc.roles;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import fr.vampireuhc.VampireUHC;
import fr.vampireuhc.config.MessageUtil;
import fr.vampireuhc.markers.MarkerManager;
import fr.vampireuhc.markers.TightAuraTier;
import fr.vampireuhc.player.Camp;
import fr.vampireuhc.player.VampireUHCPlayer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;

public class BansheeRole implements Role {
    private VampireUHCPlayer banshee;

    @Override
    public Camp getDefaultCamp() {
        return Camp.VILLAGEOIS;
    }

    @Override
    public Component getDescription() {
        MiniMessage mm = MiniMessage.miniMessage();
        return mm.deserialize(   
            "<gray>Vous pleurez ou criez, à chaque épisode, le nombre d'auras obscures proches de vous.</gray>\n\n"
            + "<dark_purple>▸</dark_purple> <gray>En chaque début d'épisode, en fonction du nombre d'auras obscures présentes dans un rayon de <italic>50</italic> blocs autour de vous, un message est envoyé à tous les joueurs :</gray>\n\n"
            + "  <green>Aucun message</green>\n"
            + "  <gray>→ Aucune aura obscure dans un rayon de <italic>50</italic> blocs.</gray>\n"
            + "  <yellow>« La Banshee pleure... »</yellow>\n"
            + "  <gray>→ Entre 1 et 2 auras obscures dans un rayon de <italic>50</italic> blocs.</gray>\n"
            + "  <red>« La Banshee pousse un cri effrayant ! »</red>\n"
            + "  <gray>→ +3 auras obscures dans un rayon de <italic>50</italic> blocs</gray>\n\n"         
        );
    }

    @Override
    public String getName() {
        return "Banshee";
    }

    @Override
    public boolean isVampire() {
        return false;
    }

    @Override
    public void onAssign(VampireUHCPlayer player) {
        this.banshee = player;
    }

    // Pouvoir passif de la Banshee 

    @Override
    public void onEpisodeStart(int episode) {
        scream();
    }

    private void scream() {
        if (banshee == null || !banshee.isAlive()) {
            return;
        }

        Player bukkitBanshee = Bukkit.getPlayer(banshee.getUuid());

        if (bukkitBanshee == null) {
            return;
        }

        int darkAuras = scan(bukkitBanshee);

        if (darkAuras == 0) {
            return;
        } else if (darkAuras <= 2) {
            MessageUtil.broadcast("La Banshee <gold>pleure</gold>...");
            return;
        } else {
            MessageUtil.broadcast("La Banshee pousse un <gold>cri</gold> déchirant !");
            return;
        }
    }

    private int scan(Player bukkitBanshe) {
        int result = 0;
        MarkerManager markerManager = VampireUHC.getInstance().getMarkerManager();

        // On scanne toutes les entités dans un rayon de 50 blocs
        for (Entity entity : bukkitBanshe.getNearbyEntities(50, 50, 50)) {
            // Si c'est un joueur, on poursuit
            if (entity instanceof Player player) {
                // Si le joueur est en survie (donc pas un mort en spectateur ou un admin en créa), on poursuit
                if (player.getGameMode() == GameMode.SURVIVAL) {
                    // On calcule le TightAuraTier du joueur (Obscure, Lumineuse ou Neutre)
                    TightAuraTier aura = markerManager.computeAuraTier(player.getUniqueId()).getTight();
                    // Si son aura est Obscure, on incrémente
                    if (aura == TightAuraTier.OBSCURE) {
                        result++;
                    }
                }
                
            }
        }
        return result;
    }
}
