package com.thepigcat.foodspoiling;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import org.jetbrains.annotations.NotNull;

public final class FSTags {
    public static final TagKey<Item> UNSPOILABLE_FOODS = bind("unspoilable_foods");
    public static final TagKey<Item> RAW_MEATS = bind("raw_meats");
    public static final TagKey<Item> COOKED_MEATS = bind("cooked_meats");

    private static @NotNull TagKey<Item> bind(String path) {
        return TagKey.create(Registries.ITEM, new ResourceLocation(FoodSpoiling.MODID, path));
    }
}
