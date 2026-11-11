import me.dcplugin.artifex.api.placeholders.CustomPlaceholder;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * %rank% placeholder - the player's rank
 *
 * Formats:
 *   %rank%             → "VIP" (rank name)
 *   %rank_prefix%      → "&6[VIP] &r" (chat prefix)
 *   %rank_color%       → "&6" (rank color)
 *   %rank_level%       → "3" (numeric rank level)
 *   %rank_next%        → "Premium" (next rank)
 *   %rank_progress%    → "75%" (progress towards the next rank)
 *
 * Ranks: Novice(1) → Player(2) → VIP(3) → Premium(4) → Legendary(5)
 *
 * Put this file in plugins/Artifex/items/
 * Run /ci reload
 *
 * Examples:
 *   Chat: "%rank_prefix%%player%&7: %message%"
 *   Scoreboard: "&7Rank: %rank_color%%rank%"
 *   Tablist: "%rank_prefix%%player_name%"
 *
 * To set a rank:
 *   /api-item setrank <player> <level>
 */
public class StatusPlaceholder extends CustomPlaceholder {

    private static final String[] RANK_NAMES = {
        "Novice", "Player", "VIP", "Premium", "Legendary"
    };
    private static final String[] RANK_COLORS = {
        "&7", "&a", "&6", "&b", "&d"
    };
    private static final String[] RANK_PREFIXES = {
        "&7[Novice] &r",
        "&a[Player] &r",
        "&6[VIP] &r",
        "&b[Premium] &r",
        "&d[Legendary] &r"
    };

    private static final Map<UUID, Integer> playerRanks = new HashMap<>();

    public StatusPlaceholder() {
        super("rank"); // %rank%
    }

    @Override
    public String getValue(Player player) {
        if (player == null) return "Novice";
        return getRankName(player);
    }

    // ===== Main methods =====

    public static int getRankLevel(Player player) {
        return playerRanks.getOrDefault(player.getUniqueId(), 1);
    }

    public static void setRank(Player player, int level) {
        level = Math.max(1, Math.min(level, RANK_NAMES.length));
        playerRanks.put(player.getUniqueId(), level);
    }

    public static void addRank(Player player) {
        int current = getRankLevel(player);
        if (current < RANK_NAMES.length) {
            setRank(player, current + 1);
        }
    }

    public static String getRankName(Player player) {
        int level = getRankLevel(player);
        return RANK_NAMES[Math.min(level - 1, RANK_NAMES.length - 1)];
    }

    public static String getRankColor(Player player) {
        int level = getRankLevel(player);
        return RANK_COLORS[Math.min(level - 1, RANK_COLORS.length - 1)];
    }

    public static String getRankPrefix(Player player) {
        int level = getRankLevel(player);
        return RANK_PREFIXES[Math.min(level - 1, RANK_PREFIXES.length - 1)];
    }

    public static String getNextRank(Player player) {
        int level = getRankLevel(player);
        if (level >= RANK_NAMES.length) return "Maximum";
        return RANK_NAMES[level]; // level-1 + 1 = level
    }

    /**
     * Progress towards the next rank (based on time/activity)
     * This is just an example — in a real plugin take it from the database
     */
    public static int getProgress(Player player) {
        int level = getRankLevel(player);
        if (level >= RANK_NAMES.length) return 100;
        // Example: every rank requires more time
        // In a real plugin — a database query
        return Math.min(100, level * 25);
    }
}
