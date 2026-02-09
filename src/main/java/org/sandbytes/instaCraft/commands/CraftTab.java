package org.sandbytes.instaCraft.commands;

import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.sandbytes.instaCraft.InstaCraft;
import org.sandbytes.instaCraft.utils.CraftingUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

public class CraftTab implements TabCompleter {

    private final InstaCraft plugin;
    private static final List<String> SUBCOMMANDS = Arrays.asList(
            "reload", "list", "recipe", "browse", "search", "stats", "fav", "alias", "spy");
    private static final List<String> FAV_SUBS = Arrays.asList("add", "remove", "list", "craft");
    private static final List<String> ALIAS_SUBS = Arrays.asList("set", "remove", "list");

    public CraftTab(InstaCraft instance) {
        this.plugin = instance;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String s, @NotNull String[] args) {
        List<String> suggestions = new ArrayList<>();

        if (args.length == 1) {
            String search = args[0].toLowerCase();
            for (String sub : SUBCOMMANDS) {
                if (sub.startsWith(search)) suggestions.add(sub);
            }
            // Also suggest craftable materials and aliases
            Set<Material> craftable = CraftingUtils.getCraftableMaterials();
            for (Material material : craftable) {
                String name = material.name().toLowerCase();
                if (name.startsWith(search)) suggestions.add(name);
            }
            for (String alias : plugin.getAliasManager().getAliases().keySet()) {
                if (alias.startsWith(search)) suggestions.add(alias);
            }
        } else if (args.length == 2) {
            String sub = args[0].toLowerCase();
            String search = args[1].toLowerCase();

            switch (sub) {
                case "recipe":
                    suggestMaterials(search, suggestions);
                    break;
                case "fav":
                    for (String fs : FAV_SUBS) {
                        if (fs.startsWith(search)) suggestions.add(fs);
                    }
                    break;
                case "alias":
                    for (String as : ALIAS_SUBS) {
                        if (as.startsWith(search)) suggestions.add(as);
                    }
                    break;
            }
        } else if (args.length == 3) {
            String sub = args[0].toLowerCase();
            String action = args[1].toLowerCase();
            String search = args[2].toLowerCase();

            if ("fav".equals(sub)) {
                if ("craft".equals(action) || "remove".equals(action)) {
                    // Suggest favorite names
                    if (sender instanceof Player) {
                        for (String name : plugin.getFavoritesManager().getFavoriteNames(((Player) sender).getUniqueId())) {
                            if (name.startsWith(search)) suggestions.add(name);
                        }
                    }
                }
            } else if ("alias".equals(sub) && "remove".equals(action)) {
                for (String alias : plugin.getAliasManager().getAliases().keySet()) {
                    if (alias.startsWith(search)) suggestions.add(alias);
                }
            }
        } else if (args.length == 4) {
            String sub = args[0].toLowerCase();
            String action = args[1].toLowerCase();
            String search = args[3].toLowerCase();

            if ("fav".equals(sub) && "add".equals(action)) {
                suggestMaterials(search, suggestions);
            } else if ("alias".equals(sub) && "set".equals(action)) {
                suggestMaterials(search, suggestions);
            }
        }

        return suggestions;
    }

    private void suggestMaterials(String search, List<String> suggestions) {
        Set<Material> craftable = CraftingUtils.getCraftableMaterials();
        for (Material material : craftable) {
            String name = material.name().toLowerCase();
            if (name.startsWith(search)) suggestions.add(name);
        }
    }
}
