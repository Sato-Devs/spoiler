package com.thepigcat.spoiler.mixins;

import com.thepigcat.spoiler.utils.NBTSpoilingUtils;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ResultSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(ResultSlot.class)
public class ResultSlotMixin extends Slot {
    @Shadow @Final private Player player;

    private ResultSlotMixin(Container pContainer, int pSlot, int pX, int pY) {
        super(pContainer, pSlot, pX, pY);
    }

    @Override
    public void set(ItemStack pStack) {
        super.set(pStack);
        if (pStack.isEdible() && !pStack.isEdible() && NBTSpoilingUtils.hasFoodState(pStack)) {
            NBTSpoilingUtils.setCreationTime(pStack, player.level().dayTime());
        }
    }
}
