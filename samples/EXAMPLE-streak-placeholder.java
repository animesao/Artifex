import me.dcplugin.artifex.api.placeholders.CustomPlaceholder;
import org.bukkit.entity.Player;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * %streak% placeholder - daily login streak
 *
 * Formats:
 *   %streak%           → "5" (number of days in a row)
 *   %streak_best%      → "12" (best streak)
 *   %streak_status%    → "🔥 Active" / "❌ Broken"
 *
 * Put this file in plugins/Artifex/items/
 * Run /ci reload
 *
 * Examples:
 *   "Login streak: %streak% days in a row"
 *   "Best streak: %streak_best%"
 *   Scoreboard: "&7Streak: &e%streak% &7days &c%streak_status%"
 */
public class StreakPlaceholder extends CustomPlaceholder {

    // Current streak
    private static final Map<UUID, Integer> currentStreak = new HashMap<>();
    // Best streak
    private static final Map<UUID, Integer> bestStreak = new HashMap<>();
    // Date of the last login
    private static final Map<UUID, LocalDate> lastJoinDate = new HashMap<>();

    public StreakPlaceholder() {
        super("streak"); // %streak%
    }

    @Override
    public String getValue(Player player) {
        if (player == null) return "0";
        return String.valueOf(currentStreak.getOrDefault(player.getUniqueId(), 0));
    }

    /**
     * Call when a player joins the server
     */
    public static void onJoin(Player player) {
        UUID uuid = player.getUniqueId();
        LocalDate today = LocalDate.now();
        LocalDate lastJoin = lastJoinDate.get(uuid);

        if (lastJoin == null) {
            // First login — start the streak
            currentStreak.put(uuid, 1);
        } else {
            long daysBetween = ChronoUnit.DAYS.between(lastJoin, today);
            if (daysBetween == 1) {
                // Login on the next day — continue the streak
                currentStreak.merge(uuid, 1, Integer::sum);
            } else if (daysBetween > 1) {
                // Missed a day — reset the streak
                currentStreak.put(uuid, 1);
            }
            // daysBetween == 0 — already logged in today, no change
        }

        lastJoinDate.put(uuid, today);

        // Update the best streak
        int current = currentStreak.getOrDefault(uuid, 0);
        int best = bestStreak.getOrDefault(uuid, 0);
        if (current > best) {
            bestStreak.put(uuid, current);
        }
    }

    public static int getCurrentStreak(Player player) {
        return currentStreak.getOrDefault(player.getUniqueId(), 0);
    }

    public static int getBestStreak(Player player) {
        return bestStreak.getOrDefault(player.getUniqueId(), 0);
    }

    public static String getStatus(Player player) {
        int streak = getCurrentStreak(player);
        if (streak >= 7) return "🔥 Fiery streak!";
        if (streak >= 3) return "🔥 Active";
        if (streak >= 1) return "✅ Started";
        return "❌ Broken";
    }
}
