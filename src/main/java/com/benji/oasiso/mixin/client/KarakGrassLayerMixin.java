package com.benji.oasiso.mixin.client;

import com.benji.oasiso.client.block.KarakGrassSway;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderType;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public abstract class KarakGrassLayerMixin {
    @Inject(method="renderChunkLayer", at=@At(value="INVOKE",
            target="Lnet/minecraft/client/renderer/RenderType;setupRenderState()V",shift=At.Shift.AFTER),require=0)
    private void oasiso$grassSway(RenderType layer,PoseStack pose,double cameraX,double cameraY,double cameraZ,
                                 Matrix4f projection,CallbackInfo ci) {
        KarakGrassSway.useShader(layer,cameraX,cameraY,cameraZ);
    }
}
