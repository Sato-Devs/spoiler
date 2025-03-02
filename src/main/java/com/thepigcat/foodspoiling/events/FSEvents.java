package com.thepigcat.foodspoiling.events;

import com.thepigcat.foodspoiling.FoodSpoiling;
import com.thepigcat.foodspoiling.FoodSpoilingConfig;
import com.thepigcat.foodspoiling.FSTags;
import com.thepigcat.foodspoiling.utils.SpoilingUtils;
import it.unimi.dsi.fastutil.objects.ObjectArraySet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.items.IItemHandler;

import java.util.Set;

@Mod.EventBusSubscriber(modid = FoodSpoiling.MODID)
public final class FSEvents {
    // BlockEntities that we need to tick because they are loaded
    private static final Set<BlockPos> TICKING_BLOCK_ENTITIES = new ObjectArraySet<>();
    private static int tickCounter = 0;

    // Use Capability Token for correct 1.20.1 Forge implementation
    private static final Capability<IItemHandler> ITEM_HANDLER_CAPABILITY = CapabilityManager.get(new CapabilityToken<>() {
    });

    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent event) {
        if (event.phase == TickEvent.Phase.END && !event.level.isClientSide()) {
            Level level = event.level;

            if (level.getGameTime() % FoodSpoilingConfig.checkInterval == 0) {
                processFoodSpoilage(level);
            }
        }
    }

    private static void processFoodSpoilage(Level level) {
        for (BlockPos pos : TICKING_BLOCK_ENTITIES) {
            if (level.isLoaded(pos)) {
                BlockEntity blockEntity = level.getBlockEntity(pos);
                if (blockEntity != null && !blockEntity.isRemoved()) {
                    ResourceLocation containerId = BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(blockEntity.getType());
                    double spoilageModifier = SpoilingUtils.getContainerSpoilageModifier(containerId);

                    // Skip if it's a freezer (modifier is 0)
                    if (spoilageModifier <= 0) continue;

                    LazyOptional<IItemHandler> capability = blockEntity.getCapability(ForgeCapabilities.ITEM_HANDLER);
                    if (capability.isPresent()) {
                        spoilItemsInHandler(capability.orElseThrow(NullPointerException::new), spoilageModifier, level.dayTime(), level.registryAccess());
                    }
                } else {
                    TICKING_BLOCK_ENTITIES.remove(pos);
                }
            }
        }
    }

    private static void spoilItemsInHandler(IItemHandler itemHandler, double spoilageModifier, long dayTime, HolderLookup.Provider lookup) {
        // Process items using capability
        for (int i = 0; i < itemHandler.getSlots(); i++) {
            ItemStack stack = itemHandler.getStackInSlot(i);
            if (!stack.isEmpty() && stack.is(FSTags.SPOILABLE_FOODS)) {
                if (SpoilingUtils.hasFoodData(stack)) {
                    // TODO: Do shit regarding actual state
                } else {
                    ItemStack extracted = itemHandler.extractItem(i, stack.getCount(), false);
                    SpoilingUtils.initialize(extracted, dayTime, lookup);
                    itemHandler.insertItem(i, extracted, false);
                }
            }
        }
    }

    @SubscribeEvent
    public static void onChunkLoad(ChunkEvent.Load event) {
        if (event.getLevel().isClientSide()) return;

        ChunkAccess chunk = event.getChunk();
        Set<BlockPos> blockEntityPositions = chunk.getBlockEntitiesPos();

        for (BlockPos pos : blockEntityPositions) {
            BlockEntity blockEntity = chunk.getBlockEntity(pos);
            if (blockEntity != null) {
                TICKING_BLOCK_ENTITIES.add(pos);
            }
        }
    }

    @SubscribeEvent
    public static void onChunkUnload(ChunkEvent.Unload event) {
        if (event.getLevel().isClientSide()) return;

        for (BlockPos pos : event.getChunk().getBlockEntitiesPos()) {
            TICKING_BLOCK_ENTITIES.remove(pos);
        }
    }

//    @SubscribeEvent
//    public static void onContainerOpen(PlayerContainerEvent.Open event) {
//        if (FoodSpoilingConfig.debugContainerClass) {
//            FoodSpoiling.LOGGER.info("Container opened: {}", event.getContainer().getClass().getName());
//        }
//
//        // Resume spoilage for items that were paused
//        Level level = event.getEntity().level();
//        long gameTime = level.getGameTime();
//
//        for (Slot slot : event.getContainer().slots) {
//            ItemStack stack = slot.getItem();
//            if (!stack.isEmpty() && stack.is(FSTags.SPOILABLE_FOODS)) {
//                FoodSpoilingUtils.resumeSpoilage(stack, gameTime);
//            }
//        }
//    }

    @SubscribeEvent
    public static void onItemTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        if (!stack.is(FSTags.SPOILABLE_FOODS) || !FoodSpoilingConfig.showFoodTooltip) return;

        if (SpoilingUtils.hasFoodData(stack)) {
            event.getToolTip().addAll(SpoilingUtils.getSpoilingTooltip(stack, event.getEntity().level()));
        }

    }

    /**
     * Registers a block entity position for food spoilage tracking
     * Called by the LevelChunkMixin
     */
    public static void registerBlockEntity(BlockPos pos) {
        if (pos != null) {
            TICKING_BLOCK_ENTITIES.add(pos);
        }
    }

    /**
     * Unregisters a block entity position from food spoilage tracking
     * Called by the LevelChunkMixin
     */
    public static void unregisterBlockEntity(BlockPos pos) {
        if (pos != null) {
            TICKING_BLOCK_ENTITIES.remove(pos);
        }
    }

}