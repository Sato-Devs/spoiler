package com.thepigcat.foodspoiling.events;

import com.thepigcat.foodspoiling.FoodSpoiling;
import com.thepigcat.foodspoiling.api.FoodQuality;
import com.thepigcat.foodspoiling.FSTags;
import com.thepigcat.foodspoiling.utils.FoodSpoilingUtils;
import it.unimi.dsi.fastutil.objects.ObjectArraySet;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;
import java.util.Set;

@Mod.EventBusSubscriber(modid = FoodSpoiling.MODID)
public final class FSEvents {
    public static final Set<BlockPos> STORAGE_BLOCK_ENTITIES = new ObjectArraySet<>();
    public static final List<String> STATES = List.of(
            "fresh",
            "stale",
            "weak",
            "dead",
            "rotten"
    );

    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent event) {
        if (event.phase == TickEvent.Phase.END && !event.level.isClientSide) {
            Level level = event.level;
            long gameTime = level.getGameTime();

            // Add apples every second
            if (gameTime % 20 == 0) {
                for (BlockPos pos : STORAGE_BLOCK_ENTITIES) {
                    if (level.isLoaded(pos)) {
                        BlockEntity blockEntity = level.getBlockEntity(pos);
                        if (blockEntity != null && !blockEntity.isRemoved()) {
                            tryAddApple(blockEntity);
                        }
                    }
                }
            }
        }
    }

    private static void tryAddApple(BlockEntity blockEntity) {
        // Try capability approach first
        blockEntity.getCapability(ForgeCapabilities.ITEM_HANDLER).ifPresent(handler -> {
            for (int i = 0; i < handler.getSlots(); i++) {
                ItemStack stack = handler.getStackInSlot(i);
                if (!stack.isEmpty() && stack.is(FSTags.FOOD)) {
                    FoodQuality state = FoodSpoilingUtils.getFoodState(stack);
                    FoodSpoiling.LOGGER.debug("State: {}", state);
                    ItemStack extracted = handler.extractItem(i, stack.getCount(), false);
                    if (state != null) {
                        int a = STATES.indexOf(state.name()) + 1;
                        FoodSpoilingUtils.setFoodState(extracted, new FoodQuality(STATES.get(a < STATES.size() ? a : 0), 0));
                    } else {
                        FoodSpoilingUtils.setFoodState(extracted, new FoodQuality(STATES.get(0), 0));
                    }
                    handler.insertItem(i, extracted, false);
                }
            }
        });

    }

    @SubscribeEvent
    public static void onBuildTooltip(ItemTooltipEvent event) {
        event.getToolTip().add(Component.literal("State: " + FoodSpoilingUtils.getFoodState(event.getItemStack())));
    }
}
