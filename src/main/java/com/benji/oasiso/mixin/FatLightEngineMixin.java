package com.benji.oasiso.mixin;

import com.benji.oasiso.common.cowrit.FatCoatings;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LightChunkGetter;
import net.minecraft.world.level.lighting.BlockLightEngine;
import net.minecraft.world.level.lighting.BlockLightSectionStorage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.function.BiConsumer;

@Mixin(BlockLightEngine.class)
abstract class FatLightEngineMixin {
    @Unique
    private LightChunkGetter oasiso$source;

    @Inject(method = "<init>(Lnet/minecraft/world/level/chunk/LightChunkGetter;Lnet/minecraft/world/level/lighting/BlockLightSectionStorage;)V", at = @At("RETURN"), require = 1)
    private void oasiso$rememberSource(LightChunkGetter source, BlockLightSectionStorage storage, CallbackInfo ci) {
        oasiso$source = source;
    }

    @Inject(method = "getEmission", at = @At("RETURN"), cancellable = true, require = 1)
    private void oasiso$filterEmission(long packedPos, BlockState state, CallbackInfoReturnable<Integer> cir) {
        if (cir.getReturnValueI() > 0 && oasiso$source.getLevel() instanceof Level level && FatCoatings.has(level, BlockPos.of(packedPos))) {
            cir.setReturnValue(0);
        }
    }

    @ModifyArg(method = "propagateLightSources", index = 0, require = 1, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/chunk/LightChunk;findBlockLightSources(Ljava/util/function/BiConsumer;)V"))
    private BiConsumer<BlockPos, BlockState> oasiso$filterChunkSources(BiConsumer<BlockPos, BlockState> accept) {
        if (!(oasiso$source.getLevel() instanceof Level level)) return accept;
        return (pos, state) -> {
            if (!FatCoatings.has(level, pos)) accept.accept(pos, state);
        };
    }
}
