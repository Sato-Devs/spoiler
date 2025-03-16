package com.thepigcat.spoiling.api;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.thepigcat.spoiling.FSRegistries;
import net.minecraft.resources.ResourceKey;

public record FoodStage(ResourceKey<FoodQuality> quality, int days) {
    public static final Codec<FoodStage> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            ResourceKey.codec(FSRegistries.FOOD_QUALITY_KEY).fieldOf("quality").forGetter(FoodStage::quality),
            Codec.INT.fieldOf("days").forGetter(FoodStage::days)
    ).apply(inst, FoodStage::new));
}
