package fr.vampireuhc.roles;

import fr.vampireuhc.player.Camp;
import fr.vampireuhc.player.VampireUHCPlayer;

import net.kyori.adventure.text.Component;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;

/**
 * Bourreau : villageois pvp pur.
 * À l'annonce des rôles, il reçoit un livre Sharpness II. À chaque épisode, le
 * premier coup qu'il porte à un joueur inflige 50% de dégâts en plus.
 */
public class BourreauRole implements Role {

    private VampireUHCPlayer bourreau;

    // Gate "premier coup de l'épisode" : épisode du dernier premier coup (-1 = jamais).
    private int lastFirstHitEpisode = -1;

    @Override
    public Camp getDefaultCamp() {
        return Camp.VILLAGEOIS;
    }

    @Override
    public String getName() {
        return "Bourreau";
    }

    @Override
    public boolean isVampire() {
        return false;
    }

    @Override
    public Component getDescription() {
        return net.kyori.adventure.text.minimessage.MiniMessage.miniMessage().deserialize(
            "<gray>Votre objectif est de gagner avec le <green>village</green>.\n\n"
            + "<gray>Pour ce faire, vous disposez, dès l'annonce des rôles, de :</gray>\n"
            + "<dark_purple>• Un livre <gold>Sharpness II</gold>.</dark_purple>\n\n"
            + "<gray>Vous possédez également un effet passif : à <yellow>chaque épisode</yellow>, votre <gold>premier coup</gold> porté à un joueur inflige <red>50% de dégâts en plus</red>.</gray>"
        );
    }

    @Override
    public void onAssign(VampireUHCPlayer player) {
        this.bourreau = player;
    }

    @Override
    public void onAssign(VampireUHCPlayer player, boolean restoring) {
        this.bourreau = player;

        // À la restauration (reload serveur), on ne redistribue PAS le kit.
        if (restoring) {
            return;
        }

        Player bukkitBourreau = Bukkit.getPlayer(player.getUuid());
        if (bukkitBourreau == null || !bukkitBourreau.isOnline()) {
            return;
        }

        // Livre Sharpness II (le bourreau l'applique sur son épée).
        ItemStack book = new ItemStack(Material.ENCHANTED_BOOK);
        EnchantmentStorageMeta meta = (EnchantmentStorageMeta) book.getItemMeta();
        meta.addStoredEnchant(Enchantment.SHARPNESS, 2, true);
        book.setItemMeta(meta);

        bukkitBourreau.getInventory().addItem(book);
    }

    // Consomme le bonus premier-coup de l'épisode. Renvoie true si ce coup
    // bénéficie du bonus (+50%).
    public boolean tryApplyFirstHitBonus(int currentEpisode) {
        if (lastFirstHitEpisode == currentEpisode) {
            return false;
        }
        lastFirstHitEpisode = currentEpisode;
        return true;
    }

    // Restauration de l'état après un redémarrage.
    public void restoreState(int lastFirstHitEpisode) {
        this.lastFirstHitEpisode = lastFirstHitEpisode;
    }

    public int getLastFirstHitEpisode() {
        return lastFirstHitEpisode;
    }
}