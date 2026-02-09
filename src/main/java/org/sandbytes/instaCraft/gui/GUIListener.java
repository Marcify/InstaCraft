package org.sandbytes.instaCraft.gui;

import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.sandbytes.instaCraft.InstaCraft;
import org.sandbytes.instaCraft.config.CraftConfig;
import org.sandbytes.instaCraft.config.GUIConfig;
import org.sandbytes.instaCraft.config.MessagesConfig;
import org.sandbytes.instaCraft.managers.CooldownManager;
import org.sandbytes.instaCraft.managers.EconomyManager;
import org.sandbytes.instaCraft.managers.LimitsManager;
import org.sandbytes.instaCraft.utils.ColorUtils;
import org.sandbytes.instaCraft.utils.CraftLogger;
import org.sandbytes.instaCraft.utils.CraftingUtils;

public class GUIListener implements Listener {

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        String title = event.getView().getTitle();

        GUIConfig guiConfig = GUIConfig.getInstance();
        String browserTitleBase = ColorUtils.colorize(guiConfig.getBrowserTitle().split("\\(")[0]).trim();
        String searchTitleBase = ColorUtils.colorize(guiConfig.getSearchTitle().split("%filter%")[0]).trim();
        String recipeTitleBase = ColorUtils.colorize(guiConfig.getRecipeTitle().split("%item%")[0]).trim();

