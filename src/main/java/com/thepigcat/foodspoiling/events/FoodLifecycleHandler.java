package com.thepigcat.foodspoiling.events;

import com.thepigcat.foodspoiling.FSTags;
import com.thepigcat.foodspoiling.FoodSpoiling;
import com.thepigcat.foodspoiling.utils.FoodSpoilingUtils;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.player.EntityItemPickupEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;

@Mod.EventBusSubscriber(modid = FoodSpoiling.MODID)
public class FoodLifecycleHandler {

    // When an item stack is created in a player's inventory
    @SubscribeEvent
    public static void onItemCrafted(PlayerEvent.ItemCraftedEvent event) {
        ItemStack result = event.getCrafting();
        FoodSpoilingUtils.initializeIfFood(result, event.getEntity().level().getGameTime());
    }

    // When an item is picked up from the ground
    @SubscribeEvent
    public static void onItemPickup(EntityItemPickupEvent event) {
        ItemStack stack = event.getItem().getItem();
        FoodSpoilingUtils.initializeIfFood(stack, event.getEntity().level().getGameTime());
    }

    // When food is used (eaten)
    @SubscribeEvent
    public static void onFoodConsumed(LivingEntityUseItemEvent.Finish event) {
        if (FoodSpoilingUtils.isFoodItem(event.getItem())) {
            // Apply food state effects
            FoodSpoilingUtils.applyFoodStateEffects(event.getItem(), (Player) event.getEntity());
        }
    }

    // When the player joins, check inventory for any food without spoilage tags
    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        // Check and initialize any uninitialized food in the player's inventory
        event.getEntity().getInventory().items.forEach(stack -> {
            FoodSpoilingUtils.initializeIfFood(stack, event.getEntity().level().getGameTime());
        });
    }
}