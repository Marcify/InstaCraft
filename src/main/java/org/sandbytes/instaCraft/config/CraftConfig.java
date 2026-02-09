package org.sandbytes.instaCraft.config;

import org.bukkit.Material;

import java.util.List;

/**
 * Main plugin configuration (config.yml).
 * Handles all gameplay settings like cooldowns, economy, limits, etc.
 */
public class CraftConfig extends BaseConfig {

    private static final CraftConfig instance = new CraftConfig();
    private static final int CURRENT_CONFIG_VERSION = 2;

    private CraftConfig() {
        super("config.yml");
    }

    @Override
    public synchronized void load() {
        super.load();
        
        // Handle config version migration
        int version = config.getInt("config-version", 0);
        if (version < CURRENT_CONFIG_VERSION) {
            config.set("config-version", CURRENT_CONFIG_VERSION);
            save();
        }
    }

    // ==================== General Settings ====================

    public int getCooldown() {
        return config.getInt("cooldown", 0);
    }

    public int getMaxCraftAmount() {
        return config.getInt("max-craft-amount", 64);
    }

    public String getFilterMode() {
        return config.getString("filter-mode", "blacklist");
    }

    public List<String> getFilteredItems() {
        return config.getStringList("filtered-items");
    }

    // ==================== Logging ====================

    public boolean isCraftingLogEnabled() {
        return config.getBoolean("crafting-log", false);
    }

    public boolean isLogRotationEnabled() {
        return config.getBoolean("log-rotation.enabled", false);
    }

    public int getLogMaxSizeMb() {
        return config.getInt("log-rotation.max-size-mb", 10);
    }

    // ==================== Recipe Types ====================

    public List<String> getEnabledRecipeTypes() {
        return config.getStringList("enabled-recipe-types");
    }

    // ==================== Sound & Particles ====================

    public boolean isCraftSoundEnabled() {
        return config.getBoolean("craft-sound.enabled", true);
    }

    public String getCraftSoundName() {
        return config.getString("craft-sound.sound", "BLOCK_ANVIL_USE");
    }

    public float getCraftSoundVolume() {
        return (float) config.getDouble("craft-sound.volume", 0.5);
    }

    public float getCraftSoundPitch() {
        return (float) config.getDouble("craft-sound.pitch", 1.0);
    }

    public boolean isCraftParticlesEnabled() {
        return config.getBoolean("craft-particles.enabled", true);
    }

    // ==================== Economy ====================

    public boolean isEconomyEnabled() {
        return config.getBoolean("economy.enabled", false);
    }

    public double getDefaultCraftCost() {
        return config.getDouble("economy.default-cost", 0.0);
    }

    public double getItemCraftCost(Material material) {
        String path = "economy.item-costs." + material.name();
        return config.contains(path) ? config.getDouble(path) : getDefaultCraftCost();
    }

    // ==================== XP ====================

    public boolean isXpEnabled() {
        return config.getBoolean("xp.enabled", false);
    }

    public String getXpMode() {
        return config.getString("xp.mode", "reward");
    }

    public int getDefaultXpAmount() {
        return config.getInt("xp.default-amount", 0);
    }

    public int getItemXpAmount(Material material) {
        String path = "xp.item-amounts." + material.name();
        return config.contains(path) ? config.getInt(path) : getDefaultXpAmount();
    }

    // ==================== World Restrictions ====================

    public boolean isWorldRestrictionsEnabled() {
        return config.getBoolean("world-restrictions.enabled", false);
    }

    public String getWorldRestrictionsMode() {
        return config.getString("world-restrictions.mode", "blacklist");
    }

    public List<String> getRestrictedWorlds() {
        return config.getStringList("world-restrictions.worlds");
    }

    public boolean isWorldAllowed(String worldName) {
        if (!isWorldRestrictionsEnabled()) return true;
        String mode = getWorldRestrictionsMode();
        List<String> worlds = getRestrictedWorlds();
        return "whitelist".equalsIgnoreCase(mode) ? worlds.contains(worldName) : !worlds.contains(worldName);
    }

    // ==================== Item Filtering ====================

    public boolean isItemAllowed(Material material) {
        List<String> filtered = getFilteredItems();
        if (filtered.isEmpty()) return true;
        String mode = getFilterMode();
        return "whitelist".equalsIgnoreCase(mode) 
                ? filtered.contains(material.name()) 
                : !filtered.contains(material.name());
    }

    // ==================== Spy ====================

    public boolean isSpyEnabled() {
        return config.getBoolean("spy.enabled", false);
    }

    public List<String> getSpyWatchList() {
        return config.getStringList("spy.watch-items");
    }

    // ==================== Limits ====================

    public boolean isLimitsEnabled() {
        return config.getBoolean("limits.enabled", false);
    }

    public int getLimitsResetMinutes() {
        return config.getInt("limits.reset-minutes", 1440);
    }

    public int getDefaultItemLimit() {
        return config.getInt("limits.default-limit", 0);
    }

    public int getItemLimit(Material material) {
        String path = "limits.item-limits." + material.name();
        return config.contains(path) ? config.getInt(path) : getDefaultItemLimit();
    }

    // ==================== Statistics ====================

    public boolean isStatisticsEnabled() {
        return config.getBoolean("statistics.enabled", true);
    }

    // ==================== Update Checker ====================

    public boolean isUpdateCheckerEnabled() {
        return config.getBoolean("update-checker.enabled", true);
    }

    public String getUpdateRepository() {
        return config.getString("update-checker.repository", "SandBytes/InstaCraft");
    }

    public static CraftConfig getInstance() {
        return instance;
    }
}
