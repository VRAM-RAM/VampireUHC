package fr.vampireuhc.roles;

import fr.vampireuhc.VampireUHC;
import fr.vampireuhc.config.MessageUtil;
import fr.vampireuhc.game.GamePhase;
import fr.vampireuhc.player.Camp;
import fr.vampireuhc.player.VampireUHCPlayer;

import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;

import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.TextComponent;

/**
 * Baba Yaga : villageoise à deux pouvoirs à usage unique.
 *
 * Résurrection : quand un joueur meurt, elle reçoit un message cliquable
 * (valable 10s) pour le ressusciter. Un vampire ressuscité inflige à Baba Yaga
 * une faiblesse permanente (invisible). La personne ressuscitée meurt 5s après
 * Baba Yaga - sauf si c'est elle qui l'a tuée.
 *
 * Malédiction : /vuhc maudire <joueur> retire pendant 10 minutes toute
 * absorption aux pommes d'or de la cible.
 */
public class BabaYagaRole implements Role {

    // Fenêtre de clic de résurrection : 10 secondes.
    private static final long RESURRECTION_WINDOW_MS = 10_000;
    // Durée de la malédiction : 10 minutes.
    private static final long CURSE_DURATION_MS = 600_000;
    // Lien de mort : le ressuscité meurt 5s après Baba Yaga.
    private static final long DEATH_LINK_DELAY_TICKS = 100;

    private VampireUHCPlayer babaYaga;

    private boolean resurrectionUsed = false;
    private UUID pendingResurrection;
    private long pendingExpiry;
    private BukkitTask pendingTask;

    private UUID linkedResurrected;
    private boolean vampirePenalty = false;

    private boolean curseUsed = false;
    private UUID cursedPlayer;
    private long curseExpiry;

    @Override
    public Camp getDefaultCamp() {
        return Camp.VILLAGEOIS;
    }

    @Override
    public String getName() {
        return "Baba Yaga";
    }

    @Override
    public boolean isVampire() {
        return false;
    }

    @Override
    public String getDescription() {
        return (
            "<gray>Votre objectif est de gagner avec le <green>village</green>.\n\n"
            + "Vous disposez de deux pouvoirs <gold>à usage unique</gold> :\n"
            + "<dark_purple>• <gold>Résurrection</gold> : lorsqu'un joueur meurt, un message cliquable vous est envoyé pendant <gold>10 secondes</gold>. Cliquez pour le ressusciter avec <gold>tous ses pouvoirs</gold> (10 coeurs, position aléatoire).</dark_purple>\n"
            + "<dark_purple>• <gold>Malédiction</gold> : <gray>/vuhc maudire <joueur></gray> retire toute absorption des pommes d'or de ce joueur pendant 10 minutes.</dark_purple>\n\n"
            + "<gray>Si vous ressuscitez un <dark_red>vampire</dark_red>, vous subissez une <dark_red>faiblesse</dark_red> permanente (invisible à vos yeux).\n"
            + "Si <gold>vous mourez</gold>, la personne ressuscitée meurt également 5 secondes plus tard — <gold>sauf si c'est elle qui vous a tuée</gold> !</gray>"
        );
    }

    @Override
    public void onAssign(VampireUHCPlayer player) {
        this.babaYaga = player;
    }

    @Override
    public void onGameEnd() {
        clearPending();
        linkedResurrected = null;
    }

    // --- Résurrection ---

