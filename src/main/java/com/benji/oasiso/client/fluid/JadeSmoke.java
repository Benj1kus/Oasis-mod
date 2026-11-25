package com.benji.oasiso.client.fluid;

import com.benji.oasiso.Oasiso;
import com.benji.oasiso.registry.ModKarakFluids;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.ParticleStatus;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterShadersEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fluids.FluidType;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;

import java.io.IOException;
import java.util.*;

@Mod.EventBusSubscriber(modid = Oasiso.MODID, value = Dist.CLIENT)
public final class JadeSmoke {
    private static final int RADIUS = 36, SIDE = 2 * RADIUS + 1, COLUMNS = SIDE * SIDE;
    private static final int Y_RANGE = 7, COLUMNS_PER_TICK = 128, MAX_PLUMES = 14;
    private static final Map<BlockPos, Tile> tiles = new HashMap<>();
    private static final int MAX_LOW = 240, MAX_RISING = 88;
    private static final List<Puff> puffs = new ArrayList<>();
    private static final List<BlockPos> sources = new ArrayList<>();
    private static int clock;
    private static ClientLevel world;
    private static BlockPos anchor;
    private static int cursor;
    private static ShaderInstance shader;

    private record Tile(double top) {
    }

    @Mod.EventBusSubscriber(modid = Oasiso.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static final class Shaders {
        @SubscribeEvent
        public static void register(RegisterShadersEvent event) throws IOException {
            event.registerShader(new ShaderInstance(event.getResourceProvider(), ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "jade_smoke"), DefaultVertexFormat.POSITION_TEX_COLOR), s -> shader = s);
        }
    }

