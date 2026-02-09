package org.sandbytes.instaCraft.config;

import org.bukkit.configuration.file.YamlConfiguration;
import org.sandbytes.instaCraft.InstaCraft;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;

public abstract class BaseConfig {

    private final String fileName;
    protected volatile File file;
    protected volatile YamlConfiguration config;

    protected BaseConfig(String fileName) {
        this.fileName = fileName;
    }

    public synchronized void load() {
        file = new File(InstaCraft.getInstance().getDataFolder(), fileName);

        if (!file.exists()) {
            InstaCraft.getInstance().saveResource(fileName, false);
        }

        config = YamlConfiguration.loadConfiguration(file);

        InputStream defStream = InstaCraft.getInstance().getResource(fileName);
        if (defStream != null) {
            try (InputStreamReader reader = new InputStreamReader(defStream)) {
                YamlConfiguration defConfig = YamlConfiguration.loadConfiguration(reader);
                config.setDefaults(defConfig);
                config.options().copyDefaults(true);
            } catch (java.io.IOException e) {
                InstaCraft.getInstance().getLogger().warning("Failed to read defaults for " + fileName + ": " + e.getMessage());
            }
        }

        save();
    }

    public synchronized void save() {
        try {
            config.save(file);
        } catch (Exception e) {
            InstaCraft.getInstance().getLogger().severe("An error occurred while saving " + fileName + "!");
        }
    }

    public synchronized void reload() {
        load();
    }
}
