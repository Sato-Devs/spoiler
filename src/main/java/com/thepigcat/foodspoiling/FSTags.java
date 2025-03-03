package com.thepigcat.foodspoiling;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import org.jetbrains.annotations.NotNull;

public final class FSTags {
    public static final TagKey<Item> UNSPOILABLE_FOODS = bindItemTag("unspoilable_foods");
    public static final TagKey<Item> RAW_MEATS = bindItemTag("raw_meats");
    public static final TagKey<Item> COOKED_MEATS = bindItemTag("cooked_meats");
    public static final TagKey<EntityType<?>> ENTITIES_WITH_INVENTORY = bindEntityTypeTag("entities_with_inventory");

    private static @NotNull TagKey<Item> bindItemTag(String path) {
        return TagKey.create(Registries.ITEM, new ResourceLocation(FoodSpoiling.MODID, path));
    }

    private static @NotNull TagKey<EntityType<?>> bindEntityTypeTag(String path) {
        return TagKey.create(Registries.ENTITY_TYPE, new ResourceLocation(FoodSpoiling.MODID, path));
    }
}
