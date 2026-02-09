package org.sandbytes.instaCraft.commands;

import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.sandbytes.instaCraft.InstaCraft;
import org.sandbytes.instaCraft.config.CraftConfig;
import org.sandbytes.instaCraft.config.GUIConfig;
import org.sandbytes.instaCraft.config.MessagesConfig;
import org.sandbytes.instaCraft.config.RecipesConfig;
import org.sandbytes.instaCraft.gui.RecipeBrowserGUI;
import org.sandbytes.instaCraft.managers.CooldownManager;
import org.sandbytes.instaCraft.managers.EconomyManager;
import org.sandbytes.instaCraft.managers.FavoritesManager;
import org.sandbytes.instaCraft.managers.LimitsManager;
import org.sandbytes.instaCraft.managers.SpyManager;
import org.sandbytes.instaCraft.managers.StatisticsManager;
import org.sandbytes.instaCraft.utils.ColorUtils;
import org.sandbytes.instaCraft.utils.CraftLogger;
import org.sandbytes.instaCraft.utils.CraftingUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class CraftCommand implements CommandExecutor {

    private final InstaCraft plugin;
    private static final int LIST_PAGE_SIZE = 10;

    private static final String HELP_MESSAGE = "&6&lInstaCraft"
            + "\n&6/craft <item> [amount] &f- &eInstantly craft an item"
            + "\n&6/craft list [page] &f- &eList all craftable items"
            + "\n&6/craft browse &f- &eOpen the recipe browser GUI"
            + "\n&6/craft search &f- &eSearch items in GUI"
            + "\n&6/craft recipe <item> &f- &eView recipe for an item"
            + "\n&6/craft fav <add|remove|list|craft> &f- &eManage favorites"
            + "\n&6/craft alias <set|remove|list> &f- &eManage item aliases"
            + "\n&6/craft stats &f- &eView your crafting statistics"
            + "\n&6/craft spy &f- &eToggle admin craft spy"
            + "\n&6/craft reload &f- &eReload all configs";

    public CraftCommand(InstaCraft instance) {
        this.plugin = instance;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String s, @NotNull String[] args) {
        MessagesConfig messages = MessagesConfig.getInstance();

        if (args.length == 0) {
            sender.sendMessage(ColorUtils.colorize(HELP_MESSAGE));
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "reload": return handleReload(sender, messages);
            case "list": return handleList(sender, messages, args);
            case "recipe": return handleRecipe(sender, messages, args);
            case "browse": return handleBrowse(sender, messages);
            case "search": return handleSearch(sender, messages);
            case "stats": return handleStats(sender, messages);
            case "fav": return handleFavorites(sender, messages, args);
            case "alias": return handleAlias(sender, messages, args);
            case "spy": return handleSpy(sender, messages);
            default: return handleCraft(sender, messages, args);
        }
    }

    private boolean handleCraft(CommandSender sender, MessagesConfig messages, String[] args) {
        if (!sender.hasPermission("instacraft.use")) {
            sender.sendMessage(ColorUtils.colorize(messages.getMessage("no_permission")
                    .replace("%permission%", "instacraft.use")));
            return true;
        }
        if (!(sender instanceof Player)) {
            sender.sendMessage(ColorUtils.colorize(messages.getMessage("player_only")));
            return true;
        }

        Player player = (Player) sender;
        CraftConfig config = CraftConfig.getInstance();

        if (!config.isWorldAllowed(player.getWorld().getName())) {
            player.sendMessage(ColorUtils.colorize(messages.getMessage("world_restricted")));
            return true;
        }

        int amount = 1;
        if (args.length >= 2) {
            try {
                amount = Integer.parseInt(args[1]);
                if (amount < 1) amount = 1;
                int maxAmount = config.getMaxCraftAmount();
                if (amount > maxAmount) amount = maxAmount;
            } catch (NumberFormatException e) {
                player.sendMessage(ColorUtils.colorize(messages.getMessage("usage")
                        .replace("%command%", "/craft <item> [amount]")));
                return true;
            }
        }

        CooldownManager cooldownManager = plugin.getCooldownManager();
        int cooldownSeconds = config.getCooldown();
        long remaining = cooldownManager.getRemainingCooldown(player, cooldownSeconds);
        if (remaining > 0) {
            player.sendMessage(ColorUtils.colorize(messages.getMessage("cooldown")
                    .replace("%time%", String.valueOf(remaining))));
            return true;
        }

        Material material = plugin.getAliasManager().resolveInput(args[0]);
        if (material == null) {
            player.sendMessage(ColorUtils.colorize(messages.getMessage("invalid_item")
                    .replace("%item%", args[0])));
            return true;
        }

        if (!player.hasPermission("instacraft.craft.*")
                && !player.hasPermission("instacraft.craft." + material.name().toLowerCase())) {
            if (player.isPermissionSet("instacraft.craft.*") && !player.hasPermission("instacraft.craft.*")) {
                player.sendMessage(ColorUtils.colorize(messages.getMessage("no_permission")
                        .replace("%permission%", "instacraft.craft." + material.name().toLowerCase())));
                return true;
            }
        }

        if (!config.isItemAllowed(material)) {
            player.sendMessage(ColorUtils.colorize(messages.getMessage("item_blocked")
                    .replace("%item%", material.name())));
            return true;
        }

        boolean isCustom = RecipesConfig.getInstance().hasCustomRecipe(material);
        if (!isCustom && CraftingUtils.getCraftingRecipe(material) == null) {
            player.sendMessage(ColorUtils.colorize(messages.getMessage("cannot_craft")
                    .replace("%item%", material.name())));
            return true;
        }

        LimitsManager limitsManager = plugin.getLimitsManager();
        int allowed = limitsManager.getAllowedAmount(player, material, amount);
        if (allowed <= 0) {
            player.sendMessage(ColorUtils.colorize(messages.getMessage("limit_reached")
                    .replace("%item%", material.name())));
            return true;
        }
        amount = allowed;

        if (!CraftingUtils.hasRequiredMaterials(player, material, amount)) {
            player.sendMessage(ColorUtils.colorize(messages.getMessage("missing_materials")
                    .replace("%item%", material.name())));
            return true;
        }

        if (config.isXpEnabled() && "cost".equalsIgnoreCase(config.getXpMode())) {
            int xpCost = config.getItemXpAmount(material) * amount;
            if (xpCost > 0 && player.getTotalExperience() < xpCost) {
                player.sendMessage(ColorUtils.colorize(messages.getMessage("not_enough_xp")
                        .replace("%xp%", String.valueOf(xpCost))));
                return true;
            }
        }

        if (config.isEconomyEnabled()) {
            EconomyManager econ = plugin.getEconomyManager();
            if (econ.isAvailable()) {
                double cost = config.getItemCraftCost(material) * amount;
                if (cost > 0) {
                    if (!econ.hasBalance(player, cost)) {
                        player.sendMessage(ColorUtils.colorize(messages.getMessage("not_enough_money")
                                .replace("%cost%", econ.format(cost))));
                        return true;
                    }
                    if (!econ.withdraw(player, cost)) {
                        player.sendMessage(ColorUtils.colorize(messages.getMessage("not_enough_money")
                                .replace("%cost%", econ.format(cost))));
                        return true;
                    }
                    player.sendMessage(ColorUtils.colorize(messages.getMessage("money_charged")
                            .replace("%cost%", econ.format(cost))));
                }
            }
        }

        boolean dropped = CraftingUtils.craftItem(player, material, amount);
        int resultPerCraft = CraftingUtils.getResultAmount(material);
        int totalCrafted = resultPerCraft * amount;

        player.sendMessage(ColorUtils.colorize(messages.getMessage("crafted_success")
                .replace("%item%", material.name())
                .replace("%amount%", String.valueOf(totalCrafted))));

        if (amount > 1) {
            String breakdown = CraftingUtils.getMaterialBreakdown(material, amount);
            player.sendMessage(ColorUtils.colorize(messages.getMessage("materials_used")
                    .replace("%materials%", breakdown)));
        }
        if (dropped) {
            player.sendMessage(ColorUtils.colorize(messages.getMessage("inventory_full")));
        }

        if (config.isXpEnabled()) {
            int xpAmount = config.getItemXpAmount(material) * amount;
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

        playCraftEffects(player, config);

        if (cooldownSeconds > 0) {
            cooldownManager.setCooldown(player);
        }
        if (config.isCraftingLogEnabled()) {
            CraftLogger.log(player.getName(), material.name(), amount);
        }
        if (config.isStatisticsEnabled()) {
            plugin.getStatisticsManager().recordCraft(
                    player.getUniqueId(), player.getName(), material, totalCrafted);
        }
        limitsManager.recordCraft(player.getUniqueId(), material, totalCrafted);
        plugin.getSpyManager().notifyCraft(player, material, totalCrafted);

        return true;
    }

    private void playCraftEffects(Player player, CraftConfig config) {
        if (config.isCraftSoundEnabled()) {
            try {
                Sound sound = Sound.valueOf(config.getCraftSoundName());
                player.playSound(player.getLocation(), sound,
                        config.getCraftSoundVolume(), config.getCraftSoundPitch());
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Invalid craft sound: " + config.getCraftSoundName());
            }
        }
        if (config.isCraftParticlesEnabled()) {
            player.getWorld().spawnParticle(Particle.VILLAGER_HAPPY,
                    player.getLocation().add(0, 1, 0), 15, 0.5, 0.5, 0.5, 0);
        }
    }

    private boolean handleFavorites(CommandSender sender, MessagesConfig messages, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ColorUtils.colorize(messages.getMessage("player_only")));
            return true;
        }
        Player player = (Player) sender;
        FavoritesManager favManager = plugin.getFavoritesManager();

        if (args.length < 2) {
            sender.sendMessage(ColorUtils.colorize(messages.getMessage("usage_fav")));
            return true;
        }

        switch (args[1].toLowerCase()) {
            case "add": {
                if (args.length < 4) {
                    sender.sendMessage(ColorUtils.colorize(messages.getMessage("usage_fav_add")));
                    return true;
                }
                String name = args[2];
                Material material = plugin.getAliasManager().resolveInput(args[3]);
                if (material == null) {
                    sender.sendMessage(ColorUtils.colorize(messages.getMessage("invalid_item")
                            .replace("%item%", args[3])));
                    return true;
                }
                favManager.addFavorite(player.getUniqueId(), name, material.name());
                sender.sendMessage(ColorUtils.colorize(messages.getMessage("fav_added")
                        .replace("%name%", name).replace("%item%", material.name())));
                return true;
            }
            case "remove": {
                if (args.length < 3) {
                    sender.sendMessage(ColorUtils.colorize(messages.getMessage("usage_fav_remove")));
                    return true;
                }
                String name = args[2];
                if (favManager.getFavorite(player.getUniqueId(), name) == null) {
                    sender.sendMessage(ColorUtils.colorize(messages.getMessage("fav_not_found")
                            .replace("%name%", name)));
                    return true;
                }
                favManager.removeFavorite(player.getUniqueId(), name);
                sender.sendMessage(ColorUtils.colorize(messages.getMessage("fav_removed")
                        .replace("%name%", name)));
                return true;
            }
            case "list": {
                Map<String, String> favorites = favManager.getFavorites(player.getUniqueId());
                if (favorites.isEmpty()) {
                    sender.sendMessage(ColorUtils.colorize(messages.getMessage("fav_empty")));
                    return true;
                }
                sender.sendMessage(ColorUtils.colorize(messages.getMessage("fav_list_header")));
                for (Map.Entry<String, String> entry : favorites.entrySet()) {
                    sender.sendMessage(ColorUtils.colorize(messages.getRawMessage("fav_list_item")
                            .replace("%name%", entry.getKey())
                            .replace("%item%", entry.getValue())));
                }
                return true;
            }
            case "craft": {
                if (args.length < 3) {
                    sender.sendMessage(ColorUtils.colorize(messages.getMessage("usage_fav_craft")));
                    return true;
                }
                String favMaterial = favManager.getFavorite(player.getUniqueId(), args[2]);
                if (favMaterial == null) {
                    sender.sendMessage(ColorUtils.colorize(messages.getMessage("fav_not_found")
                            .replace("%name%", args[2])));
                    return true;
                }
                String[] craftArgs = args.length >= 4
                        ? new String[]{favMaterial, args[3]}
                        : new String[]{favMaterial};
                return handleCraft(sender, messages, craftArgs);
            }
            default:
                sender.sendMessage(ColorUtils.colorize(messages.getMessage("usage_fav")));
                return true;
        }
    }

    private boolean handleAlias(CommandSender sender, MessagesConfig messages, String[] args) {
        if (!sender.hasPermission("instacraft.reload")) {
            sender.sendMessage(ColorUtils.colorize(messages.getMessage("no_permission")
                    .replace("%permission%", "instacraft.reload")));
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(ColorUtils.colorize(messages.getMessage("usage_alias")));
            return true;
        }

        switch (args[1].toLowerCase()) {
            case "set": {
                if (args.length < 4) {
                    sender.sendMessage(ColorUtils.colorize(messages.getMessage("usage_alias_set")));
                    return true;
                }
                Material material = Material.matchMaterial(args[3]);
                if (material == null) {
                    sender.sendMessage(ColorUtils.colorize(messages.getMessage("invalid_item")
                            .replace("%item%", args[3])));
                    return true;
                }
                plugin.getAliasManager().setAlias(args[2], material.name());
                sender.sendMessage(ColorUtils.colorize(messages.getMessage("alias_set")
                        .replace("%alias%", args[2]).replace("%item%", material.name())));
                return true;
            }
            case "remove": {
                if (args.length < 3) {
                    sender.sendMessage(ColorUtils.colorize(messages.getMessage("usage_alias_remove")));
                    return true;
                }
                plugin.getAliasManager().removeAlias(args[2]);
                sender.sendMessage(ColorUtils.colorize(messages.getMessage("alias_removed")
                        .replace("%alias%", args[2])));
                return true;
            }
            case "list": {
                Map<String, String> aliases = plugin.getAliasManager().getAliases();
                if (aliases.isEmpty()) {
                    sender.sendMessage(ColorUtils.colorize(messages.getMessage("alias_empty")));
                    return true;
                }
                sender.sendMessage(ColorUtils.colorize(messages.getMessage("alias_list_header")));
                for (Map.Entry<String, String> entry : aliases.entrySet()) {
                    sender.sendMessage(ColorUtils.colorize(messages.getRawMessage("alias_list_item")
                            .replace("%alias%", entry.getKey())
                            .replace("%item%", entry.getValue())));
                }
                return true;
            }
            default:
                sender.sendMessage(ColorUtils.colorize(messages.getMessage("usage_alias")));
                return true;
        }
    }

    private boolean handleSpy(CommandSender sender, MessagesConfig messages) {
        if (!sender.hasPermission("instacraft.spy")) {
            sender.sendMessage(ColorUtils.colorize(messages.getMessage("no_permission")
                    .replace("%permission%", "instacraft.spy")));
            return true;
        }
        if (!(sender instanceof Player)) {
            sender.sendMessage(ColorUtils.colorize(messages.getMessage("player_only")));
            return true;
        }
        Player player = (Player) sender;
        SpyManager spyManager = plugin.getSpyManager();
        spyManager.toggleSpy(player.getUniqueId());
        if (spyManager.isSpy(player.getUniqueId())) {
            sender.sendMessage(ColorUtils.colorize(messages.getMessage("spy_enabled")));
        } else {
            sender.sendMessage(ColorUtils.colorize(messages.getMessage("spy_disabled")));
        }
        return true;
    }

    private boolean handleReload(CommandSender sender, MessagesConfig messages) {
        if (!sender.hasPermission("instacraft.reload")) {
            sender.sendMessage(ColorUtils.colorize(messages.getMessage("no_permission")
                    .replace("%permission%", "instacraft.reload")));
            return true;
        }

        CraftConfig.getInstance().reload();
        MessagesConfig.getInstance().reload();
        RecipesConfig.getInstance().reload();
        GUIConfig.getInstance().reload();
        CraftingUtils.clearCache();
        CraftingUtils.buildCache();
        plugin.getAliasManager().reload();

        // Re-setup economy if enabled
        if (CraftConfig.getInstance().isEconomyEnabled()) {
            plugin.getEconomyManager().setup();
        }

        // Ensure statistics are loaded
        if (CraftConfig.getInstance().isStatisticsEnabled()) {
            plugin.getStatisticsManager().ensureLoaded();
        }

        // Restart limits timer with potentially new config
        plugin.getLimitsManager().restart();

        // Restart craft logger
        CraftLogger.shutdown();
        CraftLogger.init();

        sender.sendMessage(ColorUtils.colorize(messages.getMessage("reload_success")));
        return true;
    }

    private boolean handleList(CommandSender sender, MessagesConfig messages, String[] args) {
        if (!sender.hasPermission("instacraft.use")) {
            sender.sendMessage(ColorUtils.colorize(messages.getMessage("no_permission")
                    .replace("%permission%", "instacraft.use")));
            return true;
        }

        List<Material> craftable = new ArrayList<>(CraftingUtils.getCraftableMaterials());
        craftable.sort((a, b) -> a.name().compareTo(b.name()));

        int totalPages = Math.max(1, (int) Math.ceil((double) craftable.size() / LIST_PAGE_SIZE));
        int page = 1;
        if (args.length >= 2) {
            try {
                page = Integer.parseInt(args[1]);
            } catch (NumberFormatException ignored) {}
        }
        page = Math.max(1, Math.min(page, totalPages));

        int start = (page - 1) * LIST_PAGE_SIZE;
        int end = Math.min(start + LIST_PAGE_SIZE, craftable.size());

        sender.sendMessage(ColorUtils.colorize(messages.getMessage("list_header")
                .replace("%page%", String.valueOf(page))
                .replace("%total%", String.valueOf(totalPages))));

        for (int i = start; i < end; i++) {
            sender.sendMessage(ColorUtils.colorize(messages.getRawMessage("list_item")
                    .replace("%item%", craftable.get(i).name().toLowerCase())));
        }

        if (page < totalPages) {
            sender.sendMessage(ColorUtils.colorize(messages.getRawMessage("list_footer")
                    .replace("%next%", String.valueOf(page + 1))));
        }

        return true;
    }

    private boolean handleRecipe(CommandSender sender, MessagesConfig messages, String[] args) {
        if (!sender.hasPermission("instacraft.use")) {
            sender.sendMessage(ColorUtils.colorize(messages.getMessage("no_permission")
                    .replace("%permission%", "instacraft.use")));
            return true;
        }
        if (args.length < 2) {
            sender.sendMessage(ColorUtils.colorize(messages.getMessage("usage_recipe")));
            return true;
        }

        Material material = plugin.getAliasManager().resolveInput(args[1]);
        if (material == null) {
            sender.sendMessage(ColorUtils.colorize(messages.getMessage("invalid_item")
                    .replace("%item%", args[1])));
            return true;
        }

        Map<Material, Integer> ingredients = CraftingUtils.getMaterialsForItem(material);
        if (ingredients.isEmpty()) {
            sender.sendMessage(ColorUtils.colorize(messages.getMessage("cannot_craft")
                    .replace("%item%", material.name())));
            return true;
        }

        sender.sendMessage(ColorUtils.colorize(messages.getMessage("recipe_header")
                .replace("%item%", material.name())));
        for (Map.Entry<Material, Integer> entry : ingredients.entrySet()) {
            sender.sendMessage(ColorUtils.colorize(messages.getRawMessage("recipe_ingredient")
                    .replace("%amount%", String.valueOf(entry.getValue()))
                    .replace("%material%", entry.getKey().name().toLowerCase())));
        }

        return true;
    }

    private boolean handleBrowse(CommandSender sender, MessagesConfig messages) {
        if (!sender.hasPermission("instacraft.use")) {
            sender.sendMessage(ColorUtils.colorize(messages.getMessage("no_permission")
                    .replace("%permission%", "instacraft.use")));
            return true;
        }
        if (!(sender instanceof Player)) {
            sender.sendMessage(ColorUtils.colorize(messages.getMessage("player_only")));
            return true;
        }
        RecipeBrowserGUI.openBrowser((Player) sender, 0);
        return true;
    }

    private boolean handleSearch(CommandSender sender, MessagesConfig messages) {
        if (!sender.hasPermission("instacraft.use")) {
            sender.sendMessage(ColorUtils.colorize(messages.getMessage("no_permission")
                    .replace("%permission%", "instacraft.use")));
            return true;
        }
        if (!(sender instanceof Player)) {
            sender.sendMessage(ColorUtils.colorize(messages.getMessage("player_only")));
            return true;
        }
        Player player = (Player) sender;
        RecipeBrowserGUI.setAwaitingSearch(player.getUniqueId());
        player.sendMessage(ColorUtils.colorize(messages.getMessage("search_prompt")));
        return true;
    }


    private boolean handleStats(CommandSender sender, MessagesConfig messages) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ColorUtils.colorize(messages.getMessage("player_only")));
            return true;
        }
        Player player = (Player) sender;
        CraftConfig config = CraftConfig.getInstance();

        if (!config.isStatisticsEnabled()) {
            sender.sendMessage(ColorUtils.colorize(messages.getMessage("stats_disabled")));
            return true;
        }

        StatisticsManager stats = plugin.getStatisticsManager();
        int total = stats.getTotalCrafts(player.getUniqueId());
        String topItem = stats.getTopItem(player.getUniqueId());

        sender.sendMessage(ColorUtils.colorize(messages.getMessage("stats_header")));
        sender.sendMessage(ColorUtils.colorize(messages.getRawMessage("stats_total")
                .replace("%total%", String.valueOf(total))));
        sender.sendMessage(ColorUtils.colorize(messages.getRawMessage("stats_top_item")
                .replace("%item%", topItem)));

        return true;
    }
}
