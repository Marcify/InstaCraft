package org.sandbytes.instaCraft.gui;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.sandbytes.instaCraft.InstaCraft;
import org.sandbytes.instaCraft.config.CraftConfig;
import org.sandbytes.instaCraft.config.GUIConfig;
import org.sandbytes.instaCraft.config.MessagesConfig;
import org.sandbytes.instaCraft.utils.ColorUtils;
import org.sandbytes.instaCraft.utils.CraftingUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class RecipeBrowserGUI {

    private static final int SEARCH_TIMEOUT_SECONDS = 30;
    private static final Map<UUID, Integer> playerPages = new ConcurrentHashMap<>();
    private static final Map<UUID, String> playerSearches = new ConcurrentHashMap<>();
    private static final Map<UUID, Integer> searchTimeoutTasks = new ConcurrentHashMap<>();
    private static final Map<UUID, Boolean> searchFromGUI = new ConcurrentHashMap<>();

    public static void openBrowser(Player player, int page) {
        openBrowser(player, page, null);
    }

    public static void openBrowser(Player player, int page, String filter) {
        GUIConfig guiConfig = GUIConfig.getInstance();
        CraftConfig config = CraftConfig.getInstance();
        List<Material> craftable = new ArrayList<>(CraftingUtils.getCraftableMaterials());

        if (filter != null && !filter.isEmpty()) {
            String lowerFilter = filter.toLowerCase();
            craftable = craftable.stream()
                    .filter(m -> m.name().toLowerCase().contains(lowerFilter))
                    .collect(Collectors.toList());
        }

        Collections.sort(craftable, (a, b) -> a.name().compareTo(b.name()));

        int rows = guiConfig.getBrowserRows();
        int itemsPerPage = (rows - 1) * 9;
        int totalPages = Math.max(1, (int) Math.ceil((double) craftable.size() / itemsPerPage));
        page = Math.max(0, Math.min(page, totalPages - 1));
        playerPages.put(player.getUniqueId(), page);

        String title;
        if (filter != null && !filter.isEmpty()) {
            playerSearches.put(player.getUniqueId(), filter);
            title = ColorUtils.colorize(guiConfig.getSearchTitle()
                    .replace("%filter%", filter)
                    .replace("%page%", String.valueOf(page + 1))
                    .replace("%total%", String.valueOf(totalPages)));
        } else {
            playerSearches.remove(player.getUniqueId());
            title = ColorUtils.colorize(guiConfig.getBrowserTitle()
                    .replace("%page%", String.valueOf(page + 1))
                    .replace("%total%", String.valueOf(totalPages)));
        }

        Inventory gui = Bukkit.createInventory(null, rows * 9, title);

        int start = page * itemsPerPage;
        int end = Math.min(start + itemsPerPage, craftable.size());

        for (int i = start; i < end; i++) {
            Material mat = craftable.get(i);
            ItemStack item = new ItemStack(mat);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(ColorUtils.colorize(
                        guiConfig.getItemNameFormat().replace("%item%", formatName(mat.name()))));
                
                List<String> lore = new ArrayList<>();
                for (String line : guiConfig.getItemLore()) {
                    lore.add(ColorUtils.colorize(line));
                }

                if (config.isEconomyEnabled() && guiConfig.isShowCost()) {
                    double cost = config.getItemCraftCost(mat);
                    if (cost > 0) {
                        lore.add(ColorUtils.colorize(
                                guiConfig.getCostFormat().replace("%cost%", String.format("%.2f", cost))));
                    }
                }

                meta.setLore(lore);
                item.setItemMeta(meta);
            }
            gui.setItem(i - start, item);
        }

        // Add filler items if enabled
        if (guiConfig.isFillerEnabled()) {
            ItemStack filler = createFillerItem(guiConfig);
            List<Integer> fillerSlots = guiConfig.getFillerSlots();
            int navRowStart = (rows - 1) * 9;
            
            if (fillerSlots.isEmpty()) {
                for (int slot = navRowStart; slot < rows * 9; slot++) {
                    if (gui.getItem(slot) == null) {
                        gui.setItem(slot, filler);
                    }
                }
            } else {
                for (int slot : fillerSlots) {
                    if (slot >= 0 && slot < rows * 9 && gui.getItem(slot) == null) {
                        gui.setItem(slot, filler);
                    }
                }
            }
        }

        // Navigation bar
        if (page > 0 && guiConfig.isPrevPageEnabled()) {
            gui.setItem(guiConfig.getPrevPageSlot(), createNavItem(
                    getMaterial(guiConfig.getPrevPageMaterial(), Material.ARROW),
                    guiConfig.getPrevPageName(),
                    guiConfig.getPrevPageLore()));
        }

        if (guiConfig.isSearchEnabled()) {
            gui.setItem(guiConfig.getSearchSlot(), createNavItem(
                    getMaterial(guiConfig.getSearchMaterial(), Material.COMPASS),
                    guiConfig.getSearchName(),
                    guiConfig.getSearchLore()));
        }

        if (guiConfig.isPageInfoEnabled()) {
            gui.setItem(guiConfig.getPageInfoSlot(), createNavItem(
                    getMaterial(guiConfig.getPageInfoMaterial(), Material.BOOK),
                    guiConfig.getPageInfoName()
                            .replace("%page%", String.valueOf(page + 1))
                            .replace("%total%", String.valueOf(totalPages)),
                    guiConfig.getPageInfoLore()));
        }

        if (page < totalPages - 1 && guiConfig.isNextPageEnabled()) {
            gui.setItem(guiConfig.getNextPageSlot(), createNavItem(
                    getMaterial(guiConfig.getNextPageMaterial(), Material.ARROW),
                    guiConfig.getNextPageName(),
                    guiConfig.getNextPageLore()));
        }

        player.openInventory(gui);
    }

    public static void openRecipeView(Player player, Material material) {
        Map<Material, Integer> ingredients = CraftingUtils.getMaterialsForItem(material);
        if (ingredients.isEmpty()) return;

        GUIConfig guiConfig = GUIConfig.getInstance();
        int rows = guiConfig.getRecipeRows();
        
        String title = ColorUtils.colorize(guiConfig.getRecipeTitle()
                .replace("%item%", formatName(material.name())));
        
        Inventory gui = Bukkit.createInventory(null, rows * 9, title);

        // Show result
        int resultAmount = CraftingUtils.getResultAmount(material);
        ItemStack result = new ItemStack(material, resultAmount);
        ItemMeta resultMeta = result.getItemMeta();
        if (resultMeta != null) {
            resultMeta.setDisplayName(ColorUtils.colorize(
                    guiConfig.getResultNameFormat().replace("%item%", formatName(material.name()))));
            
            List<String> lore = new ArrayList<>();
            for (String line : guiConfig.getResultLore()) {
                lore.add(ColorUtils.colorize(line
                        .replace("%amount%", String.valueOf(resultAmount))
                        .replace("%item%", formatName(material.name()))));
            }
            resultMeta.setLore(lore);
            result.setItemMeta(resultMeta);
        }
        gui.setItem(guiConfig.getRecipeResultSlot(), result);

        // Show ingredients
        int slot = 0;
        for (Map.Entry<Material, Integer> entry : ingredients.entrySet()) {
            if (slot > 8) break;
            ItemStack ingredient = new ItemStack(entry.getKey(), entry.getValue());
            ItemMeta meta = ingredient.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(ColorUtils.colorize(
                        guiConfig.getIngredientNameFormat().replace("%item%", formatName(entry.getKey().name()))));
                
                List<String> lore = new ArrayList<>();
                for (String line : guiConfig.getIngredientLore()) {
                    lore.add(ColorUtils.colorize(line
                            .replace("%amount%", String.valueOf(entry.getValue()))
                            .replace("%item%", formatName(entry.getKey().name()))));
                }
                meta.setLore(lore);
                ingredient.setItemMeta(meta);
            }
            gui.setItem(slot, ingredient);
            slot++;
        }

        // Back button
        gui.setItem(guiConfig.getRecipeBackSlot(), createNavItem(
                getMaterial(guiConfig.getBackButtonMaterial(), Material.BARRIER),
                guiConfig.getBackButtonName(),
                guiConfig.getBackButtonLore()));

        player.openInventory(gui);
    }

    public static boolean isAwaitingSearch(UUID playerId) {
        return playerSearches.containsKey(playerId) && "PENDING".equals(playerSearches.get(playerId));
    }

    public static void setAwaitingSearch(UUID playerId) {
        setAwaitingSearch(playerId, false);
    }

    public static void setAwaitingSearch(UUID playerId, boolean fromGUI) {
        playerSearches.put(playerId, "PENDING");
        searchFromGUI.put(playerId, fromGUI);
        
        int taskId = Bukkit.getScheduler().runTaskLater(InstaCraft.getInstance(), () -> {
            if (isAwaitingSearch(playerId)) {
                playerSearches.remove(playerId);
                searchTimeoutTasks.remove(playerId);
                boolean wasFromGUI = searchFromGUI.remove(playerId) == Boolean.TRUE;
                
                Player player = Bukkit.getPlayer(playerId);
                if (player != null && player.isOnline()) {
                    player.sendMessage(ColorUtils.colorize(
                            MessagesConfig.getInstance().getMessage("search_expired")));
                    if (wasFromGUI) {
                        openBrowser(player, getPlayerPage(playerId));
                    }
                }
            }
        }, SEARCH_TIMEOUT_SECONDS * 20L).getTaskId();
        
        searchTimeoutTasks.put(playerId, taskId);
    }

    public static void clearSearch(UUID playerId) {
        playerSearches.remove(playerId);
        
        Integer taskId = searchTimeoutTasks.remove(playerId);
        if (taskId != null) {
            Bukkit.getScheduler().cancelTask(taskId);
        }
    }

    public static boolean wasSearchFromGUI(UUID playerId) {
        return searchFromGUI.remove(playerId) == Boolean.TRUE;
    }

    public static String getSearchFilter(UUID playerId) {
        String filter = playerSearches.get(playerId);
        return "PENDING".equals(filter) ? null : filter;
    }

    public static int getPlayerPage(UUID playerId) {
        return playerPages.getOrDefault(playerId, 0);
    }

    public static void removePlayer(UUID playerId) {
        playerPages.remove(playerId);
    }

    private static ItemStack createNavItem(Material material, String name, List<String> loreLines) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ColorUtils.colorize(name));
            if (loreLines != null && !loreLines.isEmpty()) {
                List<String> coloredLore = new ArrayList<>();
                for (String line : loreLines) {
                    coloredLore.add(ColorUtils.colorize(line));
                }
                meta.setLore(coloredLore);
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    static ItemStack createNavItem(Material material, String name) {
        return createNavItem(material, name, null);
    }

    private static ItemStack createFillerItem(GUIConfig guiConfig) {
        Material mat = getMaterial(guiConfig.getFillerMaterial(), Material.GRAY_STAINED_GLASS_PANE);
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ColorUtils.colorize(guiConfig.getFillerName()));
            item.setItemMeta(meta);
        }
        return item;
    }

    private static Material getMaterial(String name, Material fallback) {
        try {
            return Material.valueOf(name.toUpperCase());
        } catch (IllegalArgumentException e) {
            return fallback;
        }
    }

    public static String formatName(String materialName) {
        StringBuilder sb = new StringBuilder();
        String[] parts = materialName.split("_");
        for (int i = 0; i < parts.length; i++) {
            if (i > 0) sb.append(" ");
            if (parts[i].length() > 0) {
                sb.append(parts[i].charAt(0)).append(parts[i].substring(1).toLowerCase());
            }
        }
        return sb.toString();
    }

    // Getters for GUIListener to access configured slots/materials
    public static int getConfiguredPrevPageSlot() {
        return GUIConfig.getInstance().getPrevPageSlot();
    }

    public static int getConfiguredNextPageSlot() {
        return GUIConfig.getInstance().getNextPageSlot();
    }

    public static int getConfiguredSearchSlot() {
        return GUIConfig.getInstance().getSearchSlot();
    }

    public static int getConfiguredPageInfoSlot() {
        return GUIConfig.getInstance().getPageInfoSlot();
    }

    public static int getConfiguredBackButtonSlot() {
        return GUIConfig.getInstance().getRecipeBackSlot();
    }

    public static Material getConfiguredBackButtonMaterial() {
        return getMaterial(GUIConfig.getInstance().getBackButtonMaterial(), Material.BARRIER);
    }

    public static int getItemsPerPage() {
        int rows = GUIConfig.getInstance().getBrowserRows();
        return (rows - 1) * 9;
    }
}
