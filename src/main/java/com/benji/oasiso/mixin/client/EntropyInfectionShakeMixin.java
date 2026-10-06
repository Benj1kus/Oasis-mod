package com.benji.oasiso.mixin.client;

import com.benji.oasiso.client.entropy.EntropyInfectionClient;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntityRenderer.class)
public abstract class EntropyInfectionShakeMixin {
    @Inject(method="isShaking",at=@At("RETURN"),cancellable=true)
    private void oasiso$entropyShaking(LivingEntity entity,CallbackInfoReturnable<Boolean> cir) {
        if(EntropyInfectionClient.isShaking(entity))cir.setReturnValue(true);
    }
}
