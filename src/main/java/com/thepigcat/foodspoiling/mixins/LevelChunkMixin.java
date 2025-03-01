package com.thepigcat.foodspoiling.mixins;

import com.thepigcat.foodspoiling.events.FSEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.UpgradeData;
import net.minecraft.world.level.levelgen.blending.BlendingData;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelChunk.class)
public abstract class LevelChunkMixin extends ChunkAccess {
    @Shadow public abstract BlockState getBlockState(BlockPos p_62923_);

    @Shadow protected abstract boolean isInLevel();

    public LevelChunkMixin() {
        super(null, null, null, null, 0, null, null);
    }

    @Inject(method = "setBlockEntity", at = @At("HEAD"))
    private void foodspoiling$setBlockEntity(BlockEntity blockEntity, CallbackInfo ci) {
        if (getBlockState(blockEntity.getBlockPos()).hasBlockEntity()) {
            FSEvents.STORAGE_BLOCK_ENTITIES.add(blockEntity.getBlockPos());
        }
    }

    @Inject(method = "removeBlockEntity", at = @At("HEAD"))
    private void foodspoiling$removeBlockEntity(BlockPos pos, CallbackInfo ci) {
        if (isInLevel()) {
            FSEvents.STORAGE_BLOCK_ENTITIES.remove(pos);
        }
    }
}
