import me.dcplugin.artifex.api.placeholders.CustomPlaceholder;
import org.bukkit.entity.Player;

/**
 * %playtime% placeholder - the player's play time
 * 
 * Formats:
 *   %playtime%          → "2h 15m 30s"
 *   %playtime_hours%    → "2"
 *   %playtime_minutes%  → "135"
 *   %playtime_seconds%  → "8130"
 *
 * Put this file in plugins/Artifex/items/
 * Run /ci reload
 * Use it in any text!
 *
 * Examples:
 *   "Time played: %playtime%"
 *   "Hours on the server: %playtime_hours%"
 *   Scoreboard: "&7Time: &e%playtime%"
 */
public class PlaytimePlaceholder extends CustomPlaceholder {

    // Storage of the join time (timestamp)
    private static final java.util.Map<java.util.UUID, Long> joinTimes = new java.util.HashMap<>();
    // Accumulated time (in seconds) up to the current session
    private static final java.util.Map<java.util.UUID, Long> totalSeconds = new java.util.HashMap<>();

    public PlaytimePlaceholder() {
        super("playtime"); // %playtime%
    }

    @Override
    public String getValue(Player player) {
        if (player == null) return "0s";
        long seconds = getTotalSeconds(player);
        return formatTime(seconds);
    }

    /**
     * Call when a player joins the server
     */
    public static void onJoin(Player player) {
        joinTimes.put(player.getUniqueId(), System.currentTimeMillis());
    }

    /**
     * Call when a player leaves the server
     */
    public static void onQuit(Player player) {
        long joinTime = joinTimes.remove(player.getUniqueId());
        if (joinTime > 0) {
            long sessionSeconds = (System.currentTimeMillis() - joinTime) / 1000;
            totalSeconds.merge(player.getUniqueId(), sessionSeconds, Long::sum);
        }
    }

    /**
     * Get the total time in seconds
     */
    public static long getTotalSeconds(Player player) {
        long total = totalSeconds.getOrDefault(player.getUniqueId(), 0L);
        Long joinTime = joinTimes.get(player.getUniqueId());
        if (joinTime != null) {
            total += (System.currentTimeMillis() - joinTime) / 1000;
        }
        return total;
    }

    /**
     * Format seconds into a readable form
     */
    private String formatTime(long totalSeconds) {
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;

        if (hours > 0) {
            return hours + "h " + minutes + "m " + seconds + "s";
        } else if (minutes > 0) {
            return minutes + "m " + seconds + "s";
        } else {
            return seconds + "s";
        }
    }
}
