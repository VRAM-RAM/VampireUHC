package fr.vampireuhc.roles.usurped;

import java.util.List;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import com.google.gson.JsonObject;

import fr.vampireuhc.config.MessageUtil;
import fr.vampireuhc.markers.MarkerManager;
import fr.vampireuhc.markers.MarkerType;
import fr.vampireuhc.player.Camp;
import fr.vampireuhc.player.VampireUHCPlayer;

/**
 * Marchand de Sable copié par le Sosie : identique au vrai Marchand (marqueurs
 * SABLE_LUMINEUX_DOPPELGANGER / SABLE_NEUTRE_DOPPELGANGER, aura fixée au
 * dépôt, effet de mort), mais la lenteur ne dure que 1 minute (au lieu de 3).
 */
public class UsurpedSandMerchant implements UsurpedPower {

    private VampireUHCPlayer sosie;
    private int lastSandEpisode = -1;

    private static final int SLOWNESS_SECONDS = 60;
    private static final int SLOWNESS_AMPLIFIER = 0;
    private static final int BLINDNESS_SECONDS = 30;
    private static final int BLINDNESS_AMPLIFIER = 3;

    private long effectsAppliedAtMillis = -1;

    @Override
    public String getName() {
        return "Marchand de Sable";
    }

    @Override
    public void onEnter(VampireUHCPlayer doppelganger) {
        this.sosie = doppelganger;
    }

    @Override
    public void onExit() {
    }

    public void sandPlayer(MarkerManager manager, VampireUHCPlayer target, int current_episode) {
        if (sosie == null) {
            return;
        }

        Player bukkitMerchant = Bukkit.getPlayer(sosie.getUuid());
        if (bukkitMerchant == null) {
            return;
        }

        if (lastSandEpisode == current_episode) {
            bukkitMerchant.sendMessage(MessageUtil.error("Vous ne pouvez ensabler qu'un joueur par épisode !"));
            return;
        }

        if (manager.hasMarker(target.getUuid(), MarkerType.SABLE_LUMINEUX_DOPPELGANGER)) {
            bukkitMerchant.sendMessage(MessageUtil.info("Le joueur <dark_blue>" + target.getLastKnownName() + "</dark_blue> est déjà ensablé !"));
            return;
        }

        if (manager.hasMarker(target.getUuid(), MarkerType.SABLE_NEUTRE_DOPPELGANGER)) {
            bukkitMerchant.sendMessage(MessageUtil.info("Le joueur <dark_blue>" + target.getLastKnownName() + "</dark_blue> est déjà ensablé !"));
            return;
        }

        Player bukkitTarget = Bukkit.getPlayer(target.getUuid());
        if (bukkitTarget == null) {
            bukkitMerchant.sendMessage(MessageUtil.error("Le joueur que vous ciblez n'est pas connecté."));
            return;
        }

        if (!isWithinRadius(bukkitMerchant, bukkitTarget, 10)) {
            bukkitMerchant.sendMessage(MessageUtil.error("Le joueur que vous ciblez n'est pas suffisamment proche de vous !"));
            return;
        }

        Camp camp = target.getRole().getDefaultCamp();

        if (camp == null) {
            bukkitMerchant.sendMessage(MessageUtil.error("Le joueur que vous ciblez n'est dans aucun camp !"));
            return;
        }

        switch (camp) {
            case Camp.VILLAGEOIS:
                manager.addMarker(bukkitTarget.getUniqueId(), MarkerType.SABLE_LUMINEUX_DOPPELGANGER, bukkitMerchant.getUniqueId());
                break;
            default:
                manager.addMarker(bukkitTarget.getUniqueId(), MarkerType.SABLE_NEUTRE_DOPPELGANGER, bukkitMerchant.getUniqueId());
        }

        bukkitMerchant.sendMessage(MessageUtil.successTarget("Le joueur", target.getLastKnownName() + " a été ensablé !"));

        this.lastSandEpisode = current_episode;
    }

