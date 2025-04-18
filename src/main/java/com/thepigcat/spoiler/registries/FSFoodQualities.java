package com.thepigcat.spoiler.registries;

import com.thepigcat.spoiler.FSRegistries;
import com.thepigcat.spoiler.FoodSpoiling;
import com.thepigcat.spoiler.api.FoodQuality;
import com.thepigcat.spoiler.utils.RGBAColor;
import net.minecraft.data.worldgen.BootstapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.alchemy.Potions;

import java.util.HashMap;
import java.util.Map;
import java.util.function.UnaryOperator;

public final class FSFoodQualities {
    private static final Map<ResourceKey<FoodQuality>, FoodQuality.Builder> SHOP_ENTRIES = new HashMap<>();

    public static final ResourceKey<FoodQuality> FRESH = register("fresh", builder -> builder
            .textColor(new RGBAColor(255, 134, 221, 79))
            .tintColor(new RGBAColor(255, 255, 255, 255))
            .saturation(10)
            .usableInRecipes(FoodQuality.RecipeType.CRAFTING)
    );
    public static final ResourceKey<FoodQuality> SPOILED = register("spoiled", builder -> builder
            .textColor(new RGBAColor(255, 135, 170, 78))
            .tintColor(new RGBAColor(79, 97, 179, 50))
            .saturation(10)
            .effects(Potions.HEALING, 0.75f)
    );
    public static final ResourceKey<FoodQuality> STALE = register("stale", builder -> builder
            .textColor(new RGBAColor(255, 170, 167, 78))
            .tintColor(new RGBAColor(148, 97, 179, 50))
            .saturation(10)
            .effects(Potions.HEALING, 0.75f)
    );
    public static final ResourceKey<FoodQuality> MOLDY = register("moldy", builder -> builder
            .textColor(new RGBAColor(255, 122, 132, 9))
            .tintColor(new RGBAColor(193, 97, 179, 50))
            .saturation(10)
            .effects(Potions.HEALING, 0.75f)
    );
    public static final ResourceKey<FoodQuality> AGED = register("aged", builder -> builder
            .textColor(new RGBAColor(255, 112, 92, 12))
            .tintColor(new RGBAColor(255, 97, 179, 50))
            .saturation(10)
            .effects(new MobEffectInstance(MobEffects.ABSORPTION), 0.75f)
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
