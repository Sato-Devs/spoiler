package com.thepigcat.spoiling.datagen;

import com.thepigcat.spoiling.FoodSpoiling;
import com.thepigcat.spoiling.registries.FSItems;
import net.minecraft.data.PackOutput;
import net.minecraftforge.client.model.generators.ItemModelProvider;
import net.minecraftforge.common.data.ExistingFileHelper;

public class FSItemModelProvider extends ItemModelProvider {
    public FSItemModelProvider(PackOutput output, ExistingFileHelper existingFileHelper) {
        super(output, FoodSpoiling.MODID, existingFileHelper);
    }

    @Override
    protected void registerModels() {
        basicItem(FSItems.DECOMPOSED_GOO.get());
        basicItem(FSItems.ROTTEN_MASS.get());
    }
}
