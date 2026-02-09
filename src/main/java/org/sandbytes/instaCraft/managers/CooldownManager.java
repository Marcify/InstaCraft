package org.sandbytes.instaCraft.managers;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CooldownManager implements Listener {

    private final Map<UUID, Long> cooldowns = new HashMap<>();

    /**
     * Returns remaining cooldown in seconds, or 0 if no cooldown active.
     */
    public long getRemainingCooldown(Player player, int cooldownSeconds) {
        if (cooldownSeconds <= 0) return 0;
        if (player.hasPermission("instacraft.cooldown.bypass")) return 0;

        Long lastUse = cooldowns.get(player.getUniqueId());
        if (lastUse == null) return 0;

        long elapsed = (System.currentTimeMillis() - lastUse) / 1000;
        if (elapsed < cooldownSeconds) {
            return cooldownSeconds - elapsed;
        }
        return 0;
    }

    public void setCooldown(Player player) {
        cooldowns.put(player.getUniqueId(), System.currentTimeMillis());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        cooldowns.remove(event.getPlayer().getUniqueId());
    }

    public void clear() {
        cooldowns.clear();
    }
}
