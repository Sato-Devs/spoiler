package com.thepigcat.foodspoiling.datagen;

import com.thepigcat.foodspoiling.FoodSpoiling;
import com.thepigcat.foodspoiling.registries.FoodQualities;
import net.minecraft.data.DataGenerator;
import net.minecraftforge.data.event.GatherDataEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = FoodSpoiling.MODID)
public final class Datagatherer {
    @SubscribeEvent
    public static void onGatherData(GatherDataEvent event) {
        DataGenerator generator = event.getGenerator();
        generator.addProvider(event.includeServer(), new DatapackRegistryProvider(generator.getPackOutput(), event.getLookupProvider()));
    }
}
