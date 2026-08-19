package fr.vampireuhc.roles;

import fr.vampireuhc.player.VampireUHCPlayer;
import fr.vampireuhc.player.Camp;

import java.util.UUID;

import fr.vampireuhc.markers.MarkerManager;
import fr.vampireuhc.markers.MarkerType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;



public class SaviorRole implements Role {
    private VampireUHCPlayer salva;
    private boolean applied_this_episode;
    private int episode;
    private UUID last_applied_Uuid;

    public SaviorRole(VampireUHCPlayer player) {
        this.salva = player;
    }

    @Override
    public boolean isVampire() {
        return false;
    }

    @Override
    public Component getDescription() {
        MiniMessage mm = MiniMessage.miniMessage();
        return mm.deserialize(
            "<gray>Vous protégez secrètement les villageois en semant la confusion chez les vampires.</gray>\n\n"
            + "<dark_purple>▸</dark_purple> <gray>À chaque épisode, placez une marque <green>Salvation</green> sur un joueur : <gold>/vuhc proteger <joueur></gold></gray>\n"
            + "<dark_purple>▸</dark_purple> <gray>La marque possède une <yellow>aura lumineuse</yellow>.</gray>\n\n"
            + "<bold><dark_purple>Effet :</dark_purple></bold>\n"
            + "  <gray>Si un vampire ou le Maître cible un joueur portant Salvation :</gray>\n"
            + "  <gray>• La marque vampire <red>n'est pas appliquée</red>.</gray>\n"
            + "  <gray>• Les vampires croient que leur action a fonctionné.</gray>\n"
            + "  <gray>• La marque Salvation disparaît.</gray>\n\n"
            + "<gray>Vous créez de fausses informations tout en protégeant ceux que vous jugez sûrs.</gray>"
        );
    }

    @Override
    public String getName() {
        return "Salvateur";
    }

    @Override
    public Camp getDefaultCamp() {
        return Camp.VILLAGEOIS;
    }

    @Override
    public void onAssign(VampireUHCPlayer vampireUHCPlayer) {
        this.salva = vampireUHCPlayer;
    }

    // Pouvoir spécifique au Salvateur : la marque de la salvation

    public boolean applySalvation(MarkerManager manager, VampireUHCPlayer target, int current_episode) {
        if (salva == null) {
            return false;
        } 

        if (episode == current_episode || applied_this_episode == true) {
            return false;
        }

        // On ne peut pas protéger le même joueur deux épisodes consécutifs
        if (last_applied_Uuid != null && last_applied_Uuid.equals(target.getUuid())) {
            return false;
        }

        this.episode = current_episode;

        // On clear l'ancien marqueur salvation
        manager.clearMarkersOfType(last_applied_Uuid, MarkerType.SALVATION);

        manager.addMarker(target.getUuid(), MarkerType.SALVATION, salva.getUuid());
        
        this.last_applied_Uuid = target.getUuid();
        this.applied_this_episode = true;
        return true;
    }
}
