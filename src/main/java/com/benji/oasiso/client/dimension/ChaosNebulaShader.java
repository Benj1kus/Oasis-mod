package com.benji.oasiso.client.dimension;

import com.benji.oasiso.Oasiso;
import com.mojang.blaze3d.shaders.Uniform;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterShadersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;

import java.io.IOException;

@Mod.EventBusSubscriber(modid = Oasiso.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ChaosNebulaShader {
    public static final float NEBULA_BRIGHTNESS = 0.15F;
    private static ShaderInstance shader;

    private ChaosNebulaShader() {}

    @SubscribeEvent
    public static void registerShaders(RegisterShadersEvent event) throws IOException {
        event.registerShader(new ShaderInstance(event.getResourceProvider(),
                ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "chaos_nebula"),
                DefaultVertexFormat.POSITION_COLOR), loaded -> shader = loaded);
    }

    public static boolean render(Matrix4f matrix, float size, float seconds) {
        ShaderInstance current = shader;
        if (current == null) return false;
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1, 1, 1, 1);
        RenderSystem.setShader(() -> current);
        set(current, "Time", seconds);
        set(current, "Brightness", NEBULA_BRIGHTNESS);
        BufferBuilder b = Tesselator.getInstance().getBuilder();
        b.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

        face(b, matrix, size, -1,-1,-1, 1,-1,-1, 1,1,-1, -1,1,-1);
        face(b, matrix, size, 1,-1,1, -1,-1,1, -1,1,1, 1,1,1);
        face(b, matrix, size, -1,-1,1, -1,-1,-1, -1,1,-1, -1,1,1);
        face(b, matrix, size, 1,-1,-1, 1,-1,1, 1,1,1, 1,1,-1);
        face(b, matrix, size, -1,1,-1, 1,1,-1, 1,1,1, -1,1,1);
        face(b, matrix, size, -1,-1,1, 1,-1,1, 1,-1,-1, -1,-1,-1);
        BufferUploader.drawWithShader(b.end());
        return true;
    }

    private static void set(ShaderInstance current, String name, float value) {
        Uniform uniform = current.getUniform(name);
        if (uniform != null) uniform.set(value);
    }

    private static void face(BufferBuilder b, Matrix4f m, float s,
                             int x1,int y1,int z1, int x2,int y2,int z2,
                             int x3,int y3,int z3, int x4,int y4,int z4) {
        vertex(b,m,s,x1,y1,z1); vertex(b,m,s,x2,y2,z2);
        vertex(b,m,s,x3,y3,z3); vertex(b,m,s,x4,y4,z4);
    }

    private static void vertex(BufferBuilder b, Matrix4f m, float s, int x, int y, int z) {
        b.vertex(m, x*s, y*s, z*s).color((x+1)*0.5F, (y+1)*0.5F, (z+1)*0.5F, 1.0F).endVertex();
    }
}
