package com.thepigcat.spoiling.mixins;

import com.thepigcat.spoiling.FSTags;
import com.thepigcat.spoiling.utils.SpoilingRecipeUtils;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ShapelessRecipe.class)
public abstract class ShapelessRecipeMixin {

    @Shadow public abstract ItemStack getResultItem(RegistryAccess pRegistryAccess);

    @Inject(
            method = "assemble(Lnet/minecraft/world/inventory/CraftingContainer;Lnet/minecraft/core/RegistryAccess;)Lnet/minecraft/world/item/ItemStack;",
            at = @At("HEAD"),
            cancellable = true
    )
    private void spoiling$assemble(CraftingContainer pContainer, RegistryAccess pRegistryAccess, CallbackInfoReturnable<ItemStack> cir) {
        ItemStack resultItem = getResultItem(pRegistryAccess).copy();
        if (resultItem.isEdible() && !resultItem.is(FSTags.UNSPOILABLE_FOODS)) {
            SpoilingRecipeUtils.setResultProgress(pContainer, pRegistryAccess, resultItem);

            cir.setReturnValue(resultItem);
        }
    }
}
