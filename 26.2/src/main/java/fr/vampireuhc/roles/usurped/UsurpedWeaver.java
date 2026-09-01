package fr.vampireuhc.roles.usurped;

import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import com.google.gson.JsonObject;

import fr.vampireuhc.config.MessageUtil;
import fr.vampireuhc.markers.Marker;
import fr.vampireuhc.markers.MarkerManager;
import fr.vampireuhc.markers.MarkerType;
import fr.vampireuhc.player.VampireUHCPlayer;
import net.kyori.adventure.text.minimessage.MiniMessage;

/**
 * Tisseur copié par le Sosie : réseau de fils identique au vrai Tisseur, mais
 * le rayon de pose est réduit à 5 blocs (vs 20). Le réseau (marques
 * FIL_DOPPELGANGER) s'effondre à la mort du Sosie et fonctionne à partir de 3
 * nœuds, comme le vrai.
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

        Player bukkitWeaver = Bukkit.getPlayer(sosie.getUuid());
        if (bukkitWeaver == null) {
            return;
        }

        if (target == sosie) {
            bukkitWeaver.sendMessage(MessageUtil.error("Vous ne pouvez pas vous tisser vous-même !"));
            return;
        }

        if (manager.hasMarker(target.getUuid(), MarkerType.FIL_DOPPELGANGER)) {
            bukkitWeaver.sendMessage(MessageUtil.info("Le joueur <dark_blue>" + target.getLastKnownName() + "</dark_blue> appartient déjà à votre toîle !"));
            return;
        }

        if (countThreads(manager) >= 4) {
            bukkitWeaver.sendMessage(MessageUtil.error("Vous avez déjà posé 4 fils !"));
            return;
        }

        Player bukkitTarget = Bukkit.getPlayer(target.getUuid());
        if (bukkitTarget == null) {
            bukkitWeaver.sendMessage(MessageUtil.error("Le joueur que vous ciblez n'est pas connecté."));
            return;
        }

        // Rayon réduit pour le Sosie : 5 blocs (vs 20 pour le vrai Tisseur).
        if (!isWithinRadius(bukkitWeaver, bukkitTarget, 5)) {
            bukkitWeaver.sendMessage(MessageUtil.error("Le joueur que vous ciblez n'est pas suffisamment proche de vous !"));
            return;
        }

        manager.addMarker(bukkitTarget.getUniqueId(), MarkerType.FIL_DOPPELGANGER, bukkitWeaver.getUniqueId());
        bukkitWeaver.sendMessage(MessageUtil.successTarget("Le joueur", target.getLastKnownName() + " a été ajouté à votre toîle !"));
    }

    public void tryInformDeathOfNodeAndDestroyWeb(MarkerManager manager, VampireUHCPlayer deadNode) {
        if (sosie == null || countThreads(manager) < 3) {
            return;
        }

        if (!manager.hasMarker(deadNode.getUuid(), MarkerType.FIL_DOPPELGANGER)) {
            return;
        }

        Player bukkitWeaver = Bukkit.getPlayer(sosie.getUuid());
        if (bukkitWeaver == null) {
            return;
        }

        String deadNodeAura = manager.computeAuraTier(deadNode.getUuid()).toString();
        collapseWeb(manager);

        MiniMessage mm = MiniMessage.miniMessage();
        bukkitWeaver.sendMessage(mm.deserialize(
            "<dark_red>Le noeud <gold>" + deadNode.getLastKnownName() + "</gold> est décédé. Son aura était <gold>" + deadNodeAura + "</gold>. Votre toîle s'effondre.</dark_red>"
        ));
    }

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

        Player bukkitWeaver = Bukkit.getPlayer(sosie.getUuid());
        if (bukkitWeaver == null) {
            return;
        }

        bukkitWeaver.sendMessage(MessageUtil.warn("Un noeud de votre réseau a assassiné."));
    }

    private boolean isWithinRadius(Player player1, Player player2, double radius) {
        return player1.getLocation().distanceSquared(player2.getLocation()) <= radius * radius;
    }

    @Override
    public void saveState(JsonObject obj) {
    }

    @Override
    public void restoreState(JsonObject obj) {
    }
}