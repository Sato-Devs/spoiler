package com.thepigcat.foodspoiling.utils;

import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Pair;
import com.thepigcat.foodspoiling.FSRegistries;
import com.thepigcat.foodspoiling.FoodSpoiling;
import com.thepigcat.foodspoiling.FoodSpoilingConfig;
import com.thepigcat.foodspoiling.api.FoodQuality;
import com.thepigcat.foodspoiling.api.FoodStage;
import com.thepigcat.foodspoiling.api.FoodStages;
import com.thepigcat.foodspoiling.registries.FSFoodStages;
import com.thepigcat.foodspoiling.registries.FSItems;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public final class SpoilingUtils {
    public static void initialize(ItemStack stack, long dayTime, float spoilingModifier, HolderLookup.Provider lookup) {
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

        NBTSpoilingUtils.initItem(stack);
        NBTSpoilingUtils.setCreationTime(stack, dayTime);
        NBTSpoilingUtils.setFoodStages(stack, stages);
        NBTSpoilingUtils.setMaxSpoilingProgress(stack, getMaxProgress(stack, lookup));
        NBTSpoilingUtils.setSpoilingModifier(stack, spoilingModifier);
        NBTSpoilingUtils.setLastDayTime(stack, dayTime);

    }

    public static int getMaxProgress(ItemStack stack, HolderLookup.Provider lookup) {
        FoodStage lastStage = getLastStage(stack, lookup);
        if (lastStage != null) {
            return lastStage.days() * 24000;
        }
        return -1;
    }

    public static FoodStage getCurStage(ItemStack stack, long dayTime, HolderLookup.Provider lookup) {
        FoodStages stages = getStages(stack, lookup);
        if (stages != null) {
            long creationTime = NBTSpoilingUtils.getCreationTime(stack);
            int creationDay = Math.round((creationTime - creationTime % 24000) / 24000f);
            int curDay = (int) (dayTime / 24000 - creationDay);
            for (FoodStage stage : stages.stages()) {
                if (curDay < stage.days()) {
                    return stage;
                }
            }
        }
        return null;
    }

    public static FoodStages getStages(ItemStack itemStack, HolderLookup.Provider lookup) {
        if (NBTSpoilingUtils.hasFoodState(itemStack)) {
            ResourceKey<FoodStages> foodStages = NBTSpoilingUtils.getFoodStages(itemStack);
            if (foodStages != null) {
                return lookup.lookupOrThrow(FSRegistries.FOOD_STAGES_KEY).getOrThrow(foodStages).value();
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
                long expirationDate = (creationTime + (totalDays * 24000L));
                int expirationDay = (int) (expirationDate / 24000);
                String expirationDateText = String.format("Day %d - %s", expirationDay, timeToHoursMinutes(level, expirationDate));
                ResourceKey<FoodQuality> key = qualityKey != null ? qualityKey : stages.get(stages.size() - 1).quality();
                Holder.Reference<FoodQuality> quality = access.lookupOrThrow(FSRegistries.FOOD_QUALITY_KEY).getOrThrow(key);
                float freshness = getFreshness(NBTSpoilingUtils.getSpoilingProgress(stack), NBTSpoilingUtils.getMaxSpoilingProgress(stack));
                List<Component> tooltip = new ArrayList<>(List.of(
                        Component.literal("Quality: ").withStyle(ChatFormatting.GRAY).append(registryTranslation(key).copy().withStyle(Style.EMPTY.withColor(quality.value().textColor()))),
                        Component.literal("Freshness: ").withStyle(ChatFormatting.GRAY).append(Math.round(freshness * 100) + "%"),
                        Component.literal("Expires in: ").withStyle(ChatFormatting.GRAY).append(Component.literal(expirationDateText).withStyle(ChatFormatting.YELLOW))
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

    public static float getContainerSpoilageModifier(ResourceLocation blockentityId) {
        if (FoodSpoilingConfig.containerModifiers.containsKey(blockentityId)) {
            return FoodSpoilingConfig.containerModifiers.get(blockentityId);
        }

        return 1.0f;
    }

}
