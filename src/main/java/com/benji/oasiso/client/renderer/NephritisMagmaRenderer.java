package com.benji.oasiso.client.renderer;

import com.benji.oasiso.Oasiso;
import com.benji.oasiso.common.block.NephritisMagmaBlock;
import com.benji.oasiso.common.block.entity.NephritisMagmaBlockEntity;
import com.benji.oasiso.registry.ModBlockEntities;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.*;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;
import org.lwjgl.opengl.*;

import java.util.*;

@Mod.EventBusSubscriber(modid = Oasiso.MODID, value = Dist.CLIENT)
public final class NephritisMagmaRenderer implements BlockEntityRenderer<NephritisMagmaBlockEntity> {
    public static final int MAX_COLUMNS = 24, DISTANCE = 36, VARIANTS = 12;
    private static final Set<BlockPos> SOURCES = new HashSet<>();
    private static final Map<Integer, List<NephritisMagmaMesh.Cell>> MESHES = new HashMap<>();
    private static ClientLevel lastLevel;
    private static long meshTick = Long.MIN_VALUE;
    private static Matrix4f view;

    public NephritisMagmaRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public boolean shouldRenderOffScreen(NephritisMagmaBlockEntity block) {
        return true;
    }

    @Override
    public int getViewDistance() {
        return DISTANCE;
    }

    @Override
    public void render(NephritisMagmaBlockEntity block, float partial, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
    }

    @SubscribeEvent
    public static void world(RenderLevelStageEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_WEATHER || mc.level == null) return;
        if (lastLevel != mc.level) {
            lastLevel = mc.level;
            MESHES.clear();
            meshTick = Long.MIN_VALUE;
        }
        SOURCES.clear();
        BlockPos cameraBlock = BlockPos.containing(event.getCamera().getPosition());
        int chunkX = cameraBlock.getX() >> 4, chunkZ = cameraBlock.getZ() >> 4;
        int radius = (DISTANCE + 15) / 16;
        for (int cx = chunkX - radius; cx <= chunkX + radius; cx++) {
            for (int cz = chunkZ - radius; cz <= chunkZ + radius; cz++) {
                var chunk = mc.level.getChunkSource().getChunk(cx, cz, ChunkStatus.FULL, false);
                if (chunk == null) continue;
                for (var entry : chunk.getBlockEntities().entrySet()) {
                    var state = chunk.getBlockState(entry.getKey());
                    if (state.getBlock() instanceof NephritisMagmaBlock && state.getValue(NephritisMagmaBlock.LEAKING)) {
                        SOURCES.add(entry.getKey().immutable());
                    }
                }
            }
        }
        if (SOURCES.isEmpty()) return;
        view = new Matrix4f().identity();
        Vec3 eye = event.getCamera().getPosition();
        List<BlockPos> visible = new ArrayList<>(SOURCES);
        visible.removeIf(p -> p.distToCenterSqr(eye.x, eye.y, eye.z) > DISTANCE * DISTANCE || !mc.level.getBlockState(p).hasProperty(NephritisMagmaBlock.LEAKING) || !mc.level.getBlockState(p).getValue(NephritisMagmaBlock.LEAKING));
        visible.sort(Comparator.comparingDouble(p -> p.distToCenterSqr(eye.x, eye.y, eye.z)));
        if (visible.size() > MAX_COLUMNS) visible = new ArrayList<>(visible.subList(0, MAX_COLUMNS));
        Collections.reverse(visible);
        long tick = mc.level.getGameTime();
        if (tick != meshTick) {
            MESHES.clear();
            meshTick = tick;
        }
        var oldShader = RenderSystem.getShader();
        float[] color = RenderSystem.getShaderColor().clone();
        boolean depth = GL11.glIsEnabled(GL11.GL_DEPTH_TEST), cull = GL11.glIsEnabled(GL11.GL_CULL_FACE), blend = GL11.glIsEnabled(GL11.GL_BLEND);
        boolean depthWrite = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
        int src = GL11.glGetInteger(GL14.GL_BLEND_SRC_RGB), dst = GL11.glGetInteger(GL14.GL_BLEND_DST_RGB);
        int srcA = GL11.glGetInteger(GL14.GL_BLEND_SRC_ALPHA), dstA = GL11.glGetInteger(GL14.GL_BLEND_DST_ALPHA);
        try {
            RenderSystem.enableDepthTest();
            RenderSystem.depthMask(false);
            RenderSystem.disableCull();
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.setShaderColor(1, 1, 1, 1);
            RenderSystem.setShader(GameRenderer::getPositionColorShader);
            BufferBuilder b = Tesselator.getInstance().getBuilder();
            b.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
            for (BlockPos pos : visible) {
                int variant = Math.floorMod(Long.hashCode(pos.asLong()), VARIANTS);
                var cells = MESHES.computeIfAbsent(variant, v -> NephritisMagmaMesh.build(v, tick / 20.0));
                double x = pos.getX() + .5 - eye.x, y = pos.getY() + 1.01 - eye.y, z = pos.getZ() + .5 - eye.z;
                double len = Math.sqrt(x * x + z * z), rx = len < .0001 ? 1 : z / len, rz = len < .0001 ? 0 : -x / len;
                for (var c : cells) {
                    vertex(b, x + rx * c.x0(), y + c.y0(), z + rz * c.x0(), c);
                    vertex(b, x + rx * c.x0(), y + c.y1(), z + rz * c.x0(), c);
                    vertex(b, x + rx * c.x1(), y + c.y1(), z + rz * c.x1(), c);
                    vertex(b, x + rx * c.x1(), y + c.y0(), z + rz * c.x1(), c);
                }
            }
            BufferUploader.drawWithShader(b.end());
        } finally {
            RenderSystem.setShader(() -> oldShader);
            RenderSystem.setShaderColor(color[0], color[1], color[2], color[3]);
            RenderSystem.depthMask(depthWrite);
            if (depth) RenderSystem.enableDepthTest();
            else RenderSystem.disableDepthTest();
            if (cull) RenderSystem.enableCull();
            else RenderSystem.disableCull();
            RenderSystem.blendFuncSeparate(src, dst, srcA, dstA);
            if (blend) RenderSystem.enableBlend();
            else RenderSystem.disableBlend();
        }
    }

    private static void vertex(BufferBuilder b, double x, double y, double z, NephritisMagmaMesh.Cell c) {
        b.vertex(view, (float) x, (float) y, (float) z).color(c.r(), c.g(), c.b(), c.a()).endVertex();
    }

    @Mod.EventBusSubscriber(modid = Oasiso.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static final class Registration {
        @SubscribeEvent
        public static void register(EntityRenderersEvent.RegisterRenderers event) {
            event.registerBlockEntityRenderer(ModBlockEntities.NEPHRITIS_MAGMA_BE.get(), NephritisMagmaRenderer::new);
        }
    }
}
