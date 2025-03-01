package com.thepigcat.foodspoiling.utils;

import com.thepigcat.foodspoiling.FoodSpoiling;
import com.thepigcat.foodspoiling.FoodSpoilingConfig;
import com.thepigcat.foodspoiling.api.FoodQuality;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class FoodSpoilingUtils {
    private static final String STATE_TAG = "FoodState";
    private static final String CREATION_TIME_TAG = "CreationTime";
    private static final String REMAINING_LIFETIME_TAG = "RemainingLifetime";
    private static final String FOOD_CLASS_TAG = "FoodClass";
    private static final String STACK_ID_TAG = "StackId";

    /**
     * Gets the current food state from an item stack
     */
    @Nullable
    public static FoodQuality getFoodState(ItemStack stack) {
        if (stack.isEmpty()) return null;

        CompoundTag nbt = stack.getTag();
        if (nbt != null && nbt.contains(STATE_TAG)) {
            CompoundTag stateTag = nbt.getCompound(STATE_TAG);
            return new FoodQuality(
                    stateTag.getString("name"),
                    stateTag.getInt("day"),
                    stateTag.getInt("color")
            );
        }
        return null;
    }

    /**
     * Sets the food state for an item stack
     */
    public static void setFoodState(ItemStack stack, FoodQuality state) {
        if (stack.isEmpty()) return;

        CompoundTag nbt = stack.getOrCreateTag();
        CompoundTag stateTag = new CompoundTag();
        stateTag.putString("name", state.name());
        stateTag.putInt("day", state.day());
        stateTag.putInt("color", state.color());
        nbt.put(STATE_TAG, stateTag);
    }

    /**
     * Initializes food state when an item is first created/encountered
     */
    public static void initializeFoodState(ItemStack stack, long worldTime) {
        if (stack.isEmpty() || hasFoodState(stack)) return;

        String foodClass = determineFoodClass(stack.getItem());
        if (foodClass == null) return; // Not a tracked food item

        // Generate a unique ID for the stack that will be the same for identical items
        String stackId = generateStackId(stack, worldTime);

        CompoundTag nbt = stack.getOrCreateTag();
        nbt.putLong(CREATION_TIME_TAG, worldTime);
        nbt.putString(FOOD_CLASS_TAG, foodClass);
        nbt.putString(STACK_ID_TAG, stackId);

        List<List<Object>> stateProgression = FoodSpoilingConfig.stateProgressions.get(foodClass);
        if (stateProgression != null && !stateProgression.isEmpty()) {
            String initialState = (String) stateProgression.get(0).get(0);
            int stateColor = getStateColor(initialState);
            setFoodState(stack, new FoodQuality(initialState, 0, stateColor));
        }
    }

    /**
     * Generate a consistent ID for similar items created at the same world time
     */
    private static String generateStackId(ItemStack stack, long worldTime) {
        // Round world time to nearest hour to allow stacking of items created close together
        long roundedTime = (worldTime / (FoodSpoilingConfig.dayLength / 24)) * (FoodSpoilingConfig.dayLength / 24);
        String itemName = ForgeRegistries.ITEMS.getKey(stack.getItem()).toString();

        // Items created at the same "hour" of game time will have the same ID
        return itemName + "_" + roundedTime;
    }

    /**
     * Updates the food state based on the current world time
     */
    public static void updateFoodState(ItemStack stack, long worldTime, double spoilageModifier) {
        if (stack.isEmpty() || !hasFoodState(stack)) return;

        CompoundTag nbt = stack.getTag();
        if (nbt == null || !nbt.contains(CREATION_TIME_TAG) || !nbt.contains(FOOD_CLASS_TAG)) return;

        long creationTime = nbt.getLong(CREATION_TIME_TAG);
        String foodClass = nbt.getString(FOOD_CLASS_TAG);

        // Calculate elapsed days
        double elapsedGameDays = calculateElapsedDays(creationTime, worldTime);
        elapsedGameDays *= spoilageModifier; // Apply container modifier

        // Get current state
        FoodQuality currentState = getFoodState(stack);
        if (currentState == null) return;

        // Get state progression for this food class
        List<List<Object>> stateProgression = FoodSpoilingConfig.stateProgressions.get(foodClass);
        if (stateProgression == null || stateProgression.isEmpty()) return;

        // Find the appropriate state based on elapsed time
        double accumulatedDays = 0;
        String newState = currentState.name();
        int currentIndex = -1;

        for (int i = 0; i < stateProgression.size(); i++) {
            List<Object> stateInfo = stateProgression.get(i);
            String stateName = (String) stateInfo.get(0);
            double stateDuration = ((Number) stateInfo.get(1)).doubleValue();

            if (stateName.equals(currentState.name())) {
                currentIndex = i;
            }

            if (i >= currentIndex && currentIndex != -1) {
                if (accumulatedDays <= elapsedGameDays && elapsedGameDays < (accumulatedDays + stateDuration)) {
                    newState = stateName;
                    break;
                }
                accumulatedDays += stateDuration;
            }
        }

        // If we've exceeded all states, use the last one
        if (accumulatedDays <= elapsedGameDays && !stateProgression.isEmpty()) {
            List<Object> lastState = stateProgression.get(stateProgression.size() - 1);
            newState = (String) lastState.get(0);

            // Check if we should convert to rotten pile
            if (FoodSpoilingConfig.becomeRottenItem &&
                    newState.equals("rotten") &&
                    elapsedGameDays > (accumulatedDays + ((Number)lastState.get(1)).doubleValue())) {
                convertToRottenItem(stack);
                return;
            }
        }

        // Update the state if it changed
        if (!newState.equals(currentState.name())) {
            int color = getStateColor(newState);
            setFoodState(stack, new FoodQuality(newState, (int)elapsedGameDays, color));
        }
    }

    /**
     * Pauses spoilage by storing the remaining lifetime
     */
    public static void pauseSpoilage(ItemStack stack, long worldTime) {
        if (stack.isEmpty() || !hasFoodState(stack)) return;

        CompoundTag nbt = stack.getTag();
        if (nbt == null || !nbt.contains(CREATION_TIME_TAG)) return;

        long creationTime = nbt.getLong(CREATION_TIME_TAG);
        long elapsedTime = worldTime - creationTime;

        nbt.putLong(REMAINING_LIFETIME_TAG, elapsedTime);
        nbt.remove(CREATION_TIME_TAG); // Remove creation time while paused
    }

    /**
     * Resumes spoilage by calculating a new creation time
     */
    public static void resumeSpoilage(ItemStack stack, long worldTime) {
        if (stack.isEmpty() || !hasFoodState(stack)) return;

        CompoundTag nbt = stack.getTag();
        if (nbt == null || !nbt.contains(REMAINING_LIFETIME_TAG)) return;

        long remainingLifetime = nbt.getLong(REMAINING_LIFETIME_TAG);
        long newCreationTime = worldTime - remainingLifetime;

        nbt.putLong(CREATION_TIME_TAG, newCreationTime);
        nbt.remove(REMAINING_LIFETIME_TAG); // Remove remaining lifetime once resumed
    }

    /**
     * Checks if the item is in a container that affects spoilage
     */
    public static double getContainerSpoilageModifier(String containerId) {
        if (FoodSpoilingConfig.freezingContainers.contains(containerId)) {
            return 0.0; // No spoilage in freezing containers
        }

        if (FoodSpoilingConfig.containerModifiers.containsKey(containerId)) {
            return FoodSpoilingConfig.containerModifiers.get(containerId);
        }

        return 1.0; // Default: normal spoilage rate
    }

    /**
     * Checks if an item has already been initialized with food state
     */
    public static boolean hasFoodState(ItemStack stack) {
        if (stack.isEmpty()) return false;

        CompoundTag nbt = stack.getTag();
        return nbt != null && (nbt.contains(STATE_TAG) || nbt.contains(CREATION_TIME_TAG));
    }

    /**
     * Calculate elapsed days based on game time and day length
     */
    private static double calculateElapsedDays(long creationTime, long currentTime) {
        long elapsedTicks = currentTime - creationTime;
        return (double) elapsedTicks / FoodSpoilingConfig.dayLength;
    }

    /**
     * Determine which food class an item belongs to
     */
    @Nullable
    private static String determineFoodClass(Item item) {
        for (Map.Entry<String, Set<Item>> entry : FoodSpoilingConfig.foodClasses.entrySet()) {
            if (entry.getValue().contains(item)) {
                return entry.getKey();
            }
        }
        return null;
    }

    /**
     * Get the color associated with a state
     */
    private static int getStateColor(String state) {
        switch (state) {
            case "fresh": return 0x00FF00; // Green
            case "stale": return 0xCCCC00; // Yellow-green
            case "bruised": return 0xAAAA00; // Darker yellow
            case "mushy": return 0x996600; // Brown-yellow
            case "moldy": return 0x00CCCC; // Cyan
            case "spoiled": return 0xCC6600; // Orange-brown
            case "rancid": return 0xCC3300; // Dark orange
            case "rotten": return 0x660000; // Dark red
            case "sour": return 0xFFCC00; // Yellow
            case "weak": return 0xCCFF00; // Lime
            case "dead": return 0x666666; // Gray
            case "aged": return 0x996633; // Brown
            case "flat": return 0xCCCCCC; // Light gray
            default: return 0xFFFFFF; // White (default)
        }
    }

    /**
     * Apply food state effects when food is consumed
     */
    public static void applyFoodStateEffects(ItemStack stack, Player player) {
        FoodQuality state = getFoodState(stack);
        if (state == null) return;

        String stateName = state.name();
        // Get the state effects from config
        for (String effectName : getStateEffects(stateName)) {
            if (!effectName.isEmpty()) {
                MobEffect effect = ForgeRegistries.MOB_EFFECTS.getValue(new ResourceLocation(effectName));
                if (effect != null) {
                    // Duration and amplifier can be configured based on the state
                    int duration = 200; // 10 seconds
                    int amplifier = 0;  // Level 1

                    // Adjust duration and amplifier based on food state seriousness
                    if (stateName.equals("rotten") || stateName.equals("rancid")) {
                        duration = 600;   // 30 seconds
                        amplifier = 1;    // Level 2
                    } else if (stateName.equals("spoiled") || stateName.equals("moldy")) {
                        duration = 400;   // 20 seconds
                        amplifier = 0;    // Level 1
                    }

                    player.addEffect(new MobEffectInstance(effect, duration, amplifier));
                }
            }
        }

        // Apply hunger/saturation modifiers
        if (FoodSpoilingConfig.stateEffects.containsKey(stateName)) {
            // Could add code here to modify the nutritional value
            // But that would require additional mixins into the food consumption code
        }
    }

    /**
     * Get effect names for a specific food state
     */
    private static List<String> getStateEffects(String stateName) {
        // This would typically be retrieved from your config
        switch (stateName) {
            case "rotten":
            case "rancid":
                return List.of("minecraft:poison", "minecraft:hunger");
            case "spoiled":
            case "moldy":
                return List.of("minecraft:hunger");
            default:
                return List.of();
        }
    }

    /**
     * Checks if an item is actually food with nutritional values
     */
    public static boolean isFoodItem(ItemStack stack) {
        if (stack.isEmpty()) return false;
        return stack.getItem().isEdible();
    }

    /**
     * Initialize food state only for actual food items
     */
    public static void initializeIfFood(ItemStack stack, long worldTime) {
        if (isFoodItem(stack) && !hasFoodState(stack)) {
            initializeFoodState(stack, worldTime);
        }
    }

    /**
     * Convert an item to the rotten pile item
     */
    private static void convertToRottenItem(ItemStack stack) {
        // This would be where you'd replace the item with rotten_pile
        // For example: stack.setItem(FoodSpoilingRegistry.ROTTEN_PILE.get());
        FoodSpoiling.LOGGER.debug("Item fully rotted and should be converted to rotten pile");
    }
}