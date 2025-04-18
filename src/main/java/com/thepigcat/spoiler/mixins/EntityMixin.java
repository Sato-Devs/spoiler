package com.thepigcat.spoiler.mixins;

import com.thepigcat.spoiler.events.FSEvents;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = Entity.class, remap = false)
public abstract class EntityMixin {
    @Inject(method = "onRemovedFromWorld", at = @At("HEAD"))
    private void spoiling$onRemovedFromWorld(CallbackInfo ci) {
        FSEvents.unregisterEntity(((Entity) (Object) this).getUUID());
    }
}
