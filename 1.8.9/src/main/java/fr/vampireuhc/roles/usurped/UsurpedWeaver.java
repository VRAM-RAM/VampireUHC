package fr.vampireuhc.roles.usurped;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.UUID;

import fr.vampireuhc.config.MessageUtil;
import fr.vampireuhc.markers.Marker;
import fr.vampireuhc.markers.MarkerManager;
import fr.vampireuhc.markers.MarkerType;
import fr.vampireuhc.player.VampireUHCPlayer;

/**
 * Tisseur copié par le Sosie : réseau indépendant basé sur la variante
 * FIL_DOPPELGANGER (aucune interférence avec le vrai Tisseur), rayon réduit à
 * 5 blocs (au lieu de 20).
 */
public class UsurpedWeaver implements UsurpedPower {

    private VampireUHCPlayer sosie;

    @Override
    public String getName() {
        return "Tisseur";
    }

    @Override
    public void onEnter(VampireUHCPlayer doppelganger) {
        this.sosie = doppelganger;
    }

    @Override
    public void onExit() {
    }

    // Nombre réel de fils actifs : dérivé des marqueurs du Sosie à la demande.
    private int countThreads(MarkerManager manager) {
        if (sosie == null) {
            return 0;
        }
        int count = 0;
        for (UUID id : manager.getAllPlayers()) {
            for (Marker m : manager.getMarkers(id, MarkerType.FIL_DOPPELGANGER)) {
                if (sosie.getUuid().equals(m.getSource())) {
                    count++;
                }
            }
        }
        return count;
    }

    public void weavePlayer(MarkerManager manager, VampireUHCPlayer target) {
        if (sosie == null) {
            return;
        }

        Player bukkitSosie = Bukkit.getPlayer(sosie.getUuid());

        if (bukkitSosie == null) {
            return;
        }

        if (target == sosie) {
            bukkitSosie.sendMessage(MessageUtil.error("Vous ne pouvez pas vous tisser vous-même !"));
            return;
        }

        if (manager.hasMarker(target.getUuid(), MarkerType.FIL_DOPPELGANGER)) {
            bukkitSosie.sendMessage(MessageUtil.info("Le joueur <dark_blue>" + target.getLastKnownName() + "</dark_blue> appartient déjà à votre toîle !"));
            return;
        }

        if (countThreads(manager) >= 4) {
            bukkitSosie.sendMessage(MessageUtil.error("Vous avez déjà posé 4 fils !"));
            return;
        }

        Player bukkitTarget = Bukkit.getPlayer(target.getUuid());

        if (bukkitTarget == null) {
            bukkitSosie.sendMessage(MessageUtil.error("Le joueur que vous ciblez n'est pas connecté."));
            return;
        }

        // Imperfection du Sosie : rayon réduit (5 blocs au lieu de 20).
        if (!isWithinRadius(bukkitSosie, bukkitTarget, 5)) {
            bukkitSosie.sendMessage(MessageUtil.error("Le joueur que vous ciblez n'est pas suffisamment proche de vous !"));
            return;
        }

        manager.addMarker(bukkitTarget.getUniqueId(), MarkerType.FIL_DOPPELGANGER, bukkitSosie.getUniqueId());
        bukkitSosie.sendMessage(MessageUtil.successTarget("Le joueur", target.getLastKnownName() + " a été ajouté à votre toîle !"));
    }

    public void tryInformDeathOfNodeAndDestroyWeb(MarkerManager manager, VampireUHCPlayer deadNode) {
        if (sosie == null || countThreads(manager) < 3) {
            return;
        }

        if (!manager.hasMarker(deadNode.getUuid(), MarkerType.FIL_DOPPELGANGER)) {
            return;
        }

        Player bukkitSosie = Bukkit.getPlayer(sosie.getUuid());

        if (bukkitSosie == null) {
            return;
        }

        String deadNodeAura = manager.computeAuraTier(deadNode.getUuid()).toString();

        collapseWeb(manager);
        bukkitSosie.sendMessage((
            "<dark_red>Le noeud <gold>" + deadNode.getLastKnownName() + "</gold> est décédé. Son aura était <gold>" + deadNodeAura + "</gold>. Votre toîle s'effondre.</dark_red>"
        ));
    }

    // Effondre la toile du Sosie : supprime tous ses fils (variante DOPPELGANGER).
    public void collapseWeb(MarkerManager manager) {
        manager.clearMarkersOfTypeOnAllPlayers(MarkerType.FIL_DOPPELGANGER);
    }

    public void tryInformMurderByNodeOfWeb(MarkerManager manager, VampireUHCPlayer killer) {
        if (sosie == null || countThreads(manager) < 3) {
            return;
        }

        if (!manager.hasMarker(killer.getUuid(), MarkerType.FIL_DOPPELGANGER)) {
            return;
        }

        Player bukkitSosie = Bukkit.getPlayer(sosie.getUuid());

        if (bukkitSosie == null) {
            return;
        }

        bukkitSosie.sendMessage(MessageUtil.warn("Un noeud de votre réseau a assassiné."));
    }

    private boolean isWithinRadius(Player player1, Player player2, double radius) {
        return player1.getLocation().distanceSquared(player2.getLocation()) <= radius * radius;
    }

    @Override
    public void saveState(com.google.gson.JsonObject obj) {
        // Le réseau est dérivé des marqueurs persistés : aucun état propre.
    }

    @Override
    public void restoreState(com.google.gson.JsonObject obj) {
    }
}