package com.benji.oasiso.client.soulcast;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterShadersEvent;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.io.IOException;

@Mod.EventBusSubscriber(modid = "oasiso", value = Dist.CLIENT)
public final class RelaxBlink {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath("oasiso", "textures/gui/blink_overlay.png");
    private static ShaderInstance shader;
    private static boolean hasTexture;

    @Mod.EventBusSubscriber(modid = "oasiso", value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static final class Shaders {
        @SubscribeEvent
        public static void register(RegisterShadersEvent event) throws IOException {
            var resources = event.getResourceProvider();
            boolean found = resources.getResource(TEXTURE).isPresent();
            event.registerShader(new ShaderInstance(resources,
                    ResourceLocation.fromNamespaceAndPath("oasiso", "relax_blink"), DefaultVertexFormat.POSITION_TEX),
                    loaded -> {
                        shader = loaded;
                        hasTexture = found;
                    });
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void draw(RenderGuiEvent.Post event) {
        float closure = SoulCastClient.blinkClosure(event.getPartialTick());
        if (shader == null || closure <= 0) return;
        event.getGuiGraphics().flush();
        var previous = RenderSystem.getShader();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        try {
            RenderSystem.setShader(() -> shader);
            if (hasTexture) RenderSystem.setShaderTexture(0, TEXTURE);
            shader.safeGetUniform("Closure").set(closure);
            shader.safeGetUniform("HasTexture").set(hasTexture ? 1.0F : 0.0F);
            BufferBuilder b = Tesselator.getInstance().getBuilder();
            b.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
            b.vertex(-1, 1, 0).uv(0, 0).endVertex();
            b.vertex(-1, -1, 0).uv(0, 1).endVertex();
            b.vertex(1, -1, 0).uv(1, 1).endVertex();
            b.vertex(1, 1, 0).uv(1, 0).endVertex();
            BufferUploader.drawWithShader(b.end());
        } finally {
            RenderSystem.depthMask(true);
            RenderSystem.enableDepthTest();
            RenderSystem.disableBlend();
            if (previous != null) RenderSystem.setShader(() -> previous);
        }
    }
}
