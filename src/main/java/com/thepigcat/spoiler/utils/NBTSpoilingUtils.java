package com.thepigcat.spoiler.utils;

import com.thepigcat.spoiler.FSRegistries;
import com.thepigcat.spoiler.api.FoodStages;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public final class NBTSpoilingUtils {
    public static final String FOOD_STATE_KEY = "food_state";
    public static final String CREATION_TIME_KEY = "creation_time";
    public static final String FOOD_STAGES_KEY = "food_stages";
    public static final String SPOILING_MODIFIER_KEY = "spoiling_modifier";
    public static final String SPOILING_PROGRESS_KEY = "spoiling_progress";
    public static final String MAX_SPOILING_PROGRESS_KEY = "max_spoiling_progress";
    public static final String LAST_DAY_TIME_KEY = "last_day_time";

    public static void initItem(ItemStack stack) {
        stack.getOrCreateTag();
    }

    public static boolean hasFoodState(ItemStack stack) {
        if (stack.hasTag()) {
            CompoundTag foodState = getFoodState(stack);
            return foodState != null && NBTSpoilingUtils.getFoodStages(stack) != null;
        }
        return false;
    }

    public static @Nullable CompoundTag getFoodState(ItemStack stack) {
        if (stack.getTag() != null) {
            return stack.getTag().getCompound(FOOD_STATE_KEY);
        }
        return null;
    }

    public static void setFoodState(ItemStack stack, CompoundTag foodState) {
        stack.getOrCreateTag().put(FOOD_STATE_KEY, foodState);
    }

    public static void setCreationTime(ItemStack stack, long creationTime) {
        CompoundTag foodState = getFoodState(stack);
        if (foodState != null) {
            foodState.putLong(CREATION_TIME_KEY, creationTime);
            setFoodState(stack, foodState);
        }
    }

    public static long getCreationTime(ItemStack stack) {
        CompoundTag foodState = getFoodState(stack);
        if (foodState != null) {
            return foodState.getLong(CREATION_TIME_KEY);
        }
        return -1;
    }

    public static void setLastDayTime(ItemStack stack, long lastDayTime) {
        CompoundTag foodState = getFoodState(stack);
        if (foodState != null) {
            foodState.putLong(LAST_DAY_TIME_KEY, lastDayTime);
            setFoodState(stack, foodState);
        }
    }

    public static long getLastDayTime(ItemStack stack) {
        CompoundTag foodState = getFoodState(stack);
        if (foodState != null) {
            return foodState.getLong(LAST_DAY_TIME_KEY);
        }
        return -1;
    }

    public static @Nullable ResourceKey<FoodStages> getFoodStages(ItemStack stack) {
        CompoundTag foodState = getFoodState(stack);
        if (foodState != null) {
            String foodStateString = foodState.getString(FOOD_STAGES_KEY);
            if (!foodStateString.isEmpty()) {
                return ResourceKey.create(FSRegistries.FOOD_STAGES_KEY, new ResourceLocation(foodStateString));
            }
        }
        return null;
    }

    public static void setFoodStages(ItemStack stack, ResourceKey<FoodStages> foodStages) {
        CompoundTag foodState = getFoodState(stack);
        if (foodState != null) {
            foodState.putString(FOOD_STAGES_KEY, foodStages.location().toString());
            setFoodState(stack, foodState);
        }
    }

    public static float getSpoilingModifier(ItemStack stack) {
        CompoundTag foodState = getFoodState(stack);
        if (foodState != null) {
            return foodState.getFloat(SPOILING_MODIFIER_KEY);
        }
        return -1;
    }

    public static void setSpoilingModifier(ItemStack stack, float spoilingModifier) {
        CompoundTag foodState = getFoodState(stack);
        if (foodState != null) {
            foodState.putFloat(SPOILING_MODIFIER_KEY, spoilingModifier);
            setFoodState(stack, foodState);
        }
    }

    public static float getSpoilingProgress(ItemStack stack) {
        CompoundTag foodState = getFoodState(stack);
        if (foodState != null) {
            return foodState.getFloat(SPOILING_PROGRESS_KEY);
        }
        return -1;
    }

    public static void setSpoilingProgress(ItemStack stack, float spoilingProgress) {
        CompoundTag foodState = getFoodState(stack);
        if (foodState != null) {
            foodState.putFloat(SPOILING_PROGRESS_KEY, spoilingProgress);
            setFoodState(stack, foodState);
        }
    }

    public static float getMaxSpoilingProgress(ItemStack stack) {
        CompoundTag foodState = getFoodState(stack);
        if (foodState != null) {
            return foodState.getFloat(MAX_SPOILING_PROGRESS_KEY);
        }
        return -1;
    }

    public static void setMaxSpoilingProgress(ItemStack stack, float maxSpoilingProgress) {
        CompoundTag foodState = getFoodState(stack);
        if (foodState != null) {
            foodState.putFloat(MAX_SPOILING_PROGRESS_KEY, maxSpoilingProgress);
            setFoodState(stack, foodState);
        }
    }
}
