package org.sandbytes.instaCraft.config;

import java.util.Arrays;
import java.util.List;

/**
 * GUI configuration (gui.yml).
 * Handles all GUI customization settings.
 */
public class GUIConfig extends BaseConfig {

    private static final GUIConfig instance = new GUIConfig();

    private GUIConfig() {
        super("gui.yml");
    }

    public static GUIConfig getInstance() {
        return instance;
    }

    // ==================== Browser Settings ====================

    public String getBrowserTitle() {
        return config.getString("browser.title", "&6InstaCraft - Browse (%page%/%total%)");
    }

    public String getSearchTitle() {
        return config.getString("browser.search-title", "&6Search: %filter% (%page%/%total%)");
    }

    public int getBrowserRows() {
        int rows = config.getInt("browser.rows", 6);
        return Math.max(3, Math.min(6, rows));
    }

    // ==================== Recipe View Settings ====================

    public String getRecipeTitle() {
        return config.getString("recipe-view.title", "&6Recipe: %item%");
    }

    public int getRecipeRows() {
        return Math.max(3, config.getInt("recipe-view.rows", 3));
    }

    public int getRecipeResultSlot() {
        return config.getInt("recipe-view.result-slot", 13);
    }

    public int getRecipeBackSlot() {
        return config.getInt("recipe-view.back-button-slot", 22);
    }

    // ==================== Navigation - Previous Page ====================

    public boolean isPrevPageEnabled() {
        return config.getBoolean("navigation.previous-page.enabled", true);
    }

    public int getPrevPageSlot() {
        return config.getInt("navigation.previous-page.slot", 45);
    }

    public String getPrevPageMaterial() {
        return config.getString("navigation.previous-page.material", "ARROW");
    }

    public String getPrevPageName() {
        return config.getString("navigation.previous-page.name", "&aPrevious Page");
    }

    public List<String> getPrevPageLore() {
        return config.getStringList("navigation.previous-page.lore");
    }

    // ==================== Navigation - Next Page ====================

    public boolean isNextPageEnabled() {
        return config.getBoolean("navigation.next-page.enabled", true);
    }

    public int getNextPageSlot() {
        return config.getInt("navigation.next-page.slot", 53);
    }

    public String getNextPageMaterial() {
        return config.getString("navigation.next-page.material", "ARROW");
    }

    public String getNextPageName() {
        return config.getString("navigation.next-page.name", "&aNext Page");
    }

    public List<String> getNextPageLore() {
        return config.getStringList("navigation.next-page.lore");
    }

    // ==================== Navigation - Search ====================

    public boolean isSearchEnabled() {
        return config.getBoolean("navigation.search.enabled", true);
    }

    public int getSearchSlot() {
        return config.getInt("navigation.search.slot", 47);
    }

    public String getSearchMaterial() {
        return config.getString("navigation.search.material", "COMPASS");
    }

    public String getSearchName() {
        return config.getString("navigation.search.name", "&eSearch Items");
    }

    public List<String> getSearchLore() {
        return config.getStringList("navigation.search.lore");
    }

    // ==================== Navigation - Page Info ====================

    public boolean isPageInfoEnabled() {
        return config.getBoolean("navigation.page-info.enabled", true);
    }

    public int getPageInfoSlot() {
        return config.getInt("navigation.page-info.slot", 49);
    }

    public String getPageInfoMaterial() {
        return config.getString("navigation.page-info.material", "BOOK");
    }

    public String getPageInfoName() {
        return config.getString("navigation.page-info.name", "&6Page %page%/%total%");
    }

    public List<String> getPageInfoLore() {
        return config.getStringList("navigation.page-info.lore");
    }

    // ==================== Navigation - Back Button ====================

    public String getBackButtonMaterial() {
        return config.getString("navigation.back-button.material", "BARRIER");
    }

    public String getBackButtonName() {
        return config.getString("navigation.back-button.name", "&cBack to Browser");
    }

    public List<String> getBackButtonLore() {
        return config.getStringList("navigation.back-button.lore");
    }

    // ==================== Item Display ====================

    public String getItemNameFormat() {
        return config.getString("item-display.name-format", "&e%item%");
    }

    public List<String> getItemLore() {
        List<String> lore = config.getStringList("item-display.lore");
        if (lore.isEmpty()) {
            return Arrays.asList("&7Left-click to view recipe", "&7Right-click to craft x1");
        }
        return lore;
    }

    public boolean isShowCost() {
        return config.getBoolean("item-display.show-cost", true);
    }

    public String getCostFormat() {
        return config.getString("item-display.cost-format", "&7Cost: &6%cost%");
    }

    // ==================== Recipe Display ====================

    public String getResultNameFormat() {
        return config.getString("recipe-display.result-name", "&a&lResult: %item%");
    }

    public List<String> getResultLore() {
        List<String> lore = config.getStringList("recipe-display.result-lore");
        if (lore.isEmpty()) {
            return Arrays.asList("&7Amount: &f%amount%", "", "&aClick to craft x1");
        }
        return lore;
    }

    public String getIngredientNameFormat() {
        return config.getString("recipe-display.ingredient-name", "&e%item%");
    }

    public List<String> getIngredientLore() {
        List<String> lore = config.getStringList("recipe-display.ingredient-lore");
        if (lore.isEmpty()) {
            return Arrays.asList("&7Required: &f%amount%");
        }
        return lore;
    }

    // ==================== Filler ====================

    public boolean isFillerEnabled() {
        return config.getBoolean("filler.enabled", false);
    }

    public String getFillerMaterial() {
        return config.getString("filler.material", "GRAY_STAINED_GLASS_PANE");
    }

    public String getFillerName() {
        return config.getString("filler.name", " ");
    }

    public List<Integer> getFillerSlots() {
        return config.getIntegerList("filler.slots");
    }
}
