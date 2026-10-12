package com.benji.oasiso.client.renderer;

import com.benji.oasiso.ModSounds;
import com.benji.oasiso.Oasiso;
import com.benji.oasiso.client.model.EntropyWellModel;
import com.benji.oasiso.client.particle.GeyserSmokeParticle;
import com.benji.oasiso.common.block.entity.EntropyWellBlockEntity;
import com.benji.oasiso.registry.EntropyWellRegistry;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.*;
import software.bernie.geckolib.cache.object.*;
import software.bernie.geckolib.renderer.GeoBlockRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;
import software.bernie.geckolib.util.RenderUtils;

@Mod.EventBusSubscriber(modid = Oasiso.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class EntropyWellRenderer extends GeoBlockRenderer<EntropyWellBlockEntity> {
    private final Matrix4f inverseBase = new Matrix4f();
    private static ClientLevel budgetLevel;
    private static long budgetTick;
    private static int bubbles, waves;

    public EntropyWellRenderer(BlockEntityRendererProvider.Context context) {
        super(new EntropyWellModel());
        addRenderLayer(new AutoGlowingGeoLayer<EntropyWellBlockEntity>(this) {
            @Override
            protected RenderType getRenderType(EntropyWellBlockEntity b) {
                return RenderType.eyes(EntropyWellModel.glow(b));
            }
        });
    }

    @SubscribeEvent
    public static void register(EntityRenderersEvent.RegisterRenderers e) {
        e.registerBlockEntityRenderer(EntropyWellRegistry.WELL.get(), EntropyWellRenderer::new);
    }

    @Override
    public void preRender(PoseStack p, EntropyWellBlockEntity b, BakedGeoModel model, MultiBufferSource buffers, VertexConsumer v, boolean again, float partial, int light, int overlay, float r, float g, float blue, float a) {
        if (!again) inverseBase.set(p.last().pose()).invert();
        super.preRender(p, b, model, buffers, v, again, partial, light, overlay, r, g, blue, a);
    }

    @Override
    public void renderRecursively(PoseStack p, EntropyWellBlockEntity b, GeoBone bone, RenderType type, MultiBufferSource buffers, VertexConsumer v, boolean again, float partial, int light, int overlay, float r, float g, float blue, float a) {
        if (!again && bone.getName().equals("well_smoke")) {
            p.pushPose();
            RenderUtils.prepMatrixForBone(p, bone);
            Vector3f point = new Vector3f(bone.getPivotX() / 16F, bone.getPivotY() / 16F, bone.getPivotZ() / 16F);
            new Matrix4f(inverseBase).mul(p.last().pose()).transformPosition(point);
            p.popPose();
            effects(b, point);
        }
        super.renderRecursively(p, b, bone, type, buffers, v, again, partial, light, overlay, r, g, blue, a);
    }

    private static void effects(EntropyWellBlockEntity b, Vector3f point) {
        if (!(b.getLevel() instanceof ClientLevel level)) return;
        var mc = Minecraft.getInstance();
        var pos = b.getBlockPos();
        long tick = level.getGameTime();
        if (mc.isPaused()) return;
        if (budgetLevel != level || budgetTick != tick) {
            budgetLevel = level;
            budgetTick = tick;
            bubbles = 0;
            waves = 0;
        }
        boolean punch = b.punchPending;
        b.punchPending = false;
        if (mc.gameRenderer.getMainCamera().getPosition().distanceToSqr(pos.getX() + .5, pos.getY() + 2.5, pos.getZ() + .5) > 32 * 32)
            return;
        if (tick >= b.nextBubble) {
            b.nextBubble = tick + 4 + level.random.nextInt(5);
            if (bubbles++ < 16)
                level.addParticle(Oasiso.ENTROPY_BULB.get(), pos.getX() + point.x + (level.random.nextDouble() - .5) * .18, pos.getY() + point.y + .06, pos.getZ() + point.z + (level.random.nextDouble() - .5) * .18, 0, 0, 0);
        }
        if (punch && (b.lastPunch == Long.MIN_VALUE || tick - b.lastPunch >= 20)) {
            b.lastPunch = tick;
            if (waves++ < 2) {
                GeyserSmokeParticle.spawnWellShockwave(level, pos.getX() + .5, pos.getY() + .10, pos.getZ() + .5);
                level.playLocalSound(
                        pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D,
                        ModSounds.WELL_PUNCH.get(),
                        SoundSource.BLOCKS,
                        1.0F,
                        0.75F + level.random.nextFloat() * 0.5F,
                        false
                );
            }
        }
    }
}
