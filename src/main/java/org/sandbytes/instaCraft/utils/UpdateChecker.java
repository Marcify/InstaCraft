package org.sandbytes.instaCraft.utils;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.sandbytes.instaCraft.InstaCraft;
import org.sandbytes.instaCraft.config.CraftConfig;
import org.sandbytes.instaCraft.config.MessagesConfig;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.logging.Level;

public class UpdateChecker implements Listener {

    private final InstaCraft plugin;
    private volatile String latestVersion = null;
    private volatile boolean updateAvailable = false;

    public UpdateChecker(InstaCraft plugin) {
        this.plugin = plugin;
    }

    public void check() {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            HttpURLConnection connection = null;
            try {
                String repo = CraftConfig.getInstance().getUpdateRepository();
                URL url = new URL("https://api.github.com/repos/" + repo + "/releases/latest");
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setRequestProperty("Accept", "application/vnd.github.v3+json");
                connection.setConnectTimeout(5000);
                connection.setReadTimeout(5000);

                int responseCode = connection.getResponseCode();
                if (responseCode == 404) {
                    // No releases yet or repo not found - this is fine for new plugins
                    plugin.getLogger().info("No releases found yet. Update checking will work once you publish a release.");
                    return;
                }
                if (responseCode == 200) {
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                        StringBuilder response = new StringBuilder();
                        String line;
                        while ((line = reader.readLine()) != null) {
                            response.append(line);
                        }

                        String tagName = extractJsonValue(response.toString(), "tag_name");
                        if (tagName != null) {
                            latestVersion = tagName;
                            String cleanLatest = latestVersion.startsWith("v") ? latestVersion.substring(1) : latestVersion;
                            String currentVersion = plugin.getDescription().getVersion();

                            if (!cleanLatest.equals(currentVersion)) {
                                updateAvailable = true;
                                plugin.getLogger().info("A new version is available: " + latestVersion + " (current: " + currentVersion + ")");
                            } else {
                                plugin.getLogger().info("You are running the latest version.");
                            }
                        }
                    }
                } else {
                    plugin.getLogger().warning("Update check returned HTTP " + responseCode);
                }
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Could not check for updates: " + e.getMessage());
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
        });
    }

    /**
     * Simple JSON string value extractor. Looks for "key":"value" pattern.
     */
    private String extractJsonValue(String json, String key) {
        String searchKey = "\"" + key + "\"";
        int keyIndex = json.indexOf(searchKey);
        if (keyIndex == -1) return null;

        // Find the colon after the key
        int colonIndex = json.indexOf(':', keyIndex + searchKey.length());
        if (colonIndex == -1) return null;

        // Find the opening quote of the value
        int valueStart = json.indexOf('"', colonIndex + 1);
        if (valueStart == -1) return null;

        // Find the closing quote of the value
        int valueEnd = json.indexOf('"', valueStart + 1);
        if (valueEnd == -1) return null;

        return json.substring(valueStart + 1, valueEnd);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (!updateAvailable) return;

        Player player = event.getPlayer();
        if (player.hasPermission("instacraft.reload")) {
            String currentVersion = plugin.getDescription().getVersion();
            String message = MessagesConfig.getInstance().getMessage("update_available")
                    .replace("%version%", latestVersion)
                    .replace("%current%", currentVersion);
            Bukkit.getScheduler().runTaskLater(plugin, () ->
                    player.sendMessage(ColorUtils.colorize(message)), 40L);
        }
    }
}
