package fr.vampireuhc.roles;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import fr.vampireuhc.markers.MarkerManager;
import fr.vampireuhc.markers.MarkerType;
import fr.vampireuhc.player.Camp;
import fr.vampireuhc.player.VampireUHCPlayer;
import fr.vampireuhc.config.MessageUtil;

public class SandMerchantRole implements Role {
    private VampireUHCPlayer sandMerchant;
    // Gates "une fois par épisode" : l'épisode de dernière utilisation (-1 = jamais).
    private int lastSandEpisode = -1;

    // Fenêtre des effets de mort (durées partagées par l'application directe et
    // la livraison différée aux ensablés qui reviennent).
    private static final int SLOWNESS_SECONDS = 180;
    private static final int SLOWNESS_AMPLIFIER = 0;
    private static final int BLINDNESS_SECONDS = 30;
    private static final int BLINDNESS_AMPLIFIER = 3;

    // Moment où les effets ont été déclenchés (-1 = jamais) : permet de livrer
    // les effets restants aux joueurs qui étaient dans la grâce de déconnexion.
    private long effectsAppliedAtMillis = -1;

    @Override
    public Camp getDefaultCamp() {
        return Camp.VILLAGEOIS;
    }

    @Override
    public boolean isVampire() {
        return false;
    }

    @Override
    public String getDescription() {
        return (
            "<gray>Vous soutenez le village en ensablant discrètement des joueurs.</gray>\n\n"
            + "<dark_purple>▸</dark_purple> <gray>Ensablez un joueur : <gold>/vuhc ensabler <joueur></gold> (rayon de <yellow>10 blocs</yellow>)</gray>\n"
            + "<dark_purple>▸</dark_purple> <gray>Vous pouvez vous ensabler vous-même, mais chaque joueur ne peut être ensablé qu'<yellow>une seule fois</yellow>.</gray>\n\n"
            + "<bold><dark_purple>Comportement du marqueur :</dark_purple></bold>\n"
            + "  <gray>• Joueur <green>villageois</green> → marqueur <yellow>lumineux</yellow>.</gray>\n"
            + "  <gray>• Joueur <red>non-villageois</red> → marqueur <white>neutre</white>.</gray>\n"
            + "  <gray>• L'aura est fixe au moment du dépôt (même si les marques changent de propriétaire).</gray>\n\n"
            + "<bold><dark_purple>Effet à votre mort :</dark_purple></bold>\n"
            + "  <gray>Tous les joueurs ensablés subissent <red>blindness</red> (30s) et <red>lenteur</red> (3 min).</gray>"
        );
    }

    @Override
    public String getName() {
        return "Marchand de Sable";
    }

    public VampireUHCPlayer getSandMerchant() {
        return sandMerchant;
    }


    public SandMerchantRole(VampireUHCPlayer player) {
        this.sandMerchant = player;
    }

    @Override
    public void onAssign(VampireUHCPlayer player) {
        this.sandMerchant = player;
    }

    // Pouvoir spécial : ensablage

