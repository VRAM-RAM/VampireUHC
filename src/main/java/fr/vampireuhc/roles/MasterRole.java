package fr.vampireuhc.roles;

import fr.vampireuhc.markers.MarkerType;
import fr.vampireuhc.markers.MarkerManager;
import fr.vampireuhc.player.VampireUHCPlayer;
import fr.vampireuhc.player.Camp;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;

// On définit le rôle "Maitre". 
public class MasterRole implements Role {
    private VampireUHCPlayer master;

    @Override
    public boolean isVampire() {
        return true;
    }

    // Pour chaque rôle, on doit définir son nom :
    @Override
    public String getName() {
        return "Maître";
    }

    // Sa description :
    @Override
    public String getDescription() {
        return "Le Maître est le chef des vampires. Toutes les X minutes, il peut poser une marque Maitre sur le joueur non-vampire de son choix. Il ne vote pas pour les marques vampires, mais, en cas d'égalité, prend la décision finale. Voici les effets qu'écopperont les joueurs marqués par la marque Maitre : \n 1 marque => Aucun effet, ne fait qu'obscursir l'aura du joueur marqué. \n 2 marques => Lorsque le joueur marqué consomme en pomme d'or, il ne gagne qu'un seul coeur d'absorption. \n 3 marques => Le joueur marqué est infecté : il rejoint le camp des vampires et doit gagner avec ce dernier. \n Attention ! Une fois un joueur infecté, toutes les marques Maitre disparaissent, vous ne pourrez plus en poser et ainsi plus aucun joueur ne sera affecté par la perte d'absorption !";
    }
    
    // Mais aussi son camp :
    @Override 
    public Camp getDefaultCamp() {
        return fr.vampireuhc.player.Camp.VAMPIRE;
    }

    // Et l'assignation à un joueur
    @Override
    public void onAssign(VampireUHCPlayer player) {
        this.master = player;
    }

    // Maintenant, les pouvoirs spécifiques au maitre :

    // Marquer un joueur
    public void markPlayer(MarkerManager markerManager, VampireUHCPlayer target) {
        if (master == null) {
            return;
        }
        if (target.getCamp() != Camp.VAMPIRE)  {
            markerManager.addMarker(target.getUuid(), MarkerType.MARQUE_MAITRE, master.getUuid());

            // Si il y a 3 marqueurs sur un joueur, il est infecté
            if (markerManager.countMarkers(target.getUuid(), MarkerType.MARQUE_MAITRE) >= 3) {
                infect(markerManager, target);
            } else {
                // Sinon, renvoie juste un message de succès au Maitre
                var bukkitMaster = Bukkit.getPlayer(master.getUuid());
                if (bukkitMaster != null) {
                    bukkitMaster.sendMessage(ChatColor.DARK_PURPLE + "Vous avez posé votre marque maitre sur " + ChatColor.GOLD +  target.getLastKnownName() + ChatColor.DARK_PURPLE + ".");
                }
            }
        }
    }

    // Helper pour infecter un joueur
    private void infect(MarkerManager markerManager, VampireUHCPlayer target) {
        
        for (UUID id : markerManager.getAllPlayers()) {
            markerManager.clearMarkersofType(id, MarkerType.MARQUE_MAITRE);
        }

        // Notification au Maitre
        if (master != null) {
            var bukkitMaster = Bukkit.getPlayer(master.getUuid());
            if (bukkitMaster != null) {
                bukkitMaster.sendMessage(ChatColor.RED + "Le joueur " + ChatColor.GOLD +  target.getLastKnownName() + 
                    ChatColor.RED + " a été infecté !");
            }
        }

        // Infection du joueur cible
        target.infect();
    }
}