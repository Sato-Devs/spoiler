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
    private static final ForgeConfigSpec.IntValue CHECK_INTERVAL = BUILDER
            .comment("How often food items are evaluated in ticks")
            .defineInRange("general.checkInterval", 100, 1, Integer.MAX_VALUE);

    private static final ForgeConfigSpec.IntValue DAY_LENGTH = BUILDER
            .comment("Length of each day in ticks")
            .defineInRange("general.dayLength", 24000, 1, Integer.MAX_VALUE);

    private static final ForgeConfigSpec.BooleanValue DEBUG_CONTAINER_CLASS = BUILDER
            .comment("Prints the class name of any container upon opening")
            .define("general.debugContainerClass", false);

    // Spoiling Section
    private static final ForgeConfigSpec.BooleanValue RENDER_SPOILED_OVERLAY = BUILDER
            .comment("Applies an increasing color tint on items as they rot")
            .define("spoiling.renderSpoiledOverlay", true);

    private static final ForgeConfigSpec.BooleanValue RENDER_SPOILED_OVERLAY_FOOD_ONLY = BUILDER
            .comment("When 'Render Spoiled Overlay' is enabled, it only applies on food items")
            .define("spoiling.renderSpoiledOverlayFoodOnly", true);

    private static final ForgeConfigSpec.BooleanValue SPOIL_IN_CREATIVE_MODE = BUILDER
            .comment("Allows items to rot in creative mode")
            .define("spoiling.spoilInCreativeMode", false);

    private static final ForgeConfigSpec.BooleanValue SPOIL_IN_PLAYER_INVENTORY_ONLY = BUILDER
            .comment("Allows items to rot in the player's inventory only")
            .define("spoiling.spoilInPlayerInventoryOnly", false);

    private static final ForgeConfigSpec.BooleanValue BECOME_ROTTEN_ITEM = BUILDER
            .comment("Whether food items should become a rotten pile when fully rotten")
            .define("spoiling.becomeRottenItem", true);

    // Tooltips Section
    private static final ForgeConfigSpec.BooleanValue SHOW_FOOD_TOOLTIP = BUILDER
            .comment("Shows a status tooltip on food items")
            .define("tooltips.showFoodTooltip", true);

    private static final ForgeConfigSpec.BooleanValue SHOW_REMAINING_DAYS = BUILDER
            .comment("Shows remaining days until rotten")
            .define("tooltips.showRemainingDays", true);

    private static final ForgeConfigSpec.BooleanValue SHOW_REMAINING_PERCENTAGE = BUILDER
            .comment("Shows remaining percentage until rotten")
            .define("tooltips.showRemainingPercentage", true);

    // Container modifiers
    private static final ForgeConfigSpec.ConfigValue<List<? extends String>> FREEZING_CONTAINERS = BUILDER
            .comment("List of containers that freeze items inside, halting spoilage")
            .defineList("containers.freezing", List.of("minecraft:barrel"), FoodSpoilingConfig::validateContainerName);

    private static final ForgeConfigSpec.ConfigValue<List<? extends String>> PRESERVING_CONTAINERS = BUILDER
            .comment("List of containers that slow down spoilage")
            .defineList("containers.preserving", List.of("minecraft:barrel"), FoodSpoilingConfig::validateContainerName);

    private static final ForgeConfigSpec.ConfigValue<Map<String, Double>> CONTAINER_MODIFIERS = BUILDER
            .comment("Spoilage rate modifiers for different containers (1.0 = normal rate, 0.5 = half rate, 2.0 = double rate)")
            .define("containers.modifiers", Map.of(
                    "minecraft:chest", 0.5,
                    "minecraft:barrel", 0.8
            ), FoodSpoilingConfig::validateContainerModifier);

    // Food Class Configuration
    private static final Map<String, ForgeConfigSpec.ConfigValue<List<? extends String>>> FOOD_CLASSES = new HashMap<>();
    private static final Map<String, ForgeConfigSpec.ConfigValue<List<List<Object>>>> FOOD_CLASS_STATES = new HashMap<>();

    static {
        BUILDER.comment("Food Classes Configuration").push("foodClasses");

        // Define food classes
        defineClass("dry_produce", List.of("minecraft:wheat", "minecraft:bread"));
        defineClass("hard_produce", List.of("minecraft:apple", "minecraft:carrot", "minecraft:potato"));
        defineClass("soft_produce", List.of("minecraft:melon_slice", "minecraft:sweet_berries"));
        defineClass("pickled_produce", List.of());
        defineClass("cooked_meat", List.of("minecraft:cooked_beef", "minecraft:cooked_porkchop", "minecraft:cooked_mutton"));
        defineClass("raw_meat", List.of("minecraft:beef", "minecraft:porkchop", "minecraft:mutton"));
        defineClass("cooked_meal", List.of("minecraft:cookie", "minecraft:pumpkin_pie"));
        defineClass("baked_goods", List.of("minecraft:cake"));
        defineClass("desserts", List.of());
        defineClass("soup_stew", List.of("minecraft:mushroom_stew", "minecraft:rabbit_stew", "minecraft:beetroot_soup"));
        defineClass("alcoholic_drinks", List.of());
        defineClass("drinks", List.of("minecraft:potion"));
        defineClass("raw_eggs", List.of());
        defineClass("cooked_eggs", List.of());
        defineClass("liquid_dairy", List.of("minecraft:milk_bucket"));
        defineClass("soft_dairy", List.of());
        defineClass("hard_dairy", List.of());
        defineClass("fat", List.of());
        defineClass("seed", List.of("minecraft:wheat_seeds", "minecraft:pumpkin_seeds", "minecraft:melon_seeds", "minecraft:beetroot_seeds"));

        BUILDER.pop();

        // Define states for each class
        BUILDER.comment("Food States Configuration").push("foodStates");

        // State definitions
        BUILDER.comment("State affects (saturationMod multiplier, hunger multiplier, effect list)").push("stateEffects");

        defineState("fresh", List.of(1.0, 1.0, List.of()));
        defineState("stale", List.of(0.9, 0.9, List.of()));
        defineState("spoiled", List.of(0.5, 0.5, List.of("hunger", "poison")));
        defineState("moldy", List.of(0.7, 0.7, List.of("hunger", "poison")));
        defineState("aged", List.of(0.9, 0.9, List.of()));
        defineState("rancid", List.of(0.3, 0.3, List.of("hunger", "poison")));
        defineState("mushy", List.of(0.5, 0.5, List.of()));
        defineState("bruised", List.of(0.9, 0.9, List.of()));
        defineState("flat", List.of(0.8, 0.8, List.of()));
        defineState("rotten", List.of(0.1, 0.1, List.of("hunger", "poison")));
        defineState("sour", List.of(0.6, 0.6, List.of("hunger")));
        defineState("weak", List.of(0.7, 0.7, List.of()));
        defineState("dead", List.of(0.4, 0.4, List.of()));

        BUILDER.pop();

        BUILDER.pop();

        // Define state progressions for each class
        BUILDER.comment("State Progressions by Food Class").push("stateProgressions");

        defineStateProgression("seed", List.of(
                List.of("fresh", 30.0),
                List.of("stale", 20.0),
                List.of("weak", 15.0),
                List.of("dead", 10.0),
                List.of("rotten", 5.0)
        ));

        defineStateProgression("produce", List.of(
                List.of("fresh", 6.0),
                List.of("bruised", 5.0),
                List.of("mushy", 4.0),
                List.of("moldy", 3.0),
                List.of("rotten", 3.0)
        ));

        defineStateProgression("raw_meat", List.of(
                List.of("fresh", 4.0),
                List.of("spoiled", 3.0),
                List.of("rancid", 3.0),
                List.of("rotten", 3.0)
        ));

        defineStateProgression("cooked_meat", List.of(
                List.of("fresh", 8.0),
                List.of("stale", 4.0),
                List.of("spoiled", 4.0),
                List.of("rotten", 3.0)
        ));

        defineStateProgression("cooked_meal", List.of(
                List.of("fresh", 5.0),
                List.of("stale", 4.0),
                List.of("spoiled", 4.0),
                List.of("rotten", 3.0)
        ));

        defineStateProgression("baked_goods", List.of(
                List.of("fresh", 6.0),
                List.of("stale", 5.0),
                List.of("moldy", 5.0),
                List.of("rotten", 3.0)
        ));

        defineStateProgression("desserts", List.of(
                List.of("fresh", 7.0),
                List.of("stale", 6.0),
                List.of("moldy", 5.0),
                List.of("rotten", 3.0)
        ));

        defineStateProgression("soup_stew", List.of(
                List.of("fresh", 6.0),
                List.of("stale", 5.0),
                List.of("sour", 4.0),
                List.of("rancid", 3.0)
        ));

        defineStateProgression("alcoholic_drinks", List.of(
                List.of("fresh", 50.0),
                List.of("aged", 50.0),
                List.of("flat", 50.0),
                List.of("rancid", 20.0)
        ));

        defineStateProgression("drinks", List.of(
                List.of("fresh", 4.0),
                List.of("stale", 5.0),
                List.of("sour", 4.0),
                List.of("rancid", 3.0)
        ));

        defineStateProgression("raw_eggs", List.of(
                List.of("fresh", 8.0),
                List.of("spoiled", 4.0),
                List.of("rancid", 3.0),
                List.of("rotten", 2.0)
        ));

        defineStateProgression("cooked_eggs", List.of(
                List.of("fresh", 8.0),
                List.of("stale", 6.0),
                List.of("spoiled", 5.0),
                List.of("rotten", 4.0)
        ));

        defineStateProgression("liquid_dairy", List.of(
                List.of("fresh", 6.0),
                List.of("sour", 4.0),
                List.of("rancid", 3.0),
                List.of("rotten", 2.0)
        ));

        defineStateProgression("soft_dairy", List.of(
                List.of("fresh", 8.0),
                List.of("stale", 6.0),
                List.of("moldy", 5.0),
                List.of("rancid", 4.0),
                List.of("rotten", 3.0)
        ));

        defineStateProgression("hard_dairy", List.of(
                List.of("fresh", 20.0),
                List.of("stale", 15.0),
                List.of("moldy", 10.0),
                List.of("rancid", 5.0),
                List.of("rotten", 3.0)
        ));

        defineStateProgression("fat", List.of(
                List.of("fresh", 30.0),
                List.of("stale", 20.0),
                List.of("moldy", 7.0),
                List.of("rancid", 7.0),
                List.of("rotten", 4.0)
        ));

        defineStateProgression("soft_produce", List.of(
                List.of("fresh", 5.0),
                List.of("bruised", 3.0),
                List.of("mushy", 3.0),
                List.of("moldy", 3.0),
                List.of("rotten", 3.0)
        ));

        defineStateProgression("hard_produce", List.of(
                List.of("fresh", 10.0),
                List.of("bruised", 6.0),
                List.of("mushy", 5.0),
                List.of("moldy", 4.0),
                List.of("rotten", 4.0)
        ));

        defineStateProgression("pickled_produce", List.of(
                List.of("fresh", 30.0),
                List.of("aged", 20.0),
                List.of("sour", 10.0),
                List.of("rancid", 5.0)
        ));

        defineStateProgression("dry_produce", List.of(
                List.of("fresh", 20.0),
                List.of("stale", 15.0),
                List.of("moldy", 10.0),
                List.of("rotten", 5.0)
        ));

        BUILDER.pop();

        // Item Overrides
        BUILDER.comment("Override specific items to use different food classes").push("itemOverrides");

        BUILDER.pop();
    }

    // Final spec
    public static final ForgeConfigSpec SPEC = BUILDER.build();

    // Runtime values
    public static int checkInterval;
    public static int dayLength;
    public static boolean debugContainerClass;
    public static boolean renderSpoiledOverlay;
    public static boolean renderSpoiledOverlayFoodOnly;
    public static boolean spoilInCreativeMode;
    public static boolean spoilInPlayerInventoryOnly;
    public static boolean becomeRottenItem;
    public static boolean showFoodTooltip;
    public static boolean showRemainingDays;
    public static boolean showRemainingPercentage;
    public static Set<ResourceLocation> freezingContainers;
    public static Set<ResourceLocation> preservingContainers;
    public static Map<ResourceLocation, Double> containerModifiers;
    public static Map<String, Set<Item>> foodClasses = new HashMap<>();
    public static Map<String, List<Double>> stateEffects = new HashMap<>();
    public static Map<String, List<List<Object>>> stateProgressions = new HashMap<>();

    private static void defineClass(String className, List<String> defaultItems) {
        FOOD_CLASSES.put(className, BUILDER
                .comment("Items in the " + className + " food class")
                .defineList("classes." + className, defaultItems, FoodSpoilingConfig::validateItemName));
    }

    private static void defineState(String stateName, List<Object> stateData) {
        BUILDER.comment(stateName + " state effects")
                .define("state." + stateName, stateData, obj -> true);
    }

    private static void defineStateProgression(String className, List<List<Object>> stateProgression) {
        FOOD_CLASS_STATES.put(className, BUILDER
                .comment("State progression for " + className + " (state name, days in state)")
                .define("progression." + className, stateProgression, obj -> true));
    }

    private static boolean validateItemName(final Object obj) {
        return obj instanceof final String itemName &&
                (ForgeRegistries.ITEMS.containsKey(new ResourceLocation(itemName)) || itemName.startsWith("modid:"));
    }

    private static boolean validateContainerName(final Object obj) {
        return obj instanceof final String containerName &&
                (ForgeRegistries.BLOCKS.containsKey(new ResourceLocation(containerName)) || containerName.contains(":"));
    }

    private static boolean validateContainerModifier(final Object obj) {
        return true; // Accept any map for simplicity
    }

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event) {
        // Load general settings
        checkInterval = CHECK_INTERVAL.get();
        dayLength = DAY_LENGTH.get();
        debugContainerClass = DEBUG_CONTAINER_CLASS.get();

        // Load spoiling settings
        renderSpoiledOverlay = RENDER_SPOILED_OVERLAY.get();
        renderSpoiledOverlayFoodOnly = RENDER_SPOILED_OVERLAY_FOOD_ONLY.get();
        spoilInCreativeMode = SPOIL_IN_CREATIVE_MODE.get();
        spoilInPlayerInventoryOnly = SPOIL_IN_PLAYER_INVENTORY_ONLY.get();
        becomeRottenItem = BECOME_ROTTEN_ITEM.get();

        // Load tooltip settings
        showFoodTooltip = SHOW_FOOD_TOOLTIP.get();
        showRemainingDays = SHOW_REMAINING_DAYS.get();
        showRemainingPercentage = SHOW_REMAINING_PERCENTAGE.get();

        // Load container settings
        freezingContainers = FREEZING_CONTAINERS.get().stream().map(ResourceLocation::new).collect(Collectors.toSet());
        preservingContainers = PRESERVING_CONTAINERS.get().stream().map(ResourceLocation::new).collect(Collectors.toSet());
        containerModifiers = CONTAINER_MODIFIERS.get().entrySet().stream()
                .map(e -> new AbstractMap.SimpleEntry<>(new ResourceLocation(e.getKey()), e.getValue()))
                .collect(Collectors.toMap(AbstractMap.SimpleEntry::getKey, AbstractMap.SimpleEntry::getValue));

        // Load food classes
        foodClasses.clear();
        for (String className : FOOD_CLASSES.keySet()) {
            Set<Item> items = FOOD_CLASSES.get(className).get().stream()
                    .map(itemName -> {
                        if (itemName.startsWith("modid:")) {
                            return null; // These will be handled when the actual modid is known
                        }
                        return ForgeRegistries.ITEMS.getValue(new ResourceLocation(itemName));
                    })
                    .filter(item -> item != null)
                    .collect(Collectors.toSet());

            foodClasses.put(className, items);
        }

        // Load state progressions
        stateProgressions.clear();
        for (String className : FOOD_CLASS_STATES.keySet()) {
            stateProgressions.put(className, FOOD_CLASS_STATES.get(className).get());
        }
    }
}