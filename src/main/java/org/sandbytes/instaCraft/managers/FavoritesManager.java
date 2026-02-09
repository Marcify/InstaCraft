package org.sandbytes.instaCraft.managers;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.sandbytes.instaCraft.InstaCraft;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class FavoritesManager {

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

        file = new File(InstaCraft.getInstance().getDataFolder(), "favorites.yml");
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                InstaCraft.getInstance().getLogger().warning("Could not create favorites.yml: " + e.getMessage());
            }
        }
        data = YamlConfiguration.loadConfiguration(file);

        // Auto-save every 60 seconds (1200 ticks) asynchronously
        saveTaskId = Bukkit.getScheduler().runTaskTimerAsynchronously(
                InstaCraft.getInstance(), this::saveIfDirty, 1200L, 1200L
        ).getTaskId();
    }

    public void shutdown() {
        if (saveTaskId != -1) {
            Bukkit.getScheduler().cancelTask(saveTaskId);
            saveTaskId = -1;
        }
        save();
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
            InstaCraft.getInstance().getLogger().warning("Could not save favorites.yml: " + e.getMessage());
        }
    }

    public synchronized void addFavorite(UUID playerId, String name, String material) {
        String path = "players." + playerId.toString() + "." + name.toLowerCase();
        data.set(path, material.toUpperCase());
        dirty = true;
    }

    public synchronized void removeFavorite(UUID playerId, String name) {
        String path = "players." + playerId.toString() + "." + name.toLowerCase();
        data.set(path, null);
        dirty = true;
    }

    public synchronized String getFavorite(UUID playerId, String name) {
        if (data == null) return null;
        return data.getString("players." + playerId.toString() + "." + name.toLowerCase());
    }

    public synchronized Map<String, String> getFavorites(UUID playerId) {
        Map<String, String> favorites = new HashMap<>();
        if (data == null) return favorites;
        String path = "players." + playerId.toString();
        if (data.getConfigurationSection(path) != null) {
            for (String key : data.getConfigurationSection(path).getKeys(false)) {
                favorites.put(key, data.getString(path + "." + key));
            }
        }
        return favorites;
    }

    public synchronized List<String> getFavoriteNames(UUID playerId) {
        return new ArrayList<>(getFavorites(playerId).keySet());
    }
}
