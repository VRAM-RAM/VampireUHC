package fr.vampireuhc.roles;

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
            + "<dark_purple>• Si vous mourrez de la main d'un <dark_red>vampire</dark_red>, vous ressuscitez et devez toujours gagner avec le village.</dark_purple>\n"
            + "<dark_purple>• Si vous mourrez de la main d'un <green>villageois</green>, vous ressuscitez et devez gagner <gold>seule</gold>, en éliminant tous les autres joueurs.</dark_purple>\n"
            + "<dark_purple>• Si vous mourrez de la main d'un rôle <gold>solitaire</gold>, vous mourrez définitivement.</dark_purple>\n\n"
            + "<gray>Si vous devenez <gold>solitaire</gold>, vous gagnerez un effet <dark_purple>résistance</dark_purple> le jour et <dark_purple>force</dark_purple> la nuit, ainsi que <dark_purple>speed</dark_purple> si vous tuez votre assassin.\n\n</gray>"
            + "<gray>Si vous ressuscitez après avoir été tué par un <dark_red>vampire</dark_red>, vous gagnerez un effet <dark_red>faiblesse</dark_red> le jour.</gray>"
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

    public boolean onDeath(VampireUHCPlayer killer) {
        if (whiteLady == null) {
            return false;
        }

        Camp camp = null;

        if (killer.getCamp() != null) {
            // En gros, si le tueur est infecte, son Camp est defini et on le recupere
            camp = killer.getCamp();
        } else {
            // Sinon on prend le camp par defaut
            camp = killer.getRole().getDefaultCamp();
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
        // Si elle tue son tueur, on update le booléen.
        if (player.getUniqueId() == killer.getUuid()) {
            this.killedKiller = true;
        }
    }

    public void applyEffects(Player player, boolean night) {
        // Si elle n'a rien, on return
        if (!isSolo || !killedByVampire) {
            return;
        }
        
        // Si tuée par vampire, on ajoute faiblesse de jour
        if (killedByVampire || !night) {
            player.addPotionEffect(effect(PotionEffectType.WEAKNESS, 1));
            return;
        }

        // Sinon, on ajoute les effets en fonction du jour ou de la nuit (cas de solitaire)        
        if (night) {
            player.addPotionEffect(effect(PotionEffectType.STRENGTH, 1));
        } else {
            player.addPotionEffect(effect(PotionEffectType.RESISTANCE, 1));
        }

        if (killedKiller) {
            player.addPotionEffect(effect(PotionEffectType.SPEED, 1));
        }
    }

    private PotionEffect effect(PotionEffectType type, int amplifier) {
        return new PotionEffect(type, 20 * 95, amplifier, true, false, true);
    }
}
