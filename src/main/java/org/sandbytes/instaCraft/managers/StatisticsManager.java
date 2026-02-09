package org.sandbytes.instaCraft.managers;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.sandbytes.instaCraft.InstaCraft;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class StatisticsManager {

    private File file;
    private YamlConfiguration data;
    private volatile boolean dirty = false;
    private int saveTaskId = -1;

    public void load() {
        // Cancel existing save task if reloading
        if (saveTaskId != -1) {
            Bukkit.getScheduler().cancelTask(saveTaskId);
            saveTaskId = -1;
        }

        file = new File(InstaCraft.getInstance().getDataFolder(), "statistics.yml");
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                InstaCraft.getInstance().getLogger().warning("Could not create statistics.yml: " + e.getMessage());
            }
        }
        data = YamlConfiguration.loadConfiguration(file);

        // Auto-save every 60 seconds (1200 ticks) asynchronously
        saveTaskId = Bukkit.getScheduler().runTaskTimerAsynchronously(
                InstaCraft.getInstance(), this::saveIfDirty, 1200L, 1200L
        ).getTaskId();
    }

    /**
     * Ensures statistics are loaded. Safe to call multiple times —
     * only loads if not already initialized.
     */
    public void ensureLoaded() {
        if (data == null) {
            load();
        }
    }

    public void shutdown() {
        if (saveTaskId != -1) {
            Bukkit.getScheduler().cancelTask(saveTaskId);
            saveTaskId = -1;
        }
        save(); // Final synchronous save
    }

    private void saveIfDirty() {
        if (dirty) {
            save();
        }
    }

    public synchronized void save() {
        if (data == null || file == null) return;
        try {
            data.save(file);
            dirty = false;
        } catch (IOException e) {
            InstaCraft.getInstance().getLogger().warning("Could not save statistics.yml: " + e.getMessage());
        }
    }

    public synchronized void recordCraft(UUID playerId, String playerName, Material material, int amount) {
        String basePath = "players." + playerId.toString();

        data.set(basePath + ".name", playerName);

        int totalCrafts = data.getInt(basePath + ".total-crafts", 0);
        data.set(basePath + ".total-crafts", totalCrafts + amount);

        String itemPath = basePath + ".items." + material.name();
        int itemCount = data.getInt(itemPath, 0);
        data.set(itemPath, itemCount + amount);

        dirty = true;
    }

    public synchronized int getTotalCrafts(UUID playerId) {
        if (data == null) return 0;
        return data.getInt("players." + playerId.toString() + ".total-crafts", 0);
    }

    public synchronized Map<String, Integer> getItemCrafts(UUID playerId) {
        Map<String, Integer> items = new HashMap<>();
        if (data == null) return items;
        String path = "players." + playerId.toString() + ".items";
        if (data.getConfigurationSection(path) != null) {
            for (String key : data.getConfigurationSection(path).getKeys(false)) {
                items.put(key, data.getInt(path + "." + key, 0));
            }
        }
        return items;
    }

    public synchronized String getTopItem(UUID playerId) {
        Map<String, Integer> items = getItemCrafts(playerId);
        if (items.isEmpty()) return "None";

        return items.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("None");
    }
}
