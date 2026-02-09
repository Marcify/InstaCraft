package org.sandbytes.instaCraft.hooks;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.sandbytes.instaCraft.InstaCraft;
import org.sandbytes.instaCraft.managers.StatisticsManager;
import org.sandbytes.instaCraft.utils.CraftingUtils;

public class InstaCraftExpansion extends PlaceholderExpansion {

    private final InstaCraft plugin;

    public InstaCraftExpansion(InstaCraft plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "instacraft";
    }

    @Override
    public @NotNull String getAuthor() {
        return plugin.getDescription().getAuthors().isEmpty()
                ? "SandBytes" : plugin.getDescription().getAuthors().get(0);
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public @Nullable String onPlaceholderRequest(Player player, @NotNull String params) {
        if (player == null) return "";

        StatisticsManager stats = plugin.getStatisticsManager();

        switch (params.toLowerCase()) {
            case "total_crafts":
                return String.valueOf(stats.getTotalCrafts(player.getUniqueId()));
            case "top_item":
                return stats.getTopItem(player.getUniqueId());
            case "craftable_count":
                return String.valueOf(CraftingUtils.getCraftableMaterials().size());
            case "favorites_count":
                return String.valueOf(plugin.getFavoritesManager().getFavoriteNames(player.getUniqueId()).size());
            default:
                // %instacraft_item_<MATERIAL>% — per-item craft count
                if (params.toLowerCase().startsWith("item_")) {
                    String itemName = params.substring(5).toUpperCase();
                    return String.valueOf(
                            stats.getItemCrafts(player.getUniqueId())
                                    .getOrDefault(itemName, 0));
                }
                // %instacraft_limit_<MATERIAL>% — remaining limit for item
                if (params.toLowerCase().startsWith("limit_")) {
                    String itemName = params.substring(6).toUpperCase();
                    Material mat = Material.matchMaterial(itemName);
                    if (mat == null) return "0";
                    int remaining = plugin.getLimitsManager().getRemainingLimit(player, mat);
                    return remaining < 0 ? "unlimited" : String.valueOf(remaining);
                }
                return null;
        }
    }
}
