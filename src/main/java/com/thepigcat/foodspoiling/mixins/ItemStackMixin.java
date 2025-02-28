package com.thepigcat.foodspoiling.mixins;

import com.thepigcat.foodspoiling.ExampleMod;
import com.thepigcat.foodspoiling.Tags;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.stream.Stream;

@Mixin(ItemStack.class)
public abstract class ItemStackMixin {
    @Shadow public abstract Stream<TagKey<Item>> getTags();

    @Inject(method = "<init>(Lnet/minecraft/world/level/ItemLike;ILnet/minecraft/nbt/CompoundTag;)V", at = @At("TAIL"))
    private void spoiling$init(ItemLike item, int count, CompoundTag nbt, CallbackInfo ci) {
        if (getTags().anyMatch(tag -> tag.equals(Tags.FOOD))) {
            ExampleMod.FOOD_ITEMS.computeIfAbsent(item.asItem(), k -> new ArrayList<>()).add((ItemStack) ((Object) this));
            ExampleMod.LOGGER.debug("Food items: {}", ExampleMod.FOOD_ITEMS);
        }
    }
}
