package com.benji.oasiso.client.layer;

import com.benji.oasiso.Oasiso;
import com.benji.oasiso.common.entity.CrusaderWizardEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3d;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

import java.util.Map;
import java.util.WeakHashMap;

public class CrusaderWizardMagicParticlesLayer extends GeoRenderLayer<CrusaderWizardEntity> {

    private static final int LANTERN_INTERVAL = 2;
    private static final int SOUL_INTERVAL = 2;

    private final Map<CrusaderWizardEntity, ParticleState> states = new WeakHashMap<>();

    private final RandomSource random = RandomSource.create();

    public CrusaderWizardMagicParticlesLayer(GeoRenderer<CrusaderWizardEntity> renderer) {
        super(renderer);
    }

    @Override
    public void render(PoseStack poseStack, CrusaderWizardEntity animatable, BakedGeoModel bakedModel, RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer, float partialTick, int packedLight, int packedOverlay) {
        if (!(animatable.level() instanceof ClientLevel level)) {
            return;
        }

        if (!animatable.isAlive()) {
            return;
        }

        ParticleState state = this.states.computeIfAbsent(animatable, entity -> new ParticleState());

        Vec3 lanternPosition = getBonePosition(bakedModel, "lan_particles");

        if (lanternPosition != null && animatable.tickCount - state.lastLanternTick >= LANTERN_INTERVAL) {
            state.lastLanternTick = animatable.tickCount;
            spawnLanternStar(level, lanternPosition);
        }

        Vec3 smokePosition = getBonePosition(bakedModel, "magic_smoke");

        if (smokePosition != null && animatable.tickCount - state.lastSoulTick >= SOUL_INTERVAL) {
            state.lastSoulTick = animatable.tickCount;
            spawnMagicSoul(level, smokePosition);
        }
    }

    private void spawnLanternStar(ClientLevel level, Vec3 position) {

        double velocityX = (this.random.nextDouble() - 0.5D) * 0.018D;
        double velocityY = 0.008D + this.random.nextDouble() * 0.018D;
        double velocityZ = (this.random.nextDouble() - 0.5D) * 0.018D;

        double offsetX = (this.random.nextDouble() - 0.5D) * 0.08D;
        double offsetY = (this.random.nextDouble() - 0.5D) * 0.06D;
        double offsetZ = (this.random.nextDouble() - 0.5D) * 0.08D;

        level.addParticle(Oasiso.PURPLE_STARS.get(), position.x + offsetX, position.y + offsetY, position.z + offsetZ, velocityX, velocityY, velocityZ);
    }

    private void spawnMagicSoul(ClientLevel level, Vec3 position) {

        double velocityX = (this.random.nextDouble() - 0.5D) * 0.012D;
        double velocityY = 0.035D + this.random.nextDouble() * 0.035D;
        double velocityZ = (this.random.nextDouble() - 0.5D) * 0.012D;

        double offsetX = (this.random.nextDouble() - 0.5D) * 0.10D;
        double offsetZ = (this.random.nextDouble() - 0.5D) * 0.10D;

        level.addParticle(ParticleTypes.SOUL, position.x + offsetX, position.y, position.z + offsetZ, velocityX, velocityY, velocityZ);
    }

    private Vec3 getBonePosition(BakedGeoModel model, String boneName) {
        GeoBone bone = model.getBone(boneName).orElse(null);
        if (bone == null) {
            return null;
        }
        Vector3d position = bone.getWorldPosition();
        return new Vec3(position.x, position.y, position.z);
    }

    private static final class ParticleState {
        private int lastLanternTick = Integer.MIN_VALUE / 2;
        private int lastSoulTick = Integer.MIN_VALUE / 2;
    }
}