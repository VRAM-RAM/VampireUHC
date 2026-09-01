package fr.vampireuhc.roles.usurped;

import com.google.gson.JsonObject;
import fr.vampireuhc.VampireUHC;
import fr.vampireuhc.config.MessageUtil;
import fr.vampireuhc.markers.AuraTier;
import fr.vampireuhc.markers.MarkerManager;
import fr.vampireuhc.player.VampireUHCPlayer;

import java.util.Random;
import java.util.UUID;

import org.bukkit.entity.Player;

/**
 * Prêtre usurpé : perception imparfaite, chaque probabilité d'erreur est
 * augmentée de 10 points (0→10 / 5→15 / 10→20 / 25→35 / 40→50). S'il y a
 * erreur, un tier d'aura aléatoire différent est révélé. Le Sosie dispose de
 * son propre pool d'usages (gate épisode + cible successive indépendants).
 */
public class UsurpedPriest implements UsurpedPower {

    private VampireUHCPlayer sosie;

    private int lastPerceiveEpisode = -1;
    private UUID lastPerceiveTarget;

    @Override
    public String getName() {
        return "Prêtre";
    }

    @Override
    public void onEnter(VampireUHCPlayer doppelganger) {
        this.sosie = doppelganger;
    }

    @Override
    public void onExit() {
        // Pas de tâche planifiée à nettoyer pour l'instant.
    }

    // Retourne : 0 = succès, 1 = déjà utilisé cet épisode, 2 = cible déjà perçue.
    public int perceive(MarkerManager markerManager, VampireUHCPlayer target, int currentEpisode) {
        if (sosie == null) {
            return 1;
        }
        if (lastPerceiveEpisode == currentEpisode) {
            return 1;
        }
        if (target.getUuid().equals(lastPerceiveTarget)) {
            return 2;
        }

        lastPerceiveEpisode = currentEpisode;
        lastPerceiveTarget = target.getUuid();

        AuraTier real = markerManager.computeAuraTier(target.getUuid());
        AuraTier revealed = roll(markerManager.computeAuraTier(sosie.getUuid()), real);

        String targetName = target.getLastKnownName();
        Player bukkitPlayer = VampireUHC.getInstance().getServer().getPlayer(sosie.getUuid());
        if (bukkitPlayer != null) {
            bukkitPlayer.sendMessage(MessageUtil.info(
                    "<gold>L'aura de <white>" + targetName + "</white> est : " + revealed + ".</gold>"));
        }
        return 0;
    }

    // Probabilités d'erreur = celles du Prêtre + 10 points.
    private AuraTier roll(AuraTier sosieTier, AuraTier real) {
        int errorPercent;
        switch (sosieTier) {
            case TRES_LUMINEUSE: errorPercent = 10; break;
            case LUMINEUSE:      errorPercent = 15; break;
            case NEUTRE:         errorPercent = 20; break;
            case OBSCURE:        errorPercent = 35; break;
            case TRES_OBSCURE:   errorPercent = 50; break;
            default:             errorPercent = 10; break;
        }

        if (errorPercent <= 0 || new Random().nextInt(100) >= errorPercent) {
            return real;
        }

        // Révéler un tier différent, jamais le vrai.
        AuraTier[] tiers = AuraTier.values();
        AuraTier wrong = tiers[new Random().nextInt(tiers.length)];
        while (wrong == real) {
            wrong = tiers[new Random().nextInt(tiers.length)];
        }
        return wrong;
    }

    @Override
    public void saveState(JsonObject obj) {
        obj.addProperty("usurpedPriestLastEpisode", lastPerceiveEpisode);
        if (lastPerceiveTarget != null) {
            obj.addProperty("usurpedPriestLastTarget", lastPerceiveTarget.toString());
        }
    }

    @Override
    public void restoreState(JsonObject obj) {
        if (obj.has("usurpedPriestLastEpisode")) {
            lastPerceiveEpisode = obj.get("usurpedPriestLastEpisode").getAsInt();
        }
        if (obj.has("usurpedPriestLastTarget") && !obj.get("usurpedPriestLastTarget").isJsonNull()) {
            try {
                lastPerceiveTarget = UUID.fromString(obj.get("usurpedPriestLastTarget").getAsString());
            } catch (IllegalArgumentException ignored) {
                // UUID invalide : on repart sans cible.
            }
        }
    }

    public int getLastPerceiveEpisode() {
        return lastPerceiveEpisode;
    }

    public UUID getLastPerceiveTarget() {
        return lastPerceiveTarget;
    }
}