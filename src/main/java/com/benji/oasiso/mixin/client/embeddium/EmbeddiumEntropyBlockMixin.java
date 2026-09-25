package com.benji.oasiso.mixin.client.embeddium;

import com.benji.oasiso.client.block.entropy.EntropyBlockVisuals;
import com.benji.oasiso.common.block.EntropyBlock;
import me.jellysquid.mods.sodium.client.render.chunk.compile.pipeline.BlockRenderer;
import me.jellysquid.mods.sodium.client.render.chunk.compile.pipeline.BlockRenderContext;
import me.jellysquid.mods.sodium.client.render.chunk.compile.ChunkBuildBuffers;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = BlockRenderer.class, remap = false)
public abstract class EmbeddiumEntropyBlockMixin {

    @Inject(method = "renderModel", at = @At("HEAD"), cancellable = true)
    private void oasiso$overrideEmbeddiumEntropy(BlockRenderContext context, ChunkBuildBuffers buffers, CallbackInfo ci) {
        if (context.state().getBlock() instanceof EntropyBlock) {
            EntropyBlockVisuals.track(context.pos());
            ci.cancel();
        }
    }
}