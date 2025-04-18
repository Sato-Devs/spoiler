package com.thepigcat.spoiler.utils;

import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Pair;
import com.thepigcat.spoiler.FSRegistries;
import com.thepigcat.spoiler.FoodSpoilingConfig;
import com.thepigcat.spoiler.api.FoodQuality;
import com.thepigcat.spoiler.api.FoodStage;
import com.thepigcat.spoiler.api.FoodStages;
import com.thepigcat.spoiler.compat.ColdSweatCompat;
import com.thepigcat.spoiler.compat.LSOCompat;
import com.thepigcat.spoiler.registries.FSFoodStages;
import com.thepigcat.spoiler.registries.FSItems;
import net.minecraft.ChatFormatting;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
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
import net.minecraftforge.fml.ModList;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public final class SpoilingUtils {
    public static void initialize(ItemStack stack, long dayTime, float spoilingModifier, HolderLookup.Provider lookup) {
        ResourceKey<FoodStages> stages = getFoodStagesResourceKey(stack, lookup);

        NBTSpoilingUtils.initItem(stack);
        NBTSpoilingUtils.setCreationTime(stack, dayTime);
        NBTSpoilingUtils.setLastDayTime(stack, dayTime);
        NBTSpoilingUtils.setFoodStages(stack, stages);
        NBTSpoilingUtils.setMaxSpoilingProgress(stack, getMaxProgress(stack, lookup));
        NBTSpoilingUtils.setSpoilingModifier(stack, spoilingModifier);

    }

    private static @NotNull ResourceKey<FoodStages> getFoodStagesResourceKey(ItemStack stack, HolderLookup.Provider lookup) {
        List<Holder.Reference<FoodStages>> foodStageElements = lookup.lookupOrThrow(FSRegistries.FOOD_STAGES_KEY).listElements().toList();
        ResourceKey<FoodStages> stages = null;
        for (Holder.Reference<FoodStages> foodStages : foodStageElements) {
            Optional<TagKey<Item>> key = foodStages.value().key();
            if (key.isPresent() && stack.is(key.get())) {
                stages = foodStages.key();
                break;
            }
        }

        if (stages == null) {
            stages = FSFoodStages.DEFAULT;
        }

        return stages;
    }

    public static int getMaxProgress(ItemStack stack, HolderLookup.Provider lookup) {
        FoodStage lastStage = getLastStage(stack, lookup);
        if (lastStage != null) {
            return lastStage.days() * 24000;
        }
        return -1;
    }

    public static FoodStage getCurStage(ItemStack stack, HolderLookup.Provider lookup) {
        FoodStages stages = getStages(stack, lookup);
        if (stages != null) {
            float spoilingProgress = NBTSpoilingUtils.getSpoilingProgress(stack);
            float maxSpoilingProgress = NBTSpoilingUtils.getMaxSpoilingProgress(stack);
            int totalDays = (int) (maxSpoilingProgress / 24000);
            int curDay = (int) ((spoilingProgress / maxSpoilingProgress) * totalDays);
            for (FoodStage stage : stages.stages()) {
                if (curDay < stage.days()) {
                    return stage;
                }
            }
        }
        return null;
    }

    public static float getTemperature(Level level, BlockPos pos) {
        if (hasColdSweat()) {
            return (float) ColdSweatCompat.getTempFahrenheit(ColdSweatCompat.getTemperature(level, pos));
        }
        if (hasLSO()) {
            return (float) LSOCompat.getTemperature(level, pos);
        }
        return 1f;
    }

    public static boolean hasColdSweat() {
        return ModList.get().isLoaded("cold_sweat");
    }

    public static boolean hasLSO() {
        return ModList.get().isLoaded("legendarysurvivaloverhaul");
    }

    public static FoodStages getStages(ItemStack itemStack, HolderLookup.Provider lookup) {
        if (NBTSpoilingUtils.hasFoodState(itemStack)) {
            ResourceKey<FoodStages> foodStages = NBTSpoilingUtils.getFoodStages(itemStack);
            if (foodStages != null) {
                HolderLookup.RegistryLookup<FoodStages> registryLookup = lookup.lookupOrThrow(FSRegistries.FOOD_STAGES_KEY);
                Holder.Reference<FoodStages> foodStagesReference = registryLookup.get(foodStages).orElse(null);
                if (foodStagesReference == null) {
                    foodStagesReference = registryLookup.getOrThrow(getFoodStagesResourceKey(itemStack, lookup));
                }
                return foodStagesReference.value();
            }
        }
        return null;
    }

    public static ItemStack createRottenMass(ItemStack foodStack) {
        ItemStack stack = new ItemStack(FSItems.ROTTEN_MASS.get(), foodStack.getCount());
        NBTSpoilingUtils.setFoodState(stack, NBTSpoilingUtils.getFoodState(foodStack));
        return stack;
    }

    public static ItemStack createDecomposedGoo(ItemStack rottenMassStack) {
        return new ItemStack(FSItems.DECOMPOSED_GOO.get(), rottenMassStack.getCount());
    }

    public static List<Component> getSpoilingTooltip(ItemStack stack, Player player, boolean isShiftDown) {
        Level level = player.level();
        long creationTime = NBTSpoilingUtils.getCreationTime(stack);
        int creationDay = Math.round((creationTime - creationTime % 24000) / 24000f);
        //long diff = level.dayTime() - creationTime;
        long dayTime = level.dayTime();
        int curDay = (int) (dayTime / 24000 - creationDay);
        RegistryAccess access = level.registryAccess();
        ResourceKey<FoodStages> foodStagesType = NBTSpoilingUtils.getFoodStages(stack);
        if (foodStagesType != null) {
            HolderLookup.RegistryLookup<FoodStages> registryLookup = access.lookupOrThrow(FSRegistries.FOOD_STAGES_KEY);
            Holder.Reference<FoodStages> foodStagesReference = registryLookup.get(foodStagesType).orElse(null);
            if (foodStagesReference == null) {
                foodStagesReference = registryLookup.getOrThrow(getFoodStagesResourceKey(stack, access));
            }
            List<FoodStage> stages = foodStagesReference.value().stages();
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
                long expirationDate = (creationTime + (totalDays * 24000L));
                int expirationDay = (int) (expirationDate / 24000);
                String expirationDateText = String.format("Day %d - %s", expirationDay, timeToHoursMinutes(level, expirationDate));
                ResourceKey<FoodQuality> key = qualityKey != null ? qualityKey : stages.get(stages.size() - 1).quality();
                Holder.Reference<FoodQuality> quality = access.lookupOrThrow(FSRegistries.FOOD_QUALITY_KEY).getOrThrow(key);
                float freshness = getFreshness(NBTSpoilingUtils.getSpoilingProgress(stack), NBTSpoilingUtils.getMaxSpoilingProgress(stack));
                List<Component> tooltip = new ArrayList<>();
                if (NBTSpoilingUtils.getSpoilingModifier(stack) == 0) {
                    tooltip.add(Component.literal("Frozen").withStyle(Style.EMPTY.withColor(FoodSpoilingConfig.frozenTintColor)));
                }
                tooltip.addAll(List.of(
                        Component.literal("Quality: ").withStyle(ChatFormatting.GRAY).append(registryTranslation(key).copy().withStyle(Style.EMPTY.withColor(quality.value().textColor().toARGB()))),
                        Component.literal("Freshness: ").withStyle(ChatFormatting.GRAY).append(Math.round(freshness * 100) + "%"),
                        Component.literal("Expires on: ").withStyle(ChatFormatting.GRAY).append(Component.literal(expirationDateText).withStyle(ChatFormatting.YELLOW))
                ));
                if (!isShiftDown) {
                    tooltip.add(Component.literal("Hold <Shift> for more information").withStyle(ChatFormatting.GRAY));
                } else if (curStage != null) {
                    tooltip.addAll(createAdvancedTooltip(stack, player, curStage, nextStage, creationTime, daysToNext, access));
                }
                return tooltip;
            }
        }
        return Collections.emptyList();
    }

    public static FoodStage getLastStage(ItemStack stack, HolderLookup.Provider lookup) {
        FoodStages stages = getStages(stack, lookup);
        return stages != null ? stages.stages().get(stages.stages().size() - 1) : null;
    }

    public static float getFreshness(float progress, float maxProgress) {
        return 1f - (progress / maxProgress);
    }

    private static List<Component> createAdvancedTooltip(ItemStack stack, Player player, FoodStage curStage, FoodStage nextStage, long creationTime, int daysToNext, RegistryAccess access) {
        Holder.Reference<FoodQuality> quality = access.lookupOrThrow(FSRegistries.FOOD_QUALITY_KEY).getOrThrow(curStage.quality());
        List<Component> advTooltip = new ArrayList<>();
        long curTime = (long) (creationTime + NBTSpoilingUtils.getSpoilingProgress(stack));
        int timeToNext = ((curStage.days() - daysToNext) * 24000) - (int) (curTime - (creationTime + (daysToNext * 24000)));
        int days = timeToNext / 24000;
        MutableComponent nextStageComponent = Component.empty();
        if (nextStage != null) {
            Holder.Reference<FoodQuality> nextQuality = access.lookupOrThrow(FSRegistries.FOOD_QUALITY_KEY).getOrThrow(nextStage.quality());
            nextStageComponent.append(Component.literal("Next Stage ")
                    .append(Component.literal("%s".formatted(registryTranslation(nextQuality.key()).getString())).withStyle(Style.EMPTY.withColor(nextQuality.value().textColor().toARGB()))));
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

    public static float getContainerSpoilageModifier(ResourceLocation blockentityId) {
        if (FoodSpoilingConfig.containerModifiers.containsKey(blockentityId)) {
            return FoodSpoilingConfig.containerModifiers.get(blockentityId);
        }

        return 1.0f;
    }

}
