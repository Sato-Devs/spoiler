package com.thepigcat.foodspoiling.datagen;

import com.thepigcat.foodspoiling.FoodSpoiling;
import com.thepigcat.foodspoiling.api.FoodQuality;
import com.thepigcat.foodspoiling.registries.FSFoodQualities;
import com.thepigcat.foodspoiling.registries.FSItems;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.EntityTypeTags;
import net.minecraftforge.common.Tags;
import net.minecraftforge.common.data.LanguageProvider;

public class FSEnUSLangProvider extends LanguageProvider {
    public FSEnUSLangProvider(PackOutput output) {
        super(output, FoodSpoiling.MODID, "en_us");
    }

    @Override
    protected void addTranslations() {
        add(FSFoodQualities.AGED, "Aged");
        add(FSFoodQualities.FRESH, "Fresh");
        add(FSFoodQualities.MOLDY, "Moldy");
        add(FSFoodQualities.SPOILED, "Spoiled");
        add(FSFoodQualities.STALE, "Stale");

        addItem(FSItems.ROTTEN_MASS, "Rotten Mass");
        addItem(FSItems.DECOMPOSED_GOO, "Decomposed Goo");
    }

    public void add(ResourceKey<FoodQuality> key, String name) {
        String translationKey = key.registry().getPath() + "." + key.location().getNamespace() + "." + key.location().getPath();
        add(translationKey, name);
    }
}
