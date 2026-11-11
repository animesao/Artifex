import me.dcplugin.artifex.api.commands.CustomCommand;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 📢 /bc command - Server announcement
 * 
 * Usage:
 *   /bc <text>      - send an announcement to everyone
 *   /bc -t <text>   - send a title to everyone
 *   /bc -a <text>   - send an actionbar to everyone
 * 
 * Permission: dci.command.bc
 */
public class bc_commandItem extends CustomCommand {

    public bc_commandItem() {
        super(
            "bc",
            "Server announcement",
            "/bc <text>",
            "dci.command.bc"
        );
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (args.length == 0) {
            msg(sender, "&cUsage: /bc <text>");
            msg(sender, "&7Flags: &e-t &7(title), &e-a &7(actionbar)");
            return true;
        }

        String type = "chat";
        String messageStart = args[0];
        int startIndex = 0;

        // Check the flags
        if (messageStart.equals("-t") && args.length > 1) {
            type = "title";
            startIndex = 1;
        } else if (messageStart.equals("-a") && args.length > 1) {
            type = "actionbar";
            startIndex = 1;
        }

        // Build the message
        StringBuilder messageBuilder = new StringBuilder();
        for (int i = startIndex; i < args.length; i++) {
            if (i > startIndex) messageBuilder.append(" ");
            messageBuilder.append(args[i]);
        }
        String message = messageBuilder.toString();

        if (message.isEmpty()) {
            msg(sender, "&cPlease specify the message text!");
            return true;
        }

        // Color codes
        String coloredMessage = colorize(message);
        String prefix = "&8[&6Announcement&8] &r";

        // Get the sender's name
        String senderName = sender instanceof Player ? sender.getName() : "Console";

        switch (type) {
            case "title":
                // Title announcement
                for (Player player : Bukkit.getOnlinePlayers()) {
                    player.sendTitle(coloredMessage, prefix + "&7from " + senderName, 10, 60, 20);
                    player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);
                }
                msg(sender, "&a✅ Title announcement sent!");
                break;

            case "actionbar":
                // Actionbar announcement
                for (Player player : Bukkit.getOnlinePlayers()) {
                    player.spigot().sendMessage(
                        net.md_5.bungee.api.ChatMessageType.ACTION_BAR,
                        net.md_5.bungee.api.chat.TextComponent.fromLegacyText(coloredMessage)
                    );
                    player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1.5f);
                }
                msg(sender, "&a✅ Actionbar announcement sent!");
                break;

            default:
                // Regular chat announcement
                String fullMessage = prefix + coloredMessage + " &7(from " + senderName + ")";
                for (Player player : Bukkit.getOnlinePlayers()) {
                    player.sendMessage(colorize(fullMessage));
                    player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);
                }
                msg(sender, "&a✅ Announcement sent!");
                break;
        }

        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args, int cursor) {
        if (cursor == 0) {
            List<String> options = new ArrayList<>();
            options.add("-t");
            options.add("-a");
            return options;
        }
        return Collections.emptyList();
    }
}
