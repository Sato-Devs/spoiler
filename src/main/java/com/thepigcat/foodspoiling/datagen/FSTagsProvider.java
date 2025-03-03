package com.thepigcat.foodspoiling.datagen;

import com.thepigcat.foodspoiling.FSTags;
import com.thepigcat.foodspoiling.FoodSpoiling;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.TagsProvider;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.common.data.ExistingFileHelper;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

public class FSTagsProvider {
    public static class EntityTypeProvider extends TagsProvider<EntityType<?>> {
        protected EntityTypeProvider(PackOutput p_256596_, CompletableFuture<HolderLookup.Provider> p_256513_, @Nullable ExistingFileHelper existingFileHelper) {
            super(p_256596_, Registries.ENTITY_TYPE, p_256513_, FoodSpoiling.MODID, existingFileHelper);
        }

        @Override
        protected void addTags(HolderLookup.Provider pProvider) {
            tag(FSTags.ENTITIES_WITH_INVENTORY, EntityType.PLAYER, EntityType.DONKEY, EntityType.MULE, EntityType.LLAMA, EntityType.CHEST_MINECART, EntityType.HOPPER_MINECART);
        }

        public void tag(TagKey<EntityType<?>> entityTypeTagKey, EntityType<?>... entityTypes) {
            TagAppender<EntityType<?>> tag = tag(entityTypeTagKey);
            for (EntityType<?> e : entityTypes) {
                tag.add(BuiltInRegistries.ENTITY_TYPE.getResourceKey(e).get());
            }
        }
    }
}