    // Appelé à la mort d'un joueur : propose la résurrection cliquable.
    public void offerResurrection(VampireUHCPlayer victim) {
        if (resurrectionUsed || victim == null
                || !isBabaYagaAlive()) {
            return;
        }

        // Le défunt est déjà setDead(), on vérifie quand même qu'il a un rôle
        // (morts avant l'assignation : pas de résurrection possible).
        if (victim.getRole() == null || victim.getUuid().equals(babaYaga.getUuid())) {
            return;
        }

        VampireUHC plugin = VampireUHC.getInstance();
        if (plugin.getGameManager().getPhase() == GamePhase.ENDED) {
            return;
        }

        clearPending();
        pendingResurrection = victim.getUuid();
        pendingExpiry = System.currentTimeMillis() + RESURRECTION_WINDOW_MS;
        pendingTask = Bukkit.getScheduler().runTaskLater(plugin, this::expirePending, RESURRECTION_WINDOW_MS / 50);

        Player baba = Bukkit.getPlayer(babaYaga.getUuid());
        if (baba == null) {
            return;
        }

        baba.sendMessage(MessageUtil.serialize(
                "<dark_purple>Un joueur vient de mourir ! Vous avez <gold>10 secondes</gold> pour le ressusciter :</dark_purple>"));

        TextComponent line = new TextComponent(" » Ressusciter ");
        line.setColor(net.md_5.bungee.api.ChatColor.GRAY);
        TextComponent name = new TextComponent(victim.getLastKnownName());
        name.setColor(net.md_5.bungee.api.ChatColor.GOLD);
        name.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/vuhc ressusciter"));
        line.addExtra(name);
        baba.spigot().sendMessage(line);
    }

    // Appelé par /vuhc ressusciter : ramène le défunt proposé à la vie.
    public void resurrect() {
        if (resurrectionUsed || !isBabaYagaAlive()) {
            return;
        }

        VampireUHC plugin = VampireUHC.getInstance();
        Player baba = Bukkit.getPlayer(babaYaga.getUuid());

        if (pendingResurrection == null) {
            if (baba != null) {
                baba.sendMessage(MessageUtil.error("Aucun joueur à ressusciter pour l'instant."));
            }
            return;
        }
        if (System.currentTimeMillis() > pendingExpiry
                || plugin.getGameManager().getPhase() == GamePhase.ENDED) {
            clearPending();
            if (baba != null) {
                baba.sendMessage(MessageUtil.error("Le délai de résurrection est dépassé."));
            }
            return;
        }

        VampireUHCPlayer victim = plugin.getPlayerManager().get(pendingResurrection);
        Player bukkitVictim = victim != null ? Bukkit.getPlayer(victim.getUuid()) : null;
        if (victim == null || bukkitVictim == null || !bukkitVictim.isOnline()) {
            clearPending();
            if (baba != null) {
                baba.sendMessage(MessageUtil.error("Ce joueur n'est plus en ligne."));
            }
            return;
        }

        // Résurrection proprement dite.
        victim.setAlive();
        bukkitVictim.setGameMode(GameMode.SURVIVAL);
        bukkitVictim.setHealth(bukkitVictim.getMaxHealth());
        plugin.getMapManager().teleportPlayerRandomly(bukkitVictim);

        // Le lien de mort s'applique quel que soit le camp du ressuscité.
        linkedResurrected = victim.getUuid();

        // Un vampire ressuscité coûte une faiblesse permanente à Baba Yaga.
        if (victim.getCamp() == Camp.VAMPIRE) {
            vampirePenalty = true;
        }

        resurrectionUsed = true;
        clearPending();

        if (baba != null) {
            baba.sendMessage(MessageUtil.successTarget("Vous avez ressuscité", victim.getLastKnownName()));
        }
        bukkitVictim.sendMessage(MessageUtil.info("Vous avez été ressuscité par la Baba Yaga. Gardez vos pouvoirs, vous devez toujours gagner avec votre camp."));
    }

    // La fenêtre de 10s est passée (ou le serveur a redémarré) : on oublie la
    // proposition, sans pouvoir de relance.
    private void expirePending() {
        if (pendingResurrection != null) {
            clearPending();
            Player baba = Bukkit.getPlayer(babaYaga.getUuid());
            if (baba != null) {
                baba.sendMessage(MessageUtil.error("Le délai de résurrection est écoulé."));
            }
        }
    }

    private void clearPending() {
        if (pendingTask != null) {
            pendingTask.cancel();
            pendingTask = null;
        }
        pendingResurrection = null;
        pendingExpiry = 0;
    }

    // --- Lien de mort ---

