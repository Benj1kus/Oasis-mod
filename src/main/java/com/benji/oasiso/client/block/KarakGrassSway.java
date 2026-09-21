package com.benji.oasiso.client.block;

import com.benji.oasiso.Oasiso;
import com.benji.oasiso.common.block.KarakBigGrassBlock;
import com.benji.oasiso.common.block.KarakSmallGrassBlock;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterShadersEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.io.IOException;
import java.util.*;

@Mod.EventBusSubscriber(modid = Oasiso.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class KarakGrassSway {
    private static final int MAX_ACTIVE = 16; // Согласовано с 16 парами uniform в шейдере.
    private static final int DURATION = 25;
    private static final double VIEW_RANGE = 32;
    private static final LinkedHashMap<BlockPos, Motion> ACTIVE = new LinkedHashMap<>();
    private static final Map<UUID, Vec3> PREVIOUS = new HashMap<>();
    private static ClientLevel world;
    private static int clock;
    private static float partial;
    private static ShaderInstance shader;
    private static Shape smallShape, tallShape;

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        if (world != mc.level) {
            world = mc.level;
            ACTIVE.clear();
            PREVIOUS.clear();
            clock = 0;
        }
        if (world == null || mc.player == null || mc.isPaused()) return;
        clock++;
        ACTIVE.entrySet().removeIf(entry -> clock - entry.getValue().start >= DURATION || !isAnimatedPart(world.getBlockState(entry.getKey())));
        Set<UUID> seen = new HashSet<>();
        Vec3 camera = mc.gameRenderer.getMainCamera().getPosition();
        for (Player player : world.players()) {
            seen.add(player.getUUID());
            Vec3 now = player.position(), old = PREVIOUS.put(player.getUUID(), now);
            if (old == null || player.isSpectator() || player.distanceToSqr(camera) > VIEW_RANGE * VIEW_RANGE) continue;
            double dx = now.x - old.x, dz = now.z - old.z, dy = now.y - old.y;
            double speed = Math.sqrt(dx * dx + dz * dz);


            if (speed < .012 || speed > 2.0 || Math.abs(dy) > 2.0) continue;

            AABB current = player.getBoundingBox();
            AABB swept = current.minmax(current.move(-dx, -dy, -dz));

            for (int x = Mth.floor(swept.minX); x <= Mth.floor(swept.maxX - 1e-5); x++)
                for (int z = Mth.floor(swept.minZ); z <= Mth.floor(swept.maxZ - 1e-5); z++)
                    for (int y = Mth.floor(swept.minY); y <= Mth.floor(swept.maxY - 1e-5); y++) {
                        BlockPos p = new BlockPos(x, y, z);
                        BlockState state = world.getBlockState(p);

                        boolean tall = state.getBlock() instanceof KarakBigGrassBlock;

                        if (!tall && !(state.getBlock() instanceof KarakSmallGrassBlock)) continue;

                        if (tall && state.getValue(DoublePlantBlock.HALF) == DoubleBlockHalf.LOWER) p = p.above();

                        if (!isAnimatedPart(world.getBlockState(p))) continue;
                        Motion previous = ACTIVE.get(p);
                        if (previous != null && clock - previous.start < 8) continue;
                        if (ACTIVE.size() >= MAX_ACTIVE && previous == null)
                            ACTIVE.remove(ACTIVE.keySet().iterator().next());
                        float strength = (tall ? .30F : .150F) * (float) Math.min(1.3, .65 + speed * 2);
                        ACTIVE.put(p, new Motion(clock, (float) (dx / speed) * strength, (float) (dz / speed) * strength));
                    }
        }
        PREVIOUS.keySet().retainAll(seen);
    }

    @SubscribeEvent
    public static void frame(TickEvent.RenderTickEvent event) {
        if (event.phase == TickEvent.Phase.START)
            partial = Minecraft.getInstance().isPaused() ? 0 : event.renderTickTime;
    }

    private static boolean isAnimatedPart(BlockState state) {
        return state.getBlock() instanceof KarakSmallGrassBlock || state.getBlock() instanceof KarakBigGrassBlock && state.getValue(DoublePlantBlock.HALF) == DoubleBlockHalf.UPPER;
    }

    public static void useShader(RenderType type, double cameraX, double cameraY, double cameraZ) {
        Minecraft mc = Minecraft.getInstance();
        if (type != RenderType.cutout() || shader == null || ACTIVE.isEmpty() || mc.level != world || world == null)
            return;
        int count = 0;
        for (var entry : ACTIVE.entrySet()) {
            BlockPos p = entry.getKey();
            BlockState state = world.getBlockState(p);
            if (!isAnimatedPart(state)) continue;
            boolean tall = state.getBlock() instanceof KarakBigGrassBlock;
            Shape shape = tall ? tallShape : smallShape;
            if (shape == null) {
                shape = Shape.read(mc.getBlockRenderer().getBlockModel(state), state);
                if (shape == null) continue;
                if (tall) tallShape = shape;
                else smallShape = shape;
            }
            Motion motion = entry.getValue();
            float age = clock - motion.start + partial;
            float wobble = (float) (Math.sin(age * .72) * Math.exp(-age * .15));
            Vec3 offset = state.getOffset(world, p);
            set4("Plant" + count, (float) (p.getX() + offset.x - cameraX), (float) (p.getY() + offset.y - cameraY), (float) (p.getZ() + offset.z - cameraZ), tall ? 1 : 0);
            set4("Bend" + count, motion.x * wobble, motion.z * wobble, 0, 0);
            if (++count == MAX_ACTIVE) break;
        }
        if (count == 0) return;
        set1("ActiveCount", count);
        uploadShape("Small", smallShape);
        uploadShape("Tall", tallShape);
        RenderSystem.setShader(() -> shader);
    }

    private static void uploadShape(String prefix, Shape s) {
        if (s == null) {
            set4(prefix + "UV", -2, -2, -1, -1);
            set4(prefix + "Bounds", 0, 0, 0, 0);
            return;
        }
        set4(prefix + "UV", s.u0, s.v0, s.u1, s.v1);
        set4(prefix + "Bounds", s.minX, s.maxX, s.minZ, s.maxZ);
    }

    private static void set1(String name, float v) {
        var u = shader.getUniform(name);
        if (u != null) u.set(v);
    }

    private static void set4(String name, float x, float y, float z, float w) {
        var u = shader.getUniform(name);
        if (u != null) u.set(x, y, z, w);
    }

    private record Motion(int start, float x, float z) {
    }

    private static final class Shape {
        float u0, v0, u1, v1, minX = Float.POSITIVE_INFINITY, maxX = Float.NEGATIVE_INFINITY, minZ = Float.POSITIVE_INFINITY, maxZ = Float.NEGATIVE_INFINITY;

        static Shape read(BakedModel model, BlockState state) {
            List<BakedQuad> quads = model.getQuads(state, null, RandomSource.create(42));
            if (quads.isEmpty()) return null;
            Shape result = new Shape();
            var sprite = quads.get(0).getSprite();
            result.u0 = sprite.getU0();
            result.u1 = sprite.getU1();
            result.v0 = sprite.getV0();
            result.v1 = sprite.getV1();
            for (BakedQuad quad : quads) {
                int[] data = quad.getVertices();
                int stride = data.length / 4;
                for (int i = 0; i < 4; i++) {
                    float x = Float.intBitsToFloat(data[i * stride]), z = Float.intBitsToFloat(data[i * stride + 2]);
                    result.minX = Math.min(result.minX, x);
                    result.maxX = Math.max(result.maxX, x);
                    result.minZ = Math.min(result.minZ, z);
                    result.maxZ = Math.max(result.maxZ, z);
                }
            }
            return result;
        }
    }

    @Mod.EventBusSubscriber(modid = Oasiso.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static final class Registration {
        @SubscribeEvent
        public static void shaders(RegisterShadersEvent event) throws IOException {
            event.registerShader(new ShaderInstance(event.getResourceProvider(), ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "karak_grass_sway"), DefaultVertexFormat.BLOCK), loaded -> {
                shader = loaded;
                smallShape = tallShape = null;
            });
        }
    }

    private KarakGrassSway() {
    }
}