        if (title.startsWith(browserTitleBase) || title.startsWith(searchTitleBase)) {
            event.setCancelled(true);
            handleBrowserClick(player, event);
        } else if (title.startsWith(recipeTitleBase)) {
            event.setCancelled(true);
            handleRecipeViewClick(player, event);
        }
    }

    private void handleBrowserClick(Player player, InventoryClickEvent event) {
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;

        int slot = event.getRawSlot();
        int page = RecipeBrowserGUI.getPlayerPage(player.getUniqueId());
        String filter = RecipeBrowserGUI.getSearchFilter(player.getUniqueId());

        GUIConfig guiConfig = GUIConfig.getInstance();
        int prevSlot = RecipeBrowserGUI.getConfiguredPrevPageSlot();
        int nextSlot = RecipeBrowserGUI.getConfiguredNextPageSlot();
        int searchSlot = RecipeBrowserGUI.getConfiguredSearchSlot();
        int pageInfoSlot = RecipeBrowserGUI.getConfiguredPageInfoSlot();
        int itemsPerPage = RecipeBrowserGUI.getItemsPerPage();

        if (slot == prevSlot && guiConfig.isPrevPageEnabled()) {
            RecipeBrowserGUI.openBrowser(player, page - 1, filter);
            return;
        }
        if (slot == searchSlot && guiConfig.isSearchEnabled()) {
            player.closeInventory();
            RecipeBrowserGUI.setAwaitingSearch(player.getUniqueId(), true);
            player.sendMessage(ColorUtils.colorize(MessagesConfig.getInstance().getMessage("search_prompt")));
            return;
        }
        if (slot == nextSlot && guiConfig.isNextPageEnabled()) {
            RecipeBrowserGUI.openBrowser(player, page + 1, filter);
            return;
        }
        if (slot == pageInfoSlot) return;

        if (slot >= 0 && slot < itemsPerPage) {
            Material material = clicked.getType();
            if (event.getClick() == ClickType.RIGHT || event.getClick() == ClickType.SHIFT_RIGHT) {
                craftFromGUI(player, material);
            } else {
                RecipeBrowserGUI.openRecipeView(player, material);
            }
        }
    }

    private void handleRecipeViewClick(Player player, InventoryClickEvent event) {
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;

        int slot = event.getRawSlot();
        GUIConfig guiConfig = GUIConfig.getInstance();
        int backSlot = RecipeBrowserGUI.getConfiguredBackButtonSlot();
        Material backMaterial = RecipeBrowserGUI.getConfiguredBackButtonMaterial();
        int resultSlot = guiConfig.getRecipeResultSlot();

        if (slot == backSlot && clicked.getType() == backMaterial) {
            int page = RecipeBrowserGUI.getPlayerPage(player.getUniqueId());
            String filter = RecipeBrowserGUI.getSearchFilter(player.getUniqueId());
            RecipeBrowserGUI.openBrowser(player, page, filter);
            return;
        }
        if (slot == resultSlot) {
            craftFromGUI(player, clicked.getType());
        }
    }

    /**
     * Full craft logic from GUI — mirrors CraftCommand.handleCraft checks.
     */
    private void craftFromGUI(Player player, Material material) {
        MessagesConfig messages = MessagesConfig.getInstance();
        CraftConfig config = CraftConfig.getInstance();
        InstaCraft plugin = InstaCraft.getInstance();

        // World restriction
        if (!config.isWorldAllowed(player.getWorld().getName())) {
            player.sendMessage(ColorUtils.colorize(messages.getMessage("world_restricted")));
            return;
        }

        // Permission check
        if (!player.hasPermission("instacraft.use")) {
            player.sendMessage(ColorUtils.colorize(messages.getMessage("no_permission")
                    .replace("%permission%", "instacraft.use")));
            return;
        }

        // Per-item permission
        if (!player.hasPermission("instacraft.craft.*")
                && !player.hasPermission("instacraft.craft." + material.name().toLowerCase())) {
            if (player.isPermissionSet("instacraft.craft.*") && !player.hasPermission("instacraft.craft.*")) {
                player.sendMessage(ColorUtils.colorize(messages.getMessage("no_permission")
                        .replace("%permission%", "instacraft.craft." + material.name().toLowerCase())));
                return;
            }
        }

        // Item filter
        if (!config.isItemAllowed(material)) {
            player.sendMessage(ColorUtils.colorize(messages.getMessage("item_blocked")
                    .replace("%item%", material.name())));
            return;
        }

        // Check if craftable (custom or vanilla)
        boolean isCustom = org.sandbytes.instaCraft.config.RecipesConfig.getInstance().hasCustomRecipe(material);
        if (!isCustom && CraftingUtils.getCraftingRecipe(material) == null) {
            player.sendMessage(ColorUtils.colorize(messages.getMessage("cannot_craft")
                    .replace("%item%", material.name())));
            return;
        }

        // Cooldown
        CooldownManager cooldownManager = plugin.getCooldownManager();
        int cooldownSeconds = config.getCooldown();
        long remaining = cooldownManager.getRemainingCooldown(player, cooldownSeconds);
        if (remaining > 0) {
            player.sendMessage(ColorUtils.colorize(messages.getMessage("cooldown")
                    .replace("%time%", String.valueOf(remaining))));
            return;
        }

        // Crafting limits
        LimitsManager limitsManager = plugin.getLimitsManager();
        int allowed = limitsManager.getAllowedAmount(player, material, 1);
        if (allowed <= 0) {
            player.sendMessage(ColorUtils.colorize(messages.getMessage("limit_reached")
                    .replace("%item%", material.name())));
            return;
        }

        // Materials check
        if (!CraftingUtils.hasRequiredMaterials(player, material, 1)) {
            player.sendMessage(ColorUtils.colorize(messages.getMessage("missing_materials")
                    .replace("%item%", material.name())));
            return;
        }

        // XP cost check
        if (config.isXpEnabled() && "cost".equalsIgnoreCase(config.getXpMode())) {
            int xpCost = config.getItemXpAmount(material);
            if (xpCost > 0 && player.getTotalExperience() < xpCost) {
                player.sendMessage(ColorUtils.colorize(messages.getMessage("not_enough_xp")
                        .replace("%xp%", String.valueOf(xpCost))));
                return;
            }
        }

        // Economy check
        if (config.isEconomyEnabled()) {
            EconomyManager econ = plugin.getEconomyManager();
            if (econ.isAvailable()) {
                double cost = config.getItemCraftCost(material);
                if (cost > 0) {
                    if (!econ.hasBalance(player, cost)) {
                        player.sendMessage(ColorUtils.colorize(messages.getMessage("not_enough_money")
                                .replace("%cost%", econ.format(cost))));
                        return;
                    }
                    if (!econ.withdraw(player, cost)) {
                        player.sendMessage(ColorUtils.colorize(messages.getMessage("not_enough_money")
                                .replace("%cost%", econ.format(cost))));
                        return;
                    }
                    player.sendMessage(ColorUtils.colorize(messages.getMessage("money_charged")
                            .replace("%cost%", econ.format(cost))));
                }
            }
        }

        // Perform craft
        boolean dropped = CraftingUtils.craftItem(player, material, 1);
        int totalCrafted = CraftingUtils.getResultAmount(material);

        player.sendMessage(ColorUtils.colorize(messages.getMessage("crafted_success")
                .replace("%item%", material.name())
                .replace("%amount%", String.valueOf(totalCrafted))));

        if (dropped) {
            player.sendMessage(ColorUtils.colorize(messages.getMessage("inventory_full")));
        }

        // XP reward/cost
        if (config.isXpEnabled()) {
            int xpAmount = config.getItemXpAmount(material);
            if (xpAmount > 0) {
                if ("reward".equalsIgnoreCase(config.getXpMode())) {
                    player.giveExp(xpAmount);
                    player.sendMessage(ColorUtils.colorize(messages.getMessage("xp_rewarded")
                            .replace("%xp%", String.valueOf(xpAmount))));
                } else {
                    player.giveExp(-xpAmount);
                    player.sendMessage(ColorUtils.colorize(messages.getMessage("xp_deducted")
                            .replace("%xp%", String.valueOf(xpAmount))));
                }
            }
        }

        // Sound & particles
        if (config.isCraftSoundEnabled()) {
            try {
                Sound sound = Sound.valueOf(config.getCraftSoundName());
                player.playSound(player.getLocation(), sound,
                        config.getCraftSoundVolume(), config.getCraftSoundPitch());
            } catch (IllegalArgumentException ignored) {}
        }
        if (config.isCraftParticlesEnabled()) {
            player.getWorld().spawnParticle(Particle.VILLAGER_HAPPY,
                    player.getLocation().add(0, 1, 0), 15, 0.5, 0.5, 0.5, 0);
        }

        // Cooldown
        if (cooldownSeconds > 0) {
            cooldownManager.setCooldown(player);
        }

        // Logging
        if (config.isCraftingLogEnabled()) {
            CraftLogger.log(player.getName(), material.name(), 1);
        }

        // Statistics
        if (config.isStatisticsEnabled()) {
            plugin.getStatisticsManager().recordCraft(
                    player.getUniqueId(), player.getName(), material, totalCrafted);
        }

        // Limits
        limitsManager.recordCraft(player.getUniqueId(), material, totalCrafted);

        // Spy
        plugin.getSpyManager().notifyCraft(player, material, totalCrafted);
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) return;
        Player player = (Player) event.getPlayer();
        String title = event.getView().getTitle();

        GUIConfig guiConfig = GUIConfig.getInstance();
        String browserTitleBase = ColorUtils.colorize(guiConfig.getBrowserTitle().split("\\(")[0]).trim();
        String searchTitleBase = ColorUtils.colorize(guiConfig.getSearchTitle().split("%filter%")[0]).trim();
        String recipeTitleBase = ColorUtils.colorize(guiConfig.getRecipeTitle().split("%item%")[0]).trim();

        if (!title.startsWith(browserTitleBase)
                && !title.startsWith(recipeTitleBase)
                && !title.startsWith(searchTitleBase)) {
            RecipeBrowserGUI.removePlayer(player.getUniqueId());
        }
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        if (!RecipeBrowserGUI.isAwaitingSearch(player.getUniqueId())) return;

        event.setCancelled(true);
        String input = event.getMessage().trim();
        boolean fromGUI = RecipeBrowserGUI.wasSearchFromGUI(player.getUniqueId());
        RecipeBrowserGUI.clearSearch(player.getUniqueId());

        if ("cancel".equalsIgnoreCase(input)) {
            player.sendMessage(ColorUtils.colorize(MessagesConfig.getInstance().getMessage("search_cancelled")));
            if (fromGUI) {
                org.bukkit.Bukkit.getScheduler().runTask(InstaCraft.getInstance(), () ->
                        RecipeBrowserGUI.openBrowser(player, 0));
            }
            return;
        }

        org.bukkit.Bukkit.getScheduler().runTask(InstaCraft.getInstance(), () ->
                RecipeBrowserGUI.openBrowser(player, 0, input));
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        java.util.UUID uuid = event.getPlayer().getUniqueId();
        RecipeBrowserGUI.removePlayer(uuid);
        RecipeBrowserGUI.clearSearch(uuid);
        InstaCraft.getInstance().getSpyManager().removePlayer(uuid);
        // Note: LimitsManager data is NOT cleared on quit so limits persist until the reset timer fires
    }
}
