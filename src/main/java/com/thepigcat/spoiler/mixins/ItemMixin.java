package com.thepigcat.spoiler.mixins;

import com.thepigcat.spoiler.utils.NBTSpoilingUtils;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.extensions.IForgeItem;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(Item.class)
public abstract class ItemMixin implements IForgeItem {
    @Override
    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
        if (NBTSpoilingUtils.hasFoodState(oldStack) && NBTSpoilingUtils.hasFoodState(newStack)) {
            return false;
        }
        return IForgeItem.super.shouldCauseReequipAnimation(oldStack, newStack, slotChanged);
    }
}
