package org.sandbytes.instaCraft.managers;

import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.sandbytes.instaCraft.InstaCraft;

public class EconomyManager {

    private Economy economy;
    private boolean available;

    public void setup() {
        if (Bukkit.getPluginManager().getPlugin("Vault") == null) {
            available = false;
            InstaCraft.getInstance().getLogger().info("Vault not found. Economy features disabled.");
            return;
        }

        RegisteredServiceProvider<Economy> rsp = Bukkit.getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            available = false;
            InstaCraft.getInstance().getLogger().warning("No economy provider found. Economy features disabled.");
            return;
        }

        economy = rsp.getProvider();
        available = true;
        InstaCraft.getInstance().getLogger().info("Vault economy hooked: " + economy.getName());
    }

    public boolean isAvailable() {
        return available && economy != null;
    }

    public double getBalance(Player player) {
        if (!isAvailable()) return 0;
        return economy.getBalance(player);
    }

    public boolean hasBalance(Player player, double amount) {
        if (!isAvailable()) return true;
        return economy.has(player, amount);
    }

    public boolean withdraw(Player player, double amount) {
        if (!isAvailable()) return true;
        if (amount <= 0) return true;
        EconomyResponse response = economy.withdrawPlayer(player, amount);
        return response.transactionSuccess();
    }

    public String format(double amount) {
        if (!isAvailable()) return String.format("%.2f", amount);
        return economy.format(amount);
    }
}
