import me.dcplugin.artifex.api.placeholders.CustomPlaceholder;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * %combo% placeholder - kill combo counter
 *
 * Formats:
 *   %combo%            → "x5" (current combo)
 *   %combo_multiplier% → "2.5x" (damage multiplier)
 *   %combo_timer%      → "8" (seconds until the reset)
 *   %combo_max%        → "15" (maximum combo of the session)
 *
 * The combo resets after 10 seconds without a kill.
 * Every kill increases the combo by 1.
 * Damage multiplier = 1 + (combo * 0.1), maximum 3.0x
 *
 * Put this file in plugins/Artifex/items/
 * Run /ci reload
 *
 * Examples:
 *   Scoreboard: "&7Combo: &c%combo% &7| &e%combo_multiplier%"
 *   ActionBar: "&cCombo x%combo%! Multiplier: %combo_multiplier%"
 *
 * To activate the combo system use triggers:
 *   triggers:
 *     on_kill:
 *       - 'combo_add:1'
 *       - 'message:&cCombo x%combo%! &eMultiplier: %combo_multiplier%'
 */
public class ComboPlaceholder extends CustomPlaceholder {

    private static final long COMBO_TIMEOUT_MS = 10_000; // 10 seconds

    // Current combo
    private static final Map<UUID, Integer> comboCount = new HashMap<>();
    // Time of the last kill
    private static final Map<UUID, Long> lastKillTime = new HashMap<>();
    // Maximum combo of the session
    private static final Map<UUID, Integer> maxCombo = new HashMap<>();

    public ComboPlaceholder() {
        super("combo"); // %combo%
    }

    @Override
    public String getValue(Player player) {
        if (player == null) return "x0";
        return "x" + getCombo(player);
    }

    // ===== Main methods =====

    /**
     * Add a combo (call on kill)
     */
    public static void addCombo(Player player) {
        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();
        Long lastKill = lastKillTime.get(uuid);

        if (lastKill != null && (now - lastKill) > COMBO_TIMEOUT_MS) {
            // The combo expired — start over
            comboCount.put(uuid, 1);
        } else {
            comboCount.merge(uuid, 1, Integer::sum);
        }

        lastKillTime.put(uuid, now);

        // Update the maximum
        int current = comboCount.getOrDefault(uuid, 0);
        int best = maxCombo.getOrDefault(uuid, 0);
        if (current > best) {
            maxCombo.put(uuid, current);
        }
    }

    /**
     * Reset the combo
     */
    public static void resetCombo(Player player) {
        comboCount.remove(player.getUniqueId());
        lastKillTime.remove(player.getUniqueId());
    }

    /**
     * Get the current combo (with a timeout check)
     */
    public static int getCombo(Player player) {
        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();
        Long lastKill = lastKillTime.get(uuid);

        if (lastKill != null && (now - lastKill) > COMBO_TIMEOUT_MS) {
            comboCount.put(uuid, 0);
            return 0;
        }
        return comboCount.getOrDefault(uuid, 0);
    }

    /**
     * Get the damage multiplier
     * Formula: 1 + (combo * 0.1), maximum 3.0
     */
    public static double getMultiplier(Player player) {
        int combo = getCombo(player);
        return Math.min(1.0 + (combo * 0.1), 3.0);
    }

    /**
     * Get the seconds until the combo resets
     */
    public static int getTimerSeconds(Player player) {
        Long lastKill = lastKillTime.get(player.getUniqueId());
        if (lastKill == null) return 0;
        long elapsed = (System.currentTimeMillis() - lastKill) / 1000;
        long remaining = Math.max(0, (COMBO_TIMEOUT_MS / 1000) - elapsed);
        return (int) remaining;
    }

    /**
     * Get the maximum combo of the session
     */
    public static int getMaxCombo(Player player) {
        return maxCombo.getOrDefault(player.getUniqueId(), 0);
    }
}
