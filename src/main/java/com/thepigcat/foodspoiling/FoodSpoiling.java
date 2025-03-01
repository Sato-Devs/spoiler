package com.thepigcat.foodspoiling;

import com.mojang.logging.LogUtils;
import com.thepigcat.foodspoiling.api.FoodQuality;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DataPackRegistryEvent;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Mod(FoodSpoiling.MODID)
public final class FoodSpoiling {
    public static final Map<Item, List<ItemStack>> FOOD_ITEMS = new HashMap<>();
    public static final String MODID = "foodspoiling";
    public static final Logger LOGGER = LogUtils.getLogger();

    public FoodSpoiling() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();;
        modEventBus.addListener(this::registerDatapackRegistry);
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, FoodSpoilingConfig.SPEC);
    }

    private void registerDatapackRegistry(DataPackRegistryEvent.NewRegistry event) {
        event.dataPackRegistry(FSRegistries.FOOD_QUALITY_KEY, FoodQuality.CODEC, FoodQuality.CODEC);
    }

    public static ResourceLocation rl(String path) {
        return new ResourceLocation(MODID, path);
    }

}
