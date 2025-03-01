package com.thepigcat.foodspoiling.registries;

import com.thepigcat.foodspoiling.FSRegistries;
import com.thepigcat.foodspoiling.FoodSpoiling;
import com.thepigcat.foodspoiling.api.FoodQuality;
import net.minecraft.data.worldgen.BootstapContext;
import net.minecraft.resources.ResourceKey;

import java.util.HashMap;
import java.util.Map;
import java.util.function.UnaryOperator;

public final class FoodQualities {
    private static final Map<ResourceKey<FoodQuality>, FoodQuality.Builder> SHOP_ENTRIES = new HashMap<>();

    public static final ResourceKey<FoodQuality> TEST = register("test", builder -> builder
            .name("test")
            .day(10)
    );

    private static void register(BootstapContext<FoodQuality> context, ResourceKey<FoodQuality> key, FoodQuality.Builder builder) {
        context.register(key, builder.build());
    }

    private static ResourceKey<FoodQuality> key(String name) {
        return ResourceKey.create(FSRegistries.FOOD_QUALITY_KEY, FoodSpoiling.rl(name));
    }

    private static ResourceKey<FoodQuality> register(String name, UnaryOperator<FoodQuality.Builder> builder) {
        ResourceKey<FoodQuality> key = key(name);
        SHOP_ENTRIES.put(key, builder.apply(FoodQuality.builder()));
        return key;
    }

    public static void bootstrap(BootstapContext<FoodQuality> context) {
        for (Map.Entry<ResourceKey<FoodQuality>, FoodQuality.Builder> entries : SHOP_ENTRIES.entrySet()) {
            register(context, entries.getKey(), entries.getValue());
        }
    }
}
