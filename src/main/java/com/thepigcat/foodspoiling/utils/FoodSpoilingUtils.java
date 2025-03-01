package com.thepigcat.foodspoiling.utils;

import com.thepigcat.foodspoiling.api.FoodQuality;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public final class FoodSpoilingUtils {
    public static final String FOOD_STATE_KEY = "food_state";
    public static final String FOOD_STATE_NAME_KEY = "name";
    public static final String FOOD_STATE_DAY_KEY = "day";

    public static @Nullable FoodQuality getFoodState(ItemStack itemStack) {
        if (itemStack.hasTag()) {
            CompoundTag tag = itemStack.getTag().getCompound(FOOD_STATE_KEY);
            return new FoodQuality(tag.getString(FOOD_STATE_NAME_KEY), tag.getInt(FOOD_STATE_DAY_KEY), 0);
        }
        return null;
    }

    public static void setFoodState(ItemStack itemStack, FoodQuality state) {
        CompoundTag tag = itemStack.getOrCreateTag().getCompound(FOOD_STATE_KEY);
        tag.putString(FOOD_STATE_NAME_KEY, state.name());
        tag.putInt(FOOD_STATE_DAY_KEY, state.day());
        itemStack.getOrCreateTag().put(FOOD_STATE_KEY, tag);
    }

}
