package com.thepigcat.spoiler.mixins;

import com.thepigcat.spoiler.events.FSEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.core.Registry;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockState;
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
    public LevelChunkMixin(ChunkPos p_187621_, UpgradeData p_187622_, LevelHeightAccessor p_187623_, Registry<Biome> p_187624_, long p_187625_, @Nullable LevelChunkSection[] p_187626_, @Nullable BlendingData p_187627_) {
        super(p_187621_, p_187622_, p_187623_, p_187624_, p_187625_, p_187626_, p_187627_);
    }

    @Shadow public abstract BlockState getBlockState(BlockPos pos);
    @Shadow public abstract BlockEntity getBlockEntity(BlockPos pos);
    @Shadow protected abstract boolean isInLevel();

    @Inject(method = "setBlockEntity", at = @At("RETURN"))
    private void foodspoiling$setBlockEntity(BlockEntity blockEntity, CallbackInfo ci) {
        try {
            if (blockEntity != null && !blockEntity.isRemoved() && isInLevel()) {
                BlockPos pos = blockEntity.getBlockPos();
                if (getBlockState(pos).hasBlockEntity()) {
                    FSEvents.registerBlockEntity(pos);
                }
            }
        } catch (Exception e) {
            // Log but don't crash
        }
    }

    @Inject(method = "removeBlockEntity", at = @At("RETURN"))
    private void foodspoiling$removeBlockEntity(BlockPos pos, CallbackInfo ci) {
        try {
            if (isInLevel()) {
                // Remove from FSEvents through a public method
                FSEvents.unregisterBlockEntity(pos);
            }
        } catch (Exception e) {
            // Log but don't crash
        }
    }
}