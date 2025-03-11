package com.thepigcat.foodspoiling.registries;

import com.thepigcat.foodspoiling.FoodSpoiling;
import com.thepigcat.foodspoiling.content.recipe.FoodCombinationCrafting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class FSRecipes {
    public static final DeferredRegister<RecipeSerializer<?>> RECIPES = DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS, FoodSpoiling.MODID);

    public static final RegistryObject<SimpleCraftingRecipeSerializer<FoodCombinationCrafting>> FOOD_COMBINATION = RECIPES.register("food_combination", () -> new SimpleCraftingRecipeSerializer<>(FoodCombinationCrafting::new));

}
