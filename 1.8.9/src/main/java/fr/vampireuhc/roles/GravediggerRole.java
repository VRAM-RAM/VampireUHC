package fr.vampireuhc.roles;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Effect;
import org.bukkit.entity.Player;

import fr.vampireuhc.config.MessageUtil;
import fr.vampireuhc.markers.MarkerType;
import fr.vampireuhc.player.Camp;
import fr.vampireuhc.player.VampireUHCPlayer;

public class GravediggerRole implements Role {
    private VampireUHCPlayer gravedigger;

    // Cadavres indexés par clé stable : monde + bloc (ignore yaw/pitch).
    private final Map<String, List<MarkerType>> markersByKey = new HashMap<>();

    // Cadavres déjà exhumés : consulté uniquement par le Sosie (UsurpedGravedigger)
    // pour lui interdire un cadavre déjà exhumé par le vrai Fossoyeur (ou par
    // lui-même). Le vrai Fossoyeur n'est jamais bloqué. Remis à zéro en fin de partie.
    private static final Set<String> EXHUMED_CORPSES = new HashSet<>();

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
        EXHUMED_CORPSES.clear();
    }

    @Override
    public String getDescription() {
        return (
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

        // On crée les particules (pas d'API particules en 1.8 : effet MOBFLAME)
        location.getWorld().playEffect(location, Effect.MOBSPAWNER_FLAMES, 0);
        location.getWorld().playEffect(location.clone().add(0.3, 0.1, 0.2), Effect.MOBSPAWNER_FLAMES, 0);

        this.markersByKey.put(corpseKey(location), markers);
    }

    // Clé stable d'un cadavre : monde + coordonnées de bloc.
    public static String corpseKey(Location location) {
        return location.getWorld().getName() + ":"
                + location.getBlockX() + "," + location.getBlockY() + "," + location.getBlockZ();
    }

    // Registre des exhumations déjà réalisées : écrit par les deux Fossoyeurs
    // (vrai + Sosie), lu uniquement par le Sosie pour son blocage.
    public static boolean isCorpseExhumed(String key) {
        return EXHUMED_CORPSES.contains(key);
    }

    public static boolean markCorpseExhumed(String key) {
        return EXHUMED_CORPSES.add(key);
    }


    // Second pouvoir (actif) : /vuhc exhumer

    public void exhum(Location location) {
        if (gravedigger == null || location == null || location.getWorld() == null) {
            return;
        }
        Player bukkitGraveDigger = Bukkit.getPlayer(gravedigger.getUuid());

        if (bukkitGraveDigger == null || !bukkitGraveDigger.isOnline()) {
            return;
        }

        String key = corpseKey(location);

        // Le vrai Fossoyeur n'est jamais bloqué : il peut exhumer un cadavre
        // même si le Sosie (copie du pouvoir) l'a déjà exhumé. Une même
        // exhumation reste unique grâce au retrait de markersByKey ci-dessous.
        List<MarkerType> markers = markersByKey.remove(key);

        if (markers == null) {
            bukkitGraveDigger.sendMessage(MessageUtil.warn("Vous ne trouvez aucun cadavre à exhumer."));
            return;
        }
        markCorpseExhumed(key);
        String message = "<dark_purple>Vous exhumez un cadavre. Vous y trouvez :</dark_purple>\n\n";

        for (MarkerType marker: markers) {
            message += marker.toLegacy();
        }

        bukkitGraveDigger.sendMessage(MessageUtil.serialize(message));
    }
}
