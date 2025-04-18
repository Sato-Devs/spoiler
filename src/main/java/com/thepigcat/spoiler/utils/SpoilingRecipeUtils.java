package com.thepigcat.spoiler.utils;

import com.thepigcat.spoiler.FSTags;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;

public class SpoilingRecipeUtils {
    public static void setResultProgress(CraftingContainer pContainer, RegistryAccess pRegistryAccess, ItemStack resultItem) {
        int foodItems = 0;
        float progress = 0;
        for (int i = 0; i < pContainer.getContainerSize(); i++) {
            ItemStack stack = pContainer.getItem(i);
            if (stack.isEdible() && !stack.is(FSTags.UNSPOILABLE_FOODS) && NBTSpoilingUtils.hasFoodState(stack)) {
                progress += NBTSpoilingUtils.getSpoilingProgress(stack);
                foodItems++;
            }
        }

        SpoilingUtils.initialize(resultItem, 0, 1f, pRegistryAccess);
        NBTSpoilingUtils.setSpoilingProgress(resultItem, progress / foodItems);
        float maxProgress = SpoilingUtils.getMaxProgress(resultItem, pRegistryAccess);
        NBTSpoilingUtils.setMaxSpoilingProgress(resultItem, maxProgress);
    }
}
