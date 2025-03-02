package com.thepigcat.foodspoiling.datagen;

import com.thepigcat.foodspoiling.FSRegistries;
import com.thepigcat.foodspoiling.FoodSpoiling;
import com.thepigcat.foodspoiling.registries.FSFoodStages;
import com.thepigcat.foodspoiling.registries.FSFoodQualities;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistrySetBuilder;
import net.minecraft.data.PackOutput;
import net.minecraftforge.common.data.DatapackBuiltinEntriesProvider;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

public class DatapackRegistryProvider extends DatapackBuiltinEntriesProvider {
    public DatapackRegistryProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries, BUILDER, Set.of(FoodSpoiling.MODID));
    }

    public static final RegistrySetBuilder BUILDER = new RegistrySetBuilder()
            .add(FSRegistries.FOOD_QUALITY_KEY, FSFoodQualities::bootstrap)
            .add(FSRegistries.FOOD_STAGES_KEY, FSFoodStages::bootstrap);
}