    // La mort de Baba Yaga active le lien : la personne ressuscitée meurt 5s
    // plus tard — sauf si c'est elle qui l'a tuée (le lien est alors brisé).
    public void onBabaYagaDeath(UUID killerUuid) {
        clearPending();

        if (linkedResurrected == null) {
            return;
        }
        if (killerUuid != null && killerUuid.equals(linkedResurrected)) {
            Player resurrected = Bukkit.getPlayer(linkedResurrected);
            linkedResurrected = null;
            if (resurrected != null) {
                resurrected.sendMessage(MessageUtil.info("Vous avez tué la Baba Yaga : le lien de mort est brisé !"));
            }
            return;
        }

        VampireUHC plugin = VampireUHC.getInstance();
        UUID target = linkedResurrected;
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            VampireUHCPlayer resuscited = plugin.getPlayerManager().get(target);
            if (resuscited == null || !resuscited.isAlive()
                    || plugin.getGameManager().getPhase() == GamePhase.ENDED) {
                return;
            }
            Player p = Bukkit.getPlayer(target);
            if (p != null && p.isOnline()) {
                p.setHealth(0);
            }
        }, DEATH_LINK_DELAY_TICKS);
    }

    // --- Malédiction ---

    public boolean cursePlayer(VampireUHCPlayer target) {
        if (curseUsed || target == null || !isBabaYagaAlive()) {
            return false;
        }
        curseUsed = true;
        cursedPlayer = target.getUuid();
        curseExpiry = System.currentTimeMillis() + CURSE_DURATION_MS;

        Player baba = Bukkit.getPlayer(babaYaga.getUuid());
        if (baba != null) {
            baba.sendMessage(MessageUtil.successTarget("Vous avez maudit", target.getLastKnownName())
                    + " pendant 10 minutes !");
        }
        Player bukkitTarget = Bukkit.getPlayer(target.getUuid());
        if (bukkitTarget != null) {
            bukkitTarget.sendMessage(MessageUtil.error("Vous avez été maudit par la Baba Yaga : vos pommes d'or ne vous donneront plus d'absorption pendant 10 minutes !"));
        }
        return true;
    }

    // Vrai si le joueur est actuellement sous l'effet de la malédiction.
    public boolean isCurseActive(UUID uuid) {
        return curseUsed && cursedPlayer != null
                && cursedPlayer.equals(uuid)
                && System.currentTimeMillis() <= curseExpiry;
    }

    public boolean hasUsedResurrection() {
        return resurrectionUsed;
    }

    public UUID getLinkedResurrected() {
        return linkedResurrected;
    }

    public boolean hasUsedCurse() {
        return curseUsed;
    }

    public UUID getCursedPlayer() {
        return cursedPlayer;
    }

    public long getCurseExpiry() {
        return curseExpiry;
    }

    // --- Faiblesse permanente ---

    public boolean hasVampirePenalty() {
        return vampirePenalty;
    }

    // Baba Yaga a ressuscité un vampire : elle garde une faiblesse légère,
    // masquée (aucune particule), ré-appliquée par le RoleBuffManager.
    public void applyEffects(Player player) {
        if (vampirePenalty) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 20 * 95, 0, true, false));
        }
    }

    // --- Restauration JSON ---

    public void restoreState(boolean resurrectionUsed, UUID linkedResurrected,
                             boolean vampirePenalty, boolean curseUsed,
                             UUID cursedPlayer, long curseExpiry) {
        this.resurrectionUsed = resurrectionUsed;
        this.linkedResurrected = linkedResurrected;
        this.vampirePenalty = vampirePenalty;
        this.curseUsed = curseUsed;
        // Une malédiction expirée pendant le redémarrage ne revit pas.
        if (curseUsed && System.currentTimeMillis() <= curseExpiry) {
            this.cursedPlayer = cursedPlayer;
            this.curseExpiry = curseExpiry;
        } else {
            this.cursedPlayer = null;
            this.curseExpiry = 0;
        }
        // La fenêtre de clic (10s) ne survit pas à un redémarrage.
    }

    private boolean isBabaYagaAlive() {
        return babaYaga != null && babaYaga.isAlive();
    }
}