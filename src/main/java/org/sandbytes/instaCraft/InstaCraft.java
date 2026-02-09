package org.sandbytes.instaCraft;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.sandbytes.instaCraft.commands.CraftCommand;
import org.sandbytes.instaCraft.commands.CraftTab;
import org.sandbytes.instaCraft.config.CraftConfig;
import org.sandbytes.instaCraft.config.GUIConfig;
import org.sandbytes.instaCraft.config.MessagesConfig;
import org.sandbytes.instaCraft.config.RecipesConfig;
import org.sandbytes.instaCraft.managers.ConfigManager;
import org.sandbytes.instaCraft.gui.GUIListener;
import org.sandbytes.instaCraft.hooks.InstaCraftExpansion;
import org.sandbytes.instaCraft.managers.AliasManager;
import org.sandbytes.instaCraft.managers.CooldownManager;
import org.sandbytes.instaCraft.managers.EconomyManager;
import org.sandbytes.instaCraft.managers.FavoritesManager;
import org.sandbytes.instaCraft.managers.LimitsManager;
import org.sandbytes.instaCraft.managers.SpyManager;
import org.sandbytes.instaCraft.managers.StatisticsManager;
import org.sandbytes.instaCraft.utils.CraftingUtils;
import org.sandbytes.instaCraft.utils.CraftLogger;
import org.sandbytes.instaCraft.utils.UpdateChecker;

public class InstaCraft extends JavaPlugin {

    private static InstaCraft instance;
    private CooldownManager cooldownManager;
    private EconomyManager economyManager;
    private StatisticsManager statisticsManager;
    private FavoritesManager favoritesManager;
    private AliasManager aliasManager;
    private SpyManager spyManager;
    private LimitsManager limitsManager;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        // Load configs
        CraftConfig.getInstance().load();
        MessagesConfig.getInstance().load();
        RecipesConfig.getInstance().load();
        GUIConfig.getInstance().load();

        // Build recipe cache (includes custom recipes)
        CraftingUtils.buildCache();

        // Init crafting file logger
        CraftLogger.init();

        // Init managers
        cooldownManager = new CooldownManager();
        getServer().getPluginManager().registerEvents(cooldownManager, this);

        economyManager = new EconomyManager();
        if (CraftConfig.getInstance().isEconomyEnabled()) {
            economyManager.setup();
        }

        statisticsManager = new StatisticsManager();
        if (CraftConfig.getInstance().isStatisticsEnabled()) {
            statisticsManager.load();
        }

        favoritesManager = new FavoritesManager();
        favoritesManager.load();

        aliasManager = new AliasManager();
        aliasManager.load();

        spyManager = new SpyManager();

        limitsManager = new LimitsManager();
        limitsManager.start();

        // Register commands
        loadCommands();

        // Register GUI listener
        getServer().getPluginManager().registerEvents(new GUIListener(), this);

        // PlaceholderAPI hook
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new InstaCraftExpansion(this).register();
            getLogger().info("PlaceholderAPI expansion registered.");
        }

        // Update checker
        if (CraftConfig.getInstance().isUpdateCheckerEnabled()) {
            UpdateChecker updateChecker = new UpdateChecker(this);
            getServer().getPluginManager().registerEvents(updateChecker, this);
            updateChecker.check();
        }

        getLogger().info("InstaCraft has been enabled!");
    }

    @Override
    public void onDisable() {
        CraftLogger.shutdown();
        CraftingUtils.clearCache();
        cooldownManager.clear();
        statisticsManager.shutdown();
        favoritesManager.shutdown();
        spyManager.clear();
        limitsManager.stop();
        getLogger().info("InstaCraft has been disabled!");
    }

    private void loadCommands() {
        getCommand("craft").setExecutor(new CraftCommand(this));
        getCommand("craft").setTabCompleter(new CraftTab(this));
    }

    public CooldownManager getCooldownManager() { return cooldownManager; }
    public EconomyManager getEconomyManager() { return economyManager; }
    public StatisticsManager getStatisticsManager() { return statisticsManager; }
    public FavoritesManager getFavoritesManager() { return favoritesManager; }
    public AliasManager getAliasManager() { return aliasManager; }
    public SpyManager getSpyManager() { return spyManager; }
    public LimitsManager getLimitsManager() { return limitsManager; }

    public static InstaCraft getInstance() { return instance; }
}
