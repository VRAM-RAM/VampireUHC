package fr.vampireuhc.roles;

import fr.vampireuhc.player.VampireUHCPlayer;
import fr.vampireuhc.player.Camp;

import java.util.UUID;

import fr.vampireuhc.markers.MarkerManager;
import fr.vampireuhc.markers.MarkerType;



public class SaviorRole implements Role {
    private VampireUHCPlayer salva;
    private int lastAppliedEpisode = -1;
    private UUID last_applied_Uuid;

    public SaviorRole(VampireUHCPlayer player) {
        this.salva = player;
    }

    @Override
    public boolean isVampire() {
        return false;
    }

    @Override
    public String getDescription() {
        return (
            "<gray>Vous protégez secrètement les villageois en semant la confusion chez les vampires.</gray>\n\n"
            + "<dark_purple>▸</dark_purple> <gray>À chaque épisode, placez une marque <green>Salvation</green> sur un joueur : <gold>/vuhc proteger <joueur></gold></gray>\n"
            + "<dark_purple>▸</dark_purple> <gray>La marque possède une <yellow>aura lumineuse</yellow>.</gray>\n\n"
            + "<bold><dark_purple>Effet :</dark_purple></bold>\n"
            + "  <gray>Si un vampire ou le Maître cible un joueur portant Salvation :</gray>\n"
            + "  <gray>• La marque vampire <red>n'est pas appliquée</red>.</gray>\n"
            + "  <gray>• Les vampires croient que leur action a fonctionné.</gray>\n"
            + "  <gray>• La marque Salvation disparaît.</gray>\n\n"
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

    // Restauration de l'état après un redémarrage.
    public void restoreState(int lastAppliedEpisode, UUID lastTarget) {
        this.lastAppliedEpisode = lastAppliedEpisode;
        this.last_applied_Uuid = lastTarget;
    }

    public int getLastAppliedEpisode() {
        return lastAppliedEpisode;
    }

    public UUID getLastAppliedUuid() {
        return last_applied_Uuid;
    }

    // Pouvoir spécifique au Salvateur : la marque de la salvation

    public boolean applySalvation(MarkerManager manager, VampireUHCPlayer target, int current_episode) {
        if (salva == null) {
            return false;
        }

        // Une seule protection par épisode (le compteur d'épisode sert de gate).
        if (lastAppliedEpisode == current_episode) {
            return false;
        }

        // On ne peut pas protéger le même joueur deux épisodes consécutifs
        if (last_applied_Uuid != null && last_applied_Uuid.equals(target.getUuid())) {
            return false;
        }

        this.lastAppliedEpisode = current_episode;

        // On clear l'ancien marqueur salvation
        if (last_applied_Uuid != null) {
            manager.clearMarkersOfType(last_applied_Uuid, MarkerType.SALVATION);
        }

        manager.addMarker(target.getUuid(), MarkerType.SALVATION, salva.getUuid());

        this.last_applied_Uuid = target.getUuid();
        return true;
    }
}
