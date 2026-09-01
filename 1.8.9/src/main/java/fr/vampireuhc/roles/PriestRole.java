package fr.vampireuhc.roles;

import fr.vampireuhc.VampireUHC;
import fr.vampireuhc.markers.AuraTier;
import fr.vampireuhc.markers.MarkerManager;
import fr.vampireuhc.player.Camp;
import fr.vampireuhc.player.VampireUHCPlayer;

import java.util.UUID;

/**
 * Prêtre : rôle villageois à info majeur.
 * À chaque épisode, /vuhc percevoir <joueur> révèle l'aura exacte de la cible.
 * Plus l'aura du Prêtre est obscure, plus sa perception risque d'être fausse
 * (on révèle alors un tier d'aura aléatoire différent).
 * Ne peut pas percevoir deux fois de suite le même joueur.
 */
public class PriestRole implements Role {

    private VampireUHCPlayer priest;

    // Gates : épisode de la dernière perception (-1 = jamais) et cible associée.
    private int lastPerceiveEpisode = -1;
    private UUID lastPerceiveTarget;

    @Override
    public Camp getDefaultCamp() {
        return Camp.VILLAGEOIS;
    }

    @Override
    public String getName() {
        return "Prêtre";
    }

    @Override
    public boolean isVampire() {
        return false;
    }

    @Override
    public String getDescription() {
        return (
            "<gray>Votre objectif est de gagner avec le <green>village</green>.\n\n"
            + "<dark_purple>▸</dark_purple> <gray>À chaque épisode, percevez l'aura exacte d'un joueur : <gold>/vuhc percevoir <joueur></gold></gray>\n"
            + "<dark_purple>▸</dark_purple> <gray>Vous ne pouvez pas percevoir deux fois de suite le même joueur.</gray>\n\n"
            + "<bold><dark_purple>Attention :</dark_purple></bold> <gray>plus votre propre aura est obscure, plus votre perception a de chances d'être <red>fausse</red> :</gray>\n"
            + "  <gray>• très lumineuse → <green>0%</green></gray>\n"
            + "  <gray>• lumineuse → <green>5%</green></gray>\n"
            + "  <gray>• neutre → <green>10%</green></gray>\n"
            + "  <gray>• obscure → <red>25%</red></gray>\n"
            + "  <gray>• très obscure → <red>40%</red></gray>"
        );
    }

    @Override
    public void onAssign(VampireUHCPlayer player) {
        this.priest = player;
    }

    // Tente de percevoir l'aura de la cible. Renvoie un code :
    // 0 = succès, 1 = pas votre tour (déjà utilisé cet épisode), 2 = cible déjà perçue la dernière fois.
    public int perceive(MarkerManager markerManager, VampireUHCPlayer target, int currentEpisode) {
        if (priest == null) {
            return 1;
        }

        VampireUHC plugin = VampireUHC.getInstance();

        if (lastPerceiveEpisode == currentEpisode) {
            return 1;
        }
        if (target.getUuid().equals(lastPerceiveTarget)) {
            return 2;
        }

        lastPerceiveEpisode = currentEpisode;
        lastPerceiveTarget = target.getUuid();

        AuraTier real = markerManager.computeAuraTier(target.getUuid());
        AuraTier revealed = roll(markerManager.computeAuraTier(priest.getUuid()), real);

        String targetName = target.getLastKnownName();

        plugin.getServer().getPlayer(priest.getUuid()).sendMessage(
                fr.vampireuhc.config.MessageUtil.info(
                        "<gold>L'aura de <white>" + targetName + "</white> est : " + revealed + ".</gold>"));
        return 0;
    }

    // Probabilité d'erreur selon l'aura du Prêtre, puis choix d'un tier différent.
    private AuraTier roll(AuraTier priestTier, AuraTier real) {
        int errorPercent;
        switch (priestTier) {
            case TRES_LUMINEUSE: errorPercent = 0; break;
            case LUMINEUSE:      errorPercent = 5; break;
            case NEUTRE:         errorPercent = 10; break;
            case OBSCURE:        errorPercent = 25; break;
            case TRES_OBSCURE:   errorPercent = 40; break;
            default:             errorPercent = 0; break;
        }

        if (errorPercent <= 0 || new java.util.Random().nextInt(100) >= errorPercent) {
            return real;
        }

        // Révéler un tier différent, jamais le vrai.
        AuraTier[] tiers = AuraTier.values();
        AuraTier wrong = tiers[new java.util.Random().nextInt(tiers.length)];
        while (wrong == real) {
            wrong = tiers[new java.util.Random().nextInt(tiers.length)];
        }
        return wrong;
    }

    // Restauration de l'état après un redémarrage.
    public void restoreState(int lastPerceiveEpisode, UUID lastPerceiveTarget) {
        this.lastPerceiveEpisode = lastPerceiveEpisode;
        this.lastPerceiveTarget = lastPerceiveTarget;
    }

    public int getLastPerceiveEpisode() {
        return lastPerceiveEpisode;
    }

    public UUID getLastPerceiveTarget() {
        return lastPerceiveTarget;
    }
}