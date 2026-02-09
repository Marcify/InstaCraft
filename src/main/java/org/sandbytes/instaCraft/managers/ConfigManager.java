package org.sandbytes.instaCraft.managers;

import org.sandbytes.instaCraft.config.CraftConfig;
import org.sandbytes.instaCraft.config.GUIConfig;
import org.sandbytes.instaCraft.config.MessagesConfig;
import org.sandbytes.instaCraft.config.RecipesConfig;

/**
 * Central manager for all plugin configurations.
 * Provides a single point of access for loading, reloading, and accessing configs.
 */
public class ConfigManager {

    private static final ConfigManager instance = new ConfigManager();

    private final CraftConfig craftConfig;
    private final GUIConfig guiConfig;
    private final MessagesConfig messagesConfig;
    private final RecipesConfig recipesConfig;

    private ConfigManager() {
        this.craftConfig = CraftConfig.getInstance();
        this.guiConfig = GUIConfig.getInstance();
        this.messagesConfig = MessagesConfig.getInstance();
        this.recipesConfig = RecipesConfig.getInstance();
    }

    /**
     * Loads all configuration files.
     * Should be called once during plugin startup.
     */
    public void loadAll() {
        craftConfig.load();
        guiConfig.load();
        messagesConfig.load();
        recipesConfig.load();
    }

    /**
     * Reloads all configuration files.
     * Safe to call at runtime.
     */
    public void reloadAll() {
        craftConfig.reload();
        guiConfig.reload();
        messagesConfig.reload();
        recipesConfig.reload();
    }

    /**
     * Gets the main plugin configuration.
     */
    public CraftConfig getCraftConfig() {
        return craftConfig;
    }

    /**
     * Gets the GUI configuration.
     */
    public GUIConfig getGuiConfig() {
        return guiConfig;
    }

    /**
     * Gets the messages configuration.
     */
    public MessagesConfig getMessagesConfig() {
        return messagesConfig;
    }

    /**
     * Gets the custom recipes configuration.
     */
    public RecipesConfig getRecipesConfig() {
        return recipesConfig;
    }

    public static ConfigManager getInstance() {
        return instance;
    }
}
