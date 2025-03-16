package com.thepigcat.spoiling;

import com.mojang.logging.LogUtils;
import com.thepigcat.spoiling.api.FoodQuality;
import com.thepigcat.spoiling.api.FoodStages;
import com.thepigcat.spoiling.registries.FSItems;
import com.thepigcat.spoiling.registries.FSRecipes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DataPackRegistryEvent;
import org.slf4j.Logger;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(FoodSpoiling.MODID)
public final class FoodSpoiling {
    public static final String MODID = "spoiling";
    public static final Logger LOGGER = LogUtils.getLogger();

    public FoodSpoiling() {
        IEventBus modEventbus = FMLJavaModLoadingContext.get().getModEventBus();
        modEventbus.addListener(this::registerDatapackRegistry);
        modEventbus.addListener(this::addItemsToCreativeTab);

        FSItems.ITEMS.register(modEventbus);
        FSRecipes.RECIPES.register(modEventbus);

        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, FoodSpoilingConfig.SPEC);
    }

    private void registerDatapackRegistry(DataPackRegistryEvent.NewRegistry event) {
        event.dataPackRegistry(FSRegistries.FOOD_QUALITY_KEY, FoodQuality.CODEC, FoodQuality.CODEC);
        event.dataPackRegistry(FSRegistries.FOOD_STAGES_KEY, FoodStages.CODEC, FoodStages.CODEC);
    }

    private void addItemsToCreativeTab(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.INGREDIENTS) {
            event.accept(FSItems.DECOMPOSED_GOO);
            event.accept(FSItems.ROTTEN_MASS);
        }
    }

    public static ResourceLocation rl(String path) {
        return new ResourceLocation(MODID, path);
    }

}
