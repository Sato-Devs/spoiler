package com.thepigcat.spoiling.mixins;

import com.thepigcat.spoiling.events.FSEvents;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

@Mixin(Entity.class)
public abstract class EntityMixin {
    @Shadow public abstract UUID getUUID();

    @Inject(method = "remove", at = @At("HEAD"))
    private void spoiling$remove(Entity.RemovalReason pReason, CallbackInfo ci) {
        FSEvents.unregisterEntity(this.getUUID());
    }
}