    public void sandPlayer(MarkerManager manager, VampireUHCPlayer target, int current_episode) {
        if (sandMerchant == null) {
            return;
        }

        // On cast le marchand pour pouvoir envoyer des messages
        Player bukkitMerchant = Bukkit.getPlayer(sandMerchant.getUuid());

        // Une seule fois par épisode.
        if (lastSandEpisode == current_episode) {
            bukkitMerchant.sendMessage(MessageUtil.error("Vous ne pouvez ensabler qu'un joueur par épisode !"));
            return;
        }

        // Si la cible a déjà un marqueur sable (ici lumineux), impossible de poser un autre marqueur
        if (manager.hasMarker(target.getUuid(), MarkerType.SABLE_LUMINEUX)) {
            bukkitMerchant.sendMessage(MessageUtil.info("Le joueur <dark_blue>" + target.getLastKnownName() + "</dark_blue> est déjà ensablé !"));
            return;
        }

        // Pareil avec sable neutre
        if (manager.hasMarker(target.getUuid(), MarkerType.SABLE_NEUTRE)) {
            bukkitMerchant.sendMessage(MessageUtil.info("Le joueur <dark_blue>" + target.getLastKnownName() + "</dark_blue> est déjà ensablé !"));
            return;
        }

        // On cast les joueurs bukkit à partir des joueurs VampireUHC
        Player bukkitTarget = Bukkit.getPlayer(target.getUuid());

        // En cas d'erreur, on retourne
        if (bukkitTarget == null) {
            bukkitMerchant.sendMessage(MessageUtil.error("Le joueur que vous ciblez n'est pas connecté."));
            return;
        }

        // Si la target ne se trouve pas dans la range de 10 blocs de rayon, on retourne
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
            case VILLAGEOIS:
                manager.addMarker(bukkitTarget.getUniqueId(), MarkerType.SABLE_LUMINEUX, bukkitMerchant.getUniqueId());
                break;
            default:
                manager.addMarker(bukkitTarget.getUniqueId(), MarkerType.SABLE_NEUTRE, bukkitMerchant.getUniqueId());
        }

        bukkitMerchant.sendMessage(MessageUtil.successTarget("Le joueur", target.getLastKnownName() + " a été ensablé !"));

        this.lastSandEpisode = current_episode;
    }
    
    // Helper pour savoir si le joueur se trouve dans le rayon du tisseur
    private boolean isWithinRadius(Player player1, Player player2, double radius) {
        return player1.getLocation().distanceSquared(player2.getLocation()) <= radius * radius;
    }
    
    public void makePlayersSleepOnMarchantDeath(MarkerManager manager) {
        if (sandMerchant == null) {
            return;
        }

        this.effectsAppliedAtMillis = System.currentTimeMillis();

        ArrayList<Player> players_with_marker_neutral = manager.getPlayersThatHaveMarkerType(MarkerType.SABLE_NEUTRE);
        ArrayList<Player> players_with_marker_light = manager.getPlayersThatHaveMarkerType(MarkerType.SABLE_LUMINEUX);

        applyEffectsOnPlayers(players_with_marker_light);
        applyEffectsOnPlayers(players_with_marker_neutral);
    }

    // Livraison différée : appelé au retour d'un joueur ensablé qui était hors
    // ligne au moment de la mort du marchand (fenêtre de grâce de déconnexion).
    // Les durées sont proratisées sur ce qui reste de la fenêtre.
    public void deliverPendingEffects(MarkerManager manager, UUID playerId) {
        if (sandMerchant == null || effectsAppliedAtMillis < 0) {
            return;
        }
        boolean marked = manager.hasMarker(playerId, MarkerType.SABLE_LUMINEUX)
                || manager.hasMarker(playerId, MarkerType.SABLE_NEUTRE);
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
            bukkitTarget.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, slownessRemaining * 20, SLOWNESS_AMPLIFIER, true, false));
            applied = true;
        }
        if (blindnessRemaining > 0) {
            bukkitTarget.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, blindnessRemaining * 20, BLINDNESS_AMPLIFIER, true, false));
            applied = true;
        }
        if (applied) {
            MessageUtil.sendActionBar(bukkitTarget, "<gold>Vous avez été endormi par le Marchand de Sable !");
        }
    }

    private void applyEffectsOnPlayers(List<Player> players) {
        for (Player p: players) {
            MessageUtil.sendActionBar(p, "<gold>Vous avez été endormi par le Marchand de Sable !");
            p.addPotionEffect(slowness(0));
            p.addPotionEffect(blindness(3));
        }
    }

    private PotionEffect slowness(int amplifier) {
        // 180 secondes d'effet
        return new PotionEffect(PotionEffectType.SLOW, 20 * 180, amplifier, true, false);
    }

    private PotionEffect blindness(int amplifier) {
        // 30 secondes d'effet
        return new PotionEffect(PotionEffectType.BLINDNESS, 20 * 30, amplifier, true, false);
    }

}
