package com.thepigcat.foodspoiling.api;

import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.thepigcat.foodspoiling.utils.CodecUtils;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.alchemy.Potion;

import java.util.ArrayList;
import java.util.List;

public record FoodQuality(int textColor, int tintColor, float saturationMod, float nutritionMod, List<Pair<Either<MobEffectInstance, Potion>, Float>> effects) {
    private static final Codec<Pair<Either<MobEffectInstance, Potion>, Float>> EFFECT_CODEC = RecordCodecBuilder.create(inst -> inst.group(
            Codec.either(CodecUtils.MOB_EFFECT_INSTANCE_CODEC, CodecUtils.registryCodec(BuiltInRegistries.POTION)).fieldOf("effect").forGetter(Pair::getFirst),
            Codec.FLOAT.fieldOf("probability").forGetter(Pair::getSecond)
    ).apply(inst, Pair::of));
    public static final Codec<FoodQuality> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            Codec.INT.fieldOf("text_color").forGetter(FoodQuality::textColor),
            Codec.INT.fieldOf("tint_color").forGetter(FoodQuality::tintColor),
            Codec.FLOAT.fieldOf("saturation_mod").forGetter(FoodQuality::saturationMod),
            Codec.FLOAT.fieldOf("nutrition_mod").forGetter(FoodQuality::nutritionMod),
            EFFECT_CODEC.listOf().fieldOf("effects").forGetter(FoodQuality::effects)
    ).apply(inst, FoodQuality::new));

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private int textColor;
        private int tintColor;
        private float saturation;
        private int nutrition;
        private final List<Pair<Either<MobEffectInstance, Potion>, Float>> effects = new ArrayList<>();

        private Builder() {
        }

        public Builder textColor(int textColor) {
            this.textColor = textColor;
            return this;
        }

        public Builder tintColor(int tintColor) {
            this.tintColor = tintColor;
            return this;
        }

        public Builder saturation(float saturation) {
            this.saturation = saturation;
            return this;
        }

        public Builder nutrition(int nutrition) {
            this.nutrition = nutrition;
            return this;
        }

        public Builder effects(Potion potion, float probability) {
            this.effects.add(Pair.of(Either.right(potion), probability));
            return this;
        }

        public Builder effects(MobEffectInstance effect, float probability) {
            this.effects.add(Pair.of(Either.left(effect), probability));
            return this;
        }

        public FoodQuality build() {
            return new FoodQuality(textColor, tintColor, saturation, nutrition, effects);
        }
    }
}