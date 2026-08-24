package fr.vampireuhc.roles;

import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;

import fr.vampireuhc.VampireUHC;
import fr.vampireuhc.player.Camp;
import fr.vampireuhc.player.VampireUHCPlayer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;

import org.bukkit.entity.Player;

public class ArcherRole implements Role {

    private VampireUHCPlayer archer;

    @Override
    public Camp getDefaultCamp() {
        return Camp.VILLAGEOIS;
    }

    @Override
    public Component getDescription() {
        MiniMessage mm = MiniMessage.miniMessage();
        return mm.deserialize(
            "<gray>Votre objectif est de gagner avec le <green>village</green>.\n\n"
            + "<gray>Pour ce faire, vous disposez, dès l'annonce des rôles, de :</gray>\n"
            + "<dark_purple>• Un livre Infinity.</dark_purple>\n"
            + "<dark_purple>• Un livre Power II.</dark_purple>\n"
            + "<dark_purple>• Un Arc.</dark_purple>\n"
            + "<dark_purple>• Une flèche.</dark_purple>\n\n"
            + "<gray>Vous possédez également un effet passif : lorsque vous touchez une entité d'une de vos flèches, cette entité recevra un effet de <yellow>glowing</yellow> pendant <red>15 secondes</red> que vous seul percevrez."
        );
    }

    @Override
    public boolean isVampire() {
        return false;
    }

    @Override
    public String getName() {
        return "Archer";
    }

    @Override
    public void onAssign(VampireUHCPlayer player) {
        this.archer = player;
    }

    @Override
    public void onAssign(VampireUHCPlayer player, boolean restoring) {
        this.archer = player;

        // À la restauration (reload serveur), on ne redistribue PAS le kit :
        // l'archer l'a déjà dans son inventaire.
        if (restoring) {
            return;
        }

        var bukkitArcher = Bukkit.getPlayer(player.getUuid());
        if (bukkitArcher == null || !bukkitArcher.isOnline()) {
            return;
        }

        // On créé les livres enchantés (oui, tout ça pour deux pauvres livres...)
        ItemStack Infinitybook = new ItemStack(Material.ENCHANTED_BOOK);
        ItemStack PowerTwoBook = new ItemStack(Material.ENCHANTED_BOOK);

        EnchantmentStorageMeta infinitymeta = (EnchantmentStorageMeta) Infinitybook.getItemMeta();
        EnchantmentStorageMeta powertwometa = (EnchantmentStorageMeta) PowerTwoBook.getItemMeta();

        // Infinité 
        infinitymeta.addStoredEnchant(
                Enchantment.INFINITY,
                1,
                true
        );

        // Power II
        powertwometa.addStoredEnchant(
            Enchantment.POWER,
             2,
            true
        );

        Infinitybook.setItemMeta(infinitymeta);
        PowerTwoBook.setItemMeta(powertwometa);

        // L'archer reçoit un arc, un livre infinité, un livre Power II et une flèche.
        bukkitArcher.getInventory().addItem(
                    new ItemStack(Material.BOW),
                    new ItemStack(Infinitybook),
                    new ItemStack(PowerTwoBook),
                    new ItemStack(Material.ARROW));
    }

    // Pouvoir passif : l'archer voit les ennemis qu'il touche en glowing.

    public void setGlowOnHit(Entity target, VampireUHC plugin) {
        if (archer == null) {
            return;
        }

        Player bukkitArcher = Bukkit.getPlayer(archer.getUuid());

        if (bukkitArcher == null) {
            return;
        }

        setGlow(target, bukkitArcher, plugin);

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            removeGlow(target, bukkitArcher, plugin);
        }, 20L * 15L);
    }

    private void setGlow(Entity target, Player viewer, VampireUHC plugin) {
        try {
            plugin.getGlowingEntities().setGlowing(target, viewer);
        } catch (ReflectiveOperationException e) {
            plugin.getLogger().log(
                Level.SEVERE,
                "Impossible d'appliquer le glowing",
                e
            );
        }
    }

    private void removeGlow(Entity target, Player viewer, VampireUHC plugin) {
        try {
            plugin.getGlowingEntities().unsetGlowing(target, viewer);
        } catch (ReflectiveOperationException e) {
            plugin.getLogger().log(
                Level.SEVERE,
                "Impossible de retirer le glowing",
                e
            );
        }
    }
}
