package com.thepigcat.spoiler.api;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

import java.util.List;
import java.util.Optional;

// Optional key may only be empty if this is the default food stages
public record FoodStages(Optional<TagKey<Item>> key, List<FoodStage> stages) {
    public static final Codec<FoodStages> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            TagKey.codec(Registries.ITEM).optionalFieldOf("items").forGetter(FoodStages::key),
            FoodStage.CODEC.listOf().fieldOf("stages").forGetter(FoodStages::stages)
    ).apply(inst, FoodStages::new));
}
