package com.thepigcat.foodspoiling.events;

import com.thepigcat.foodspoiling.FoodSpoiling;
import com.thepigcat.foodspoiling.FoodSpoilingConfig;
import com.thepigcat.foodspoiling.FSTags;
import com.thepigcat.foodspoiling.api.FoodStage;
import com.thepigcat.foodspoiling.api.FoodStages;
import com.thepigcat.foodspoiling.registries.FSItems;
import com.thepigcat.foodspoiling.utils.ClientUtils;
import com.thepigcat.foodspoiling.utils.NBTSpoilingUtils;
import com.thepigcat.foodspoiling.utils.SpoilingUtils;
import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import it.unimi.dsi.fastutil.objects.ObjectArraySet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.GlowItemFrame;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.player.ItemFishedEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.TradeWithVillagerEvent;
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.wrapper.InvWrapper;
import org.jetbrains.annotations.Nullable;

import java.util.*;

@Mod.EventBusSubscriber(modid = FoodSpoiling.MODID)
public final class FSEvents {
    // BlockEntities that we need to tick because they are loaded
    private static final Set<BlockPos> TICKING_BLOCK_ENTITIES = new ObjectArraySet<>();
    private static final List<Set<BlockPos>> NEW_BLOCK_ENTITIES = new ArrayList<>();
    private static final Set<UUID> TICKING_ENTITIES = new ObjectArraySet<>();

    @SubscribeEvent
    public static void onItemPickup(PlayerEvent.ItemPickupEvent event) {
        initFoodItem(event.getEntity(), event.getStack());
    }

    @SubscribeEvent
    public static void onCrafted(PlayerEvent.ItemCraftedEvent event) {
        ItemStack crafting = event.getCrafting();
        initFoodItem(event.getEntity(), crafting);
        NBTSpoilingUtils.setCreationTime(crafting, event.getEntity().level().dayTime());

    }

    @SubscribeEvent
    public static void onSmelted(PlayerEvent.ItemSmeltedEvent event) {
        initFoodItem(event.getEntity(), event.getSmelting());
    }

    @SubscribeEvent
    public static void onFished(ItemFishedEvent event) {
        for (ItemStack drop : event.getDrops()) {
            initFoodItem(event.getEntity(), drop);
        }
    }

    @SubscribeEvent
    public static void onTraded(TradeWithVillagerEvent event) {
        initFoodItem(event.getEntity(), event.getMerchantOffer().assemble());
    }

