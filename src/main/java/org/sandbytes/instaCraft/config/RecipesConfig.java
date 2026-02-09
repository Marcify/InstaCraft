package org.sandbytes.instaCraft.config;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.sandbytes.instaCraft.InstaCraft;

import java.util.HashMap;
import java.util.Map;

/**
 * Manages custom recipes defined in recipes.yml.
 * These recipes only work through /craft and are separate from server recipes.
 */
public class RecipesConfig extends BaseConfig {

    private static final RecipesConfig instance = new RecipesConfig();

    // resultMaterial -> ingredients map
    private final Map<Material, Map<Material, Integer>> customRecipes = new HashMap<>();
    // resultMaterial -> result amount
    private final Map<Material, Integer> resultAmounts = new HashMap<>();

    private RecipesConfig() {
        super("recipes.yml");
    }

    @Override
    public synchronized void load() {
        super.load();
        parseRecipes();
    }

    private void parseRecipes() {
        customRecipes.clear();
        resultAmounts.clear();

        ConfigurationSection recipesSection = config.getConfigurationSection("recipes");
        if (recipesSection == null) return;

        for (String key : recipesSection.getKeys(false)) {
            ConfigurationSection recipe = recipesSection.getConfigurationSection(key);
            if (recipe == null) continue;

            String resultName = recipe.getString("result", "");
            int resultAmount = recipe.getInt("amount", 1);
            Material resultMat = Material.matchMaterial(resultName);
            if (resultMat == null) {
                InstaCraft.getInstance().getLogger().warning(
                        "Invalid material in custom recipe '" + key + "': " + resultName);
                continue;
            }

            ConfigurationSection ingredientsSection = recipe.getConfigurationSection("ingredients");
            if (ingredientsSection == null) continue;

            Map<Material, Integer> ingredients = new HashMap<>();
            boolean valid = true;

            for (String ingKey : ingredientsSection.getKeys(false)) {
                Material ingMat = Material.matchMaterial(ingKey);
                if (ingMat == null) {
                    InstaCraft.getInstance().getLogger().warning(
                            "Invalid ingredient in custom recipe '" + key + "': " + ingKey);
                    valid = false;
                    break;
                }
                ingredients.put(ingMat, ingredientsSection.getInt(ingKey, 1));
            }

            if (valid && !ingredients.isEmpty()) {
                customRecipes.put(resultMat, ingredients);
                resultAmounts.put(resultMat, resultAmount);
            }
        }

        InstaCraft.getInstance().getLogger().info("Loaded " + customRecipes.size() + " custom recipe(s).");
    }

    public boolean hasCustomRecipe(Material material) {
        return customRecipes.containsKey(material);
    }

    public Map<Material, Integer> getIngredients(Material material) {
        Map<Material, Integer> ingredients = customRecipes.get(material);
        return ingredients != null ? new HashMap<>(ingredients) : null;
    }

    public int getResultAmount(Material material) {
        return resultAmounts.getOrDefault(material, 1);
    }

    public Map<Material, Map<Material, Integer>> getAllCustomRecipes() {
        return new HashMap<>(customRecipes);
    }

    public static RecipesConfig getInstance() {
        return instance;
    }
}
