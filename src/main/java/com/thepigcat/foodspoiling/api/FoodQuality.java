package com.thepigcat.foodspoiling.api;

import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.thepigcat.foodspoiling.utils.CodecUtils;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.alchemy.Potion;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public record FoodQuality(int textColor, int tintColor, float saturationMod, float nutritionMod, RecipeType usableInRecipes, List<Pair<Either<MobEffectInstance, Potion>, Float>> effects) {
    private static final Codec<Pair<Either<MobEffectInstance, Potion>, Float>> EFFECT_CODEC = RecordCodecBuilder.create(inst -> inst.group(
            Codec.either(CodecUtils.MOB_EFFECT_INSTANCE_CODEC, CodecUtils.registryCodec(BuiltInRegistries.POTION)).fieldOf("effect").forGetter(Pair::getFirst),
            Codec.FLOAT.fieldOf("probability").forGetter(Pair::getSecond)
    ).apply(inst, Pair::of));
    public static final Codec<FoodQuality> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            Codec.INT.fieldOf("text_color").forGetter(FoodQuality::textColor),
            Codec.INT.fieldOf("tint_color").forGetter(FoodQuality::tintColor),
            Codec.FLOAT.fieldOf("saturation_mod").forGetter(FoodQuality::saturationMod),
            Codec.FLOAT.fieldOf("nutrition_mod").forGetter(FoodQuality::nutritionMod),
            StringRepresentable.fromEnum(RecipeType::values).optionalFieldOf("recipes_usable", RecipeType.ALL).forGetter(FoodQuality::usableInRecipes),
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
        private RecipeType usableInRecipes = RecipeType.ALL;
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

        public Builder usableInRecipes(RecipeType usableInRecipes) {
            this.usableInRecipes = usableInRecipes;
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
            return new FoodQuality(textColor, tintColor, saturation, nutrition, usableInRecipes, effects);
        }
    }

    public enum RecipeType implements StringRepresentable {
        ALL,
        CRAFTING,
        NONE;

        @Override
        public @NotNull String getSerializedName() {
            return switch (this) {
                case ALL -> "all";
                case CRAFTING -> "crafting";
                case NONE -> "none";
            };
        }
    }
}