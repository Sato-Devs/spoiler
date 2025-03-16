package com.thepigcat.spoiling.events;

import com.thepigcat.spoiling.FSRegistries;
import com.thepigcat.spoiling.FoodSpoiling;
import com.thepigcat.spoiling.FoodSpoilingConfig;
import com.thepigcat.spoiling.api.FoodStage;
import com.thepigcat.spoiling.utils.NBTSpoilingUtils;
import com.thepigcat.spoiling.utils.SpoilingUtils;
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
                    if (NBTSpoilingUtils.hasFoodState(stack)) {
                        if (FoodSpoilingConfig.renderFrozenOverlay) {
                            float spoilingModifier = NBTSpoilingUtils.getSpoilingModifier(stack);
                            if (spoilingModifier == 0) {
                                return FoodSpoilingConfig.frozenTintColor;
                            }
                        }

                        if (FoodSpoilingConfig.renderSpoiledOverlay) {
                            Level level = Minecraft.getInstance().level;
                            RegistryAccess lookup = level.registryAccess();
                            FoodStage curStage = SpoilingUtils.getCurStage(stack, lookup);
                            if (curStage != null) {
                                return lookup.lookupOrThrow(FSRegistries.FOOD_QUALITY_KEY).getOrThrow(curStage.quality()).value().tintColor().toARGB();
                            }
                        }
                    }
                    return -1;
                }, item);
            }
        }
    }
}
