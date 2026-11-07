package com.benji.oasiso.client.soulcast;

import com.benji.oasiso.ModSounds;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.*;

@Mod.EventBusSubscriber(modid = "oasiso", value = Dist.CLIENT)
public final class SoulStrikeEffects {
    static final int VIEW_DISTANCE_SQR = 64 * 64;
    private static final int GROW = 11, RETRACT_AT = 100, RETRACT = 14, SHARDS = 48;
    private static final double GROUND_DELAY = 1.25;
    private static final List<Burst> bursts = new ArrayList<>();
    private static ClientLevel world;
    private static double flashEnd = Double.NEGATIVE_INFINITY;
    private static final int GLINT_RADIUS = 2 * SoulCrackPattern.SCALE;
    private static final int GLINT_ARMS = 3;
    private static final double GLINT_TURN = Math.toRadians(34);

    private record GlintPixel(int x, int z, float height, double radius, double cosPhase, double sinPhase,
                              double threshold) {
    }

    private static void syncWorld(ClientLevel level) {
        if (world == level) return;
        world = level;
        bursts.clear();
        flashEnd = Double.NEGATIVE_INFINITY;
    }

    static void hit(AbstractClientPlayer actor, long start, double markerTick) {
        Minecraft mc = Minecraft.getInstance();
        syncWorld(mc.level);
        if (world == null || actor.level() != world || mc.player == null || actor.distanceToSqr(mc.player) > VIEW_DISTANCE_SQR)
            return;
        double when = start + markerTick, now = world.getGameTime() + mc.getFrameTime();
        if (now - when >= RETRACT_AT + RETRACT) return;
        if (actor == mc.player && now - when < 4) flashEnd = now + 1.75;
        mc.player.playSound(ModSounds.SOUL_IMPACT.get(), 1.0F, 1.0F);
        SoundEvent[] crackSounds = {
                ModSounds.SOUL_CRACK1.get(),
                ModSounds.SOUL_CRACK2.get(),
                ModSounds.SOUL_CRACK3.get()
        };
        SoundEvent randomCrack = crackSounds[new Random().nextInt(crackSounds.length)];
        mc.player.playSound(randomCrack, 1.0F, 1.0F);
        long seed = actor.getUUID().getLeastSignificantBits() ^ Long.rotateLeft(start, 23);
        bursts.add(new Burst(world, actor.getPosition(mc.getFrameTime()), when, seed));
    }

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        syncWorld(mc.level);
        if (world == null || mc.isPaused()) return;
        double time = world.getGameTime();
        bursts.removeIf(b -> time - b.when >= RETRACT_AT + RETRACT);
        for (Burst b : bursts) b.spawnDue(world, time - b.when);
    }

    @SubscribeEvent
    public static void impactFrame(TickEvent.RenderTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null && mc.level == world && mc.screen == null && !mc.isPaused() && world.getGameTime() + event.renderTickTime < flashEnd)
            SoulImpactFrame.render(mc, event.renderTickTime);
    }

    @SubscribeEvent
    public static void render(RenderLevelStageEvent event) {
        Minecraft mc = Minecraft.getInstance();
        syncWorld(mc.level);
        if (world == null) return;
        double time = world.getGameTime() + event.getPartialTick();
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_CUTOUT_BLOCKS || bursts.isEmpty()) return;
        var buffers = mc.renderBuffers().bufferSource();
        var type = RenderType.debugQuads();
        var cam = event.getCamera().getPosition();
        var out = buffers.getBuffer(type);
        for (Burst b : bursts) {
            if (b.origin.distanceToSqr(cam) <= VIEW_DISTANCE_SQR) b.draw(event.getPoseStack(), out, cam, time - b.when);
        }
        buffers.endBatch(type);
    }

    private record Span(int row, int first, int last, float height, int rgb) {
    }

    private record Shard(double x, double z, SoulStrikeGround.Surface ground, double height, int rise, int delay) {
    }

    private static final class Burst {
        final Vec3 origin;
        final double when, baseX, baseZ;
        final List<Span> spans = new ArrayList<>();
        final List<Shard> shards = new ArrayList<>();
        private final List<GlintPixel> glint = new ArrayList<>();
        int next;

        Burst(ClientLevel level, Vec3 origin, double when, long seed) {
            this.origin = origin;
            this.when = when;
            int scale = SoulCrackPattern.SCALE, half = SoulCrackPattern.HALF;
            baseX = Math.floor(origin.x * scale) / scale;
            baseZ = Math.floor(origin.z * scale) / scale;
            var ground = new SoulStrikeGround(level, origin.y);
            var pattern = new SoulCrackPattern(seed);
            double glintAngle = new Random(seed ^ 0x61C47L).nextDouble() * Math.PI * 2;

            for (int z = -GLINT_RADIUS; z <= GLINT_RADIUS; z++) {
                for (int x = -GLINT_RADIUS; x <= GLINT_RADIUS; x++) {
                    double radius = Math.hypot(x, z) / GLINT_RADIUS;
                    if (radius >= 1) continue;
                    int index = (z + half) * SoulCrackPattern.SIZE + x + half;
                    if (pattern.colors[index] != 0) continue;

                    var top = ground.at(baseX + (x + 0.5) / scale, baseZ + (z + 0.5) / scale);
                    if (top == null) continue;

                    double phase = GLINT_ARMS * (Math.atan2(z, x) - glintAngle - 1.7 * radius);
                    glint.add(new GlintPixel(x, z, (float) (top.y() - origin.y), radius, Math.cos(phase), Math.sin(phase), SoulCrackPattern.ditherThreshold(x, z)));
                }
            }

            for (int z = -half; z <= half; z++) {
                int begin = -half, color = 0;
                float height = 0;
                for (int x = -half; x <= half + 1; x++) {
                    int c = x > half ? 0 : pattern.colors[(z + half) * SoulCrackPattern.SIZE + x + half];
                    float h = 0;
                    if (c != 0) {
                        var top = ground.at(baseX + (x + .5) / scale, baseZ + (z + .5) / scale);
                        if (top == null) c = 0;
                        else h = (float) (top.y() - origin.y);
                    }
                    if (c != color || (c != 0 && h != height)) {
                        if (color != 0) spans.add(new Span(z, begin, x - 1, height, color));
                        begin = x;
                        color = c;
                        height = h;
                    }
                }
            }
            Random rng = new Random(~seed);
            for (int i = 0; i < SHARDS; i++) {
                double r = 5 * Math.sqrt(rng.nextDouble()), a = rng.nextDouble() * Math.PI * 2;
                double x = origin.x + r * Math.cos(a), z = origin.z + r * Math.sin(a);
                var top = ground.at(x, z);
                if (top != null)
                    shards.add(new Shard(x, z, top, 2 + 3 * rng.nextDouble(), 18 + rng.nextInt(9), GROW + 2 + rng.nextInt(6)));
            }
            shards.sort(Comparator.comparingInt(Shard::delay));
        }

        private void drawGlint(org.joml.Matrix4f mat, VertexConsumer out, double progress) {
            double open = progress * progress * (3 - 2 * progress);
            if (open <= 0) return;

            double turn = GLINT_ARMS * GLINT_TURN * open;
            double cosTurn = Math.cos(turn);
            double sinTurn = Math.sin(turn);
            float pixel = 1F / SoulCrackPattern.SCALE;

            for (GlintPixel p : glint) {
                if (p.radius >= open) continue;

                double arm = 0.5 + 0.5 * (p.cosPhase * cosTurn + p.sinPhase * sinTurn);
                double strength = open * (1 - p.radius / open) * (0.28 + 0.72 * arm * arm * arm);
                if (strength <= p.threshold) continue;

                int red = (int) (8 + 65 * strength);
                int green = (int) (168 + 84 * strength);
                int blue = (int) (201 + 54 * strength);

                float x0 = p.x * pixel, x1 = x0 + pixel;
                float z0 = p.z * pixel, z1 = z0 + pixel;
                float y = p.height;

                out.vertex(mat, x0, y, z0).color(red, green, blue, 255).endVertex();
                out.vertex(mat, x0, y, z1).color(red, green, blue, 255).endVertex();
                out.vertex(mat, x1, y, z1).color(red, green, blue, 255).endVertex();
                out.vertex(mat, x1, y, z0).color(red, green, blue, 255).endVertex();
            }
        }


        void spawnDue(ClientLevel level, double age) {
            while (next < shards.size() && shards.get(next).delay <= age) {
                Shard s = shards.get(next++);
                var chip = new SoulBlockChip(level, s.x, s.z, s.ground, s.height, s.rise, (int) (age - s.delay));
                if (chip.isAlive()) Minecraft.getInstance().particleEngine.add(chip);
            }
        }

        void draw(PoseStack pose, VertexConsumer out, Vec3 cam, double age) {
            double progress = Math.min((age - GROUND_DELAY) / GROW, 1 - (age - RETRACT_AT) / RETRACT);
            if (progress <= 0) return;
            progress = Math.min(1, progress);
            double radius = SoulCrackPattern.HALF * progress * (2 - progress);
            pose.pushPose();
            try {
                pose.translate(baseX - cam.x, origin.y - cam.y + .004, baseZ - cam.z);
                var mat = pose.last().pose();
                drawGlint(mat, out, progress);
                for (Span s : spans) {
                    double d = radius * radius - s.row * s.row;
                    if (d < 0) continue;
                    int half = (int) Math.sqrt(d), left = Math.max(-half, s.first), right = Math.min(half, s.last);
                    if (left > right) continue;
                    float x0 = left / 16F, x1 = (right + 1) / 16F, z0 = s.row / 16F, z1 = (s.row + 1) / 16F;
                    int r = s.rgb >> 16, g = (s.rgb >> 8) & 255, b = s.rgb & 255;
                    out.vertex(mat, x0, s.height, z0).color(r, g, b, 255).endVertex();
                    out.vertex(mat, x0, s.height, z1).color(r, g, b, 255).endVertex();
                    out.vertex(mat, x1, s.height, z1).color(r, g, b, 255).endVertex();
                    out.vertex(mat, x1, s.height, z0).color(r, g, b, 255).endVertex();
                }
            } finally {
                pose.popPose();
            }
        }
    }
}
