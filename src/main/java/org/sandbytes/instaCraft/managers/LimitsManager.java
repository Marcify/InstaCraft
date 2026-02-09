package org.sandbytes.instaCraft.managers;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.sandbytes.instaCraft.InstaCraft;
import org.sandbytes.instaCraft.config.CraftConfig;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Tracks per-player crafting limits that reset on a configurable interval.
 */
public class LimitsManager {

    // playerId -> (materialName -> count crafted in current period)
    private final Map<UUID, Map<String, Integer>> playerCrafts = new HashMap<>();
    private int resetTaskId = -1;

    public void start() {
        CraftConfig config = CraftConfig.getInstance();
        if (!config.isLimitsEnabled()) return;

        int resetMinutes = config.getLimitsResetMinutes();
        long resetTicks = resetMinutes * 60L * 20L;

        resetTaskId = Bukkit.getScheduler().runTaskTimer(
                InstaCraft.getInstance(), this::resetAll, resetTicks, resetTicks
        ).getTaskId();
    }

    public void stop() {
        if (resetTaskId != -1) {
            Bukkit.getScheduler().cancelTask(resetTaskId);
            resetTaskId = -1;
        }
        playerCrafts.clear();
    }

    /**
     * Restarts the reset timer without clearing current limit data.
     * Used during config reload to pick up new reset-minutes value.
     */
    public void restart() {
        if (resetTaskId != -1) {
            Bukkit.getScheduler().cancelTask(resetTaskId);
            resetTaskId = -1;
        }
        start();
    }

    /**
     * Checks if the player can craft the given amount.
     * Returns the number they're still allowed to craft (may be less than requested).
     * Returns 0 if they've hit their limit.
     */
    public int getAllowedAmount(Player player, Material material, int requestedAmount) {
        CraftConfig config = CraftConfig.getInstance();
        if (!config.isLimitsEnabled()) return requestedAmount;
        if (player.hasPermission("instacraft.limits.bypass")) return requestedAmount;

        int limit = config.getItemLimit(material);
        if (limit <= 0) return requestedAmount; // 0 or negative = unlimited

        int used = getCraftCount(player.getUniqueId(), material);
        int remaining = limit - used;

        if (remaining <= 0) return 0;
        return Math.min(requestedAmount, remaining);
    }

    public void recordCraft(UUID playerId, Material material, int amount) {
        playerCrafts.computeIfAbsent(playerId, k -> new HashMap<>());
        Map<String, Integer> crafts = playerCrafts.get(playerId);
        crafts.merge(material.name(), amount, Integer::sum);
    }

    public int getCraftCount(UUID playerId, Material material) {
        Map<String, Integer> crafts = playerCrafts.get(playerId);
        if (crafts == null) return 0;
        return crafts.getOrDefault(material.name(), 0);
    }

    public int getRemainingLimit(Player player, Material material) {
        CraftConfig config = CraftConfig.getInstance();
        if (!config.isLimitsEnabled()) return -1;

        int limit = config.getItemLimit(material);
        if (limit <= 0) return -1; // unlimited

        int used = getCraftCount(player.getUniqueId(), material);
        return Math.max(0, limit - used);
    }

    public void removePlayer(UUID playerId) {
        playerCrafts.remove(playerId);
    }

    private void resetAll() {
        playerCrafts.clear();
        InstaCraft.getInstance().getLogger().info("Crafting limits have been reset.");
    }
}
