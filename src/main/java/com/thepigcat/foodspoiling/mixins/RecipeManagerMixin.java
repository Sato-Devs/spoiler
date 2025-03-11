package com.thepigcat.foodspoiling.mixins;

import com.mojang.datafixers.util.Pair;
import com.thepigcat.foodspoiling.FSRegistries;
import com.thepigcat.foodspoiling.FSTags;
import com.thepigcat.foodspoiling.api.FoodQuality;
import com.thepigcat.foodspoiling.api.FoodStage;
import com.thepigcat.foodspoiling.utils.NBTSpoilingUtils;
import com.thepigcat.foodspoiling.utils.SpoilingUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

@Mixin(RecipeManager.class)
public abstract class RecipeManagerMixin extends SimpleJsonResourceReloadListener {
    private RecipeManagerMixin() {
        super(null, null);
    }

    @Inject(method = "getRecipeFor(Lnet/minecraft/world/item/crafting/RecipeType;Lnet/minecraft/world/Container;Lnet/minecraft/world/level/Level;)Ljava/util/Optional;", at = @At("HEAD"), cancellable = true)
    private <C extends Container, T extends Recipe<C>> void spoiling$getRecipeFor0(RecipeType<T> pRecipeType, C pInventory, Level pLevel, CallbackInfoReturnable<Optional<T>> cir) {
        spoiling$checkForFoodItems(pRecipeType, pInventory, pLevel, cir, cir0 -> cir0.setReturnValue(Optional.empty()));
    }

    @Inject(method = "getRecipeFor(Lnet/minecraft/world/item/crafting/RecipeType;Lnet/minecraft/world/Container;Lnet/minecraft/world/level/Level;Lnet/minecraft/resources/ResourceLocation;)Ljava/util/Optional;", at = @At("HEAD"), cancellable = true)
    private <C extends Container, T extends Recipe<C>> void spoiling$getRecipeFor1(RecipeType<T> pRecipeType, C pInventory, Level pLevel, ResourceLocation pLastRecipe, CallbackInfoReturnable<Optional<Pair<ResourceLocation, T>>> cir) {
        spoiling$checkForFoodItems(pRecipeType, pInventory, pLevel, cir, cir0 -> cir0.setReturnValue(Optional.empty()));
    }

    @Inject(method = "getRecipesFor", at = @At("HEAD"), cancellable = true)
    private <C extends Container, T extends Recipe<C>> void spoiling$getRecipeFor1(RecipeType<T> pRecipeType, C pInventory, Level pLevel, CallbackInfoReturnable<List<T>> cir) {
        spoiling$checkForFoodItems(pRecipeType, pInventory, pLevel, cir, cir0 -> cir0.setReturnValue(Collections.emptyList()));
    }

    @Unique
    private static <OT> void spoiling$checkForFoodItems(RecipeType<?> type, Container pInventory, Level pLevel, CallbackInfoReturnable<OT> cir, Consumer<CallbackInfoReturnable<OT>> cirConsumer) {
        for (int i = 0; i < pInventory.getContainerSize(); i++) {
            ItemStack stack = pInventory.getItem(i);
            if (stack.isEdible() && !stack.is(FSTags.UNSPOILABLE_FOODS) && NBTSpoilingUtils.hasFoodState(stack)) {
                FoodStage curStage = SpoilingUtils.getCurStage(stack, pLevel.registryAccess());
                FoodQuality quality = pLevel.registryAccess().lookupOrThrow(FSRegistries.FOOD_QUALITY_KEY).getOrThrow(curStage.quality()).value();
                switch (quality.usableInRecipes()) {
                    case CRAFTING -> {
                        if (type != RecipeType.CRAFTING) {
                            cirConsumer.accept(cir);
                        }
                    }
                    case NONE -> cirConsumer.accept(cir);
                }
            }
        }
    }
}
