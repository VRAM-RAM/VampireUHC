package fr.vampireuhc.config;

import fr.vampireuhc.player.Camp;
import fr.vampireuhc.roles.Role;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Version legacy (1.8.9) de la façade de messages.
 *
 * Le code source conserve les chaînes MiniMessage identiques à la version
 * moderne ; {@link #serialize(String)} les traduit en codes § à l'affichage.
 * Seuls les tags réellement utilisés par le projet sont couverts.
 */
public final class MessageUtil {

    private static final Pattern TAG = Pattern.compile("</?([a-z_]+)>");

    // MiniMessage -> code §. Toute couleur reset aussi les décorations (comportement 1.8).
    private static final Map<String, String> TAGS = new HashMap<>();

    static {
        TAGS.put("black", "§0");
        TAGS.put("dark_blue", "§1");
        TAGS.put("dark_green", "§2");
        TAGS.put("dark_aqua", "§3");
        TAGS.put("dark_red", "§4");
        TAGS.put("dark_purple", "§5");
        TAGS.put("gold", "§6");
        TAGS.put("gray", "§7");
        TAGS.put("dark_gray", "§8");
        TAGS.put("blue", "§9");
        TAGS.put("green", "§a");
        TAGS.put("aqua", "§b");
        TAGS.put("red", "§c");
        TAGS.put("light_purple", "§d");
        // Alias moderne -> équivalent 1.8
        TAGS.put("purple", "§d");
        TAGS.put("yellow", "§e");
        TAGS.put("white", "§f");
        TAGS.put("bold", "§l");
        TAGS.put("italic", "§o");
        TAGS.put("underlined", "§n");
        TAGS.put("strikethrough", "§m");
        TAGS.put("obfuscated", "§k");
        TAGS.put("reset", "§r");
        TAGS.put("newline", "\n");
    }

    private MessageUtil() {
    }

    /**
     * Traduit une chaîne MiniMessage (sous-ensemble utilisé par le projet)
     * en chaîne legacy §.
     */
    public static String serialize(String mini) {
        if (mini == null || mini.isEmpty()) {
            return "";
        }
        StringBuilder out = new StringBuilder(mini.length() + 16);
        Matcher m = TAG.matcher(mini);
        String lastColor = null;
        int copied = 0;
        while (m.find()) {
            String name = m.group(1);
            String code = TAGS.get(name);
            if (code == null) {
                continue; // placeholder métier (<joueur>, <min>...) : inchangé
            }
            boolean closing = m.group(0).startsWith("</");
            appendRaw(out, mini, copied, m.start());
            copied = m.end();
            boolean isColor = code.length() == 2 && code.charAt(1) >= '0' && code.charAt(1) <= 'f'
                    && "0123456789abcdef".indexOf(code.charAt(1)) >= 0;
            if (!closing) {
                if (isColor) {
                    lastColor = code;
                }
                out.append(code);
            } else if (!isColor && !"§r".equals(code)) {
                // Fermeture d'une décoration : on repart du code couleur courant,
                // car tout code couleur reset les décorations en 1.8.
                out.append('§').append('r');
                if (lastColor != null) {
                    out.append(lastColor);
                }
            }
            // Fermeture d'une couleur : rien à faire (la prochaine couleur prendra le relais).
        }
        appendRaw(out, mini, copied, mini.length());
        return out.toString();
    }

    private static void appendRaw(StringBuilder out, String src, int from, int to) {
        if (to > from) {
            out.append(src, from, to);
        }
    }

    //  Prefix

    public static String prefix() {
        return serialize("<dark_purple><bold>VampireUHC</bold> <gray>»</gray> ");
    }

    //  Generic helpers

    public static String error(String text) {
        return prefix() + serialize("<red>" + text);
    }

    public static String success(String text) {
        return prefix() + serialize("<green>" + text);
    }

    public static String info(String text) {
        return prefix() + serialize("<gray>" + text);
    }

    public static String warn(String text) {
        return prefix() + serialize("<gold>" + text);
    }

    // Camp display

    public static String campName(Camp camp) {
        switch (camp) {
            case VAMPIRE:
                return ChatColor.DARK_RED.toString() + ChatColor.BOLD + "Vampire";
            case VILLAGEOIS:
                return ChatColor.GREEN.toString() + ChatColor.BOLD + "Villageois";
            case SOLO:
                return ChatColor.LIGHT_PURPLE.toString() + ChatColor.BOLD + "Solitaire";
            default:
                return camp.name();
        }
    }

    // Role banner (used by /vuhc role)

    public static String roleBanner(Role role) {
        String line = ChatColor.DARK_PURPLE + "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━";
        String header = ChatColor.GOLD + "  ⚔ "
                + ChatColor.YELLOW.toString() + ChatColor.BOLD + role.getName()
                + ChatColor.DARK_GRAY + "  ·  "
                + campName(role.getDefaultCamp());

        return line + "\n"
                + header + "\n"
                + line + "\n"
                + "\n"
                + serialize(role.getDescription())
                + "\n"
                + line;
    }

    // Role announce (used at game start)

    public static String roleAnnounce(Role role) {
        String header = ChatColor.GOLD + "  ⚔ "
                + ChatColor.YELLOW.toString() + ChatColor.BOLD + role.getName()
                + ChatColor.DARK_GRAY + "  ·  "
                + campName(role.getDefaultCamp());

        return prefix() + header + "\n"
                + prefix()
                + serialize(role.getDescription());
    }

    // Infected player banner

    public static String infectionBanner(List<String> vampireNames) {
        String line = ChatColor.DARK_RED + "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━";
        String header = ChatColor.RED + "  ☠ "
                + ChatColor.RED.toString() + ChatColor.BOLD + "Infection";
        String body = ChatColor.GRAY + "Vous avez été infecté et devez gagner avec les vampires !";
        String names = ChatColor.GRAY + "Vampires connus : "
                + ChatColor.YELLOW + String.join(", ", vampireNames);

        return line + "\n"
                + header + "\n"
                + line + "\n"
                + "\n"
                + body + "\n"
                + names + "\n"
                + "\n"
                + line;
    }

    // Help menu

    public static String helpBanner() {
        String line = ChatColor.DARK_PURPLE + "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━";
        String header = ChatColor.DARK_PURPLE + "  ☯ "
                + ChatColor.DARK_PURPLE.toString() + ChatColor.BOLD + "VampireUHC";

        return line + "\n" + header + "\n" + line;
    }

    public static String helpEntry(String command, String description) {
        return ChatColor.GRAY + "  "
                + ChatColor.WHITE.toString() + ChatColor.BOLD + command
                + ChatColor.DARK_GRAY + " — "
                + ChatColor.GRAY + description;
    }

    public static String helpSection(String title) {
        return ChatColor.GRAY + "  "
                + ChatColor.DARK_PURPLE.toString() + ChatColor.ITALIC + "— " + title + " —";
    }

    // Success messages with player names

    public static String successTarget(String message, String playerName) {
        return prefix() + serialize("<green>" + message + " <gold>" + playerName + "</gold><green>.</green>");
    }

    public static String successTwoTargets(String message, String name1, String name2) {
        return prefix() + serialize(
                "<green>" + message + " <gold>" + name1 + "</gold><green> et </green><gold>" + name2 + "</gold><green>.</green>");
    }

    // Action bar (aucune API 1.8 : paquet NMS v1_8_R3 par réflexion, fallback chat)

    public static void sendActionBar(Player player, String miniMessage) {
        try {
            Class<?> iChatBase = Class.forName("net.minecraft.server.v1_8_R3.IChatBaseComponent");
            Class<?> chatSerializer = Class.forName("net.minecraft.server.v1_8_R3.IChatBaseComponent$ChatSerializer");
            Object component = chatSerializer.getMethod("a", String.class)
                    .invoke(null, "{\"text\":" + escapeJson(serialize(miniMessage)) + "}");

            Class<?> packetClass = Class.forName("net.minecraft.server.v1_8_R3.PacketPlayOutChat");
            Constructor<?> ctor = packetClass.getConstructor(iChatBase, byte.class);
            Object packet = ctor.newInstance(component, (byte) 2);

            Method getHandle = player.getClass().getMethod("getHandle");
            Object handle = getHandle.invoke(player);
            Object connection = handle.getClass().getField("playerConnection").get(handle);
            Class<?> packetType = Class.forName("net.minecraft.server.v1_8_R3.Packet");
            connection.getClass().getMethod("sendPacket", packetType).invoke(connection, packet);
        } catch (Throwable t) {
            // Fallback sûr si NMS indisponible (fork exotique, version différente...)
            player.sendMessage(serialize(miniMessage));
        }
    }

    private static String escapeJson(String s) {
        StringBuilder sb = new StringBuilder(s.length() + 8);
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '"' || c == '\\') {
                sb.append('\\').append(c);
            } else if (c < 0x20) {
                sb.append(String.format("\\u%04x", (int) c));
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    // Broadcast

    public static void broadcast(String miniMessage) {
        Bukkit.broadcastMessage(prefix() + serialize(miniMessage));
    }
}
