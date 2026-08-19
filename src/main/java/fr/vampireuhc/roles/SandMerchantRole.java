package fr.vampireuhc.roles;

import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import fr.vampireuhc.markers.MarkerManager;
import fr.vampireuhc.markers.MarkerType;
import fr.vampireuhc.player.Camp;
import fr.vampireuhc.player.VampireUHCPlayer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;

public class SandMerchantRole implements Role {
    private VampireUHCPlayer sandMerchant;

    @Override
    public Camp getDefaultCamp() {
        return Camp.VILLAGEOIS;
    }

    @Override
    public boolean isVampire() {
        return false;
    }

    @Override
    public Component getDescription() {
        MiniMessage mm = MiniMessage.miniMessage();
        return mm.deserialize(
            "<gray>Vous soutenez le village en ensablant discrètement des joueurs.</gray>\n\n"
            + "<dark_purple>▸</dark_purple> <gray>Ensablez un joueur : <gold>/vuhc ensabler <joueur></gold> (rayon de <yellow>10 blocs</yellow>)</gray>\n"
            + "<dark_purple>▸</dark_purple> <gray>Vous pouvez vous ensabler vous-même, mais chaque joueur ne peut être ensablé qu'<yellow>une seule fois</yellow>.</gray>\n\n"
            + "<bold><dark_purple>Comportement du marqueur :</dark_purple></bold>\n"
            + "  <gray>• Joueur <green>villageois</green> → marqueur <yellow>lumineux</yellow>.</gray>\n"
            + "  <gray>• Joueur <red>non-villageois</red> → marqueur <white>neutre</white>.</gray>\n"
            + "  <gray>• L'aura est fixe au moment du dépôt (même si les marques changent de propriétaire).</gray>\n\n"
            + "<bold><dark_purple>Effet à votre mort :</dark_purple></bold>\n"
            + "  <gray>Tous les joueurs ensablés subissent <red>cécité</red> (30s) et <red>lenteur</red> (3 min).</gray>"
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

    public void sandPlayer(MarkerManager manager, VampireUHCPlayer target) {
        if (sandMerchant == null) {
            return;
        }

        // On cast le marchand pour pouvoir envoyer des messages
        Player bukkitMerchant = Bukkit.getPlayer(sandMerchant.getUuid());

        // Si la cible a déjà un marqueur sable (ici lumineux), impossible de poser un autre marqueur
        if (manager.hasMarker(target.getUuid(), MarkerType.SABLE_LUMINEUX)) {
            bukkitMerchant.sendMessage(ChatColor.GRAY + "Le joueur " + ChatColor.DARK_BLUE + target.getLastKnownName() + ChatColor.GRAY + " est déjà ensablé !");
            return;
        }

        // Pareil avec sable neutre
        if (manager.hasMarker(target.getUuid(), MarkerType.SABLE_NEUTRE)) {
            bukkitMerchant.sendMessage(ChatColor.GRAY + "Le joueur " + ChatColor.DARK_BLUE + target.getLastKnownName() + ChatColor.GRAY + " est déjà ensablé !");
            return;
        }

        // On cast les joueurs bukkit à partir des joueurs VampireUHC
        Player bukkitTarget = Bukkit.getPlayer(target.getUuid());

        // En cas d'erreur, on retourne
        if (bukkitTarget == null) {
            bukkitMerchant.sendMessage(ChatColor.RED + "Le joueur que vous ciblez n'est pas connecté.");
            return;
        }

        // Si la target ne se trouve pas dans la range de 10 blocs de rayon, on retourne
        if (!isWithinRadius(bukkitMerchant, bukkitTarget, 10)) {
            bukkitMerchant.sendMessage(ChatColor.RED + "Le joueur que vous ciblez n'est pas suffisamment proche de vous !");
            return;
        }

        var camp = target.getRole().getDefaultCamp();

        if (camp == null) {
            bukkitMerchant.sendMessage(ChatColor.RED + "Le joueur que vous ciblez n'est dans aucun camp !");
            return;
        }

        switch (camp) {
            case Camp.VILLAGEOIS:
                manager.addMarker(bukkitTarget.getUniqueId(), MarkerType.SABLE_LUMINEUX, bukkitMerchant.getUniqueId());
                break;
            default:
                manager.addMarker(bukkitTarget.getUniqueId(), MarkerType.SABLE_NEUTRE, bukkitMerchant.getUniqueId());
        }

        bukkitMerchant.sendMessage(ChatColor.GRAY + "Le joueur " + ChatColor.DARK_BLUE + target.getLastKnownName() + ChatColor.GRAY + " a été ensablé !");

    }
    
    // Helper pour savoir si le joueur se trouve dans le rayon du tisseur
    private boolean isWithinRadius(Player player1, Player player2, double radius) {
        return player1.getLocation().distanceSquared(player2.getLocation()) <= radius * radius;
    }
    
    public void makePlayersSleepOnMarchantDeath(MarkerManager manager) {
        if (sandMerchant == null) {
            return;
        }

        var players_with_marker_neutral = manager.getPlayersThatHaveMarkerType(MarkerType.SABLE_NEUTRE);
        var players_with_marker_light = manager.getPlayersThatHaveMarkerType(MarkerType.SABLE_LUMINEUX);
        
        applyEffectsOnPlayers(players_with_marker_light);
        applyEffectsOnPlayers(players_with_marker_neutral);
    }

    private void applyEffectsOnPlayers(List<Player> players) {
        for (Player p: players) {
            p.sendActionBar(ChatColor.GOLD + "Vous avez été endormi par le Marchand de Sable !");
            p.addPotionEffect(slowness(0));
            p.addPotionEffect(blindness(3));
        }
    }

    private PotionEffect slowness(int amplifier) {
        // 180 secondes d'effet
        return new PotionEffect(PotionEffectType.SLOWNESS, 20 * 180, amplifier, true, false, true);
    }

    private PotionEffect blindness(int amplifier) {
        // 30 secondes d'effet
        return new PotionEffect(PotionEffectType.BLINDNESS, 20 * 30, amplifier, true, false, true);
    }

}
