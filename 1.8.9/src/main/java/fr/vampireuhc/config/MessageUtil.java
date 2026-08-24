package fr.vampireuhc.config;

import fr.vampireuhc.player.Camp;
import fr.vampireuhc.roles.Role;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;

public final class MessageUtil {

    private static final MiniMessage mm = MiniMessage.miniMessage();

    private MessageUtil() {}

    //  Prefix 

    public static Component prefix() {
        return mm.deserialize("<dark_purple><bold>VampireUHC</bold> <gray>»</gray> ");
    }

    //  Generic helpers 

    public static Component error(String text) {
        return prefix().append(mm.deserialize("<red>" + text));
    }

    public static Component success(String text) {
        return prefix().append(mm.deserialize("<green>" + text));
    }

    public static Component info(String text) {
        return prefix().append(mm.deserialize("<gray>" + text));
    }

    public static Component warn(String text) {
        return prefix().append(mm.deserialize("<gold>" + text));
    }

    // Camp display 

    public static Component campName(Camp camp) {
        return switch (camp) {
            case VAMPIRE -> Component.text("Vampire", NamedTextColor.DARK_RED, TextDecoration.BOLD);
            case VILLAGEOIS -> Component.text("Villageois", NamedTextColor.GREEN, TextDecoration.BOLD);
            case SOLO -> Component.text("Solitaire", NamedTextColor.LIGHT_PURPLE, TextDecoration.BOLD);
        };
    }

    // Role banner (used by /vuhc role) 

    public static Component roleBanner(Role role) {
        Component line = Component.text("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━", NamedTextColor.DARK_PURPLE);

        Component header = Component.empty()
                .append(Component.text("  ⚔ ", NamedTextColor.GOLD))
                .append(Component.text(role.getName(), NamedTextColor.YELLOW, TextDecoration.BOLD))
                .append(Component.text("  ·  ", NamedTextColor.DARK_GRAY))
                .append(campName(role.getDefaultCamp()));

        return Component.empty()
                .append(line).append(Component.newline())
                .append(header).append(Component.newline())
                .append(line).append(Component.newline())
                .append(Component.empty().append(Component.newline()))
                .append(role.getDescription())
                .append(Component.empty().append(Component.newline()))
                .append(line);
    }

    // Role announce (used at game start)

    public static Component roleAnnounce(Role role) {
        Component line = Component.text("─────────────────────────────", NamedTextColor.DARK_PURPLE);

        Component header = Component.empty()
                .append(Component.text("  ⚔ ", NamedTextColor.GOLD))
                .append(Component.text(role.getName(), NamedTextColor.YELLOW, TextDecoration.BOLD))
                .append(Component.text("  ·  ", NamedTextColor.DARK_GRAY))
                .append(campName(role.getDefaultCamp()));

        return Component.empty()
                .append(prefix())
                .append(header).append(Component.newline())
                .append(prefix())
                .append(role.getDescription());
    }

    // Infected player banner

    public static Component infectionBanner(java.util.List<String> vampireNames) {
        Component line = Component.text("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━", NamedTextColor.DARK_RED);

        Component header = Component.empty()
                .append(Component.text("  ☠ ", NamedTextColor.RED))
                .append(Component.text("Infection", NamedTextColor.RED, TextDecoration.BOLD));

        Component body = Component.empty()
                .append(Component.text("Vous avez été infecté et devez gagner avec les vampires !", NamedTextColor.GRAY));

        Component names = Component.empty()
                .append(Component.text("Vampires connus : ", NamedTextColor.GRAY))
                .append(Component.text(String.join(", ", vampireNames), NamedTextColor.YELLOW));

        return Component.empty()
                .append(line).append(Component.newline())
                .append(header).append(Component.newline())
                .append(line).append(Component.newline())
                .append(Component.empty().append(Component.newline()))
                .append(body).append(Component.newline())
                .append(names).append(Component.newline())
                .append(Component.empty().append(Component.newline()))
                .append(line);
    }

    // Help menu

    public static Component helpBanner() {
        Component line = Component.text("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━", NamedTextColor.DARK_PURPLE);

        Component header = Component.empty()
                .append(Component.text("  ☯ ", NamedTextColor.DARK_PURPLE))
                .append(Component.text("VampireUHC", NamedTextColor.DARK_PURPLE, TextDecoration.BOLD));

        return Component.empty()
                .append(line).append(Component.newline())
                .append(header).append(Component.newline())
                .append(line);
    }

    public static Component helpEntry(String command, String description) {
        return Component.empty()
                .append(Component.text("  ", NamedTextColor.GRAY))
                .append(Component.text(command, NamedTextColor.WHITE, TextDecoration.BOLD))
                .append(Component.text(" — ", NamedTextColor.DARK_GRAY))
                .append(Component.text(description, NamedTextColor.GRAY));
    }

    public static Component helpSection(String title) {
        return Component.empty()
                .append(Component.text("  ", NamedTextColor.GRAY))
                .append(Component.text("— " + title + " —", NamedTextColor.DARK_PURPLE, TextDecoration.ITALIC));
    }

    // Success messages with player names 

    public static Component successTarget(String message, String playerName) {
        return prefix().append(mm.deserialize(
            "<green>" + message + " <gold>" + playerName + "</gold><green>.</green>"
        ));
    }

    public static Component successTwoTargets(String message, String name1, String name2) {
        return prefix().append(mm.deserialize(
            "<green>" + message + " <gold>" + name1 + "</gold><green> et </green><gold>" + name2 + "</gold><green>.</green>"
        ));
    }

    // Action bar 

    public static Component actionBar(Component component) {
        return component;
    }

    public static Component actionBar(String miniMessage) {
        return mm.deserialize(miniMessage);
    }

    // Serialize (MiniMessage string → Component)

    public static Component serialize(String miniMessage) {
        return mm.deserialize(miniMessage);
    }

    // Broadcast

    public static void broadcast(Component component) {
        Bukkit.broadcast(component);
    }

    public static void broadcast(String miniMessage) {
        Bukkit.broadcast(prefix().append(mm.deserialize(miniMessage)));
    }
}
