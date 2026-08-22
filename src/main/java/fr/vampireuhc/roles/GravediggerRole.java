package fr.vampireuhc.roles;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;

import fr.vampireuhc.config.MessageUtil;
import fr.vampireuhc.markers.MarkerType;
import fr.vampireuhc.player.Camp;
import fr.vampireuhc.player.VampireUHCPlayer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;

public class GravediggerRole implements Role {
    private VampireUHCPlayer gravedigger;

    // Cadavres indexés par clé stable : monde + bloc (ignore yaw/pitch).
    private final Map<String, List<MarkerType>> markersByKey = new HashMap<>();

    @Override
    public Camp getDefaultCamp() {
        return Camp.VILLAGEOIS;
    }

    @Override
    public boolean isVampire() {
        return false;
    }

    @Override
    public void onAssign(VampireUHCPlayer player) {
        this.gravedigger = player;
    }

    @Override
    public void onGameEnd() {
        // Les clés référencent l'ANCIEN monde : le nouveau est régénéré avec un
        // autre terrain, ces cadavres seraient inexhumables. Pas de persistance
        // volontaire (décision) : une partie = une map.
        markersByKey.clear();
    }

    @Override
    public Component getDescription() {
        MiniMessage mm = MiniMessage.miniMessage();
        return mm.deserialize(
            "<gray>Vous avez le pouvoir d'exhumer les cadavres afin de connaître leurs marqueurs au moment de leur mort.</gray>\n\n"
            + "<dark_purple>▸</dark_purple> <gray>Vous distinguez les cadavres à l'aide des particules qu'ils émettent.</gray>\n\n"
            + "<dark_purple>▸</dark_purple> <gray>Lorsque vous vous trouvez à la position exacte du cadavre, executez <gold>/vuhc exhumer</gold> pour exhumer le cadavre.</gray>\n"
        );
    }

    @Override
    public String getName() {
        return "Fossoyeur";
    }

    // Premier pouvoir (passif) : voit les particules sur les cadavres.

    public void spawnParticlesAtLocation(Location location, List<MarkerType> markers) {
        if (gravedigger == null || location == null || location.getWorld() == null) {
            return;
        }

        Player bukkitGraveDigger = Bukkit.getPlayer(gravedigger.getUuid());

        if (bukkitGraveDigger == null || !bukkitGraveDigger.isOnline()) {
            return;
        }

        // On crée les particules
        bukkitGraveDigger.spawnParticle(
            Particle.FLAME,
            location,
            20,
            0.2,
            0.2,
            0.2,
            0
        );

        this.markersByKey.put(corpseKey(location), markers);
    }

    // Clé stable d'un cadavre : monde + coordonnées de bloc.
    private static String corpseKey(Location location) {
        return location.getWorld().getName() + ":"
                + location.getBlockX() + "," + location.getBlockY() + "," + location.getBlockZ();
    }


    // Second pouvoir (actif) : /vuhc exhumer

    public void exhum(Location location) {
        if (gravedigger == null || location == null || location.getWorld() == null) {
            return;
        }
        var bukkitGraveDigger = Bukkit.getPlayer(gravedigger.getUuid());

        if (bukkitGraveDigger == null || !bukkitGraveDigger.isOnline()) {
            return;
        }

        var markers = markersByKey.remove(corpseKey(location));

        if (markers == null) {
            bukkitGraveDigger.sendMessage(MessageUtil.warn("Vous ne trouvez aucun cadavre à exhumer."));
            return;
        }

        MiniMessage mm = MiniMessage.miniMessage();
        Component message = mm.deserialize("<dark_purple>Vous exhumez un cadavre. Vous y trouvez :</dark_purple>\n\n");

        for (MarkerType marker: markers) {
            message = message.append(marker.toComponent());
        }

        bukkitGraveDigger.sendMessage(message);
    }
}
