package org.sandbytes.instaCraft.managers;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.sandbytes.instaCraft.config.CraftConfig;
import org.sandbytes.instaCraft.config.MessagesConfig;
import org.sandbytes.instaCraft.utils.ColorUtils;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Manages admin spy notifications for crafting events.
 */
public class SpyManager {

    private final Set<UUID> spyingPlayers = new HashSet<>();

    public void toggleSpy(UUID playerId) {
        if (spyingPlayers.contains(playerId)) {
            spyingPlayers.remove(playerId);
        } else {
            spyingPlayers.add(playerId);
        }
    }

    public boolean isSpy(UUID playerId) {
        return spyingPlayers.contains(playerId);
    }

    public void removePlayer(UUID playerId) {
        spyingPlayers.remove(playerId);
    }

    /**
     * Notifies all spying admins about a craft event.
     * Only notifies if the item is in the spy watch list (or list is empty = watch all).
     */
    public void notifyCraft(Player crafter, Material material, int amount) {
        CraftConfig config = CraftConfig.getInstance();
        if (!config.isSpyEnabled()) return;

        List<String> watchList = config.getSpyWatchList();
        if (!watchList.isEmpty() && !watchList.contains(material.name())) {
            return;
        }

        String message = ColorUtils.colorize(MessagesConfig.getInstance().getRawMessage("spy_notification")
                .replace("%player%", crafter.getName())
                .replace("%amount%", String.valueOf(amount))
                .replace("%item%", material.name().toLowerCase().replace("_", " ")));

        for (UUID spyId : new HashSet<>(spyingPlayers)) {
            Player spy = Bukkit.getPlayer(spyId);
            if (spy != null && spy.isOnline() && !spy.getUniqueId().equals(crafter.getUniqueId())) {
                spy.sendMessage(message);
            }
        }
    }

    public void clear() {
        spyingPlayers.clear();
    }
}
