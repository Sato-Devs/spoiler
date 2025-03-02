package com.thepigcat.foodspoiling.api;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.thepigcat.foodspoiling.FSRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

import java.util.List;

public record FoodStages(TagKey<Item> key, List<FoodStage> stages) {
    public static final Codec<FoodStages> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            TagKey.codec(Registries.ITEM).fieldOf("items").forGetter(FoodStages::key),
            FoodStage.CODEC.listOf().fieldOf("stages").forGetter(FoodStages::stages)
    ).apply(inst, FoodStages::new));
}
