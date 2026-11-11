package me.dcplugin.artifex.api.placeholders;

import me.dcplugin.artifex.Main;
import org.bukkit.entity.Player;

/**
 * Base class for creating custom placeholders
 * 
 * Example:
 * <pre>
 * public class KillsPlaceholder extends CustomPlaceholder {
 *     
 *     public KillsPlaceholder() {
 *         super("kills"); // %kills%
 *     }
 *     
 *     @Override
 *     public String getValue(Player player) {
 *         // Get the data from the database
 *         DatabaseManager db = getPlugin().getDatabaseManager();
 *         return String.valueOf(db.queryInt(
 *             "SELECT kills FROM stats WHERE uuid=?", 
 *             player.getUniqueId().toString()
 *         ));
 *     }
 * }
 * </pre>
 * 
 * Usage in any text:
 * "Kills: %kills%"
 */
public abstract class CustomPlaceholder {

    private String identifier;
    private Main plugin;

    protected CustomPlaceholder() {}

    protected CustomPlaceholder(String identifier) {
        this.identifier = identifier.toLowerCase();
    }

    /**
     * Sets the plugin (called automatically)
     */
    public void setPlugin(Main plugin) {
        this.plugin = plugin;
    }

    public Main getPlugin() {
        return plugin;
    }

    // ===== REQUIRED METHOD =====

    /**
     * Gets the placeholder value
     * 
     * @param player the player (may be null for global placeholders)
     * @return the value to substitute
     */
    public abstract String getValue(Player player);

    // ===== OPTIONAL METHODS =====

    /**
     * Called on registration
     */
    public void onRegister() {}

    /**
     * Called on unregistration
     */
    public void onUnregister() {}

    // ===== GETTERS/SETTERS =====

    public String getIdentifier() { return identifier; }
    public void setIdentifier(String identifier) { this.identifier = identifier.toLowerCase(); }
}
