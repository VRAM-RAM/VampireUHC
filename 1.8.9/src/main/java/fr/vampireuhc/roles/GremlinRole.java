package fr.vampireuhc.roles;

import fr.vampireuhc.player.VampireUHCPlayer;
import fr.vampireuhc.config.MessageUtil;

import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import java.util.Random;
import fr.vampireuhc.markers.MarkerManager;
import fr.vampireuhc.player.Camp;

public class GremlinRole implements Role {
    private VampireUHCPlayer gremlin;

    // Gates "une fois par épisode" : l'épisode de dernière utilisation (-1 = jamais).
    private int lastSwitchEpisode = -1;
    private int drainEpisode = -1;

    private BukkitTask drainTask;
    private boolean drainActive;
    private final Random random = new Random();

    public GremlinRole(VampireUHCPlayer player) {
        this.gremlin = player;
    }

    @Override
    public boolean isVampire() {
        return false;
    }

    @Override
    public String getDescription() {
        return (
            "<light_purple>Vous êtes un rôle solitaire qui manipule les marques et vole la vie de ses adversaires.</light_purple>\n\n"
            + "<bold><dark_purple>Pouvoir 1 — Échange de marques :</dark_purple></bold>\n"
            + "  <dark_purple>▸</dark_purple> <gray>Échangez <red>toutes</red> les marques de deux joueurs : <gold>/vuhc switch <j1> <j2></gold></gray>\n"
            + "  <dark_purple>▸</dark_purple> <gray>Utilisable une fois par épisode. Vous pouvez vous auto-switch.</gray>\n"
            + "<bold><dark_purple>Pouvoir 2 — Vol de vie :</dark_purple></bold>\n"
            + "  <dark_purple>▸</dark_purple> <gray>Vous pouvez activer : <gold>/vuhc drain</gold> — une fois par épisode.</gray>\n"
            + "  <dark_purple>▸</dark_purple> <gray>Pendant <yellow>5 minutes</yellow>, chaque coup a <green>30% de chance</green> de voler un demi-coeur à votre adversaire.</gray>\n"
            + "  <dark_purple>▸</dark_purple> <red>À la fin, vous écopez d'un poison léger et court.</red>"
        );
    }

    @Override
    public String getName() {
        return "Gremlin";
    }

    @Override
    public Camp getDefaultCamp() {
        return Camp.SOLO;
    }

    @Override
    public void onAssign(VampireUHCPlayer vampireUHCPlayer) {
        this.gremlin = vampireUHCPlayer;
    }

    @Override
    public void onGameEnd() {
        drainActive = false;
        if (drainTask != null) {
            drainTask.cancel();
            drainTask = null;
        }
    }

    // Pouvoirs spécifiques au rôle :
    
    public boolean SwitchMarkers(MarkerManager manager, VampireUHCPlayer target_1, VampireUHCPlayer target_2, int current_episode) {
        if (gremlin == null) {
            return false;
        }
        // Une seule fois par épisode.
        if (lastSwitchEpisode == current_episode) {
            return false;
        }

        // On échange les marqueurs
        manager.SwitchMarkers(target_1.getUuid(), target_2.getUuid());

        this.lastSwitchEpisode = current_episode;
        return true;
    }

    // Restauration de l'état après un redémarrage (le drain actif lui-même
    // n'est pas restauré : sa fenêtre est courte, on l'assume perdu).
    public void restoreState(int lastSwitchEpisode, int lastDrainEpisode) {
        this.lastSwitchEpisode = lastSwitchEpisode;
        this.drainEpisode = lastDrainEpisode;
    }

    public int getLastSwitchEpisode() {
        return lastSwitchEpisode;
    }

    public int getLastDrainEpisode() {
        return drainEpisode;
    }

    // Pouvoir de drain (/vuhc drain) :

    // Active le drain (ou pas)
    public boolean activateDrain(int current_episode) {
        if (gremlin == null) {
            return false;
        }
        if (drainActive) return false; // déjà actif, on ignore la nouvelle demande
        // Une seule fois par épisode.
        if (drainEpisode == current_episode) {
                return false;
        }

        // Retire pour debug, à ajouter si besoin :

        // Inutile avant le PvP : les dégâts mêlée sont annulés, la fenêtre de
        // 5 minutes partirait en fumée sans un seul coup possible.
        //if (fr.vampireuhc.VampireUHC.getInstance().getGameManager().getPhase()
                //!= fr.vampireuhc.game.GamePhase.PVP_ACTIVE) {
            //return false;
        //}

        this.drainEpisode = current_episode; // On met à jour l'épisode.

        fr.vampireuhc.VampireUHC plugin = fr.vampireuhc.VampireUHC.getInstance();

        Player player = Bukkit.getPlayer(gremlin.getUuid());

        this.drainActive = true;
        this.drainTask = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            this.drainActive = false;
            if (player != null && player.isOnline() && player.isValid()) {
                MessageUtil.sendActionBar(player, "<dark_red>Votre pouvoir de <dark_purple>drain</dark_purple> est épuisé ! Vous ressentez une <green>faiblesse</green>.</dark_red>");
                player.addPotionEffect(poisonEffect(1));
            }
        }, 20L * 60 * 5);
        return true;    
    }


    public void applyDrainEffect(Player player, Player victim) {
        if (!drainActive) {
            return;
        }

        int chance = random.nextInt(101);

        if (chance > 30) {
            return;
        }

        // Le vol s'applique au tick suivant : on est en plein dans l'event de
        // dégâts du coup déclencheur, et toucher à la santé (voire tuer via
        // setHealth(0)) pendant sa résolution est risqué.
        fr.vampireuhc.VampireUHC plugin = fr.vampireuhc.VampireUHC.getInstance();
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (!victim.isOnline() || victim.isDead()) {
                return;
            }
            double stolen = Math.min(1.0, victim.getHealth());
            if (stolen <= 0) {
                return;
            }
            victim.setHealth(victim.getHealth() - stolen);
            if (player.isOnline() && !player.isDead()) {
                double maxHealth = player.getMaxHealth();
                player.setHealth(Math.min(player.getHealth() + stolen, maxHealth));
            }
        });
    }


    // Helper pour les effets :
    private PotionEffect poisonEffect(int amplifier) {
        // 10 secondes d'effet
        return new PotionEffect(PotionEffectType.POISON, 20 * 10, amplifier, true, false);
    }
}
