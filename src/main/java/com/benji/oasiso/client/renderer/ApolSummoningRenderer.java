package com.benji.oasiso.client.renderer;

import com.benji.oasiso.Oasiso;
import com.benji.oasiso.common.block.entity.ApolAltarBlockEntity;
import com.benji.oasiso.common.entity.ApolSummoningEntity;
import com.benji.oasiso.common.entity.ApolArenaShape;
import com.benji.oasiso.common.entity.ApolArenaCollision;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.WeakHashMap;

public class ApolSummoningRenderer extends EntityRenderer<ApolSummoningEntity> {
    private final WeakHashMap<ApolSummoningEntity, ApolAltarBlockEntity> altarModels = new WeakHashMap<>();

    public ApolSummoningRenderer(EntityRendererProvider.Context context) {
        super(context);
        shadowRadius = 0;
    }

    @Override
    public ResourceLocation getTextureLocation(ApolSummoningEntity entity) {
        return net.minecraft.world.inventory.InventoryMenu.BLOCK_ATLAS;
    }

    @Override
    public boolean shouldRender(ApolSummoningEntity entity, Frustum frustum, double x, double y, double z) {
        return entity.shouldRender(x, y, z) && frustum.isVisible(entity.getBoundingBox().inflate(12));
    }

    @Override
    public void render(ApolSummoningEntity entity, float yaw, float partial, PoseStack pose, MultiBufferSource buffers, int light) {
        ApolSummoningBeam.render(entity, partial, pose);
        if (entity.settled() || entity.altarState().isAir()) {
            altarModels.remove(entity);
            return;
        }
        var blocks = Minecraft.getInstance().getBlockRenderer();
        var soil = ApolSummoningEntity.soil();
        for (int y = 0; y < 8; y++)
            for (int z = 0; z < 8; z++)
                for (int x = 0; x < 8; x++) {
                    if (!ApolArenaShape.contains(x, z)) continue;
                    if (y != 0 && y != 7 && !ApolArenaShape.rim(x, z)) continue;
                    pose.pushPose();
                    pose.translate(x - 4, y, z - 4);
                    blocks.renderSingleBlock(y == 7 ? entity.surface(x, z) : soil, pose, buffers, light, OverlayTexture.NO_OVERLAY);
                    pose.popPose();
                }
        ApolAltarBlockEntity altar = altarModels.computeIfAbsent(entity, e -> {
            ApolAltarBlockEntity be = new ApolAltarBlockEntity(e.origin(), e.altarState());
            be.setLevel(e.level());
            return be;
        });
        BlockEntityRenderer<ApolAltarBlockEntity> renderer = Minecraft.getInstance().getBlockEntityRenderDispatcher().getRenderer(altar);
        if (renderer != null) {
            pose.pushPose();
            pose.translate(-1, 8, -1);
            renderer.render(altar, partial, pose, buffers, light, OverlayTexture.NO_OVERLAY);
            pose.popPose();
        }
    }

    private static final class InvisibleCollisionRenderer extends EntityRenderer<ApolArenaCollision> {
        InvisibleCollisionRenderer(EntityRendererProvider.Context context) {
            super(context);
        }

        @Override
        public ResourceLocation getTextureLocation(ApolArenaCollision e) {
            return net.minecraft.world.inventory.InventoryMenu.BLOCK_ATLAS;
        }

        @Override
        public boolean shouldRender(ApolArenaCollision e, Frustum f, double x, double y, double z) {
            return false;
        }
    }

    @Mod.EventBusSubscriber(modid = Oasiso.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static final class Registration {
        @SubscribeEvent
        public static void register(EntityRenderersEvent.RegisterRenderers e) {
            e.registerEntityRenderer(ApolSummoningEntity.TYPE, ApolSummoningRenderer::new);
            e.registerEntityRenderer(ApolArenaCollision.TYPE, InvisibleCollisionRenderer::new);
        }
    }
}
