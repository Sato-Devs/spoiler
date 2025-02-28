package com.thepigcat.foodspoiling;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

public class Tags {
    public static final TagKey<Item> FOOD = TagKey.create(Registries.ITEM, new ResourceLocation(ExampleMod.MODID, "food"));
}
