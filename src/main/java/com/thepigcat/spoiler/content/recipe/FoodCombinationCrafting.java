package com.thepigcat.spoiler.content.recipe;

import com.thepigcat.spoiler.FSTags;
import com.thepigcat.spoiler.registries.FSRecipes;
import com.thepigcat.spoiler.utils.NBTSpoilingUtils;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

public class FoodCombinationCrafting extends CustomRecipe {
    public FoodCombinationCrafting(ResourceLocation pId, CraftingBookCategory pCategory) {
        super(pId, pCategory);
    }

    @Override
    public boolean matches(CraftingContainer pContainer, Level pLevel) {
        ItemStack foodItem = ItemStack.EMPTY;
        for (int i = 0; i < pContainer.getContainerSize(); i++) {
            ItemStack item = pContainer.getItem(i);

            if (item.isEmpty()) continue;

            if (foodItem.isEmpty()) {
                if (NBTSpoilingUtils.hasFoodState(item) && item.isEdible() && !item.is(FSTags.UNSPOILABLE_FOODS)) {
                    foodItem = item;
                }
            } else {
                if (!foodItem.is(item.getItem()) || !(NBTSpoilingUtils.hasFoodState(item) && item.isEdible() && !item.is(FSTags.UNSPOILABLE_FOODS))) {
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public ItemStack assemble(CraftingContainer pContainer, RegistryAccess pRegistryAccess) {
        float highestProgress = 0;
        int foods = 0;
        ItemStack result = ItemStack.EMPTY;
        for (int i = 0; i < pContainer.getContainerSize(); i++) {
            ItemStack item = pContainer.getItem(i);

            if (item.isEmpty()) continue;

            float progress = NBTSpoilingUtils.getSpoilingProgress(item);
            if (highestProgress < progress) {
                highestProgress = progress;
                result = item;
                foods++;
            }
        }
        return result.copyWithCount(foods);
    }

    @Override
    public boolean canCraftInDimensions(int pWidth, int pHeight) {
        return true;
    }

    @Override
    public @NotNull RecipeSerializer<?> getSerializer() {
        return FSRecipes.FOOD_COMBINATION.get();
    }
}
