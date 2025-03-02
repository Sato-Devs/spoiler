package com.thepigcat.foodspoiling.datagen;

import com.thepigcat.foodspoiling.FoodSpoiling;
import net.minecraft.data.DataGenerator;
import net.minecraftforge.data.event.GatherDataEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = FoodSpoiling.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class DataGatherer {
    @SubscribeEvent
    public static void onGatherData(GatherDataEvent event) {
        DataGenerator generator = event.getGenerator();
        generator.addProvider(event.includeServer(), new FSDatapackRegistryProvider(generator.getPackOutput(), event.getLookupProvider()));
        generator.addProvider(event.includeClient(), new FSEnUSLangProvider(generator.getPackOutput()));
        generator.addProvider(event.includeClient(), new FSItemModelProvider(generator.getPackOutput(), event.getExistingFileHelper()));
    }
}
