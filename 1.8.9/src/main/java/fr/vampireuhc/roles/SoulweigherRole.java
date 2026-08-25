package fr.vampireuhc.roles;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import fr.vampireuhc.markers.AuraTier;

import fr.vampireuhc.markers.MarkerManager;
import fr.vampireuhc.player.Camp;
import fr.vampireuhc.player.VampireUHCPlayer;
import fr.vampireuhc.config.MessageUtil;

public class SoulweigherRole implements Role {

    private VampireUHCPlayer soulWeighter;

    // Gate "une fois par épisode" (-1 = jamais utilisé).
    private int lastWeightEpisode = -1;


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
        this.soulWeighter = player;
    }

    @Override
    public String getName() {
        return "Peseuse d'âmes";
    }

    @Override
    public String getDescription() {
        return (
            "<gray>Vous pesez l'aura de deux joueurs pour déceler les différences.</gray>\n\n"
            + "<dark_purple>▸</dark_purple> <gray>Commande : <gold>/vuhc peser <joueur1> <joueur2></gold></gray>\n"
            + "<dark_purple>▸</dark_purple> <gray>Utilisable à chaque épisode.</gray>\n\n"
            + "<bold><dark_purple>Résultats possibles :</dark_purple></bold>\n"
            + "  <green>« La balance s'équilibre... »</green>\n"
            + "  <gray>→ Même aura exacte (très obscure, obscure, neutre...).</gray>\n"
            + "  <yellow>« La balance penche légèrement... »</yellow>\n"
            + "  <gray>→ Même catégorie (obscure, neutre ou lumineuse).</gray>\n"
            + "  <red>« La balance penche... »</red>\n"
            + "  <gray>→ Catégories différentes.</gray>\n\n"
        );
    }

    // Pouvoir actif spécifique à la peseuse d'âme

    public boolean weightAura(MarkerManager manager, VampireUHCPlayer targetOne, VampireUHCPlayer targetTwo, int currentEpisode) {
        if (soulWeighter == null) {
            return false;
        }
        // Une seule pesée par épisode (sinon brute-force des auras par paires).
        if (lastWeightEpisode == currentEpisode) {
            Player bukkitWeighter = Bukkit.getPlayer(soulWeighter.getUuid());
            if (bukkitWeighter != null) {
                bukkitWeighter.sendMessage(MessageUtil.error("Vous avez déjà pesé des âmes cet épisode."));
            }
            return false;
        }

        AuraTier auraOfFirstTarget = manager.computeAuraTier(targetOne.getUuid());
        AuraTier auraOfSecondTarget = manager.computeAuraTier(targetTwo.getUuid());

        Player bukkitPlayer = Bukkit.getPlayer(soulWeighter.getUuid());
        if (bukkitPlayer == null) {
            return false;
        }
        lastWeightEpisode = currentEpisode;

        // Les trois issues sont des résultats valides : "penche" ne doit pas
        // être interprété comme une erreur interne par l'appelant.
        if (auraOfFirstTarget == auraOfSecondTarget) {
            bukkitPlayer.sendMessage(MessageUtil.success("La balance s'équilibre..."));
            return true;
        }
        if (auraOfFirstTarget.getTight() == auraOfSecondTarget.getTight()) {
            bukkitPlayer.sendMessage(MessageUtil.warn("La balance penche légèrement..."));
            return true;
        }
        bukkitPlayer.sendMessage(MessageUtil.error("La balance penche..."));
        return true;
    }

    // Restauration de l'état après un redémarrage.
    public void restoreState(int lastWeightEpisode) {
        this.lastWeightEpisode = lastWeightEpisode;
    }

    public int getLastWeightEpisode() {
        return lastWeightEpisode;
    }
}
