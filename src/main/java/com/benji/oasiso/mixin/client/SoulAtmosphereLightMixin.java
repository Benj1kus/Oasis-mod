package com.benji.oasiso.mixin.client;

import com.benji.oasiso.client.soulstorage.SoulAtmosphere;
import net.minecraft.client.renderer.LightTexture;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import com.mojang.blaze3d.platform.NativeImage;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(LightTexture.class)
public abstract class SoulAtmosphereLightMixin {
    @Redirect(method = "updateLightTexture", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/platform/NativeImage;setPixelRGBA(III)V"))
    private void soulLight(NativeImage image, int x, int y, int rgba) {
        float strength = SoulAtmosphere.strength();

        if (strength <= 0.0F) {
            image.setPixelRGBA(x, y, rgba);
            return;
        }

        float dark = strength * 0.63F * (y / 15.0F) * (1.0F - x / 15.0F);

        int r = (int) ((rgba & 255) * (1.0F - dark));
        int g = (int) (((rgba >>> 8) & 255) * (1.0F - dark * 0.90F));
        int b = (int) (((rgba >>> 16) & 255) * (1.0F - dark * 0.75F));

        int color = (rgba & 0xFF000000) | (b << 16) | (g << 8) | r;

        image.setPixelRGBA(x, y, color);
    }
}
