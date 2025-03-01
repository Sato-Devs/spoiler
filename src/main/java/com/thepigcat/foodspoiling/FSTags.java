package com.thepigcat.foodspoiling;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

public final class FSTags {
    public static final TagKey<Item> FOOD = TagKey.create(Registries.ITEM, new ResourceLocation(FoodSpoiling.MODID, "food"));
}
