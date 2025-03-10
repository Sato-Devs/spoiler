package com.thepigcat.foodspoiling.registries;

import com.thepigcat.foodspoiling.FSRegistries;
import com.thepigcat.foodspoiling.FSTags;
import com.thepigcat.foodspoiling.FoodSpoiling;
import com.thepigcat.foodspoiling.api.FoodStage;
import com.thepigcat.foodspoiling.api.FoodStages;
import net.minecraft.data.worldgen.BootstapContext;
import net.minecraft.resources.ResourceKey;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class FSFoodStages {
    private static final Map<ResourceKey<FoodStages>, FoodStages> SHOP_ENTRIES = new HashMap<>();

    public static final ResourceKey<FoodStages> RAW_MEATS = register("raw_meats", new FoodStages(Optional.of(FSTags.RAW_MEATS), List.of(
            new FoodStage(FSFoodQualities.FRESH, 10),
            new FoodStage(FSFoodQualities.STALE, 30),
            new FoodStage(FSFoodQualities.MOLDY, 70)
    )));
    public static final ResourceKey<FoodStages> DEFAULT = register("default", new FoodStages(Optional.empty(), List.of(
            new FoodStage(FSFoodQualities.FRESH, 10),
            new FoodStage(FSFoodQualities.SPOILED, 20),
            new FoodStage(FSFoodQualities.STALE, 30),
            new FoodStage(FSFoodQualities.MOLDY, 40),
            new FoodStage(FSFoodQualities.AGED, 50)
    )));

    private static void register(BootstapContext<FoodStages> context, ResourceKey<FoodStages> key, FoodStages builder) {
        context.register(key, builder);
    }

    private static ResourceKey<FoodStages> key(String name) {
        return ResourceKey.create(FSRegistries.FOOD_STAGES_KEY, FoodSpoiling.rl(name));
    }

    private static ResourceKey<FoodStages> register(String name, FoodStages foodStages) {
        ResourceKey<FoodStages> key = key(name);
        SHOP_ENTRIES.put(key, foodStages);
        return key;
    }

    public static void bootstrap(BootstapContext<FoodStages> context) {
        for (Map.Entry<ResourceKey<FoodStages>, FoodStages> entries : SHOP_ENTRIES.entrySet()) {
            register(context, entries.getKey(), entries.getValue());
        }
    }
}
