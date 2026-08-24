package fr.vampireuhc.roles;

import fr.vampireuhc.markers.Marker;
import fr.vampireuhc.markers.MarkerManager;
import fr.vampireuhc.markers.MarkerType;
import fr.vampireuhc.player.VampireUHCPlayer;
import fr.vampireuhc.player.Camp;
import fr.vampireuhc.config.MessageUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.UUID;

public class WeaverRole implements Role {
    private VampireUHCPlayer weaver;

    public WeaverRole(VampireUHCPlayer player) {
        this.weaver = player;
    }

    @Override
    public boolean isVampire() {
        return false;
    }

    @Override
    public Component getDescription() {
        MiniMessage mm = MiniMessage.miniMessage();
        return mm.deserialize(
            "<gray>Vous tissez un réseau de joueurs pour observer les événements qui s'y produisent.</gray>\n\n"
            + "<dark_purple>▸</dark_purple> <gray>Ajouter un joueur à votre réseau : <gold>/vuhc tisser <joueur></gold></gray>\n"
            + "<dark_purple>▸</dark_purple> <gray>Le joueur doit se trouver à moins de <yellow>20 blocs</yellow>.</gray>\n"
            + "<dark_purple>▸</dark_purple> <gray>Constituez un réseau de <yellow>3 à 4 joueurs</yellow> pour activer votre pouvoir.</gray>\n\n"
            + "<bold><dark_purple>Événements :</dark_purple></bold>\n"
            + "  <red>• Mort d'un noeud de votre réseau</red> → <gray>Vous apprenez son nom + son aura exacte. Le réseau s'effondre.</gray>\n"
            + "  <dark_red>• Meurtre par un noeud de votre réseau</dark_red> → <gray>Vous êtes notifié. Le réseau ne s'effondre pas.</gray>\n\n"
            + "<gray>Tant que le réseau contient moins de 3 personnes, il ne produit aucun effet.</gray>"
        );
    }

    @Override
    public String getName() {
        return "Tisseur";
    }

    @Override
    public Camp getDefaultCamp() {
        return Camp.VILLAGEOIS;
    }

    @Override
    public void onAssign(VampireUHCPlayer vampireUHCPlayer) {
        this.weaver = vampireUHCPlayer;
    }

    // Nombre réel de fils actifs : dérivé des marqueurs à la demande, donc
    // insensible aux vols de marques (assassin) et aux switches (gremlin).
    private int countThreads(MarkerManager manager) {
        if (weaver == null) {
            return 0;
        }
        int count = 0;
        for (UUID id : manager.getAllPlayers()) {
            for (Marker m : manager.getMarkers(id, MarkerType.FIL)) {
                if (weaver.getUuid().equals(m.getSource())) {
                    count++;
                }
            }
        }
        return count;
    }

    // Pouvoir spécial : dépôt d'un fil sur un joueur

    public void weavePlayer(MarkerManager manager, VampireUHCPlayer target) {
        if (weaver == null) {
            return;
        }

        
        // On cast le tisseur pour envoyer les messages
        Player bukkitWeaver = Bukkit.getPlayer(weaver.getUuid());

        if (bukkitWeaver == null) {
            return;
        }

        if (target == weaver) {
            bukkitWeaver.sendMessage(MessageUtil.error("Vous ne pouvez pas vous tisser vous-même !"));
            return;
        }


        // Si la cible a déjà un marqueur Fil, impossible de poser un autre marqueur
        if (manager.hasMarker(target.getUuid(), MarkerType.FIL)) {
            bukkitWeaver.sendMessage(MessageUtil.info("Le joueur <dark_blue>" + target.getLastKnownName() + "</dark_blue> appartient déjà à votre toîle !"));
            return;
        }

        // Si le nombre max de fils à été posé, on return
        if (countThreads(manager) >= 4) {
            bukkitWeaver.sendMessage(MessageUtil.error("Vous avez déjà posé 4 fils !"));
            return;
        }

        // On cast les joueurs bukkit à partir des joueurs VampireUHC
        Player bukkitTarget = Bukkit.getPlayer(target.getUuid());

        // En cas d'erreur, on retourne
        if (bukkitTarget == null) {
            bukkitWeaver.sendMessage(MessageUtil.error("Le joueur que vous ciblez n'est pas connecté."));
            return;
        }

        // Si la target ne se trouve pas dans la range de 20 blocs de rayon, on retourne
        if (!isWithinRadius(bukkitWeaver, bukkitTarget, 20)) {
            bukkitWeaver.sendMessage(MessageUtil.error("Le joueur que vous ciblez n'est pas suffisamment proche de vous !"));
            return;
        }

        // On ajoute le marqueur
        manager.addMarker(bukkitTarget.getUniqueId(), MarkerType.FIL, bukkitWeaver.getUniqueId());
        bukkitWeaver.sendMessage(MessageUtil.successTarget("Le joueur", target.getLastKnownName() + " a été ajouté à votre toîle !"));
        return;
    }

    public void tryInformDeathOfNodeAndDestroyWeb(MarkerManager manager, VampireUHCPlayer deadNode) {
        // On passe la condition de la toile de trois noeuds minimum des le debut pour eviter des casts inutiles
        if (weaver == null || countThreads(manager) < 3) {
            return;
        }

        if (!manager.hasMarker(deadNode.getUuid(), MarkerType.FIL)) {
            return;
        }

        // On cast le tisseur pour envoyer le message
        Player bukkitWeaver = Bukkit.getPlayer(weaver.getUuid());

        if (bukkitWeaver == null) {
            return;
        }

        // On recupere l'aura du joueur mort
        String deadNodeAura = manager.computeAuraTier(deadNode.getUuid()).toString();
        
        // La toile s'effondre
        collapseWeb(manager);

        MiniMessage mm = MiniMessage.miniMessage();
        bukkitWeaver.sendMessage(mm.deserialize(
            "<dark_red>Le noeud <gold>" + deadNode.getLastKnownName() + "</gold> est décédé. Son aura était <gold>" + deadNodeAura + "</gold>. Votre toîle s'effondre.</dark_red>"
        ));
    }

    // Effondre la toile : supprime tous les fils. Le compteur, dérivé des
    // marqueurs, retombe à zéro automatiquement.
    public void collapseWeb(MarkerManager manager) {
        manager.clearMarkersOfTypeOnAllPlayers(MarkerType.FIL);
    }


    public void tryInformMurderByNodeOfWeb(MarkerManager manager, VampireUHCPlayer killer) {
        // On passe la condition de la toile de trois noeuds minimum des le debut pour eviter des casts inutiles
        if (weaver == null || countThreads(manager) < 3) {
            return;
        }

        if (!manager.hasMarker(killer.getUuid(), MarkerType.FIL)) {
            return;
        }

        // On cast le tisseur pour envoyer le message
        Player bukkitWeaver = Bukkit.getPlayer(weaver.getUuid());

        if (bukkitWeaver == null) {
            return;
        }

        bukkitWeaver.sendMessage(MessageUtil.warn("Un noeud de votre réseau a assassiné."));
    }


    // Helper pour savoir si le joueur se trouve dans le rayon du tisseur
    private boolean isWithinRadius(Player player1, Player player2, double radius) {
        return player1.getLocation().distanceSquared(player2.getLocation()) <= radius * radius;
    }
}
