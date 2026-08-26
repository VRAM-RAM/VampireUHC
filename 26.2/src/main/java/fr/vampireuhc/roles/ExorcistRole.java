package fr.vampireuhc.roles;

import fr.vampireuhc.markers.TightAuraTier;
import fr.vampireuhc.markers.Aura;

import net.kyori.adventure.text.Component;
import fr.vampireuhc.player.VampireUHCPlayer;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import fr.vampireuhc.markers.MarkerManager;
import fr.vampireuhc.markers.MarkerType;
import fr.vampireuhc.config.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import net.kyori.adventure.text.minimessage.MiniMessage;
import fr.vampireuhc.player.Camp;

public class ExorcistRole implements Role {
    private int lastExorcisedEpisode;
    private VampireUHCPlayer exorcist;
    private final List<UUID> alreadyExorcised = new ArrayList<>();

    @Override
    public boolean isVampire() {
        return false;
    }

    @Override
    public Camp getDefaultCamp() {
        return Camp.VILLAGEOIS;
    }

    @Override
    public void onAssign(VampireUHCPlayer player) {
        this.exorcist = player;
    }

    @Override
    public Component getDescription() {
        MiniMessage mm = MiniMessage.miniMessage();
        return mm.deserialize(   
            "<gray>Vous devez gagner avec le <green>village</green>. Pour ce faire, vous disposez de la capacité d'exorciser un joueur.</gray>\n\n"
            + "<dark_purple>▸</dark_purple> <gray>À chaque épisode, à l'aide de la commande <gold>/vuhc exorciser <joueur></gold>, vous pouvez exorciser un joueur, ce qui aura pour effets :</gray>\n\n"
            + "  <gray>→ De supprimer <gold>l'ensemble</gold> des marqueurs <dark_red>obscurs</dark_red> que portait le joueur.</gray>\n"
            + "  <gray>→ De vous donner la liste de <gold>l'ensemble</gold> des marqueurs <dark_red>obscurs</dark_red> que portait le joueur.</gray>\n"
            + "<red>⚠ Ce pouvoir n'est utilisable qu'une seule fois par joueur !</red>"
        );
    }

    @Override
    public String getName() {
        return "Exorciste";
    }

    // Pouvoir spécifique à l'exorciste :

    public void exorcisePlayer(VampireUHCPlayer target, MarkerManager markerManager, int current_episode) {
        if (exorcist == null || target == null) {
            return;
        }
        
        Player bukkitExorcist = Bukkit.getPlayer(exorcist.getUuid());

        if (bukkitExorcist == null) {
            return;
        }

        if (current_episode == lastExorcisedEpisode) {
            bukkitExorcist.sendMessage(MessageUtil.error("Vous ne pouvez exorciser un joueur qu'une seule fois par épisode !"));
            return;
        }

        if (alreadyExorcised.contains(target.getUuid())) {
            bukkitExorcist.sendMessage(MessageUtil.error("Le joueur " + target.getLastKnownName() + " a déjà été exorcisé !"));
            return;
        }

        alreadyExorcised.add(target.getUuid());

        List<MarkerType> markers = markerManager.getMarkerTypeOfAura(target.getUuid(), TightAuraTier.OBSCURE);
        markerManager.clearMarkersOfAura(target.getUuid(), Aura.OBSCURE);

        MiniMessage mm = MiniMessage.miniMessage();
        Component message = mm.deserialize("<dark_purple>Vous avez exorcisé <gold>" + target.getLastKnownName() + "</gold>. Vous ressentez la présence des marqueurs suivants :\n\n");

        for (MarkerType marker: markers) {
            message = message.append(marker.toComponent());
        }

        this.lastExorcisedEpisode = current_episode;
        bukkitExorcist.sendMessage(message);
    }

}
