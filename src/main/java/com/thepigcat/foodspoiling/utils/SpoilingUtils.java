package com.thepigcat.foodspoiling.utils;

import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Pair;
import com.thepigcat.foodspoiling.FSRegistries;
import com.thepigcat.foodspoiling.FoodSpoiling;
import com.thepigcat.foodspoiling.FoodSpoilingConfig;
import com.thepigcat.foodspoiling.api.FoodQuality;
import com.thepigcat.foodspoiling.api.FoodStage;
import com.thepigcat.foodspoiling.api.FoodStages;
import com.thepigcat.foodspoiling.registries.FSItems;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class SpoilingUtils {
    public static final String FOOD_STATE_KEY = "food_state";
    public static final String CREATION_TIME_KEY = "creation_time";
    public static final String FOOD_STAGES_KEY = "food_stages";
    public static final String SPOILING_MODIFIER_KEY = "spoiling_modifier";

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
        return stack.hasTag()
                && stack.getTag().contains(FOOD_STATE_KEY)
                && stack.getTag().getCompound(FOOD_STATE_KEY).contains(FOOD_STAGES_KEY)
                && stack.getTag().getCompound(FOOD_STATE_KEY).contains(CREATION_TIME_KEY);
    }

    public static FoodStage getCurStage(ItemStack stack, long dayTime, HolderLookup.Provider lookup) {
        FoodStages stages = getStages(stack, lookup);
        long creationTime = getCreationTime(stack);
        int creationDay = Math.round((creationTime - creationTime % 24000) / 24000f);
        int curDay = (int) (dayTime / 24000 - creationDay);
        for (FoodStage stage : stages.stages()) {
            if (curDay < stage.days()) {
                return stage;
            }
        }
        return null;
    }

    public static FoodStages getStages(ItemStack itemStack, HolderLookup.Provider lookup) {
        if (hasFoodData(itemStack)) {
            ResourceKey<FoodStages> foodStages = getFoodStages(itemStack);
            if (foodStages != null) {
                return lookup.lookupOrThrow(FSRegistries.FOOD_STAGES_KEY).getOrThrow(foodStages).value();
            }
        }
        return null;
    }

    public static ItemStack createRottenMass(ItemStack foodStack) {
        ItemStack stack = new ItemStack(FSItems.ROTTEN_MASS.get(), foodStack.getCount());
        CompoundTag tag = stack.getOrCreateTag();
        tag.put(FOOD_STATE_KEY, foodStack.getOrCreateTag().get(FOOD_STATE_KEY));
        return stack;
    }

    public static ItemStack createDecomposedGoo(ItemStack rottenMassStack) {
        return new ItemStack(FSItems.DECOMPOSED_GOO.get(), rottenMassStack.getCount());
    }

    public static List<Component> getSpoilingTooltip(ItemStack stack, Player player, boolean isShiftDown) {
        Level level = player.level();
        long creationTime = getCreationTime(stack);
        int creationDay = Math.round((creationTime - creationTime % 24000) / 24000f);
        //long diff = level.dayTime() - creationTime;
        long dayTime = level.dayTime();
        int curDay = (int) (dayTime / 24000 - creationDay);
        RegistryAccess access = level.registryAccess();
        ResourceKey<FoodStages> foodStagesType = getFoodStages(stack);
        Holder.Reference<FoodStages> foodStages = access.lookupOrThrow(FSRegistries.FOOD_STAGES_KEY).getOrThrow(foodStagesType);
        List<FoodStage> stages = foodStages.value().stages();
        if (!stages.isEmpty()) {
            ResourceKey<FoodQuality> qualityKey = null;
            FoodStage curStage = null;
            FoodStage lastStage = null;
            FoodStage nextStage = null;
            boolean foundDays = false;
            int daysToNext = 0;
            for (int i = 0; i < stages.size(); i++) {
                FoodStage stage = stages.get(i);

                if (curDay < stage.days() && !foundDays) {
                    curStage = stage;
                    qualityKey = stage.quality();
                    if (i + 1 < stages.size()) {
                        nextStage = stages.get(i + 1);
                        daysToNext = stage.days();
                    }
                    foundDays = true;
                }

                if (i == stages.size() - 1) {
                    lastStage = stage;
                }
            }
            int totalDays = lastStage.days();
            int expirationDay = creationDay + totalDays;
            long expirationDate = (creationTime + (totalDays * 24000L));
            String expirationDateText = String.format("Day %d - %s", expirationDay, timeToHoursMinutes(level, expirationDate));
            ResourceKey<FoodQuality> key = qualityKey != null ? qualityKey : stages.get(stages.size() - 1).quality();
            Holder.Reference<FoodQuality> quality = access.lookupOrThrow(FSRegistries.FOOD_QUALITY_KEY).getOrThrow(key);
            float freshness = getFreshness(dayTime, creationTime, totalDays);
            List<Component> tooltip = new ArrayList<>(List.of(
                    Component.literal("Quality: ").withStyle(ChatFormatting.GRAY).append(registryTranslation(key).copy().withStyle(Style.EMPTY.withColor(quality.value().textColor()))),
                    Component.literal("Freshness: ").withStyle(ChatFormatting.GRAY).append(Math.round(freshness * 100) + "%"),
                    Component.literal("Expiration Day: ").withStyle(ChatFormatting.GRAY).append(Component.literal(expirationDateText).withStyle(ChatFormatting.YELLOW))
            ));
            if (!isShiftDown) {
                tooltip.add(Component.literal("Hold <Shift> for more information").withStyle(ChatFormatting.GRAY));
            } else if (curStage != null) {
                tooltip.addAll(createAdvancedTooltip(stack, player, curStage, nextStage, creationTime, daysToNext, access));
            }
            return tooltip;
        }
        return Collections.emptyList();
    }

    public static float getFreshness(long dayTime, long creationTime, int totalDays) {
        return 1f - (float) (dayTime - creationTime) / (totalDays * 24000);
    }

    private static List<Component> createAdvancedTooltip(ItemStack stack, Player player, FoodStage curStage, FoodStage nextStage, long creationTime, int daysToNext, RegistryAccess access) {
        Level level = player.level();
        Holder.Reference<FoodQuality> quality = access.lookupOrThrow(FSRegistries.FOOD_QUALITY_KEY).getOrThrow(curStage.quality());
        List<Component> advTooltip = new ArrayList<>();
        int timeToNext = ((curStage.days() - daysToNext) * 24000) - (int) (level.dayTime() - (creationTime + (daysToNext * 24000)));
        FoodSpoiling.LOGGER.debug("Time to next: {}", timeToNext);
        int days = timeToNext / 24000;
        MutableComponent nextStageComponent = Component.empty();
        if (nextStage != null) {
            Holder.Reference<FoodQuality> nextQuality = access.lookupOrThrow(FSRegistries.FOOD_QUALITY_KEY).getOrThrow(nextStage.quality());
            nextStageComponent.append(Component.literal("Next Stage ")
                    .append(Component.literal("%s".formatted(registryTranslation(nextQuality.key()).getString())).withStyle(Style.EMPTY.withColor(nextQuality.value().textColor()))));
        } else {
            nextStageComponent.append(Component.literal("Decayed").withStyle(ChatFormatting.DARK_RED));
        }
        nextStageComponent.append(Component.literal(days > 0 ? " in " + days + " Days" : " in < 1 Day")).withStyle(ChatFormatting.GRAY);
        advTooltip.add(nextStageComponent);
        FoodProperties foodProperties = stack.getFoodProperties(player);
        if (foodProperties != null) {
            advTooltip.add(Component.literal("Saturation Modifier: " + foodProperties.getNutrition() * quality.value().nutritionMod()).withStyle(ChatFormatting.GRAY));
            advTooltip.add(Component.literal("Nutrition Modifier: " + foodProperties.getSaturationModifier() * quality.value().saturationMod()).withStyle(ChatFormatting.GRAY));
            advTooltip.add(Component.literal("Effects:").withStyle(ChatFormatting.GRAY));
            advTooltip.addAll(formatEffects(quality.get().effects()));
        }
        return advTooltip;
    }

    private static List<Component> formatEffects(List<Pair<Either<MobEffectInstance, Potion>, Float>> effects) {
        List<Component> tooltip = new ArrayList<>();
        for (Pair<Either<MobEffectInstance, Potion>, Float> effect : effects) {
            Either<MobEffectInstance, Potion> effectFirst = effect.getFirst();
            if (effectFirst.left().isPresent()) {
                tooltip.add(Component.translatable(effectFirst.left().get().getDescriptionId()).withStyle(ChatFormatting.GRAY));
            } else if (effectFirst.right().isPresent()) {
                ResourceLocation key = BuiltInRegistries.POTION.getKey(effectFirst.right().get());
                tooltip.add(Component.translatable("item." + key.getNamespace() + ".potion.effect." + key.getPath()));
            }
        }
        return tooltip;
    }

    private static String timeToHoursMinutes(Level world, long expirationDate) {
        int time = ((int) (expirationDate) % 24000);
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

    public static <T> Component registryTranslation(Registry<T> registry, T registryObject) {
        ResourceLocation objLoc = registry.getKey(registryObject);
        return Component.translatable(registry.key().location().getPath() + "." + objLoc.getNamespace() + "." + objLoc.getPath());
    }

    public static double getContainerSpoilageModifier(ResourceLocation blockentityId) {
        if (FoodSpoilingConfig.containerModifiers.containsKey(blockentityId)) {
            return FoodSpoilingConfig.containerModifiers.get(blockentityId);
        }

        return 1.0; // Default: normal spoilage rate
    }

}
