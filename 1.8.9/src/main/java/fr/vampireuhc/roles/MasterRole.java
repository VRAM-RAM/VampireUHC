package fr.vampireuhc.roles;

import fr.vampireuhc.VampireUHC;
import fr.vampireuhc.config.ConfigManager;
import fr.vampireuhc.config.MessageUtil;
import fr.vampireuhc.markers.MarkerType;
import fr.vampireuhc.markers.MarkerManager;
import fr.vampireuhc.player.VampireUHCPlayer;
import fr.vampireuhc.player.Camp;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;

import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

// On définit le rôle "Maitre". 
public class MasterRole implements Role {
    private VampireUHCPlayer master;
    private int lastMarkedEpisode = -1;

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
    public Component getDescription() {
        ConfigManager config = VampireUHC.getInstance().getConfigManager();
        int episodeLength = config.getEpisodeLength();
        int toInfect = config.getMarksToInfect();
        MiniMessage mm = MiniMessage.miniMessage();
        return mm.deserialize(
            "<gray>Vous êtes le chef des vampires. Vous avez <yellow>" + (8) + " coeurs</yellow> au lieu de 10.</gray>\n\n"
            + "<dark_purple>▸</dark_purple> <gray>Toutes les <yellow>" + episodeLength + " minutes</yellow>, vous pouvez poser une marque Maître sur un joueur non-vampire.</gray>\n"
            + "<dark_purple>▸</dark_purple> <gray>Vous ne votez pas pour les marques vampires, mais tranchez en cas d'égalité.</gray>\n\n"
            + "<bold><dark_purple>Effets des marques Maître :</dark_purple></bold>\n"
            + "  <gray>• 1 marque → Aucun effet visible, mais l'aura s'obscurcit.</gray>\n"
            + "  <gray>• 2 marques → Le joueur ne gagne plus qu'<yellow>1 cœur d'absorption</yellow> par pomme d'or.</gray>\n"
            + "  <gold>• " + toInfect + " marques → Le joueur est <red>infecté</red> et rejoint les vampires !</gold>\n\n"
            + "<red>⚠ Attention :</red> <gray>après une infection, toutes les marques Maître disparaissent.</gray>"
        );
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

        // Le Maitre est volontairement fragile : ~8 coeurs au lieu de 10.
        // Le max health est posé par RoleBuffManager (autorité centrale) ;
        // application immédiate ici, sans full heal (le serveur démarre pleine vie).
        fr.vampireuhc.VampireUHC.getInstance().getBuffManager().refreshMaxHealth(player.getUuid());
    }

    // Restauration de l'état après un redémarrage.
    public void restoreState(int lastMarkedEpisode) {
        this.lastMarkedEpisode = lastMarkedEpisode;
    }

    public int getLastMarkedEpisode() {
        return lastMarkedEpisode;
    }

    // Maintenant, les pouvoirs spécifiques au maitre :

    // Marquer un joueur
    public boolean markPlayer(MarkerManager markerManager, VampireUHCPlayer target, int currentEpisode) {
        if (master == null) {
            return false;
        }
        if (target.getCamp() == Camp.VAMPIRE) {
            return false;
        }
        // Pas de marque sur un cadavre : la charge d'épisode serait consommée
        // pour rien (et infect() s'appliquerait à un mort).
        if (!target.isAlive()) {
            return false;
        }
        if (lastMarkedEpisode == currentEpisode) {
            return false;
        }
        this.lastMarkedEpisode = currentEpisode;

        // La protection Salvation bloque la marque (elle est consommée),
        // mais le Maitre pense quand même que son action a fonctionné.
        boolean applied = markerManager.tryApplyMark(target.getUuid(), MarkerType.MARQUE_MAITRE, master.getUuid());

        ConfigManager config = VampireUHC.getInstance().getConfigManager();

        var bukkitMaster = Bukkit.getPlayer(master.getUuid());
        if (bukkitMaster != null) {
            bukkitMaster.sendMessage(MessageUtil.successTarget("Marque Maître posée sur", target.getLastKnownName()));
        }

        // Si il y a assez de marqueurs sur un joueur, il est infecté
        if (applied && markerManager.countMarkers(target.getUuid(), MarkerType.MARQUE_MAITRE) >= config.getMarksToInfect()) {
            infect(markerManager, target);
        }
        return true;
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
                bukkitMaster.sendMessage(MessageUtil.successTarget("Le joueur", target.getLastKnownName() + " a été infecté !"));
            }
        }

        // Infection du joueur cible
        target.infect();

        // Notification au joueur infecté
        Player bukkitTarget = Bukkit.getPlayer(target.getUuid());
        if (bukkitTarget != null) {
            bukkitTarget.sendMessage(MessageUtil.warn("Vous avez été infecté et devez gagner avec les vampires ! /vuhc role pour en savoir plus."));
        }
    }
}
