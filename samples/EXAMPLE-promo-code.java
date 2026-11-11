import me.dcplugin.artifex.Main;
import me.dcplugin.artifex.api.database.DatabaseManager;
import me.dcplugin.artifex.api.commands.CustomCommand;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Locale;
import java.util.concurrent.CompletableFuture;

/**
 * Example of promo codes with storage in SQLite or MySQL.
 *
 * This file can be copied into plugins/Artifex/items/ as promo-code.java.
 * After that run /ci reload and use /promo DEMO2026.
 *
 * In config.yml:
 * database.type: sqlite  -> local data.db
 * database.type: mysql   -> MySQL from database.mysql.*
 */
public class PromoCodeCommand extends CustomCommand {

    public PromoCodeCommand() {
        super("promo", "Activate a promo code", "/promo <code>", "dci.command.promo");
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            msg(sender, "&cThis command is for players only.");
            return true;
        }
        if (args.length == 0) {
            msg(sender, "&eUsage: /promo <code>");
            return true;
        }

        Player player = (Player) sender;
        String code = args[0].trim().toUpperCase(Locale.ROOT);
        DatabaseManager database = Main.getInstance().getDatabaseManager();

        // The table is created identically in SQLite and MySQL.
        database.executeAsync(
            "CREATE TABLE IF NOT EXISTS promo_codes (" +
                "code VARCHAR(64) PRIMARY KEY, " +
                "reward_command VARCHAR(255) NOT NULL, " +
                "max_uses INT NOT NULL DEFAULT 1, " +
                "uses INT NOT NULL DEFAULT 0)"
        ).thenCompose(created -> database.queryAllAsync(
            "SELECT reward_command, max_uses, uses FROM promo_codes WHERE code = ?",
            code
        )).thenAccept(rows -> {
            if (rows.isEmpty()) {
                sendOnMain(player, "&cThere is no such promo code.");
                return;
            }

            var row = rows.get(0);
            int maxUses = ((Number) row.get("max_uses")).intValue();
            int uses = ((Number) row.get("uses")).intValue();
            if (uses >= maxUses) {
                sendOnMain(player, "&cThis promo code has already expired.");
                return;
            }

            int updated = database.executeUpdate(
                "UPDATE promo_codes SET uses = uses + 1 WHERE code = ? AND uses < max_uses",
                code
            );
            if (updated != 1) {
                sendOnMain(player, "&cCould not activate the promo code. Please try again later.");
                return;
            }

            String reward = String.valueOf(row.get("reward_command"))
                .replace("%player%", player.getName());
            Main.getInstance().getServer().getScheduler().runTask(Main.getInstance(), () -> {
                Main.getInstance().getServer().dispatchCommand(
                    Main.getInstance().getServer().getConsoleSender(), reward
                );
                player.sendMessage(colorize("&aPromo code activated!"));
            });
        }).exceptionally(error -> {
            sendOnMain(player, "&cDatabase error. Check the console.");
            Main.getInstance().getLogger().warning("Promo code error: " + error.getMessage());
            return null;
        });
        return true;
    }

    private void sendOnMain(Player player, String message) {
        Main.getInstance().getServer().getScheduler().runTask(
            Main.getInstance(), () -> msg(player, message)
        );
    }
}
