package fr.vampireuhc.roles;

import fr.vampireuhc.markers.MarkerManager;
import fr.vampireuhc.markers.MarkerType;
import fr.vampireuhc.player.VampireUHCPlayer;
import fr.vampireuhc.player.Camp;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

public class WeaverRole implements Role {
    private VampireUHCPlayer weaver;
    private int thread;

    public WeaverRole(VampireUHCPlayer player) {
        this.weaver = player;
    }

    @Override
    public boolean isVampire() {
        return false;
    }

    @Override
    public String getDescription() {
        return "TODO";
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

    // Pouvoir spécial : dépôt d'un fil sur un joueur

    public  void weavePlayer(MarkerManager manager, VampireUHCPlayer target) {
        if (weaver == null) {
            return;
        }

        
        // On cast le tisseur pour envoyer les messages
        Player bukkitWeaver = Bukkit.getPlayer(weaver.getUuid());

        if (bukkitWeaver == null) {
            return;
        }

        if (target == weaver) {
            bukkitWeaver.sendMessage(ChatColor.RED + "Vous ne pouvez pas vous tisser vous-même !");
            return;
        }


        // Si la cible a déjà un marqueur Fil, impossible de poser un autre marqueur
        if (manager.hasMarker(target.getUuid(), MarkerType.FIL)) {
            bukkitWeaver.sendMessage(ChatColor.GRAY + "Le joueur " + ChatColor.DARK_BLUE + target.getLastKnownName() + ChatColor.GRAY + " appartient déjà à votre toîle !");
            return;
        }

        // Si le nombre max de fils à été posé, on return
        if (thread >= 4) {
            bukkitWeaver.sendMessage(ChatColor.RED + "Vous avez déjà posé 4 fils !");
            return;
        }

        // On cast les joueurs bukkit à partir des joueurs VampireUHC
        Player bukkitTarget = Bukkit.getPlayer(target.getUuid());

        // En cas d'erreur, on retourne
        if (bukkitTarget == null) {
            bukkitWeaver.sendMessage(ChatColor.RED + "Le joueur que vous ciblez n'est pas connecté.");
            return;
        }

        // Si la target ne se trouve pas dans la range de 20 blocs de rayon, on retourne
        if (!isWithinRadius(bukkitWeaver, bukkitTarget, 20)) {
            bukkitWeaver.sendMessage(ChatColor.RED + "Le joueur que vous ciblez n'est pas suffisamment proche de vous !");
            return;
        }

        // On ajoute le marqueur
        manager.addMarker(bukkitTarget.getUniqueId(), MarkerType.FIL, bukkitWeaver.getUniqueId());
        this.thread += 1;
        bukkitWeaver.sendMessage(ChatColor.GRAY + "Le joueur " + ChatColor.DARK_BLUE + target.getLastKnownName() + ChatColor.GRAY + " a été ajouté à votre toîle !");
        return;
    }

    public void tryInformDeathOfNodeAndDestroyWeb(MarkerManager manager, VampireUHCPlayer deadNode) {
        // On passe la condition de la toile de trois noeuds minimum des le debut pour eviter des casts inutiles
        if (weaver == null || thread < 3) {
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
        
        // On supprime tous les marqueurs fil
        manager.clearMarkersOfTypeOnAllPlayers(MarkerType.FIL);
        // Et on reinitialise le nombre de fils/ noeuds
        this.thread = 0;

        bukkitWeaver.sendMessage(ChatColor.DARK_RED + "Le noeud " + ChatColor.GOLD + deadNode.getLastKnownName() + ChatColor.DARK_RED + " est décédé. Son aura était "  + ChatColor.GOLD + deadNodeAura + ChatColor.DARK_RED + ". Votre toîle s'effondre.");
    }


    public void tryInformMurderByNodeOfWeb(MarkerManager manager, VampireUHCPlayer killer) {
        // On passe la condition de la toile de trois noeuds minimum des le debut pour eviter des casts inutiles
        if (weaver == null || thread < 3) {
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

        bukkitWeaver.sendMessage(ChatColor.DARK_RED + "Un noeud de votre réseau a assassiné.");
    }


    // Helper pour savoir si le joueur se trouve dans le rayon du tisseur
    private boolean isWithinRadius(Player player1, Player player2, double radius) {
        return player1.getLocation().distanceSquared(player2.getLocation()) <= radius * radius;
    }
}
