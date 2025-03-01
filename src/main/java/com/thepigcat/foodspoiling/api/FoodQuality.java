package com.thepigcat.foodspoiling.api;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record FoodQuality(String name, int day, int color) {
    public static final FoodQuality EMPTY = new FoodQuality("", 0, -1);
    public static final Codec<FoodQuality> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            Codec.STRING.fieldOf("name").forGetter(FoodQuality::name),
            Codec.INT.fieldOf("day").forGetter(FoodQuality::day),
            Codec.INT.fieldOf("color").forGetter(FoodQuality::color)
    ).apply(inst, FoodQuality::new));

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String name;
        private int day;
        private int color;

        private Builder() {
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder day(int day) {
            this.day = day;
            return this;
        }

        public Builder color(int color) {
            this.color = color;
            return this;
        }

        public FoodQuality build() {
            return new FoodQuality(name, day, color);
        }
    }
}