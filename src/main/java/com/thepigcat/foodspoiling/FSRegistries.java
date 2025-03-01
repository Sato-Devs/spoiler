package com.thepigcat.foodspoiling;

import com.thepigcat.foodspoiling.api.FoodQuality;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;

public final class FSRegistries {
    public static final ResourceKey<Registry<FoodQuality>> FOOD_QUALITY_KEY = ResourceKey.createRegistryKey(FoodSpoiling.rl("food_quality"));
}
