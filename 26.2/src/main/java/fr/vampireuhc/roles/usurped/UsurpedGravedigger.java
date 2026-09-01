package fr.vampireuhc.roles.usurped;

import fr.vampireuhc.config.MessageUtil;
import fr.vampireuhc.markers.MarkerType;
import fr.vampireuhc.player.VampireUHCPlayer;
import fr.vampireuhc.roles.GravediggerRole;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;

/**
 * Fossoyeur usurpé (26.2) : pouvoir exact (particules sur les cadavres +
 * exhumer la liste des marqueurs du défunt). Seule restriction (asymétrique) :
 * si le vrai Fossoyeur a déjà exhumé le cadavre, le Sosie ne peut pas le
 * ré-exhumer (registre partagé en lecture). Le vrai Fossoyeur, lui, peut
 * exhumer un cadavre déjà exhumé par le Sosie.
 */
public class UsurpedGravedigger implements UsurpedPower {

    private VampireUHCPlayer sosie;

    // Cadavres indexés par clé stable (même schéma que le vrai Fossoyeur).
    private final Map<String, List<MarkerType>> markersByKey = new HashMap<>();

    @Override
    public String getName() {
        return "Fossoyeur";
    }

    @Override
    public void onEnter(VampireUHCPlayer doppelganger) {
        this.sosie = doppelganger;
    }

    @Override
    public void onExit() {
        markersByKey.clear();
    }

    @Override
    public void onGameEnd() {
        markersByKey.clear();
    }

    public void spawnParticlesAtLocation(Location location, List<MarkerType> markers) {
        if (sosie == null || location == null || location.getWorld() == null) {
            return;
        }

        Player bukkitSosie = Bukkit.getPlayer(sosie.getUuid());
        if (bukkitSosie == null || !bukkitSosie.isOnline()) {
            return;
        }

        bukkitSosie.spawnParticle(Particle.FLAME, location, 20, 0.2, 0.2, 0.2, 0);

        this.markersByKey.put(GravediggerRole.corpseKey(location), markers);
    }

    public void exhum(Location location) {
        if (sosie == null || location == null || location.getWorld() == null) {
            return;
        }
        Player bukkitSosie = Bukkit.getPlayer(sosie.getUuid());
        if (bukkitSosie == null || !bukkitSosie.isOnline()) {
            return;
        }

        String key = GravediggerRole.corpseKey(location);

        // Le Sosie est bloqué si le vrai Fossoyeur (ou lui-même) a déjà exhumé
        // ce cadavre. L'inverse n'est pas vrai : le réel peut exhumer après lui.
        if (GravediggerRole.isCorpseExhumed(key)) {
            bukkitSosie.sendMessage(MessageUtil.warn("Ce cadavre a déjà été exhumé."));
            return;
        }

        List<MarkerType> markers = markersByKey.remove(key);
        if (markers == null) {
            bukkitSosie.sendMessage(MessageUtil.warn("Vous ne trouvez aucun cadavre à exhumer."));
            return;
        }

        GravediggerRole.markCorpseExhumed(key);

        var message = net.kyori.adventure.text.minimessage.MiniMessage.miniMessage()
                .deserialize("<dark_purple>Vous exhumez un cadavre. Vous y trouvez :</dark_purple>\n\n");
        for (MarkerType marker : markers) {
            message = message.append(marker.toComponent());
        }
        bukkitSosie.sendMessage(message);
    }
}