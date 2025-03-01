package com.thepigcat.foodspoiling.events;

import com.thepigcat.foodspoiling.FoodSpoiling;
import com.thepigcat.foodspoiling.FoodSpoilingConfig;
import com.thepigcat.foodspoiling.FSTags;
import com.thepigcat.foodspoiling.api.FoodQuality;
import com.thepigcat.foodspoiling.utils.FoodSpoilingUtils;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectSet;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.event.entity.player.PlayerContainerEvent;
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Mod.EventBusSubscriber(modid = FoodSpoiling.MODID)
public final class FSEvents {
    // Use ConcurrentHashMap for thread safety
    private static final Map<BlockPos, String> STORAGE_BLOCK_ENTITIES = new ConcurrentHashMap<>();
    private static final Map<BlockPos, String> CONTAINER_TYPES = new ConcurrentHashMap<>();
    private static int tickCounter = 0;

    // Use Capability Token for correct 1.20.1 Forge implementation
    private static final Capability<IItemHandler> ITEM_HANDLER_CAPABILITY = CapabilityManager.get(new CapabilityToken<>(){});

    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent event) {
        if (event.phase == TickEvent.Phase.END && !event.level.isClientSide()) {
            Level level = event.level;
            long gameTime = level.getGameTime();

            // Only process food spoilage periodically to save performance
            tickCounter++;
            if (tickCounter >= FoodSpoilingConfig.checkInterval) {
                tickCounter = 0;
                processFoodSpoilage(level, gameTime);
            }
        }
    }

    private static void processFoodSpoilage(Level level, long gameTime) {
        // Create a safe copy to iterate over
        Set<Map.Entry<BlockPos, String>> entitiesToProcess = Collections.newSetFromMap(new ConcurrentHashMap<>());
        entitiesToProcess.addAll(STORAGE_BLOCK_ENTITIES.entrySet());

        // Process block entities
        for (Map.Entry<BlockPos, String> entry : entitiesToProcess) {
            BlockPos pos = entry.getKey();
            if (level.isLoaded(pos)) {
                BlockEntity blockEntity = level.getBlockEntity(pos);
                if (blockEntity != null && !blockEntity.isRemoved()) {
                    String containerId = CONTAINER_TYPES.getOrDefault(pos, "default");
                    double spoilageModifier = FoodSpoilingUtils.getContainerSpoilageModifier(containerId);

                    // Skip if it's a freezer (modifier is 0)
                    if (spoilageModifier <= 0) continue;

                    processItemsInBlockEntity(blockEntity, gameTime, spoilageModifier);
                } else {
                    // Clean up if block entity no longer exists
                    STORAGE_BLOCK_ENTITIES.remove(pos);
                    CONTAINER_TYPES.remove(pos);
                }
            }
        }
    }

    private static void processItemsInBlockEntity(BlockEntity blockEntity, long gameTime, double spoilageModifier) {
        // Process items using capability
        LazyOptional<IItemHandler> cap = blockEntity.getCapability(ITEM_HANDLER_CAPABILITY);
        cap.ifPresent(handler -> {
            for (int i = 0; i < handler.getSlots(); i++) {
                ItemStack stack = handler.getStackInSlot(i);
                if (!stack.isEmpty() && stack.is(FSTags.FOOD)) {
                    // Initialize food state if not present
                    if (!FoodSpoilingUtils.hasFoodState(stack)) {
                        ItemStack extracted = handler.extractItem(i, stack.getCount(), true);
                        if (!extracted.isEmpty()) {
                            FoodSpoilingUtils.initializeFoodState(extracted, gameTime);
                            ItemStack simulated = handler.extractItem(i, extracted.getCount(), true);
                            if (!simulated.isEmpty() && ItemStack.isSameItemSameTags(simulated, stack)) {
                                ItemStack actualExtracted = handler.extractItem(i, extracted.getCount(), false);
                                handler.insertItem(i, extracted, false);
                            }
                        }
                    } else {
                        // Update existing food state - first simulate extraction
                        ItemStack extracted = handler.extractItem(i, stack.getCount(), true);
                        if (!extracted.isEmpty()) {
                            FoodSpoilingUtils.updateFoodState(extracted, gameTime, spoilageModifier);
                            // Only if simulation worked and tags changed, perform actual extraction/insertion
                            if (!ItemStack.isSameItemSameTags(extracted, stack)) {
                                ItemStack actualExtracted = handler.extractItem(i, stack.getCount(), false);
                                handler.insertItem(i, extracted, false);
                            }
                        }
                    }
                }
            }
        });
    }

    @SubscribeEvent
    public static void onChunkLoad(ChunkEvent.Load event) {
        if (event.getLevel().isClientSide()) return;

        // When a chunk loads, scan for block entities with item capabilities
        if (event.getChunk() instanceof LevelChunk levelChunk) {
            // Get all block entity positions in the chunk
            Set<BlockPos> blockEntityPositions = levelChunk.getBlockEntitiesPos();

            for (BlockPos pos : blockEntityPositions) {
                BlockEntity blockEntity = levelChunk.getBlockEntity(pos);
                if (blockEntity != null) {
                    // Check if this block entity has item capability
                    LazyOptional<IItemHandler> cap = blockEntity.getCapability(ITEM_HANDLER_CAPABILITY);
                    if (cap.isPresent()) {
                        ResourceLocation id = ForgeRegistries.BLOCK_ENTITY_TYPES.getKey(blockEntity.getType());
                        if (id != null) {
                            String containerId = id.toString();
                            CONTAINER_TYPES.put(pos, containerId);
                            STORAGE_BLOCK_ENTITIES.put(pos, containerId);
                        }
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public static void onChunkUnload(ChunkEvent.Unload event) {
        if (event.getLevel().isClientSide()) return;

        // When a chunk unloads, pause spoilage for items in the chunk
        if (event.getChunk() instanceof LevelChunk levelChunk) {
            Level level = (Level) event.getLevel();
            long gameTime = level.getGameTime();

            // Get all block entity positions in the chunk
            for (BlockPos pos : levelChunk.getBlockEntitiesPos()) {
                BlockEntity blockEntity = levelChunk.getBlockEntity(pos);
                if (blockEntity != null) {
                    LazyOptional<IItemHandler> cap = blockEntity.getCapability(ITEM_HANDLER_CAPABILITY);
                    cap.ifPresent(handler -> {
                        for (int i = 0; i < handler.getSlots(); i++) {
                            ItemStack stack = handler.getStackInSlot(i);
                            if (!stack.isEmpty() && stack.is(FSTags.FOOD) &&
                                    FoodSpoilingUtils.hasFoodState(stack)) {
                                // Don't modify the inventory directly here, just pause the spoilage
                                FoodSpoilingUtils.pauseSpoilage(stack, gameTime);
                            }
                        }
                    });
                }

                // Remove from tracking maps without modifying during iteration
                STORAGE_BLOCK_ENTITIES.remove(pos);
                CONTAINER_TYPES.remove(pos);
            }
        }
    }

    @SubscribeEvent
    public static void onContainerOpen(PlayerContainerEvent.Open event) {
        if (FoodSpoilingConfig.debugContainerClass) {
            FoodSpoiling.LOGGER.info("Container opened: {}", event.getContainer().getClass().getName());
        }

        // Resume spoilage for items that were paused
        Level level = event.getEntity().level();
        long gameTime = level.getGameTime();

        for (Slot slot : event.getContainer().slots) {
            ItemStack stack = slot.getItem();
            if (!stack.isEmpty() && stack.is(FSTags.FOOD)) {
                FoodSpoilingUtils.resumeSpoilage(stack, gameTime);
            }
        }
    }

    @SubscribeEvent
    public static void onItemTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        if (!stack.is(FSTags.FOOD) || !FoodSpoilingConfig.showFoodTooltip) return;

        FoodQuality state = FoodSpoilingUtils.getFoodState(stack);
        if (state != null) {
            // Format the tooltip based on state
            ChatFormatting color = getStateColor(state.name());
            MutableComponent stateText = Component.translatable("tooltip.foodspoiling.state",
                    Component.translatable("state.foodspoiling." + state.name())).withStyle(color);

            event.getToolTip().add(stateText);

            // Add remaining time if configured
            if (FoodSpoilingConfig.showRemainingDays || FoodSpoilingConfig.showRemainingPercentage) {
                CompoundTag nbt = stack.getTag();
                if (nbt != null && nbt.getString("FoodClass") != null && !nbt.getString("FoodClass").isEmpty()) {
                    String foodClass = nbt.getString("FoodClass");
                    if (FoodSpoilingConfig.stateProgressions.containsKey(foodClass)) {
                        addTimeTooltip(event, state, foodClass);
                    }
                }
            }
        }
    }

    private static void addTimeTooltip(ItemTooltipEvent event, FoodQuality state, String foodClass) {
        List<List<Object>> progression = FoodSpoilingConfig.stateProgressions.get(foodClass);
        int currentStateIndex = -1;

        // Find current state in progression
        for (int i = 0; i < progression.size(); i++) {
            if (progression.get(i).get(0).equals(state.name())) {
                currentStateIndex = i;
                break;
            }
        }

        if (currentStateIndex >= 0) {
            // Calculate days until next state
            double currentStateDuration = ((Number) progression.get(currentStateIndex).get(1)).doubleValue();
            int daysInState = state.day();
            double daysRemaining = currentStateDuration - (daysInState % currentStateDuration);

            // Add tooltip
            if (FoodSpoilingConfig.showRemainingDays) {
                String daysText = String.format("%.1f", daysRemaining);
                event.getToolTip().add(Component.translatable("tooltip.foodspoiling.days_remaining", daysText)
                        .withStyle(ChatFormatting.GRAY));
            }

            if (FoodSpoilingConfig.showRemainingPercentage) {
                int percentage = (int) ((1 - (daysInState % currentStateDuration) / currentStateDuration) * 100);
                percentage = Math.max(0, Math.min(100, percentage)); // Ensure it's between 0-100
                event.getToolTip().add(Component.translatable("tooltip.foodspoiling.freshness", percentage)
                        .withStyle(ChatFormatting.GRAY));
            }
        }
    }

    private static ChatFormatting getStateColor(String state) {
        switch (state) {
            case "fresh": return ChatFormatting.GREEN;
            case "aged": return ChatFormatting.GOLD;
            case "stale": return ChatFormatting.YELLOW;
            case "bruised": return ChatFormatting.YELLOW;
            case "mushy": return ChatFormatting.GOLD;
            case "moldy": return ChatFormatting.DARK_GREEN;
            case "spoiled": return ChatFormatting.RED;
            case "rancid": return ChatFormatting.DARK_RED;
            case "sour": return ChatFormatting.AQUA;
            case "weak": return ChatFormatting.GRAY;
            case "dead": return ChatFormatting.DARK_GRAY;
            case "flat": return ChatFormatting.GRAY;
            case "rotten": return ChatFormatting.DARK_PURPLE;
            default: return ChatFormatting.WHITE;
        }
    }

    /**
     * Registers a block entity position for food spoilage tracking
     * Called by the LevelChunkMixin
     */
    public static void registerBlockEntity(BlockPos pos, String containerId) {
        if (pos != null && containerId != null) {
            CONTAINER_TYPES.put(pos, containerId);
            STORAGE_BLOCK_ENTITIES.put(pos, containerId);
        }
    }

    /**
     * Unregisters a block entity position from food spoilage tracking
     * Called by the LevelChunkMixin
     */
    public static void unregisterBlockEntity(BlockPos pos) {
        if (pos != null) {
            STORAGE_BLOCK_ENTITIES.remove(pos);
            CONTAINER_TYPES.remove(pos);
        }
    }
}