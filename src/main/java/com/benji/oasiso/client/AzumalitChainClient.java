package com.benji.oasiso.client;

import com.benji.oasiso.Oasiso;
import com.benji.oasiso.common.chain.AzumalitChainManager;
import com.benji.oasiso.common.item.AzumalitArmorItem;
import com.benji.oasiso.network.AzumalitChainRequestPacket;
import com.benji.oasiso.network.AzumalitChainSyncPacket;
import com.benji.oasiso.network.ModMessages;
import com.benji.oasiso.registry.ModItems;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Mod.EventBusSubscriber(modid = Oasiso.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class AzumalitChainClient {

    private static final KeyMapping CHAIN_KEY = new KeyMapping("key.oasiso.azumalit_chain", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_H, "key.categories.oasiso");

    private static final ResourceLocation[] HUD_FRAMES = {ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "textures/gui/chain_attack1.png"), ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "textures/gui/chain_attack2.png"), ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "textures/gui/chain_attack3.png"), ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "textures/gui/chain_attack4.png"), ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "textures/gui/chain_attack5.png")};

    private static final int HUD_SIZE = 16;
    private static final int HUD_FRAME_TICKS = 3;
    private static final int READY_HUD_TICKS = 40;

    private static final int BEAM_SEGMENTS = 28;
    private static final int BEAM_SIDES = 6;

    private static final Map<Integer, ChainVisual> VISUALS = new HashMap<>();

    private static ClientLevel lastLevel;
    private static boolean attackHudActive;
    private static int readyHudTicks;
    private static boolean wasChestplateOnCooldown;

    private AzumalitChainClient() {
    }

    public static void handleSync(byte phase, int ownerEntityId, long chainStartGameTime, List<Integer> targetEntityIds) {
        Minecraft minecraft = Minecraft.getInstance();

        if (phase == AzumalitChainSyncPacket.PHASE_CAST_STARTED) {
            if (minecraft.player != null && minecraft.player.getId() == ownerEntityId) {
                attackHudActive = true;
                readyHudTicks = 0;
            }
            return;
        }

        if (phase == AzumalitChainSyncPacket.PHASE_CHAIN_STARTED) {
            if (!targetEntityIds.isEmpty()) {
                VISUALS.put(ownerEntityId, new ChainVisual(ownerEntityId, chainStartGameTime, new ArrayList<>(targetEntityIds)));
            }
            return;
        }

        if (phase == AzumalitChainSyncPacket.PHASE_STOPPED) {
            VISUALS.remove(ownerEntityId);

            if (minecraft.player != null && minecraft.player.getId() == ownerEntityId) {
                attackHudActive = false;
            }
        }
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;

        if (level == null || minecraft.player == null) {
            VISUALS.clear();
            attackHudActive = false;
            readyHudTicks = 0;
            wasChestplateOnCooldown = false;
            lastLevel = null;
            return;
        }

        if (lastLevel != level) {
            VISUALS.clear();
            attackHudActive = false;
            readyHudTicks = 0;
            wasChestplateOnCooldown = false;
            lastLevel = level;
        }

        while (CHAIN_KEY.consumeClick()) {
            if (minecraft.screen != null || !AzumalitArmorItem.isWearingFullSet(minecraft.player) || minecraft.player.getCooldowns().isOnCooldown(ModItems.AZUMALIT_CHESTPLATE.get()) || AzumalitArmorItem.isChainAnimationActive(minecraft.player)) {
                continue;
            }

            ModMessages.sendToServer(new AzumalitChainRequestPacket());
        }

        boolean cooldownNow = minecraft.player.getCooldowns().isOnCooldown(ModItems.AZUMALIT_CHESTPLATE.get());

        if (wasChestplateOnCooldown && !cooldownNow) {
            readyHudTicks = READY_HUD_TICKS;
        }

        wasChestplateOnCooldown = cooldownNow;

        if (readyHudTicks > 0 && !attackHudActive) {
            readyHudTicks--;
        }

        long gameTime = level.getGameTime();
        VISUALS.entrySet().removeIf(entry -> gameTime > entry.getValue().expireAt());
        spawnTargetSmoke(level, gameTime);
    }

    private static void spawnTargetSmoke(ClientLevel level, long gameTime) {
        if (gameTime % 3L != 0L) {
            return;
        }

        for (ChainVisual visual : VISUALS.values()) {
            for (int targetId : visual.targetEntityIds) {
                Entity raw = level.getEntity(targetId);

                if (!(raw instanceof LivingEntity target) || !target.isAlive()) {
                    continue;
                }

                AABB box = target.getBoundingBox();

                for (int i = 0; i < 2; i++) {
                    double x = Mth.lerp(level.random.nextDouble(), box.minX, box.maxX);
                    double y = Mth.lerp(level.random.nextDouble(), box.minY + 0.08D, box.maxY - 0.04D);
                    double z = Mth.lerp(level.random.nextDouble(), box.minZ, box.maxZ);

                    double vx = (level.random.nextDouble() - 0.5D) * 0.014D;
                    double vy = 0.012D + level.random.nextDouble() * 0.018D;
                    double vz = (level.random.nextDouble() - 0.5D) * 0.014D;

                    level.addParticle(Oasiso.MOUTH_SMOKE.get(), x, y, z, vx, vy, vz);
                }
            }
        }
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;

        if (level == null || VISUALS.isEmpty()) {
            return;
        }

        MultiBufferSource.BufferSource bufferSource = minecraft.renderBuffers().bufferSource();

        VertexConsumer consumer = bufferSource.getBuffer(RenderType.lightning());

        PoseStack poseStack = event.getPoseStack();
        Vec3 camera = event.getCamera().getPosition();
        float partialTick = event.getPartialTick();
        double gameTime = level.getGameTime() + partialTick;

        poseStack.pushPose();
        poseStack.translate(-camera.x, -camera.y, -camera.z);

        Matrix4f matrix = poseStack.last().pose();

        for (ChainVisual visual : VISUALS.values()) {
            drawChain(consumer, matrix, level, visual, gameTime, partialTick);
        }

        poseStack.popPose();
        bufferSource.endBatch(RenderType.lightning());
    }

    private static void drawChain(VertexConsumer consumer, Matrix4f matrix, ClientLevel level, ChainVisual visual, double gameTime, float partialTick) {
        if (visual.targetEntityIds.size() < 2) {
            return;
        }

        for (int linkIndex = 1; linkIndex < visual.targetEntityIds.size(); linkIndex++) {
            Entity rawStart = level.getEntity(visual.targetEntityIds.get(linkIndex - 1));
            Entity rawEnd = level.getEntity(visual.targetEntityIds.get(linkIndex));

            if (!(rawStart instanceof LivingEntity start) || !(rawEnd instanceof LivingEntity end) || !start.isAlive() || !end.isAlive()) {
                continue;
            }

            double linkStart = visual.chainStartGameTime + AzumalitChainManager.getTargetActivationOffset(linkIndex - 1);
            int linkBuildTicks = AzumalitChainManager.getLinkBuildTicks(linkIndex);

            float reveal = Mth.clamp((float) ((gameTime - linkStart) / linkBuildTicks), 0.0F, 1.0F);

            if (reveal <= 0.0F) {
                continue;
            }

            reveal = smoothstep(reveal);

            Vec3 startPos = entityCenter(start, partialTick);
            Vec3 endPos = entityCenter(end, partialTick);

            drawEnergyBeam(consumer, matrix, startPos, endPos, reveal, gameTime / 20.0D, linkIndex);
        }
    }

    private static void drawEnergyBeam(VertexConsumer consumer, Matrix4f matrix, Vec3 start, Vec3 end, float reveal, double time, int linkIndex) {
        Vec3 fullDirection = end.subtract(start);
        double fullLength = fullDirection.length();

        if (fullLength < 0.02D) {
            return;
        }

        Vec3 axis = fullDirection.scale(1.0D / fullLength);
        Vec3 reference = Math.abs(axis.y) > 0.88D ? new Vec3(1.0D, 0.0D, 0.0D) : new Vec3(0.0D, 1.0D, 0.0D);
        Vec3 waveSide = axis.cross(reference).normalize();
        Vec3 waveUp = waveSide.cross(axis).normalize();

        int visibleSegments = Math.max(1, Mth.ceil(BEAM_SEGMENTS * reveal));

        for (int segment = 0; segment < visibleSegments; segment++) {
            double t0 = segment / (double) BEAM_SEGMENTS;
            double t1 = Math.min(reveal, (segment + 1) / (double) BEAM_SEGMENTS);

            if (t1 <= t0) {
                continue;
            }

            Vec3 p0 = energyPoint(start, end, waveSide, waveUp, t0, time, linkIndex);
            Vec3 p1 = energyPoint(start, end, waveSide, waveUp, t1, time, linkIndex);

            float pulse = 0.5F + 0.5F * Mth.sin((float) (time * 8.0D + linkIndex * 1.37D + t0 * 9.0D));

            double shape0 = energyShape(t0, time, linkIndex);
            double shape1 = energyShape(t1, time, linkIndex);

            double outer0 = (0.105D + pulse * 0.022D) * shape0;
            double outer1 = (0.105D + pulse * 0.022D) * shape1;

            drawTubeSegment(consumer, matrix, p0, p1, outer0, outer1, 20, 215, 135, 105 + (int) (pulse * 55.0F));
            drawTubeSegment(consumer, matrix, p0, p1, outer0 * 0.62D, outer1 * 0.62D, 45, 255, 225, 160 + (int) (pulse * 65.0F));
            drawTubeSegment(consumer, matrix, p0, p1, outer0 * 0.22D, outer1 * 0.22D, 245, 255, 255, 220 + (int) (pulse * 35.0F));
        }
    }

    private static Vec3 energyPoint(Vec3 start, Vec3 end, Vec3 waveSide, Vec3 waveUp, double t, double time, int linkIndex) {
        Vec3 center = start.lerp(end, t);

        double envelope = Math.sin(Math.PI * t);
        double sideWave = Math.sin(time * 7.0D + linkIndex * 1.91D + t * 18.0D) * 0.030D * envelope;
        double upWave = Math.sin(time * 5.2D + linkIndex * 2.43D + t * 13.0D) * 0.020D * envelope;

        return center.add(waveSide.scale(sideWave)).add(waveUp.scale(upWave));
    }

    private static double energyShape(double t, double time, int linkIndex) {
        double travellingBulge = 0.0D;

        for (int i = 0; i < 3; i++) {
            double center = fract(time * (0.55D + i * 0.11D) + linkIndex * 0.173D + i * 0.307D);

            double delta = Math.abs(t - center);
            double wrappedDelta = Math.min(delta, 1.0D - delta);

            travellingBulge += Math.exp(-wrappedDelta * wrappedDelta * 190.0D) * 0.30D;
        }

        double microPulse = 0.94D + 0.06D * Math.sin(time * 10.0D + t * 22.0D + linkIndex);

        return (1.0D + travellingBulge) * microPulse;
    }

    private static void drawTubeSegment(VertexConsumer consumer, Matrix4f matrix, Vec3 p0, Vec3 p1, double radius0, double radius1, int red, int green, int blue, int alpha) {
        if (alpha <= 0) {
            return;
        }

        Vec3 direction = p1.subtract(p0);

        if (direction.lengthSqr() < 0.000001D) {
            return;
        }

        direction = direction.normalize();

        Vec3 reference = Math.abs(direction.y) > 0.90D ? new Vec3(1.0D, 0.0D, 0.0D) : new Vec3(0.0D, 1.0D, 0.0D);

        Vec3 side = direction.cross(reference).normalize();
        Vec3 up = side.cross(direction).normalize();

        for (int i = 0; i < BEAM_SIDES; i++) {
            double a0 = Math.PI * 2.0D * i / BEAM_SIDES;
            double a1 = Math.PI * 2.0D * (i + 1) / BEAM_SIDES;

            Vec3 start0 = tubePoint(p0, side, up, a0, radius0);
            Vec3 start1 = tubePoint(p0, side, up, a1, radius0);
            Vec3 end0 = tubePoint(p1, side, up, a0, radius1);
            Vec3 end1 = tubePoint(p1, side, up, a1, radius1);

            addDoubleSidedQuad(consumer, matrix, start0, start1, end1, end0, red, green, blue, alpha);
        }
    }

    private static Vec3 tubePoint(Vec3 center, Vec3 side, Vec3 up, double angle, double radius) {
        return center.add(side.scale(Math.cos(angle) * radius)).add(up.scale(Math.sin(angle) * radius));
    }

    private static void addDoubleSidedQuad(VertexConsumer consumer, Matrix4f matrix, Vec3 first, Vec3 second, Vec3 third, Vec3 fourth, int red, int green, int blue, int alpha) {
        addVertex(consumer, matrix, first, red, green, blue, alpha);
        addVertex(consumer, matrix, second, red, green, blue, alpha);
        addVertex(consumer, matrix, third, red, green, blue, alpha);
        addVertex(consumer, matrix, fourth, red, green, blue, alpha);

        addVertex(consumer, matrix, fourth, red, green, blue, alpha);
        addVertex(consumer, matrix, third, red, green, blue, alpha);
        addVertex(consumer, matrix, second, red, green, blue, alpha);
        addVertex(consumer, matrix, first, red, green, blue, alpha);
    }

    private static void addVertex(VertexConsumer consumer, Matrix4f matrix, Vec3 position, int red, int green, int blue, int alpha) {
        consumer.vertex(matrix, (float) position.x, (float) position.y, (float) position.z).color(red, green, blue, Mth.clamp(alpha, 0, 255)).endVertex();
    }

    public static List<Integer> getOutlineTargetIds() {
        if (VISUALS.isEmpty()) {
            return List.of();
        }

        Set<Integer> unique = new LinkedHashSet<>();

        for (ChainVisual visual : VISUALS.values()) {
            unique.addAll(visual.targetEntityIds);
        }

        return List.copyOf(unique);
    }

    public static PulseSnapshot getPulseSnapshot(double gameTime) {
        if (VISUALS.isEmpty()) {
            return PulseSnapshot.EMPTY;
        }

        Set<Integer> currentTargets = new LinkedHashSet<>();
        float strongestAlpha = 0.0F;

        for (ChainVisual visual : VISUALS.values()) {
            int targetIndex = getPulseTargetIndex(visual, gameTime);

            if (targetIndex < 0 || targetIndex >= visual.targetEntityIds.size()) {
                continue;
            }

            float alpha = getPulseAlpha(visual, targetIndex, gameTime);

            if (alpha <= 0.001F) {
                continue;
            }

            currentTargets.add(visual.targetEntityIds.get(targetIndex));
            strongestAlpha = Math.max(strongestAlpha, alpha);
        }

        if (currentTargets.isEmpty()) {
            return PulseSnapshot.EMPTY;
        }

        return new PulseSnapshot(List.copyOf(currentTargets), strongestAlpha);
    }

    private static int getPulseTargetIndex(ChainVisual visual, double gameTime) {
        int count = visual.targetEntityIds.size();

        if (count <= 0 || gameTime < visual.chainStartGameTime) {
            return -1;
        }

        double buildFinishAt = visual.buildFinishAt();
        double pulseFinishAt = buildFinishAt + AzumalitChainManager.AFTER_BUILD_HOLD_TICKS;

        if (gameTime >= pulseFinishAt) {
            return -1;
        }

        if (gameTime >= buildFinishAt) {
            return count - 1;
        }

        for (int index = count - 1; index >= 0; index--) {
            double activationAt = visual.chainStartGameTime + AzumalitChainManager.getTargetActivationOffset(index);

            if (gameTime >= activationAt) {
                return index;
            }
        }

        return -1;
    }

    private static float getPulseAlpha(ChainVisual visual, int targetIndex, double gameTime) {
        double startedAt = visual.chainStartGameTime + AzumalitChainManager.getTargetActivationOffset(targetIndex);
        double elapsed = gameTime - startedAt;

        double durationTicks = Math.max(2.25D, 6.0D - targetIndex * 0.40D);

        if (elapsed < 0.0D || elapsed > durationTicks) {
            return 0.0F;
        }

        float progress = Mth.clamp((float) (elapsed / durationTicks), 0.0F, 1.0F);
        float wave = Mth.sin(progress * Mth.PI);
        wave = smoothstep(wave);

        return wave * 0.84F;
    }

    public record PulseSnapshot(List<Integer> targetEntityIds, float alpha) {
        private static final PulseSnapshot EMPTY = new PulseSnapshot(List.of(), 0.0F);
    }

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        if (!attackHudActive && readyHudTicks <= 0) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();

        if (minecraft.player == null || minecraft.options.hideGui) {
            return;
        }

        int frame = Math.floorMod(minecraft.player.tickCount / HUD_FRAME_TICKS, HUD_FRAMES.length);
        float alpha = 1.0F;

        if (!attackHudActive) {
            float progress = 1.0F - readyHudTicks / (float) READY_HUD_TICKS;
            alpha = 1.0F - smoothstep(progress);
        }

        GuiGraphics graphics = event.getGuiGraphics();
        int x = (event.getWindow().getGuiScaledWidth() - HUD_SIZE) / 2;
        int y = event.getWindow().getGuiScaledHeight() - 72;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, alpha);

        graphics.blit(HUD_FRAMES[frame], x, y, 0, 0, HUD_SIZE, HUD_SIZE, HUD_SIZE, HUD_SIZE);

        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.disableBlend();
    }

    private static Vec3 entityCenter(LivingEntity entity, float partialTick) {
        double x = Mth.lerp(partialTick, entity.xOld, entity.getX());
        double y = Mth.lerp(partialTick, entity.yOld, entity.getY());
        double z = Mth.lerp(partialTick, entity.zOld, entity.getZ());

        return new Vec3(x, y + entity.getBbHeight() * 0.55D, z);
    }

    private static float smoothstep(float value) {
        value = Mth.clamp(value, 0.0F, 1.0F);
        return value * value * (3.0F - 2.0F * value);
    }

    private static double fract(double value) {
        return value - Math.floor(value);
    }

    private static final class ChainVisual {
        private final int ownerEntityId;
        private final long chainStartGameTime;
        private final List<Integer> targetEntityIds;

        private ChainVisual(int ownerEntityId, long chainStartGameTime, List<Integer> targetEntityIds) {
            this.ownerEntityId = ownerEntityId;
            this.chainStartGameTime = chainStartGameTime;
            this.targetEntityIds = List.copyOf(targetEntityIds);
        }

        private double buildFinishAt() {
            return this.chainStartGameTime + AzumalitChainManager.getBuildDurationTicks(this.targetEntityIds.size());
        }

        private long expireAt() {
            long buildTicks = AzumalitChainManager.getBuildDurationTicks(this.targetEntityIds.size());

            long damageTicks = (long) this.targetEntityIds.size() * AzumalitChainManager.DAMAGE_STEP_TICKS;

            return this.chainStartGameTime + buildTicks + AzumalitChainManager.AFTER_BUILD_HOLD_TICKS + damageTicks + AzumalitChainManager.AFTER_DAMAGE_HOLD_TICKS + 40L;
        }
    }

    @Mod.EventBusSubscriber(modid = Oasiso.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static final class ModBusEvents {
        private ModBusEvents() {
        }

        @SubscribeEvent
        public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
            event.register(CHAIN_KEY);
        }
    }
}
