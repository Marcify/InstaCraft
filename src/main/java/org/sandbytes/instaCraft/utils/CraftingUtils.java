package org.sandbytes.instaCraft.utils;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.BlastingRecipe;
import org.bukkit.inventory.CampfireRecipe;
import org.bukkit.inventory.FurnaceRecipe;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.inventory.SmithingRecipe;
import org.bukkit.inventory.SmokingRecipe;
import org.bukkit.inventory.StonecuttingRecipe;
import org.sandbytes.instaCraft.config.CraftConfig;
import org.sandbytes.instaCraft.config.MessagesConfig;
import org.sandbytes.instaCraft.config.RecipesConfig;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class CraftingUtils {

    private static final Map<Material, Recipe> recipeCache = new HashMap<>();
    private static final Set<Material> craftableMaterials = new HashSet<>();
    private static boolean cacheLoaded = false;

    public static void buildCache() {
        recipeCache.clear();
        craftableMaterials.clear();

        List<String> enabledTypes = CraftConfig.getInstance().getEnabledRecipeTypes();

        Bukkit.recipeIterator().forEachRemaining(recipe -> {
            if (recipe.getResult() == null || recipe.getResult().getType() == Material.AIR) return;
            if (!isRecipeTypeEnabled(recipe, enabledTypes)) return;
            Material mat = recipe.getResult().getType();
            if (!recipeCache.containsKey(mat)) {
                recipeCache.put(mat, recipe);
                craftableMaterials.add(mat);
            }
        });

        // Add custom recipe materials to craftable set
        RecipesConfig recipesConfig = RecipesConfig.getInstance();
        for (Material mat : recipesConfig.getAllCustomRecipes().keySet()) {
            craftableMaterials.add(mat);
        }

        cacheLoaded = true;
    }

    public static void clearCache() {
        recipeCache.clear();
        craftableMaterials.clear();
        cacheLoaded = false;
    }

    private static boolean isRecipeTypeEnabled(Recipe recipe, List<String> enabledTypes) {
        if (recipe instanceof ShapedRecipe || recipe instanceof ShapelessRecipe) {
            return enabledTypes.contains("CRAFTING");
        }
        if (recipe instanceof FurnaceRecipe || recipe instanceof BlastingRecipe
                || recipe instanceof SmokingRecipe || recipe instanceof CampfireRecipe) {
            return enabledTypes.contains("FURNACE");
        }
        if (recipe instanceof SmithingRecipe) {
            return enabledTypes.contains("SMITHING");
        }
        if (recipe instanceof StonecuttingRecipe) {
            return enabledTypes.contains("STONECUTTING");
        }
        return false;
    }

    public static Recipe getCraftingRecipe(Material material) {
        if (!cacheLoaded) buildCache();
        return recipeCache.get(material);
    }

    public static boolean canBeCrafted(Material material) {
        if (!cacheLoaded) buildCache();
        return craftableMaterials.contains(material) || RecipesConfig.getInstance().hasCustomRecipe(material);
    }

    public static Set<Material> getCraftableMaterials() {
        if (!cacheLoaded) buildCache();
        return Collections.unmodifiableSet(craftableMaterials);
    }

    /**
     * Checks if the player has materials for either a custom recipe or a vanilla recipe.
     */
    public static boolean hasRequiredMaterials(Player player, Material material, int amount) {
        RecipesConfig recipesConfig = RecipesConfig.getInstance();
        if (recipesConfig.hasCustomRecipe(material)) {
            Map<Material, Integer> ingredients = recipesConfig.getIngredients(material);
            if (ingredients == null || ingredients.isEmpty()) return false;
            Map<Material, Integer> totalRequired = new HashMap<>();
            ingredients.forEach((mat, count) -> totalRequired.put(mat, count * amount));
            return hasMaterialsInInventory(player.getInventory(), totalRequired);
        }

        Recipe recipe = getCraftingRecipe(material);
        if (recipe == null) return false;
        Map<Material, Integer> required = getRecipeMaterials(recipe);
        Map<Material, Integer> totalRequired = new HashMap<>();
        required.forEach((mat, count) -> totalRequired.put(mat, count * amount));
        return hasMaterialsInInventory(player.getInventory(), totalRequired);
    }

    /**
     * Overload for backward compatibility with Recipe parameter.
     */
    public static boolean hasRequiredMaterials(Player player, Recipe recipe, int amount) {
        Map<Material, Integer> required = getRecipeMaterials(recipe);
        Map<Material, Integer> totalRequired = new HashMap<>();
        required.forEach((mat, count) -> totalRequired.put(mat, count * amount));
        return hasMaterialsInInventory(player.getInventory(), totalRequired);
    }

    /**
     * Gets materials for a material (custom recipe first, then vanilla).
     */
    public static Map<Material, Integer> getMaterialsForItem(Material material) {
        RecipesConfig recipesConfig = RecipesConfig.getInstance();
        if (recipesConfig.hasCustomRecipe(material)) {
            Map<Material, Integer> ingredients = recipesConfig.getIngredients(material);
            return ingredients != null ? ingredients : new HashMap<>();
        }
        Recipe recipe = getCraftingRecipe(material);
        if (recipe != null) {
            return getRecipeMaterials(recipe);
        }
        return new HashMap<>();
    }

    public static Map<Material, Integer> getRecipeMaterials(Recipe recipe) {
        Map<Material, Integer> materials = new HashMap<>();

        if (recipe instanceof ShapedRecipe) {
            ((ShapedRecipe) recipe).getIngredientMap().forEach((key, itemStack) -> {
                if (itemStack != null) materials.merge(itemStack.getType(), 1, Integer::sum);
            });
        } else if (recipe instanceof ShapelessRecipe) {
            for (ItemStack itemStack : ((ShapelessRecipe) recipe).getIngredientList()) {
                if (itemStack != null) materials.merge(itemStack.getType(), 1, Integer::sum);
            }
        } else if (recipe instanceof FurnaceRecipe) {
            materials.put(((FurnaceRecipe) recipe).getInput().getType(), 1);
        } else if (recipe instanceof BlastingRecipe) {
            materials.put(((BlastingRecipe) recipe).getInput().getType(), 1);
        } else if (recipe instanceof SmokingRecipe) {
            materials.put(((SmokingRecipe) recipe).getInput().getType(), 1);
        } else if (recipe instanceof CampfireRecipe) {
            materials.put(((CampfireRecipe) recipe).getInput().getType(), 1);
        } else if (recipe instanceof StonecuttingRecipe) {
            materials.put(((StonecuttingRecipe) recipe).getInput().getType(), 1);
        } else if (recipe instanceof SmithingRecipe) {
            SmithingRecipe smithing = (SmithingRecipe) recipe;
            if (smithing.getBase() instanceof RecipeChoice.MaterialChoice) {
                RecipeChoice.MaterialChoice base = (RecipeChoice.MaterialChoice) smithing.getBase();
                if (!base.getChoices().isEmpty()) {
                    materials.put(base.getChoices().get(0), 1);
                }
            }
            if (smithing.getAddition() instanceof RecipeChoice.MaterialChoice) {
                RecipeChoice.MaterialChoice addition = (RecipeChoice.MaterialChoice) smithing.getAddition();
                if (!addition.getChoices().isEmpty()) {
                    materials.put(addition.getChoices().get(0), 1);
                }
            }
        }

        return materials;
    }

    private static boolean hasMaterialsInInventory(Inventory inventory, Map<Material, Integer> requiredMaterials) {
        Map<Material, Integer> playerMaterials = new HashMap<>();
        for (ItemStack item : inventory.getContents()) {
            if (item != null) playerMaterials.merge(item.getType(), item.getAmount(), Integer::sum);
        }
        for (Map.Entry<Material, Integer> entry : requiredMaterials.entrySet()) {
            if (playerMaterials.getOrDefault(entry.getKey(), 0) < entry.getValue()) return false;
        }
        return true;
    }

    /**
     * Crafts an item (custom or vanilla) and gives it to the player.
     * Returns true if some items had to be dropped.
     */
    public static boolean craftItem(Player player, Material material, int amount) {
        RecipesConfig recipesConfig = RecipesConfig.getInstance();

        Map<Material, Integer> requiredMaterials;
        ItemStack result;

        if (recipesConfig.hasCustomRecipe(material)) {
            requiredMaterials = recipesConfig.getIngredients(material);
            if (requiredMaterials == null) return false;
            int resultAmount = recipesConfig.getResultAmount(material);
            result = new ItemStack(material, resultAmount);
        } else {
            Recipe recipe = getCraftingRecipe(material);
            if (recipe == null) return false;
            requiredMaterials = getRecipeMaterials(recipe);
            result = recipe.getResult().clone();
        }

        // Remove materials
        requiredMaterials.forEach((mat, count) ->
                player.getInventory().removeItem(new ItemStack(mat, count * amount))
        );

        int totalItems = result.getAmount() * amount;
        boolean dropped = false;

        while (totalItems > 0) {
            int stackSize = Math.min(totalItems, result.getMaxStackSize());
            ItemStack toGive = result.clone();
            toGive.setAmount(stackSize);

            HashMap<Integer, ItemStack> overflow = player.getInventory().addItem(toGive);
            if (!overflow.isEmpty()) {
                for (ItemStack leftover : overflow.values()) {
                    player.getWorld().dropItemNaturally(player.getLocation(), leftover);
                }
                dropped = true;
            }
            totalItems -= stackSize;
        }

        return dropped;
    }

    /**
     * Legacy overload that takes a Recipe directly.
     */
    public static boolean craftItem(Player player, Recipe recipe, int amount) {
        Map<Material, Integer> requiredMaterials = getRecipeMaterials(recipe);
        requiredMaterials.forEach((material, count) ->
                player.getInventory().removeItem(new ItemStack(material, count * amount))
        );

        ItemStack result = recipe.getResult().clone();
        int totalItems = result.getAmount() * amount;
        boolean dropped = false;

        while (totalItems > 0) {
            int stackSize = Math.min(totalItems, result.getMaxStackSize());
            ItemStack toGive = result.clone();
            toGive.setAmount(stackSize);

            HashMap<Integer, ItemStack> overflow = player.getInventory().addItem(toGive);
            if (!overflow.isEmpty()) {
                for (ItemStack leftover : overflow.values()) {
                    player.getWorld().dropItemNaturally(player.getLocation(), leftover);
                }
                dropped = true;
            }
            totalItems -= stackSize;
        }

        return dropped;
    }

    /**
     * Gets the result amount for a material (custom or vanilla).
     */
    public static int getResultAmount(Material material) {
        RecipesConfig recipesConfig = RecipesConfig.getInstance();
        if (recipesConfig.hasCustomRecipe(material)) {
            return recipesConfig.getResultAmount(material);
        }
        Recipe recipe = getCraftingRecipe(material);
        if (recipe != null) return recipe.getResult().getAmount();
        return 1;
    }

    public static String getMaterialBreakdown(Material material, int amount) {
        Map<Material, Integer> materials = getMaterialsForItem(material);
        return formatBreakdown(materials, amount);
    }

    public static String getMaterialBreakdown(Recipe recipe, int amount) {
        Map<Material, Integer> materials = getRecipeMaterials(recipe);
        return formatBreakdown(materials, amount);
    }

    private static String formatBreakdown(Map<Material, Integer> materials, int amount) {
        MessagesConfig messages = MessagesConfig.getInstance();
        String format = messages.getRawMessage("material_format");
        String separator = messages.getRawMessage("material_separator");
        
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (Map.Entry<Material, Integer> entry : materials.entrySet()) {
            if (!first) sb.append(separator);
            first = false;
            sb.append(format
                    .replace("%amount%", String.valueOf(entry.getValue() * amount))
                    .replace("%material%", entry.getKey().name().toLowerCase().replace("_", " ")));
        }
        return sb.toString();
    }
}
