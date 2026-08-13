package fr.vampireuhc.roles;

import fr.vampireuhc.player.VampireUHCPlayer;

import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import java.util.Random;
import fr.vampireuhc.markers.MarkerManager;
import fr.vampireuhc.player.Camp;
import org.bukkit.attribute.Attribute;

public class GremlinRole implements Role {
    private VampireUHCPlayer gremlin;
    private boolean applied_this_episode;
    private int episode;

    private BukkitTask drainTask;
    private boolean drain_applied_this_episode;
    private int drainEpisode = -1;
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
        return "Le Gremlin est un rôle solitaire. Une fois par épisode, il peut échanger l'ensemble des marques de deux joueurs via /vuhc switch <joueur1> <joueur2> (il peut s'auto-switch). Pratique pour semer la zizanie, handicaper des rôles villageois ou retarder l'infection du Maître. Attention : le Cupidon est notifié si une marque Amour change de propriétaire. Son second pouvoir (/vuhc drain), utilisable une fois par épisode, lui permet de voler la vie de ses adversaires en combat : pendant 5 minutes, à chaque coup porté, il a 30 % de chance de regagner un demi-coeur. À la fin des 5 minutes, il subit un poison léger et court.";
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
        // En gros, s'il a déjà intervertit cet épisode, ça ne marche pas.
        if (episode == current_episode && applied_this_episode == true) {
            return false;
        }
        this.episode = current_episode; // On met à jour l'épisode.

        // On échange les marqueurs
        manager.SwitchMarkers(target_1.getUuid(), target_2.getUuid());
        
        this.applied_this_episode = true;
        return true;
    }

    // Pouvoir de drain (/vuhc drain) :

    // Active le drain (ou pas)
    public boolean activateDrain(int current_episode) {
        if (gremlin == null) {
            return false;
        }
        if (drainActive) return false; // déjà actif, on ignore la nouvelle demande
        // En gros, s'il a déjà appliqué son drain cet épisode, ça ne marche pas.
        if (drain_applied_this_episode && drainEpisode == current_episode) {
                return false;
        }
        this.drainEpisode = current_episode; // On met à jour l'épisode.
        this.drain_applied_this_episode = true;

        var plugin = fr.vampireuhc.VampireUHC.getInstance();

        var player = Bukkit.getPlayer(gremlin.getUuid());

        this.drainActive = true;
        this.drainTask = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            this.drainActive = false;
            if (player != null && player.isOnline() && player.isValid()) {
                player.sendActionBar(ChatColor.DARK_RED + "Votre pouvoir de " + ChatColor.DARK_PURPLE + "drain" + ChatColor.RED + " est épuisé ! Vous ressentez une" + ChatColor.GREEN + " faiblesse" + ChatColor.RED + ".");
                player.addPotionEffect(poisonEffect(1));
            }
        }, 20L * 60 * 5);
        return true;    
    }


    public void applyDrainEffect(Player player) {
        if (!drainActive) {
            return;
        }

        int chance = random.nextInt(101);

        if (chance > 30) {
            return;
        }

        // On regen le joueur d'un demi coeur, SAUF si il est full vie
        double newHealth = Math.min(player.getHealth() + 1.0, player.getAttribute(Attribute.MAX_HEALTH).getValue());
        player.setHealth(newHealth);
    }


    // Helper pour les effets :
    private PotionEffect poisonEffect(int amplifier) {
        // 10 secondes d'effet
        return new PotionEffect(PotionEffectType.POISON, 20 * 10, amplifier, true, false, false);
    }
}
