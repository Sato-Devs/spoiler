package com.thepigcat.spoiler.datagen;

import com.thepigcat.spoiler.FSRegistries;
import com.thepigcat.spoiler.FoodSpoiling;
import com.thepigcat.spoiler.registries.FSFoodStages;
import com.thepigcat.spoiler.registries.FSFoodQualities;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistrySetBuilder;
import net.minecraft.data.PackOutput;
import net.minecraftforge.common.data.DatapackBuiltinEntriesProvider;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

public class FSDatapackRegistryProvider extends DatapackBuiltinEntriesProvider {
    public FSDatapackRegistryProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries, BUILDER, Set.of(FoodSpoiling.MODID));
    }

    public static final RegistrySetBuilder BUILDER = new RegistrySetBuilder()
            .add(FSRegistries.FOOD_QUALITY_KEY, FSFoodQualities::bootstrap)
            .add(FSRegistries.FOOD_STAGES_KEY, FSFoodStages::bootstrap);
}