    private static boolean exposed(BlockPos p) {
        FluidType type = world.getFluidState(p).getFluidType();
        return (type == ModKarakFluids.KR_WATER_TYPE.get() || type == ModKarakFluids.RELAX_WATER_TYPE.get()) && world.getBlockState(p.above()).isAir();
    }


    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != world) {
            tiles.clear();
            sources.clear();
            puffs.clear();
            clock = 0;
            anchor = null;
            world = mc.level;
        }
        if (world == null || mc.player == null || mc.isPaused() || mc.options.particles().get() == ParticleStatus.MINIMAL)
            return;
        BlockPos camera = mc.gameRenderer.getMainCamera().getBlockPosition();
        if (anchor == null || !anchor.equals(camera)) {
            anchor = camera.immutable();
            tiles.keySet().removeIf(p -> Math.abs(p.getX() - anchor.getX()) > RADIUS || Math.abs(p.getZ() - anchor.getZ()) > RADIUS || Math.abs(p.getY() - anchor.getY()) > Y_RANGE);
        }
        for (int n = 0; n < COLUMNS_PER_TICK; n++) {
            cursor = (cursor + 257) % COLUMNS;
            int dx = cursor % SIDE - RADIUS, dz = cursor / SIDE - RADIUS;
            if (dx * dx + dz * dz > RADIUS * RADIUS) continue;
            BlockPos column = anchor.offset(dx, 0, dz);
            if (!world.hasChunkAt(column)) continue;
            for (int dy = -Y_RANGE; dy <= Y_RANGE; dy++) {
                BlockPos p = column.above(dy);
                if (exposed(p)) {
                    tiles.put(p, new Tile(p.getY() + world.getFluidState(p).getHeight(world, p)));
                } else tiles.remove(p);
            }
        }
        clock++;
        if (clock % 10 == 1) {
            sources.clear();
            sources.addAll(tiles.keySet());
        }
        puffs.removeIf(p -> !p.advance());
        if (sources.isEmpty()) return;
        boolean reduced = mc.options.particles().get() == ParticleStatus.DECREASED;
        if (clock % (reduced ? 6 : 3) != 0) return;
        int low = 0;
        for (Puff puff : puffs) if (!puff.rising) low++;
        int rising = puffs.size() - low;
        int patches = Math.min(8, 1 + sources.size() / 110);
        for (int i = 0; i < patches; i++) {
            if (low < Math.min(MAX_LOW, 5 + sources.size() / 3) && spawn(false)) low++;
            if ((clock + i) % 4 == 0 && rising < Math.min(MAX_RISING, 4 + sources.size() / 9) && spawn(true)) rising++;
        }
    }

    private static boolean spawn(boolean rising) {
        var random = world.random;
        BlockPos p = sources.get(random.nextInt(sources.size()));
        if (!world.hasChunkAt(p) || !tiles.containsKey(p) || !exposed(p)) return false;
        double angle = random.nextDouble() * Math.PI * 2;
        double speed = rising ? .012 : .040 + random.nextDouble() * .026;
        Puff puff = new Puff();
        puff.rising = rising;
        puff.x = p.getX() + random.nextDouble();
        puff.z = p.getZ() + random.nextDouble();
        puff.y = tiles.get(p).top + (rising ? .32 : .23);
        puff.oldX = puff.x;
        puff.oldY = puff.y;
        puff.oldZ = puff.z;
        puff.vx = Math.cos(angle) * speed;
        puff.vz = Math.sin(angle) * speed;
        puff.seed = random.nextInt(239);
        puff.life = rising ? 60 + random.nextInt(42) : 102 + random.nextInt(51);
        puff.size = rising ? .52 + random.nextDouble() * .48 : 1.4 + random.nextDouble() * 1.35;
        puff.floor = puff.y - .23;
        puffs.add(puff);
        return true;
    }

    private static class Puff {
        double x, y, z, oldX, oldY, oldZ, vx, vz, size, floor;
        int age, life, seed;
        boolean rising;

        boolean advance() {
            if (++age >= life) return false;
            oldX = x;
            oldY = y;
            oldZ = z;
            x += vx + .006 * Math.sin(seed + age * .055);
            z += vz + .006 * Math.cos(seed + age * .047);
            BlockPos here = BlockPos.containing(x, y, z);
            if (!world.hasChunkAt(here)) return false;
            if (rising) {
                y += .025 + .006 * Math.sin(seed + age * .04);
            } else {
                if (age % 3 == 1) {
                    BlockPos base = BlockPos.containing(x, floor + .05, z);
                    boolean found = false;
                    for (int dy = 1; dy >= -2; dy--) {
                        BlockPos pos = base.above(dy);
                        if (!world.getBlockState(pos.above()).isAir()) continue;
                        if (exposed(pos)) floor = pos.getY() + world.getFluidState(pos).getHeight(world, pos);
                        else if (world.getBlockState(pos).isCollisionShapeFullBlock(world, pos)) floor = pos.getY() + 1;
                        else continue;
                        found = true;
                        break;
                    }
                    if (!found) life = Math.min(life, age + 12);
                }
                y += (floor + .29 + .10 * Math.sin(seed + age * .043) - y) * .28;
            }
            if (!rising) return true;
            BlockPos at = BlockPos.containing(x, y, z);
            return world.getBlockState(at).isAir() || (exposed(at) && y >= at.getY() + world.getFluidState(at).getHeight(world, at));
        }

        void draw(BufferBuilder b, Matrix4f m, Vec3 eye, float partial, Vec3 right, Vec3 up) {
            double cx = Mth.lerp(partial, oldX, x) - eye.x;
            double cy = Mth.lerp(partial, oldY, y) - eye.y;
            double cz = Mth.lerp(partial, oldZ, z) - eye.z;
            double range = Math.sqrt(cx * cx + cy * cy + cz * cz);
            double t = (age + partial) / life;
            float fade = (float) (Math.min(1, t * 6) * Math.min(1, (1 - t) * 4) * Mth.clamp((RADIUS - range) / 7, 0, 1));
            if (fade <= 0) return;
            double width = size * (.66 + .70 * t);
            double thickness = rising ? .91 : .25;
            double height = width * Math.sqrt(1 - up.y * up.y * (1 - thickness * thickness));
            Vec3 a = right.scale(width), v = up.scale(height);
            float alpha = fade * (rising ? .55F : .47F);
            int kind = rising ? 2 : 0;
            vertex(b, m, cx - a.x - v.x, cy - a.y - v.y, cz - a.z - v.z, seed, 0, kind, alpha);
            vertex(b, m, cx + a.x - v.x, cy + a.y - v.y, cz + a.z - v.z, seed + 1, 0, kind, alpha);
            vertex(b, m, cx + a.x + v.x, cy + a.y + v.y, cz + a.z + v.z, seed + 1, 1, kind, alpha);
            vertex(b, m, cx - a.x + v.x, cy - a.y + v.y, cz - a.z + v.z, seed, 1, kind, alpha);
        }
    }

    @SubscribeEvent
    public static void render(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES || shader == null || (tiles.isEmpty() && puffs.isEmpty()))
            return;
        Minecraft mc = Minecraft.getInstance();
        ParticleStatus quality = mc.options.particles().get();
        if (mc.level != world || quality == ParticleStatus.MINIMAL) return;
        boolean reduced = quality == ParticleStatus.DECREASED;
        Vec3 eye = event.getCamera().getPosition();
        Matrix4f matrix = event.getPoseStack().last().pose();
        float time = ((world.getGameTime() % 1000000L) + event.getPartialTick()) / 20F;
        var previous = RenderSystem.getShader();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        try {
            RenderSystem.setShader(() -> shader);
            shader.safeGetUniform("Time").set(time);
            var buf = Tesselator.getInstance().getBuilder();
            buf.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
            var plumes = new ArrayList<BlockPos>();
            BlockPos fallback = null;
            for (var entry : tiles.entrySet()) {
                var p = entry.getKey();
                var tile = entry.getValue();
                double x = p.getX() - eye.x, y = tile.top - eye.y, z = p.getZ() - eye.z;
                double distance = Math.sqrt((x + .5) * (x + .5) + y * y + (z + .5) * (z + .5));
                float fade = Mth.clamp((float) (RADIUS - distance) / 3F, 0, 1);
                if (fade <= 0) continue;
                if (fallback == null || Mth.getSeed(p) < Mth.getSeed(fallback)) fallback = p;
                if (Math.floorMod(Mth.getSeed(p), 48) == 0) plumes.add(p);
            }
            if (plumes.isEmpty() && fallback != null) plumes.add(fallback);
            plumes.sort(Comparator.comparingDouble(p -> p.distToCenterSqr(eye.x, eye.y, eye.z)));
            int count = Math.min(plumes.size(), reduced ? MAX_PLUMES / 2 : MAX_PLUMES);
            for (int i = count - 1; i >= 0; i--) {
                BlockPos p = plumes.get(i);
                double x = p.getX() + .5 - eye.x, z = p.getZ() + .5 - eye.z, y = tiles.get(p).top - eye.y + .04;
                double len = Math.hypot(x, z);
                double rx = len < .001 ? 1 : -z / len, rz = len < .001 ? 0 : x / len;
                float seed = (float) Math.floorMod(Mth.getSeed(p) / 48, 239);
                double h = 1.3 + .005 * seed, w = .48 + .001 * seed;
                float alpha = .65F * Mth.clamp((float) (RADIUS - Math.sqrt(x * x + y * y + z * z)) / 3F, 0, 1);
                vertex(buf, matrix, x - rx * w, y, z - rz * w, seed, 0, 1, alpha);
                vertex(buf, matrix, x + rx * w, y, z + rz * w, seed + 1, 0, 1, alpha);
                vertex(buf, matrix, x + rx * w, y + h, z + rz * w, seed + 1, 1, 1, alpha);
                vertex(buf, matrix, x - rx * w, y + h, z - rz * w, seed, 1, 1, alpha);
            }
            Vec3 right = vector(event.getCamera().getLeftVector()).scale(-1);
            Vec3 up = vector(event.getCamera().getUpVector());
            for (Puff puff : puffs) puff.draw(buf, matrix, eye, event.getPartialTick(), right, up);
            BufferUploader.drawWithShader(buf.end());
        } finally {
            RenderSystem.depthMask(true);
            RenderSystem.enableCull();
            RenderSystem.disableBlend();
            RenderSystem.setShader(() -> previous);
        }
    }

    private static Vec3 vector(org.joml.Vector3f v) {
        return new Vec3(v.x(), v.y(), v.z());
    }

    private static void vertex(BufferBuilder b, Matrix4f m, double x, double y, double z, float u, float v, int plume, float alpha) {
        b.vertex(m, (float) x, (float) y, (float) z).uv(u, v).color(plume * 127, 0, 0, (int) (255 * alpha)).endVertex();
    }
}
