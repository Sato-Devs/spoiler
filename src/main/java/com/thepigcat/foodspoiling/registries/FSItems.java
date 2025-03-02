package com.thepigcat.foodspoiling.registries;

import com.thepigcat.foodspoiling.FoodSpoiling;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import javax.swing.plaf.synth.Region;

public final class FSItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, FoodSpoiling.MODID);

    public static final RegistryObject<Item> ROTTEN_MASS = ITEMS.register("rotten_mass", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> DECOMPOSED_GOO = ITEMS.register("decomposed_goo", () -> new Item(new Item.Properties()));
}
