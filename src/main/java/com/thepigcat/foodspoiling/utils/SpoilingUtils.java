package com.thepigcat.foodspoiling.utils;

import com.thepigcat.foodspoiling.FSRegistries;
import com.thepigcat.foodspoiling.FoodSpoiling;
import com.thepigcat.foodspoiling.FoodSpoilingConfig;
import com.thepigcat.foodspoiling.api.FoodQuality;
import com.thepigcat.foodspoiling.api.FoodStage;
import com.thepigcat.foodspoiling.api.FoodStages;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

public final class SpoilingUtils {
    public static final String FOOD_STATE_KEY = "food_state";
    public static final String CREATION_TIME_KEY = "creation_time";
    public static final String FOOD_STAGES_KEY = "food_stages";

    public static void initialize(ItemStack stack, long dayTime, HolderLookup.Provider lookup) {
        List<Holder.Reference<FoodStages>> foodStageElements = lookup.lookupOrThrow(FSRegistries.FOOD_STAGES_KEY).listElements().toList();

        CompoundTag itemTag = stack.getOrCreateTag();
        CompoundTag foodStateTag = itemTag.getCompound(FOOD_STATE_KEY);
        for (Holder.Reference<FoodStages> stages : foodStageElements) {
            TagKey<Item> key = stages.value().key();
            if (stack.is(key)) {
                foodStateTag.putLong(CREATION_TIME_KEY, dayTime);
                foodStateTag.putString(FOOD_STAGES_KEY, stages.key().location().toString());
                break;
            }
        }

        itemTag.put(FOOD_STATE_KEY, foodStateTag);

    }

    public static long getCreationTime(ItemStack stack) {
        return stack.hasTag() ? stack.getTag().getCompound(FOOD_STATE_KEY).getLong(CREATION_TIME_KEY) : 0;
    }

    public static @Nullable ResourceKey<FoodStages> getFoodStages(ItemStack stack) {
        if (stack.hasTag()) {
            String type = stack.getTag().getCompound(FOOD_STATE_KEY).getString(FOOD_STAGES_KEY);
            return ResourceKey.create(FSRegistries.FOOD_STAGES_KEY, new ResourceLocation(type));
        }
        return null;
    }

    public static boolean hasFoodData(ItemStack stack) {
        return stack.hasTag() && stack.getTag().contains(FOOD_STATE_KEY);
    }

    public static List<Component> getSpoilingTooltip(ItemStack stack, Level level) {
        long creationTime = getCreationTime(stack);
        int creationDay = Math.round((creationTime - creationTime % 24000) / 24000f);
        //long diff = level.dayTime() - creationTime;
        int curDay = (int) (level.dayTime() / 24000 - creationDay);
        RegistryAccess access = level.registryAccess();
        ResourceKey<FoodStages> foodStagesType = getFoodStages(stack);
        Holder.Reference<FoodStages> foodStages = access.lookupOrThrow(FSRegistries.FOOD_STAGES_KEY).getOrThrow(foodStagesType);
        List<FoodStage> stages = foodStages.value().stages();
        if (!stages.isEmpty()) {
            ResourceKey<FoodQuality> qualityKey = null;
            int totalDays = 0;
            boolean foundDays = false;
            for (FoodStage stage : stages) {
                totalDays += stage.days();

                if (curDay < stage.days() && !foundDays) {
                    qualityKey = stage.quality();
                    foundDays = true;
                }
            }
            int expirationDay = creationDay + totalDays;
            long expirationDate = creationTime + (totalDays * 24000L);
            String expirationDateText = String.format("Day %d - %s", expirationDay, getTime(level, expirationDate));
            ResourceKey<FoodQuality> key = qualityKey != null ? qualityKey : stages.get(stages.size() - 1).quality();
            Holder.Reference<FoodQuality> quality = access.lookupOrThrow(FSRegistries.FOOD_QUALITY_KEY).getOrThrow(key);
            float freshness = 1f - (float) (level.dayTime() - creationTime) / expirationDate;
            return List.of(
                    Component.literal("Quality: ").withStyle(ChatFormatting.GRAY).append(registryTranslation(key).copy().withStyle(Style.EMPTY.withColor(quality.value().color()))),
                    Component.literal("Freshness: ").withStyle(ChatFormatting.GRAY).append(Math.round(freshness * 100) + "%"),
                    Component.literal("Expiration Day: ").withStyle(ChatFormatting.GRAY).append(Component.literal(expirationDateText).withStyle(ChatFormatting.YELLOW))
            );
        }
        return Collections.emptyList();
    }

    private static String getTime(Level world, long expirationDate) {
        int time = ((int) (expirationDate + 6000) % 24000);
        int m = (int) (((time % 1000f) / 1000f) * 60);
        int h = time / 1000;

        String ob = "";
        String br = "";
        if (!world.dimensionType().natural()) {
            ob += ChatFormatting.OBFUSCATED;
            br += ChatFormatting.RESET;
        }
        return ob + h + br + ":" + ob + ((m < 10) ? "0" : "") + m + br;
    }

    public static <T> Component registryTranslation(ResourceKey<T> key) {
        return Component.translatable(key.registry().getPath() + "." + key.location().getNamespace() + "." + key.location().getPath());
    }

    public static double getContainerSpoilageModifier(ResourceLocation containerId) {
        if (FoodSpoilingConfig.freezingContainers.contains(containerId)) {
            return 0.0; // No spoilage in freezing containers
        }

        if (FoodSpoilingConfig.containerModifiers.containsKey(containerId)) {
            return FoodSpoilingConfig.containerModifiers.get(containerId);
        }

        return 1.0; // Default: normal spoilage rate
    }

}
