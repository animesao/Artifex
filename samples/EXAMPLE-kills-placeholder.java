import me.dcplugin.artifex.api.placeholders.CustomPlaceholder;
import org.bukkit.entity.Player;

/**
 * %kills% placeholder - kill count
 * 
 * Put this file in plugins/Artifex/items/
 * Run /ci reload
 * Use %kills% in any text!
 * 
 * Usage examples:
 * - "Your kills: %kills%"
 * - In title: "Kills: %kills%"
 * - In ActionBar: "Kills: %kills%"
 */
public class KillsPlaceholder extends CustomPlaceholder {

    // Temporary storage (in a real plugin - a database)
    private static final java.util.Map<java.util.UUID, Integer> kills = new java.util.HashMap<>();

    public KillsPlaceholder() {
        super("kills"); // %kills%
    }

    @Override
    public String getValue(Player player) {
        if (player == null) return "0";
        return String.valueOf(kills.getOrDefault(player.getUniqueId(), 0));
    }

    /**
     * Add a kill (call when a mob/player is killed)
     */
    public static void addKill(Player player) {
        kills.merge(player.getUniqueId(), 1, Integer::sum);
    }

    /**
     * Get the kill count
     */
    public static int getKills(Player player) {
        return kills.getOrDefault(player.getUniqueId(), 0);
    }

    /**
     * Reset kills
     */
    public static void resetKills(Player player) {
        kills.remove(player.getUniqueId());
    }
}
