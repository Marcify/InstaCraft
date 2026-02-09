package org.sandbytes.instaCraft.managers;

import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.sandbytes.instaCraft.InstaCraft;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Manages global item aliases defined by admins.
 * Aliases map short names to material names (e.g. dsword -> DIAMOND_SWORD).
 */
public class AliasManager {

    private File file;
    private YamlConfiguration data;
    private final Map<String, String> aliasCache = new HashMap<>();

    public void load() {
        file = new File(InstaCraft.getInstance().getDataFolder(), "aliases.yml");
        if (!file.exists()) {
            InstaCraft.getInstance().saveResource("aliases.yml", false);
        }
        data = YamlConfiguration.loadConfiguration(file);
        rebuildCache();
    }

    public void save() {
        try {
            if (data != null && file != null) {
                data.save(file);
            }
        } catch (IOException e) {
            InstaCraft.getInstance().getLogger().warning("Could not save aliases.yml: " + e.getMessage());
        }
    }

    public void reload() {
        load();
    }

    private void rebuildCache() {
        aliasCache.clear();
        if (data.getConfigurationSection("aliases") != null) {
            for (String key : data.getConfigurationSection("aliases").getKeys(false)) {
                aliasCache.put(key.toLowerCase(), data.getString("aliases." + key, "").toUpperCase());
            }
        }
    }

    /**
     * Resolves an alias to a Material. Returns null if the alias doesn't exist
     * or doesn't map to a valid material.
     */
    public Material resolveAlias(String alias) {
        String materialName = aliasCache.get(alias.toLowerCase());
        if (materialName == null) return null;
        return Material.matchMaterial(materialName);
    }

    /**
     * Tries to resolve input as alias first, then as a direct material name.
     */
    public Material resolveInput(String input) {
        Material fromAlias = resolveAlias(input);
        if (fromAlias != null) return fromAlias;
        return Material.matchMaterial(input);
    }

    public void setAlias(String alias, String materialName) {
        data.set("aliases." + alias.toLowerCase(), materialName.toUpperCase());
        aliasCache.put(alias.toLowerCase(), materialName.toUpperCase());
        save();
    }

    public void removeAlias(String alias) {
        data.set("aliases." + alias.toLowerCase(), null);
        aliasCache.remove(alias.toLowerCase());
        save();
    }

    public Map<String, String> getAliases() {
        return new HashMap<>(aliasCache);
    }
}
