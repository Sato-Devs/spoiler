package com.thepigcat.foodspoiling.registries;

import com.thepigcat.foodspoiling.FSRegistries;
import com.thepigcat.foodspoiling.FoodSpoiling;
import com.thepigcat.foodspoiling.api.FoodQuality;
import net.minecraft.data.worldgen.BootstapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.FastColor;
import net.minecraft.world.item.alchemy.Potions;

import java.util.HashMap;
import java.util.Map;
import java.util.function.UnaryOperator;

public final class FSFoodQualities {
    private static final Map<ResourceKey<FoodQuality>, FoodQuality.Builder> SHOP_ENTRIES = new HashMap<>();

    public static final ResourceKey<FoodQuality> FRESH = register("fresh", builder -> builder
            .color(FastColor.ARGB32.color(255, 134, 221, 79))
            .saturation(10)
            .effects(Potions.HEALING, 0.75f)
    );
    public static final ResourceKey<FoodQuality> SPOILED = register("spoiled", builder -> builder
            .color(FastColor.ARGB32.color(255, 135, 170, 78))
            .saturation(10)
    );
    public static final ResourceKey<FoodQuality> STALE = register("stale", builder -> builder
            .color(FastColor.ARGB32.color(255, 170, 167, 78))
            .saturation(10)
    );
    public static final ResourceKey<FoodQuality> MOLDY = register("moldy", builder -> builder
            .color(FastColor.ARGB32.color(255, 122, 132, 9))
            .saturation(10)
    );
    public static final ResourceKey<FoodQuality> AGED = register("aged", builder -> builder
            .color(FastColor.ARGB32.color(255, 112, 92, 12))
            .saturation(10)
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
