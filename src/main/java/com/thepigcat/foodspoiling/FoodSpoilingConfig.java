package com.thepigcat.foodspoiling;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.*;
import java.util.stream.Collectors;

@Mod.EventBusSubscriber(modid = FoodSpoiling.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class FoodSpoilingConfig {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    // General Section
    private static final ForgeConfigSpec.BooleanValue SPOIL_FOODS = BUILDER
            .comment("Whether food should even be spoiled")
            .define("general.spoilFood", true);

    private static final ForgeConfigSpec.IntValue CHECK_INTERVAL = BUILDER
            .comment("How often food items are evaluated in ticks")
            .defineInRange("general.checkInterval", 100, 1, Integer.MAX_VALUE);

    private static final ForgeConfigSpec.BooleanValue SPOIL_ENDER_CHEST = BUILDER
            .comment("Whether items in the ender chest should be spoiled")
            .define("general.spoilEnderChest", false);

    // Spoiling Section
    private static final ForgeConfigSpec.BooleanValue RENDER_SPOILED_OVERLAY = BUILDER
            .comment("Whether to apply a tint to the spoiled item")
            .define("spoiling.renderSpoiledOverlay", true);

    private static final ForgeConfigSpec.BooleanValue BECOME_ROTTEN_MASS = BUILDER
            .comment("Whether food items should become rotten mass when fully rotten")
            .define("spoiling.becomeRottenMass", true);

    private static final ForgeConfigSpec.BooleanValue BECOME_DECOMPOSED_GOO = BUILDER
            .comment("Whether food items should become decomposed goo when fully rotten")
            .define("spoiling.becomeDecomposedGoo", true);

    // Tooltips Section
    private static final ForgeConfigSpec.BooleanValue SHOW_FOOD_TOOLTIP = BUILDER
            .comment("Shows the food quality in the tooltip")
            .define("tooltips.showFoodTooltip", true);

    // Container modifiers
    private static final ForgeConfigSpec.ConfigValue<Map<String, Double>> CONTAINER_MODIFIERS = BUILDER
            .comment("Spoilage rate modifiers for different containers (0.0 = freeze the item (dont spoil it), 0.5 = half rate, 1.0 = normal rate, 2.0 = double rate)")
            .define("containers.modifiers", Map.of(
                    "minecraft:chest", 0.5,
                    "minecraft:barrel", 0.8
            ), FoodSpoilingConfig::validateContainerModifier);

    // Final spec
    public static final ForgeConfigSpec SPEC = BUILDER.build();

    // Runtime values
    public static boolean spoilFoods;
    public static int checkInterval;
    public static boolean spoilEnderChest;
    public static boolean renderSpoiledOverlay;
    public static boolean becomeRottenMass;
    public static boolean becomeDecomposedGoo;
    public static boolean showFoodTooltip;
    public static Map<ResourceLocation, Double> containerModifiers;

    private static boolean validateContainerModifier(final Object obj) {
        return true;
    }

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event) {
        // Load general settings
        spoilFoods = SPOIL_FOODS.get();
        checkInterval = CHECK_INTERVAL.get();
        spoilEnderChest = SPOIL_ENDER_CHEST.get();

        // Load spoiling settings
        renderSpoiledOverlay = RENDER_SPOILED_OVERLAY.get();
        becomeRottenMass = BECOME_ROTTEN_MASS.get();
        becomeDecomposedGoo = BECOME_DECOMPOSED_GOO.get();

        // Load tooltip settings
        showFoodTooltip = SHOW_FOOD_TOOLTIP.get();

        // Load container settings
        containerModifiers = CONTAINER_MODIFIERS.get().entrySet().stream()
                .map(e -> new AbstractMap.SimpleEntry<>(new ResourceLocation(e.getKey()), e.getValue()))
                .collect(Collectors.toMap(AbstractMap.SimpleEntry::getKey, AbstractMap.SimpleEntry::getValue));

    }
}