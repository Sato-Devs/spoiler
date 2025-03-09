package com.thepigcat.foodspoiling.mixins;
import com.thepigcat.foodspoiling.FSTags;
import com.thepigcat.foodspoiling.FoodSpoiling;
import com.thepigcat.foodspoiling.utils.NBTSpoilingUtils;
import com.thepigcat.foodspoiling.utils.SpoilingRecipeUtils;
import com.thepigcat.foodspoiling.utils.SpoilingUtils;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.ShapedRecipe;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ShapedRecipe.class)
public abstract class ShapedRecipeMixin {
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