    private static void initFoodItem(Player player, ItemStack stack) {
        if (!FoodSpoilingConfig.spoilFoods) return;

        if (!NBTSpoilingUtils.hasFoodState(stack) && !stack.is(FSTags.UNSPOILABLE_FOODS) && stack.isEdible()) {
            Level level = player.level();
            SpoilingUtils.initialize(stack, level.dayTime(), 1f, level.registryAccess());

        }
    }

    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent event) {
        if (!FoodSpoilingConfig.spoilFoods) return;

        if (event.phase == TickEvent.Phase.END && !event.level.isClientSide()) {
            Level level = event.level;

            if (level.getGameTime() % FoodSpoilingConfig.checkInterval == 0) {
                processFoodSpoilage(level);

                for (Set<BlockPos> entry : NEW_BLOCK_ENTITIES) {
                    for (BlockPos pos : entry) {
                        BlockEntity blockEntity = level.getBlockEntity(pos);
                        LazyOptional<IItemHandler> _itemHandler = blockEntity.getCapability(ForgeCapabilities.ITEM_HANDLER);
                        if (_itemHandler.isPresent()) {
                            IItemHandler itemHandler = _itemHandler.orElseThrow(NullPointerException::new);
                            for (int i = 0; i < itemHandler.getSlots(); i++) {
                                ItemStack stack = itemHandler.getStackInSlot(i);
                                if (!stack.isEmpty()) {
                                    if ((stack.isEdible() && !stack.is(FSTags.UNSPOILABLE_FOODS)) || stack.is(FSItems.ROTTEN_MASS.get())) {
                                        long lastGameTime = NBTSpoilingUtils.getLastGameTime(stack);
                                        if (lastGameTime != -1) {
                                            NBTSpoilingUtils.setSpoilingProgress(stack, level.getGameTime() - lastGameTime);
                                            NBTSpoilingUtils.setLastGameTime(stack, -1);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                NEW_BLOCK_ENTITIES.clear();
            }
        }
    }

    private static void processFoodSpoilage(Level level) {
        long dayTime = level.dayTime();
        RegistryAccess lookup = level.registryAccess();
        for (BlockPos pos : TICKING_BLOCK_ENTITIES) {
            if (level.isLoaded(pos)) {
                BlockEntity blockEntity = level.getBlockEntity(pos);
                if (blockEntity != null && !blockEntity.isRemoved()) {
                    ResourceLocation containerId = getBlockEntityId(blockEntity);
                    float spoilingModifier = SpoilingUtils.getContainerSpoilageModifier(containerId);

                    LazyOptional<IItemHandler> capability = blockEntity.getCapability(ForgeCapabilities.ITEM_HANDLER);
                    if (capability.isPresent()) {
                        spoilItemsInHandler(capability.orElseThrow(NullPointerException::new), spoilingModifier * getTemperatureMod(blockEntity.getLevel(), blockEntity.getBlockPos()), dayTime, lookup);
                    }
                } else {
                    TICKING_BLOCK_ENTITIES.remove(pos);
                }
            }
        }

        for (UUID uuid : TICKING_ENTITIES) {
            if (level instanceof ServerLevel serverLevel) {
                Entity entity = serverLevel.getEntity(uuid);
                if (entity != null && !entity.isRemoved()) {
                    ResourceLocation containerId = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
                    float spoilingModifier = SpoilingUtils.getContainerSpoilageModifier(containerId);

                    LazyOptional<IItemHandler> capability = entity.getCapability(ForgeCapabilities.ITEM_HANDLER);
                    if (capability.isPresent()) {
                        spoilItemsInHandler(capability.orElseThrow(NullPointerException::new), spoilingModifier * getTemperatureMod(entity.level(), entity.getOnPos()), dayTime, lookup);
                    } else if (entity instanceof ItemFrame itemFrame) {
                        spoilItemFrame(itemFrame);
                    } else if (entity instanceof GlowItemFrame itemFrame) {
                        spoilItemFrame(itemFrame);
                    } else if (entity instanceof ItemEntity itemEntity) {
                        spoilItemEntity(itemEntity);
                    }
                }
            }
        }

        if (FoodSpoilingConfig.spoilEnderChest) {
            for (Player player : level.players()) {
                spoilItemsInHandler(new InvWrapper(player.getEnderChestInventory()), 1, dayTime, lookup);
            }
        }

    }

    private static @Nullable ResourceLocation getBlockEntityId(BlockEntity blockEntity) {
        return BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(blockEntity.getType());
    }

    public static void spoilItemFrame(ItemFrame entity) {
        // Process items using capability
        ItemStack stack = entity.getItem();
        HolderLookup.Provider lookup = entity.level().registryAccess();
        long dayTime = entity.level().dayTime();
        if (!stack.isEmpty()) {
            if (stack.isEdible() && !stack.is(FSTags.UNSPOILABLE_FOODS)) {
                if (NBTSpoilingUtils.hasFoodState(stack)) {
                    increaseProgress(getTemperatureMod(entity.level(), entity.getOnPos()), entity.level().dayTime(), stack);

                    FoodStages stages = SpoilingUtils.getStages(stack, lookup);
                    FoodStage stage = SpoilingUtils.getCurStage(stack, lookup);

                    if (stage != null && stages != null) {
                        FoodStage lastStage = stages.stages().get(stages.stages().size() - 1);
                        if (stage.days() == lastStage.days()) {
                            entity.setItem(SpoilingUtils.createRottenMass(stack));
                        }
                    }

                } else {
                    ItemStack extracted = stack.copy();
                    SpoilingUtils.initialize(extracted, dayTime, 1f, lookup);
                    entity.setItem(extracted);
                }
            } else if (stack.is(FSItems.ROTTEN_MASS.get())) {
                FoodStages stages = SpoilingUtils.getStages(stack, lookup);
                if (stages != null) {
                    if (SpoilingUtils.getFreshness(NBTSpoilingUtils.getSpoilingProgress(stack), NBTSpoilingUtils.getMaxSpoilingProgress(stack)) <= 0) {
                        ItemStack extracted = stack.copy();
                        entity.setItem(SpoilingUtils.createDecomposedGoo(extracted));
                    }
                }
            }
        }
    }

    public static void spoilItemEntity(ItemEntity entity) {
        // Process items using capability
        ItemStack stack = entity.getItem();
        HolderLookup.Provider lookup = entity.level().registryAccess();
        long dayTime = entity.level().dayTime();
        if (!stack.isEmpty()) {
            if (stack.isEdible() && !stack.is(FSTags.UNSPOILABLE_FOODS)) {
                if (NBTSpoilingUtils.hasFoodState(stack)) {
                    increaseProgress(getTemperatureMod(entity.level(), entity.getOnPos()), entity.level().dayTime(), stack);

                    FoodStages stages = SpoilingUtils.getStages(stack, lookup);
                    FoodStage stage = SpoilingUtils.getCurStage(stack, lookup);

                    if (stage != null && stages != null) {
                        FoodStage lastStage = stages.stages().get(stages.stages().size() - 1);
                        if (stage.days() == lastStage.days()) {
                            entity.setItem(SpoilingUtils.createRottenMass(stack));
                        }
                    }

                } else {
                    ItemStack extracted = stack.copy();
                    SpoilingUtils.initialize(extracted, dayTime, 1f, lookup);
                    entity.setItem(extracted);
                }
            } else if (stack.is(FSItems.ROTTEN_MASS.get())) {
                FoodStages stages = SpoilingUtils.getStages(stack, lookup);
                if (stages != null) {
                    if (SpoilingUtils.getFreshness(NBTSpoilingUtils.getSpoilingProgress(stack), NBTSpoilingUtils.getMaxSpoilingProgress(stack)) <= 0) {
                        ItemStack extracted = stack.copy();
                        entity.setItem(SpoilingUtils.createDecomposedGoo(extracted));
                    }
                }
            }
        }
    }

    private static void spoilItemsInHandler(IItemHandler itemHandler, float spoilingModifier, long dayTime, HolderLookup.Provider lookup) {
        for (int i = 0; i < itemHandler.getSlots(); i++) {
            ItemStack stack = itemHandler.getStackInSlot(i);
            if (!stack.isEmpty()) {
                if (stack.isEdible() && !stack.is(FSTags.UNSPOILABLE_FOODS)) {
                    trySetSpoilingModifier(stack, spoilingModifier);

                    if (NBTSpoilingUtils.hasFoodState(stack)) {
                        increaseProgress(spoilingModifier, dayTime, stack);

                        FoodStages stages = SpoilingUtils.getStages(stack, lookup);
                        FoodStage stage = SpoilingUtils.getCurStage(stack, lookup);

                        if (stage != null && stages != null) {
                            FoodStage lastStage = stages.stages().get(stages.stages().size() - 1);
                            if (stage.days() == lastStage.days()) {
                                ItemStack extracted = itemHandler.extractItem(i, stack.getCount(), false);
                                itemHandler.insertItem(i, SpoilingUtils.createRottenMass(extracted), false);
                            }
                        }

                    } else {
                        ItemStack extracted = itemHandler.extractItem(i, stack.getCount(), false);
                        SpoilingUtils.initialize(extracted, dayTime, spoilingModifier, lookup);

                        itemHandler.insertItem(i, extracted, false);
                    }
                } else if (stack.is(FSItems.ROTTEN_MASS.get())) {
                    trySetSpoilingModifier(stack, spoilingModifier);

                    increaseProgress(spoilingModifier, dayTime, stack);

                    FoodStages stages = SpoilingUtils.getStages(stack, lookup);
                    if (stages != null) {
                        if (SpoilingUtils.getFreshness(NBTSpoilingUtils.getSpoilingProgress(stack), NBTSpoilingUtils.getMaxSpoilingProgress(stack)) <= 0) {
                            ItemStack extracted = itemHandler.extractItem(i, stack.getCount(), false);
                            itemHandler.insertItem(i, SpoilingUtils.createDecomposedGoo(extracted), false);
                        }
                    }
                }
            }
        }
    }

    private static void increaseProgress(float spoilingModifier, long dayTime, ItemStack stack) {
        long lastDayTime = NBTSpoilingUtils.getLastGameTime(stack);
        float progress = NBTSpoilingUtils.getSpoilingProgress(stack);
        if (dayTime >= lastDayTime) {
            NBTSpoilingUtils.setSpoilingProgress(stack, progress + FoodSpoilingConfig.checkInterval * spoilingModifier);
        }
    }

    private static void trySetSpoilingModifier(ItemStack stack, float spoilingModifier) {
        if (NBTSpoilingUtils.getSpoilingModifier(stack) != spoilingModifier) {
            NBTSpoilingUtils.setSpoilingModifier(stack, spoilingModifier);
        }
    }

    @SubscribeEvent
    public static void onChunkLoad(ChunkEvent.Load event) {
        if (!FoodSpoilingConfig.spoilFoods) return;

        if (event.getLevel().isClientSide()) return;

        ChunkAccess chunk = event.getChunk();
        Set<BlockPos> blockEntityPositions = chunk.getBlockEntitiesPos();

        for (BlockPos pos : blockEntityPositions) {
            BlockEntity blockEntity = chunk.getBlockEntity(pos);

            if (blockEntity != null) {

                TICKING_BLOCK_ENTITIES.add(pos);
            }
        }
        NEW_BLOCK_ENTITIES.add(blockEntityPositions);
    }

    @SubscribeEvent
    public static void onEntityAdded(EntityJoinLevelEvent event) {
        if (!FoodSpoilingConfig.spoilFoods) return;

        Entity entity = event.getEntity();
        if (entity.getType().is(FSTags.ENTITIES_WITH_INVENTORY)) {
            TICKING_ENTITIES.add(entity.getUUID());
        }
    }

    @SubscribeEvent
    public static void onChunkUnload(ChunkEvent.Unload event) {
        if (!FoodSpoilingConfig.spoilFoods) return;

        if (event.getLevel().isClientSide()) return;

        for (BlockPos pos : event.getChunk().getBlockEntitiesPos()) {
            BlockEntity blockEntity = event.getLevel().getBlockEntity(pos);
            if (blockEntity != null) {
                LazyOptional<IItemHandler> _itemHandler = blockEntity.getCapability(ForgeCapabilities.ITEM_HANDLER);
                if (_itemHandler.isPresent()) {
                    IItemHandler iItemHandler = _itemHandler.orElseThrow(NullPointerException::new);
                    for (int i = 0; i < iItemHandler.getSlots(); i++) {
                        ItemStack stack = iItemHandler.getStackInSlot(0);
                        if (!stack.isEmpty()) {
                            if ((stack.isEdible() && !stack.is(FSTags.UNSPOILABLE_FOODS)) || stack.is(FSItems.ROTTEN_MASS.get())) {
                                NBTSpoilingUtils.setLastGameTime(stack, blockEntity.getLevel().getGameTime());
                            }
                        }
                    }
                }
            }

            TICKING_BLOCK_ENTITIES.remove(pos);
        }
    }

    @SubscribeEvent
    public static void onItemTooltip(ItemTooltipEvent event) {
        if (!FoodSpoilingConfig.spoilFoods) return;

        ItemStack stack = event.getItemStack();
        if (stack.is(FSTags.UNSPOILABLE_FOODS) || !FoodSpoilingConfig.showFoodTooltip) return;

        Player entity = event.getEntity();
        if (entity != null) {
            if (((stack.isEdible() && !stack.is(FSTags.UNSPOILABLE_FOODS)) || stack.is(FSItems.ROTTEN_MASS.get())) && NBTSpoilingUtils.hasFoodState(stack) && entity.level().isClientSide) {
                event.getToolTip().addAll(SpoilingUtils.getSpoilingTooltip(stack, entity, ClientUtils.hasShiftDown()));
            }
        }

    }

    private static float getTemperatureMod(Level level, BlockPos pos) {
        if (SpoilingUtils.hasColdSweat()) {
            float temperature = SpoilingUtils.getTemperature(level, pos);
            return temperature / FoodSpoilingConfig.neutralTemperature;
        }
        return 1;
    }

    public static void registerBlockEntity(BlockPos pos) {
        if (!FoodSpoilingConfig.spoilFoods) return;

        if (pos != null) {
            TICKING_BLOCK_ENTITIES.add(pos);
        }
    }

    public static void unregisterBlockEntity(BlockPos pos) {
        if (!FoodSpoilingConfig.spoilFoods) return;

        if (pos != null) {
            TICKING_BLOCK_ENTITIES.remove(pos);
        }
    }

    public static void unregisterEntity(UUID uuid) {
        if (!FoodSpoilingConfig.spoilFoods) return;

        if (uuid != null) {
            TICKING_ENTITIES.remove(uuid);
        }
    }

}