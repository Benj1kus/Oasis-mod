package com.benji.oasiso.common.fluid;

import com.mojang.blaze3d.shaders.FogShape;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
import net.minecraftforge.common.SoundActions;
import net.minecraftforge.fluids.FluidType;
import org.joml.Vector3f;

import java.util.function.Consumer;

public final class EntropyFluidType extends FluidType {
    public EntropyFluidType() {
        super(Properties.create()
                .descriptionId("fluid_type.oasiso.entropy_water")
                .canSwim(true).canPushEntity(true).motionScale(0.014D)
                .canDrown(true)
                .fallDistanceModifier(0.0F).supportsBoating(true)
                .canExtinguish(true).canHydrate(false)
                .canConvertToSource(true)
                .sound(SoundActions.BUCKET_FILL, SoundEvents.BUCKET_FILL)
                .sound(SoundActions.BUCKET_EMPTY, SoundEvents.BUCKET_EMPTY));
    }

    @Override
    public void initializeClient(Consumer<IClientFluidTypeExtensions> consumer) {
        consumer.accept(new IClientFluidTypeExtensions() {
            private final ResourceLocation texture = ResourceLocation.fromNamespaceAndPath("oasiso", "block/entropy_water");

            @Override
            public ResourceLocation getStillTexture() { return texture; }

            @Override
            public ResourceLocation getFlowingTexture() { return texture; }

            @Override
            public int getTintColor() {
                return 0xFFFFFFFF;
            }

            @Override
            public Vector3f modifyFogColor(Camera camera, float partialTick, ClientLevel level,
                    int renderDistance, float darkenWorldAmount, Vector3f originalColor) {
                return new Vector3f(1.0F, 0.41F, 0.71F);
            }

            @Override
            public void modifyFogRender(Camera camera, FogRenderer.FogMode mode,
                    float renderDistance, float partialTick, float nearDistance,
                    float farDistance, FogShape shape) {
                RenderSystem.setShaderFogStart(-2.0F);
                RenderSystem.setShaderFogEnd(Math.min(3.0F, renderDistance));
                RenderSystem.setShaderFogShape(FogShape.SPHERE);
            }
        });
    }
}