    private boolean isWithinRadius(Player player1, Player player2, double radius) {
        return player1.getLocation().distanceSquared(player2.getLocation()) <= radius * radius;
    }

    public void makePlayersSleepOnDeath(MarkerManager manager) {
        if (sosie == null) {
            return;
        }

        this.effectsAppliedAtMillis = System.currentTimeMillis();

        List<Player> players_with_marker_neutral = manager.getPlayersThatHaveMarkerType(MarkerType.SABLE_NEUTRE_DOPPELGANGER);
        List<Player> players_with_marker_light = manager.getPlayersThatHaveMarkerType(MarkerType.SABLE_LUMINEUX_DOPPELGANGER);

        applyEffectsOnPlayers(players_with_marker_light);
        applyEffectsOnPlayers(players_with_marker_neutral);
    }

    public void deliverPendingEffects(MarkerManager manager, UUID playerId) {
        if (sosie == null || effectsAppliedAtMillis < 0) {
            return;
        }
        boolean marked = manager.hasMarker(playerId, MarkerType.SABLE_LUMINEUX_DOPPELGANGER)
                || manager.hasMarker(playerId, MarkerType.SABLE_NEUTRE_DOPPELGANGER);
        if (!marked) {
            return;
        }
        Player bukkitTarget = Bukkit.getPlayer(playerId);
        if (bukkitTarget == null || !bukkitTarget.isOnline()) {
            return;
        }

        long elapsedSeconds = (System.currentTimeMillis() - effectsAppliedAtMillis) / 1000L;
        int slownessRemaining = SLOWNESS_SECONDS - (int) elapsedSeconds;
        int blindnessRemaining = BLINDNESS_SECONDS - (int) elapsedSeconds;

        boolean applied = false;
        if (slownessRemaining > 0) {
            bukkitTarget.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, slownessRemaining * 20, SLOWNESS_AMPLIFIER, true, false, true));
            applied = true;
        }
        if (blindnessRemaining > 0) {
            bukkitTarget.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, blindnessRemaining * 20, BLINDNESS_AMPLIFIER, true, false, true));
            applied = true;
        }
        if (applied) {
            bukkitTarget.sendActionBar(MessageUtil.actionBar("<gold>Vous avez été endormi par le Marchand de Sable !"));
        }
    }

    private void applyEffectsOnPlayers(List<Player> players) {
        for (Player p : players) {
            p.sendActionBar(MessageUtil.actionBar("<gold>Vous avez été endormi par le Marchand de Sable !"));
            p.addPotionEffect(slowness(SLOWNESS_AMPLIFIER));
            p.addPotionEffect(blindness(BLINDNESS_AMPLIFIER));
        }
    }

    private PotionEffect slowness(int amplifier) {
        // 60 secondes d'effet (nerf du Sosie vs 180 s pour le vrai Marchand).
        return new PotionEffect(PotionEffectType.SLOWNESS, 20 * SLOWNESS_SECONDS, amplifier, true, false, true);
    }

    private PotionEffect blindness(int amplifier) {
        // 30 secondes d'effet.
        return new PotionEffect(PotionEffectType.BLINDNESS, 20 * BLINDNESS_SECONDS, amplifier, true, false, true);
    }

    @Override
    public void saveState(JsonObject obj) {
        obj.addProperty("usurpedSandEpisode", lastSandEpisode);
        obj.addProperty("usurpedSandEffectsAt", effectsAppliedAtMillis);
    }

    @Override
    public void restoreState(JsonObject obj) {
        lastSandEpisode = obj.has("usurpedSandEpisode")
                ? obj.get("usurpedSandEpisode").getAsInt() : -1;
        effectsAppliedAtMillis = obj.has("usurpedSandEffectsAt")
                ? obj.get("usurpedSandEffectsAt").getAsLong() : -1;
    }
}