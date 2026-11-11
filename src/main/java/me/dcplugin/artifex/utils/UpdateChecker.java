package me.dcplugin.artifex.utils;

import me.dcplugin.artifex.Main;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UpdateChecker {

    private static final int MAX_RESPONSE_CHARS = 64 * 1024;

    private final Main plugin;
    private final String githubApiUrl;
    private final String githubReleasesUrl;

    // Written from the async thread, read from the main thread — volatile is
    // mandatory, otherwise the value could stay "stale" due to the CPU cache.
    private volatile String latestVersion;
    private volatile boolean updateAvailable;

    public UpdateChecker(Main plugin) {
        this.plugin = plugin;
        this.githubApiUrl = "https://api.github.com/repos/animesao/Artifex/releases/latest";
        this.githubReleasesUrl = "https://github.com/animesao/Artifex/releases";
        this.updateAvailable = false;
    }

    public void checkForUpdates(Consumer<String> consumer) {
        if (consumer == null) return;
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            HttpURLConnection connection = null;
            try {
                URL url = new URL(githubApiUrl);
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(10000);
                connection.setReadTimeout(10000);
                connection.setRequestProperty("User-Agent", "Artifex-Plugin");
                connection.setRequestProperty("Accept", "application/vnd.github.v3+json");

                int responseCode = connection.getResponseCode();

                if (responseCode == 200) {
                    String jsonResponse = readResponse(connection);

                    // Parse tag_name from GitHub API response
                    String tagName = extractTag(jsonResponse);

                    if (tagName != null) {
                        // Remove 'v' prefix if present
                        latestVersion = tagName.startsWith("v") ? tagName.substring(1) : tagName;
                        String currentVersion = plugin.getDescription().getVersion();

                        plugin.getLogger().info("Current version: " + currentVersion + ", Latest version: " + latestVersion);

                        if (compareVersions(latestVersion, currentVersion) > 0) {
                            updateAvailable = true;
                            consumer.accept("§aUpdate found! §7Current: §e" + currentVersion + " §7-> New: §a" + latestVersion);
                            consumer.accept("§7Download: §b" + githubReleasesUrl);
                        } else {
                            consumer.accept("§aYou are using the latest version! §7(" + currentVersion + ")");
                        }
                    } else {
                        consumer.accept("§cCould not find version information");
                    }
                } else if (responseCode == 404) {
                    consumer.accept("§cNo releases found. Check: §b" + githubReleasesUrl);
                } else {
                    consumer.accept("§cCould not check for updates (code: " + responseCode + ")");
                }
            } catch (Exception e) {
                consumer.accept("§cError while checking for updates: " + e.getMessage());
                consumer.accept("§7Check for updates manually: §b" + githubReleasesUrl);
                plugin.getLogger().log(java.util.logging.Level.WARNING, "Update check error: " + e.getMessage(), e);
            } finally {
                // The connection was only closed on the happy path — on an error
                // a busy socket was left behind.
                if (connection != null) {
                    connection.disconnect();
                }
            }
        });
    }

    /**
     * Reads the response, limiting its size (protection against a "bloated" response).
     */
    private static String readResponse(HttpURLConnection connection) throws java.io.IOException {
        StringBuilder response = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (response.length() + line.length() > MAX_RESPONSE_CHARS) {
                    break;
                }
                response.append(line);
            }
        }
        return response.toString();
    }

    private String extractTag(String jsonResponse) {
        // Extract "tag_name":"xxx" from JSON
        Pattern pattern = Pattern.compile("\"tag_name\"\\s*:\\s*\"([^\"]+)\"");
        Matcher matcher = pattern.matcher(jsonResponse);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    /**
     * Compare two version strings (e.g., "1.320.203" vs "1.320.202")
     * Returns: > 0 if v1 > v2, < 0 if v1 < v2, 0 if equal
     *
     * Non-numeric parts (for example "1.2.3-beta") did not break the check:
     * the NumberFormatException aborted the whole update check.
     */
    private int compareVersions(String v1, String v2) {
        String[] parts1 = splitVersion(v1);
        String[] parts2 = splitVersion(v2);

        int maxLength = Math.max(parts1.length, parts2.length);

        for (int i = 0; i < maxLength; i++) {
            int num1 = i < parts1.length ? parseNumeric(parts1[i]) : 0;
            int num2 = i < parts2.length ? parseNumeric(parts2[i]) : 0;

            if (num1 != num2) {
                return Integer.compare(num1, num2);
            }
        }

        return 0;
    }

    private String[] splitVersion(String version) {
        if (version == null) return new String[0];
        // Discard the build suffixes: "1.2.3-beta" -> "1.2.3"
        String cleaned = version.trim();
        int dash = cleaned.indexOf('-');
        if (dash > 0) cleaned = cleaned.substring(0, dash);
        return cleaned.split("\\.");
    }

    private int parseNumeric(String part) {
        if (part == null) return 0;
        StringBuilder digits = new StringBuilder();
        for (int i = 0; i < part.length(); i++) {
            char c = part.charAt(i);
            if (c < '0' || c > '9') break;
            digits.append(c);
        }
        if (digits.length() == 0) return 0;
        try {
            return Integer.parseInt(digits.toString());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    public void notifyPlayer(Player player) {
        if (player == null) return;
        if (updateAvailable && (player.hasPermission("artifex.update") || player.hasPermission("customitems.update"))) {
            player.sendMessage(message("update.header", "&8&m                                                    "));
            player.sendMessage(message("update.title", "&6&lArtifex &7- &aUpdate available!"));
            player.sendMessage(message("update.current-version", "&7Current version: &e{version}")
                    .replace("{version}", plugin.getDescription().getVersion()));
            player.sendMessage(message("update.new-version", "&7New version: &a{version}")
                    .replace("{version}", String.valueOf(latestVersion)));
            player.sendMessage(message("update.download", "&7Download: &b{url}")
                    .replace("{url}", githubReleasesUrl));
            player.sendMessage(message("update.header", "&8&m                                                    "));
        }
    }

    /**
     * Resolves a message from the central message system
     * (overridable in items/messages.java).
     */
    private String message(String path, String defaultMessage) {
        Main plugin = Main.getInstance();
        if (plugin == null || plugin.getMessageManager() == null) return defaultMessage;
        return plugin.getMessageManager().getMessage(path, defaultMessage);
    }

    public boolean isUpdateAvailable() {
        return updateAvailable;
    }

    public String getLatestVersion() {
        return latestVersion;
    }
}
