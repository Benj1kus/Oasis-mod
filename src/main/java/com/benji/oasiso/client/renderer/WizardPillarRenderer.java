package com.benji.oasiso.client.renderer;

import com.benji.oasiso.Oasiso;
import com.benji.oasiso.common.entity.WizardPillarEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import com.benji.oasiso.client.sound.WizardPillarHealingSound;
import net.minecraft.client.Minecraft;

import java.util.Map;
import java.util.WeakHashMap;

public class WizardPillarRenderer extends EntityRenderer<WizardPillarEntity> {
    //beam
    private static final ResourceLocation CHAOS_BEAM_TEXTURE = ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "textures/entity/chaos_beam.png");
    private static final double EYE_CENTER_Y = 4.5D;

    private static final int FULL_BRIGHT = 0xF000F0;
    private static final int BEAM_SIDES = 8;
    private static final double OUTER_BEAM_RADIUS = 0.16D;
    private static final double INNER_BEAM_RADIUS = 0.065D;
    private static final long FLASH_DURATION_NS = 320_000_000L;

    private final Map<WizardPillarEntity, FlashState> flashStates = new WeakHashMap<>();
    private final Map<WizardPillarEntity, WizardPillarHealingSound> healingSounds = new WeakHashMap<>();

    public WizardPillarRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.0F;
    }

    @Override
    public void render(WizardPillarEntity pillar, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        renderSegmentFlash(pillar, poseStack, bufferSource);
        LivingEntity target = getHealTarget(pillar);

        boolean beamActive = target != null && pillar.getVisibleHeight() >= 5 && !pillar.isCollapsing();
        updateHealingSound(pillar, beamActive);

        if (beamActive) {
            renderChaosBeam(pillar, target, partialTick, poseStack, bufferSource);
            renderHealingAura(pillar, target, partialTick, poseStack, bufferSource);
        }
        super.render(pillar, entityYaw, partialTick, poseStack, bufferSource, packedLight);
    }

    private void updateHealingSound(WizardPillarEntity pillar, boolean beamActive) {
        WizardPillarHealingSound current = this.healingSounds.get(pillar);
        if (!beamActive) {
            if (current != null && current.isStopped()) {
                this.healingSounds.remove(pillar);
            }

            return;
        }
        if (current != null && !current.isStopped()) {
            return;
        }

        WizardPillarHealingSound sound = new WizardPillarHealingSound(pillar);
        this.healingSounds.put(pillar, sound);
        Minecraft.getInstance().getSoundManager().play(sound);
    }
    private LivingEntity getHealTarget(WizardPillarEntity pillar) {
        int id = pillar.getHealTargetId();
        if (id < 0) {
            return null;
        }

        Entity entity = pillar.level().getEntity(id);

        if (!(entity instanceof LivingEntity living)) {
            return null;
        }

        if (!living.isAlive()) {
            return null;
        }
        return living;
    }

    private void renderChaosBeam(WizardPillarEntity pillar, LivingEntity target, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource) {

        double pillarX = Mth.lerp(partialTick, pillar.xOld, pillar.getX());
        double pillarY = Mth.lerp(partialTick, pillar.yOld, pillar.getY());
        double pillarZ = Mth.lerp(partialTick, pillar.zOld, pillar.getZ());

        double targetX = Mth.lerp(partialTick, target.xOld, target.getX());
        double targetY = Mth.lerp(partialTick, target.yOld, target.getY());
        double targetZ = Mth.lerp(partialTick, target.zOld, target.getZ());

        Vec3 start = new Vec3(0.0D, EYE_CENTER_Y, 0.0D);
        Vec3 end = new Vec3(targetX - pillarX, targetY - pillarY + target.getBbHeight() * 0.55D, targetZ - pillarZ);
        Vec3 beamVector = end.subtract(start);

        double beamLength = beamVector.length();
        if (beamLength < 0.01D) {

            return;
        }

        float time = pillar.tickCount + partialTick;
        float textureOffset = -time * 0.035F;
        float textureEnd = textureOffset + (float) beamLength * 0.45F;

        VertexConsumer consumer = bufferSource.getBuffer(RenderType.entityTranslucent(CHAOS_BEAM_TEXTURE));

        Matrix4f matrix = poseStack.last().pose();
        Matrix3f normalMatrix = poseStack.last().normal();

        drawBeamTube(consumer, matrix, normalMatrix, start, end, OUTER_BEAM_RADIUS, 50, 255, 215, 155, textureOffset, textureEnd);
        drawBeamTube(consumer, matrix, normalMatrix, start, end, INNER_BEAM_RADIUS, 205, 255, 250, 225, textureOffset + 0.16F, textureEnd + 0.16F);
    }

    private void drawBeamTube(VertexConsumer consumer, Matrix4f matrix, Matrix3f normalMatrix, Vec3 start, Vec3 end, double radius, int red, int green, int blue, int alpha, float vStart, float vEnd) {

        Vec3 delta = end.subtract(start);
        double length = delta.length();

        if (length < 0.0001D) {
            return;
        }

        Vec3 axis = delta.scale(1.0D / length);
        Vec3 reference;

        if (Math.abs(axis.y) > 0.92D) {
            reference = new Vec3(1.0D, 0.0D, 0.0D);

        } else {
            reference = new Vec3(0.0D, 1.0D, 0.0D);
        }

        Vec3 side = axis.cross(reference);
        if (side.lengthSqr() < 0.000001D) {
            return;
        }

        side = side.normalize();
        Vec3 up = side.cross(axis).normalize();

        for (int i = 0; i < BEAM_SIDES; i++) {

            double angle1 = Math.PI * 2.0D * i / BEAM_SIDES;
            double angle2 = Math.PI * 2.0D * (i + 1) / BEAM_SIDES;

            Vec3 offset1 = getRingOffset(side, up, angle1, radius);
            Vec3 offset2 = getRingOffset(side, up, angle2, radius);

            Vec3 first = start.add(offset1);
            Vec3 second = start.add(offset2);
            Vec3 third = end.add(offset2);
            Vec3 fourth = end.add(offset1);
            Vec3 normal = offset1.add(offset2).normalize();

            addBeamVertex(consumer, matrix, normalMatrix, first, 0.0F, vStart, red, green, blue, alpha, normal);
            addBeamVertex(consumer, matrix, normalMatrix, second, 1.0F, vStart, red, green, blue, alpha, normal);
            addBeamVertex(consumer, matrix, normalMatrix, third, 1.0F, vEnd, red, green, blue, alpha, normal);
            addBeamVertex(consumer, matrix, normalMatrix, fourth, 0.0F, vEnd, red, green, blue, alpha, normal);
        }
    }

    private Vec3 getRingOffset(Vec3 side, Vec3 up, double angle, double radius) {
        return side.scale(Math.cos(angle) * radius).add(up.scale(Math.sin(angle) * radius));
    }

    private void addBeamVertex(VertexConsumer consumer, Matrix4f matrix, Matrix3f normalMatrix, Vec3 position, float u, float v, int red, int green, int blue, int alpha, Vec3 normal) {
        consumer.vertex(matrix, (float) position.x, (float) position.y, (float) position.z).color(red, green, blue, alpha).uv(u, v).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(FULL_BRIGHT).normal(normalMatrix, (float) normal.x, (float) normal.y, (float) normal.z).endVertex();
    }

    private void renderHealingAura(WizardPillarEntity pillar, LivingEntity target, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource) {
        double pillarX = Mth.lerp(partialTick, pillar.xOld, pillar.getX());
        double pillarY = Mth.lerp(partialTick, pillar.yOld, pillar.getY());
        double pillarZ = Mth.lerp(partialTick, pillar.zOld, pillar.getZ());

        double targetX = Mth.lerp(partialTick, target.xOld, target.getX());
        double targetY = Mth.lerp(partialTick, target.yOld, target.getY());
        double targetZ = Mth.lerp(partialTick, target.zOld, target.getZ());

        double localX = targetX - pillarX;
        double localY = targetY - pillarY;
        double localZ = targetZ - pillarZ;

        float time = pillar.tickCount + partialTick;
        float pulse = 0.5F + 0.5F * Mth.sin(time * 0.22F);

        VertexConsumer consumer = bufferSource.getBuffer(RenderType.lightning());
        Matrix4f matrix = poseStack.last().pose();

        double entityHalfWidth = target.getBbWidth() * 0.5D;
        double entityHeight = target.getBbHeight();
        double innerExpand = 0.12D + pulse * 0.035D;

        drawEnergyBox(consumer, matrix, localX - entityHalfWidth - innerExpand, localY - 0.08D, localZ - entityHalfWidth - innerExpand, localX + entityHalfWidth + innerExpand, localY + entityHeight + 0.08D, localZ + entityHalfWidth + innerExpand, 40, 255, 215, 38 + (int) (pulse * 32.0F));

        float secondPulse = 0.5F + 0.5F * Mth.sin(time * 0.18F + 2.1F);
        double outerExpand = 0.24D + secondPulse * 0.055D;

        drawEnergyBox(consumer, matrix, localX - entityHalfWidth - outerExpand, localY - 0.14D, localZ - entityHalfWidth - outerExpand, localX + entityHalfWidth + outerExpand, localY + entityHeight + 0.14D, localZ + entityHalfWidth + outerExpand, 70, 225, 255, 18 + (int) (secondPulse * 23.0F));

        drawAuraSquares(consumer, matrix, target, localX, localY, localZ, time);
    }

    private void drawAuraSquares(VertexConsumer consumer, Matrix4f matrix, LivingEntity target, double centerX, double baseY, double centerZ, float time) {

        final int squareCount = 14;

        double radius = target.getBbWidth() * 0.5D + 0.24D;
        double height = target.getBbHeight();

        for (int i = 0; i < squareCount; i++) {

            double direction = i % 2 == 0 ? 1.0D : -1.0D;
            double angle = i * 2.399963D + time * 0.045D * direction;
            double localRadius = radius + Math.sin(time * 0.08D + i * 1.7D) * 0.06D;
            double x = centerX + Math.cos(angle) * localRadius;
            double z = centerZ + Math.sin(angle) * localRadius;
            double normalizedHeight = (i + 0.5D) / squareCount;
            double y = baseY + normalizedHeight * height + Math.sin(time * 0.11D + i * 1.31D) * 0.16D;
            double size = 0.045D + (i % 3) * 0.014D;

            Vec3 horizontal = new Vec3(-Math.sin(angle), 0.0D, Math.cos(angle)).scale(size);
            Vec3 vertical = new Vec3(0.0D, size, 0.0D);
            Vec3 center = new Vec3(x, y, z);
            Vec3 first = center.subtract(horizontal).subtract(vertical);
            Vec3 second = center.add(horizontal).subtract(vertical);
            Vec3 third = center.add(horizontal).add(vertical);
            Vec3 fourth = center.subtract(horizontal).add(vertical);

            float colorWave = 0.5F + 0.5F * Mth.sin(time * 0.16F + i * 0.9F);

            int red = (int) Mth.lerp(colorWave, 20.0F, 75.0F);
            int green = (int) Mth.lerp(colorWave, 235.0F, 255.0F);
            int blue = (int) Mth.lerp(colorWave, 185.0F, 255.0F);
            int alpha = (int) Mth.lerp(colorWave, 90.0F, 210.0F);

            addDoubleSidedColorQuad(consumer, matrix, first, second, third, fourth, red, green, blue, alpha);
        }
    }

    private void renderSegmentFlash(WizardPillarEntity pillar, PoseStack poseStack, MultiBufferSource bufferSource) {
        FlashState state = this.flashStates.computeIfAbsent(pillar, entity -> new FlashState());

        int serial = pillar.getFlashSerial();

        if (serial != state.lastSerial) {
            state.lastSerial = serial;
            state.level = pillar.getFlashLevel();
            state.mode = pillar.getFlashMode();
            state.startedAt = System.nanoTime();
        }

        if (state.level <= 0 || state.startedAt == 0L) {
            return;
        }

        long now = System.nanoTime();

        float progress = (now - state.startedAt) / (float) FLASH_DURATION_NS;

        if (progress >= 1.0F) {
            return;
        }

        progress = Mth.clamp(progress, 0.0F, 1.0F);

        float fade = 1.0F - progress;
        float eased = 1.0F - (1.0F - progress) * (1.0F - progress) * (1.0F - progress);

        double halfSize;

        if (state.mode == 1) {

            halfSize = Mth.lerp(eased, 0.76D, 0.515D);
        } else {
            halfSize = Mth.lerp(eased, 0.515D, 0.78D);
        }

        double centerY = state.level - 0.5D;

        VertexConsumer consumer = bufferSource.getBuffer(RenderType.lightning());

        Matrix4f matrix = poseStack.last().pose();

        int alpha = (int) (fade * 205.0F);

        drawEnergyBox(consumer, matrix, -halfSize, centerY - halfSize, -halfSize, halfSize, centerY + halfSize, halfSize, 30, 255, 205, alpha);

        double brightHalf = 0.525D + fade * 0.055D;

        drawEnergyBox(consumer, matrix, -brightHalf, centerY - brightHalf, -brightHalf, brightHalf, centerY + brightHalf, brightHalf, 185, 255, 245, (int) (fade * 110.0F));
    }

    private void drawEnergyBox(VertexConsumer consumer, Matrix4f matrix, double minX, double minY, double minZ, double maxX, double maxY, double maxZ, int red, int green, int blue, int alpha) {
        addDoubleSidedColorQuad(consumer, matrix, new Vec3(minX, minY, maxZ), new Vec3(maxX, minY, maxZ), new Vec3(maxX, maxY, maxZ), new Vec3(minX, maxY, maxZ), red, green, blue, alpha);
        // back
        addDoubleSidedColorQuad(consumer, matrix, new Vec3(maxX, minY, minZ), new Vec3(minX, minY, minZ), new Vec3(minX, maxY, minZ), new Vec3(maxX, maxY, minZ), red, green, blue, alpha);
        // left
        addDoubleSidedColorQuad(consumer, matrix, new Vec3(minX, minY, minZ), new Vec3(minX, minY, maxZ), new Vec3(minX, maxY, maxZ), new Vec3(minX, maxY, minZ), red, green, blue, alpha);
        // right
        addDoubleSidedColorQuad(consumer, matrix, new Vec3(maxX, minY, maxZ), new Vec3(maxX, minY, minZ), new Vec3(maxX, maxY, minZ), new Vec3(maxX, maxY, maxZ), red, green, blue, alpha);
        // top
        addDoubleSidedColorQuad(consumer, matrix, new Vec3(minX, maxY, maxZ), new Vec3(maxX, maxY, maxZ), new Vec3(maxX, maxY, minZ), new Vec3(minX, maxY, minZ), red, green, blue, alpha);
        // bottom
        addDoubleSidedColorQuad(consumer, matrix, new Vec3(minX, minY, minZ), new Vec3(maxX, minY, minZ), new Vec3(maxX, minY, maxZ), new Vec3(minX, minY, maxZ), red, green, blue, alpha);
    }

    private void addDoubleSidedColorQuad(VertexConsumer consumer, Matrix4f matrix, Vec3 first, Vec3 second, Vec3 third, Vec3 fourth, int red, int green, int blue, int alpha) {
        //front
        addColorVertex(consumer, matrix, first, red, green, blue, alpha);
        addColorVertex(consumer, matrix, second, red, green, blue, alpha);
        addColorVertex(consumer, matrix, third, red, green, blue, alpha);
        addColorVertex(consumer, matrix, fourth, red, green, blue, alpha);

        //back
        addColorVertex(consumer, matrix, fourth, red, green, blue, alpha);
        addColorVertex(consumer, matrix, third, red, green, blue, alpha);
        addColorVertex(consumer, matrix, second, red, green, blue, alpha);
        addColorVertex(consumer, matrix, first, red, green, blue, alpha);
    }

    private void addColorVertex(VertexConsumer consumer, Matrix4f matrix, Vec3 position, int red, int green, int blue, int alpha) {
        consumer.vertex(matrix, (float) position.x, (float) position.y, (float) position.z).color(red, green, blue, alpha).endVertex();
    }
// culling
    @Override
    public boolean shouldRender(WizardPillarEntity entity, Frustum camera, double camX, double camY, double camZ) {
        return true;
    }

    @Override
    public ResourceLocation getTextureLocation(WizardPillarEntity entity) {
        return CHAOS_BEAM_TEXTURE;
    }

    private static final class FlashState {
        private int lastSerial = -1;
        private int level = -1;
        private int mode;
        private long startedAt;
    }
}