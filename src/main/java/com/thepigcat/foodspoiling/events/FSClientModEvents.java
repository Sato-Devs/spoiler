package com.thepigcat.foodspoiling.events;

import com.thepigcat.foodspoiling.FSRegistries;
import com.thepigcat.foodspoiling.FoodSpoiling;
import com.thepigcat.foodspoiling.FoodSpoilingConfig;
import com.thepigcat.foodspoiling.api.FoodStage;
import com.thepigcat.foodspoiling.utils.NBTSpoilingUtils;
import com.thepigcat.foodspoiling.utils.SpoilingUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterColorHandlersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = FoodSpoiling.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class FSClientModEvents {
    @SubscribeEvent
    public static void onTintItem(RegisterColorHandlersEvent.Item event) {
        for (Item item : BuiltInRegistries.ITEM) {
            if (item.isEdible()) {
                event.register((stack, layer) -> {
                    if (NBTSpoilingUtils.hasFoodState(stack) && FoodSpoilingConfig.renderSpoiledOverlay) {
                        Level level = Minecraft.getInstance().level;
                        RegistryAccess lookup = level.registryAccess();
                        FoodStage curStage = SpoilingUtils.getCurStage(stack, level.dayTime(), lookup);
                        if (curStage != null) {
                            return lookup.lookupOrThrow(FSRegistries.FOOD_QUALITY_KEY).getOrThrow(curStage.quality()).value().tintColor();
                        }
                    }
                    return -1;
                }, item);
            }
        }
    }
}
