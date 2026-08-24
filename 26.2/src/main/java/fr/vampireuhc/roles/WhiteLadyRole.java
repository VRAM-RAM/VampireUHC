package fr.vampireuhc.roles;

import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import fr.vampireuhc.player.Camp;
import fr.vampireuhc.player.VampireUHCPlayer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;


public class WhiteLadyRole implements Role {

    private VampireUHCPlayer whiteLady;
    private VampireUHCPlayer killer;
    private boolean isSolo;
    private boolean killedKiller;
    private boolean killedByVampire;
    private final MiniMessage mm = MiniMessage.miniMessage();


    private Component getSoloDesc() {
        return mm.deserialize(
            "<dark_red>Vous avez été assassiné par un villageois !</dark_red>\n"
            + "<dark_red>Vous devez à présent gagner <gold>seule</gold>. Pour ce faire, vous bénéficiez de :</dark_red>\n\n"
            + "<dark_purple>• Un effet <gold>résistance</gold> de jour.</dark_purple>\n"
            + "<dark_purple>• Un effet <gold>force</gold> de nuit.</dark_purple>\n\n"
            + "<dark_purple>Si vous parvenez à tuer <gold>" + killer.getLastKnownName() + "</gold>, votre assassin, vous recevrez un effet de <gold>speed</gold> permanent." 
        );
    }

    @Override
    public Camp getDefaultCamp() {
        return Camp.VILLAGEOIS;
    }

    @Override
    public Component getDescription() {
        if (isSolo) {
            return getSoloDesc();
        }
        return mm.deserialize(
            "<gray>Votre objectif est de gagner avec le <green>village</green>.\n\n"
            + "<gray>Vous ne disposez d'aucun pouvoir. Cependant :</gray>\n\n"
            + "<dark_purple>• Si vous mourez de la main d'un <dark_red>vampire</dark_red>, vous ressuscitez et devez toujours gagner avec le village.</dark_purple>\n"
            + "<dark_purple>• Si vous mourez de la main d'un <green>villageois</green>, vous ressuscitez et devez gagner <gold>seule</gold>, en éliminant tous les autres joueurs.</dark_purple>\n"
            + "<dark_purple>• Si vous mourez de la main d'un rôle <gold>solitaire</gold>, vous mourez définitivement.</dark_purple>\n\n"
            + "<gray>Si vous devenez <gold>solitaire</gold>, vous gagnerez un effet <dark_purple>résistance</dark_purple> le jour et <dark_purple>force</dark_purple> la nuit, ainsi que <dark_purple>speed</dark_purple> si vous tuez votre assassin.\n\n</gray>"
            + "<gray>Si vous ressuscitez après avoir été tué par un <dark_red>vampire</dark_red>, vous écopez d'un effet <dark_red>faiblesse</dark_red> le jour.</gray>"
        );
    }

    @Override
    public String getName() {
        return "Dame Blanche";
    }

    @Override
    public boolean isVampire() {
        return false;
    }

    @Override
    public void onAssign(VampireUHCPlayer player) {
        this.whiteLady = player;
    }

    // Restauration de l'état après un redémarrage : sans ça, une Dame Blanche
    // ressuscitée-solo redevient silencieusement villageoise et perd son objectif.
    public void restoreState(boolean isSolo, boolean killedByVampire, boolean killedKiller, UUID killerUuid) {
        this.isSolo = isSolo;
        this.killedByVampire = killedByVampire;
        this.killedKiller = killedKiller;
        if (killerUuid != null) {
            var playerManager = fr.vampireuhc.VampireUHC.getInstance().getPlayerManager();
            VampireUHCPlayer killerPlayer = playerManager.get(killerUuid);
            // Le tueur peut avoir été restauré après nous (ordre JSON arbitraire) :
            // dans ce cas on retente paresseusement via l'UUID stocké.
            this.killer = killerPlayer != null ? killerPlayer : this.killer;
            if (killerPlayer == null) {
                this.pendingKillerUuid = killerUuid;
            }
        }
    }

    private UUID pendingKillerUuid;

    // Résout le tueur si la restauration l'avait laissé en attente (appelé
    // après le chargement complet des joueurs).
    public void resolvePendingReferences() {
        if (pendingKillerUuid != null) {
            var playerManager = fr.vampireuhc.VampireUHC.getInstance().getPlayerManager();
            VampireUHCPlayer killerPlayer = playerManager.get(pendingKillerUuid);
            if (killerPlayer != null) {
                this.killer = killerPlayer;
                this.pendingKillerUuid = null;
            }
        }
    }

    public boolean isSoloState() {
        return isSolo;
    }

    public boolean wasKilledByVampire() {
        return killedByVampire;
    }

    public boolean hasKilledKiller() {
        return killedKiller;
    }

    public UUID getKillerUuid() {
        return killer != null ? killer.getUuid() : pendingKillerUuid;
    }

    public boolean onDeath(VampireUHCPlayer killer) {
        if (whiteLady == null) {
            return false;
        }

        // Mort sans tueur joueur (chute, lave, mob...) : rien à qualifier,
        // mais surtout pas de NPE qui avorterait setDead()/checkWinCondition().
        if (killer == null) {
            return false;
        }

        Camp camp = null;

        if (killer.getCamp() != null) {
            // En gros, si le tueur est infecte, son Camp est defini et on le recupere
            camp = killer.getCamp();
        } else if (killer.getRole() != null) {
            // Sinon on prend le camp par defaut
            camp = killer.getRole().getDefaultCamp();
        }

        if (camp == null) {
            return false;
        }

        switch (camp) {
            case Camp.VILLAGEOIS:
                killedByVillager(killer);
                return true;
        
            case Camp.VAMPIRE:
                killedByVampire();
                return true;

            case Camp.SOLO:
                return false;
            
            default:
                return false;
        }
    }

    private void killedByVillager(VampireUHCPlayer killer) {
        this.killer = killer;
        this.isSolo = true;
        Player bukkitLady = Bukkit.getPlayer(whiteLady.getUuid());

        if (bukkitLady == null) {
            return;
        }

        bukkitLady.sendMessage(getSoloDesc());
    }

    private void killedByVampire() {
        Player bukkitLady = Bukkit.getPlayer(whiteLady.getUuid());
        this.killedByVampire = true;

        if (bukkitLady == null) {
            return;
        }
    }

    public void killedKiller(Player player) {
        if (killer == null || player == null) {
            return;
        }
        // Si elle tue son tueur, on update le booléen.
        if (player.getUniqueId().equals(killer.getUuid())) {
            this.killedKiller = true;
        }
    }

    // Effets passifs :
    // - statut solitaire (tuée par un villageois) : résistance de jour, force de nuit,
    //   speed permanent si elle a tué son assassin ;
    // - ressuscitée après un kill vampire : faiblesse de jour.
    public void applyEffects(Player player, boolean night) {
        if (!isSolo && !killedByVampire) {
            return;
        }

        if (isSolo) {
            if (night) {
                player.addPotionEffect(effect(PotionEffectType.STRENGTH, 1));
            } else {
                player.addPotionEffect(effect(PotionEffectType.RESISTANCE, 1));
            }

            if (killedKiller) {
                player.addPotionEffect(effect(PotionEffectType.SPEED, 1));
            }
        }

        if (killedByVampire && !night) {
            player.addPotionEffect(effect(PotionEffectType.WEAKNESS, 1));
        }
    }

    private PotionEffect effect(PotionEffectType type, int amplifier) {
        return new PotionEffect(type, 20 * 95, amplifier, true, false, true);
    }
}
