package com.thepigcat.foodspoiling;

import com.thepigcat.foodspoiling.api.FoodQuality;
import com.thepigcat.foodspoiling.api.FoodStages;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;

public final class FSRegistries {
    public static final ResourceKey<Registry<FoodQuality>> FOOD_QUALITY_KEY = ResourceKey.createRegistryKey(FoodSpoiling.rl("food_quality"));
    public static final ResourceKey<Registry<FoodStages>> FOOD_STAGES_KEY = ResourceKey.createRegistryKey(FoodSpoiling.rl("food_stages"));
}
