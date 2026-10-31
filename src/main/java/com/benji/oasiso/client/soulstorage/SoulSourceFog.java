package com.benji.oasiso.client.soulstorage;

import com.mojang.logging.LogUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.commands.Commands;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import org.joml.Matrix4f;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterShadersEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.io.IOException;
import java.util.*;

@Mod.EventBusSubscriber(modid = "oasiso", value = Dist.CLIENT)
public final class SoulSourceFog {
    public static final int AREA_SIZE = 50;
    public static final float OPACITY = .14F;
    public static final int LAYERS = 4;
    public static final float BASE_HEIGHT = .12F, LAYER_GAP = .18F;
    private static final int SEARCH_RADIUS = 64;
    private static final ResourceLocation CORE = ResourceLocation.fromNamespaceAndPath("oasiso", "soul_pcore");
    private static final ResourceLocation MAGMA = ResourceLocation.fromNamespaceAndPath("oasiso", "nephritis_magma");
    private static boolean debug;
    private static int candidates;
    private static long drawCalls;
    private static ShaderInstance shader;
    private static ClientLevel world;
    private static final Map<BlockPos, Float> CELLS = new HashMap<>();
    private static Set<BlockPos> sources = Set.of();
    private static float fade, previous;
    private static long ticks;
    private static int noiseX, noiseZ;

    public static void registerShader(RegisterShadersEvent event) throws IOException {
        LogUtils.getLogger().info("[SoulFog] Registering shader");

        event.registerShader(new ShaderInstance(event.getResourceProvider(), ResourceLocation.fromNamespaceAndPath("oasiso", "soul_source_fog"), DefaultVertexFormat.POSITION_COLOR_TEX), loaded -> {
            shader = loaded;
            LogUtils.getLogger().info("[SoulFog] Shader READY");
        });
    }

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        if (world != mc.level || mc.player == null) {
            world = mc.level;
            sources = Set.of();
            CELLS.clear();
            fade = previous = 0;
            ticks = 0;
            drawCalls = 0;
            candidates = 0;
        }
        if (world == null || mc.player == null || mc.isPaused()) return;
        if (ticks++ % 10 == 0) discover(mc.player.blockPosition());
        previous = fade;
        fade = Mth.approach(fade, sources.isEmpty() ? 0F : 1F, 1F / 20F);
        if (fade == 0 && sources.isEmpty()) CELLS.clear();
        if (debug && ticks % 100 == 0) LogUtils.getLogger().info("[SoulFog] {}", status());
    }

    private static void discover(BlockPos eye) {
        Set<BlockPos> found = new HashSet<>();
        var core = BuiltInRegistries.BLOCK.get(CORE);
        var magma = BuiltInRegistries.BLOCK.get(MAGMA);
        candidates = 0;
        int minX = (eye.getX() - SEARCH_RADIUS) >> 4, maxX = (eye.getX() + SEARCH_RADIUS) >> 4;
        int minZ = (eye.getZ() - SEARCH_RADIUS) >> 4, maxZ = (eye.getZ() + SEARCH_RADIUS) >> 4;
        for (int cx = minX; cx <= maxX; cx++) {
            for (int cz = minZ; cz <= maxZ; cz++) {
                var chunk = world.getChunkSource().getChunk(cx, cz, ChunkStatus.FULL, false);
                if (chunk == null) continue;
                var sections = chunk.getSections();
                for (int index = 0; index < sections.length; index++) {
                    int y0 = world.getMinBuildHeight() + index * 16;
                    if (y0 > eye.getY() + 20 || y0 + 15 < eye.getY() - 20) continue;
                    var section = sections[index];
                    if (section.hasOnlyAir() || !section.maybeHas(state -> state.is(magma))) continue;
                    int low = Math.max(0, eye.getY() - 20 - y0), high = Math.min(15, eye.getY() + 20 - y0);
                    for (int y = low; y <= high; y++) {
                        for (int x = 0; x < 16; x++) {
                            for (int z = 0; z < 16; z++) {
                                if (!section.getBlockState(x, y, z).is(magma)) continue;
                                candidates++;
                                BlockPos p = new BlockPos((cx << 4) + x, y0 + y, (cz << 4) + z);
                                for (Direction side : Direction.values()) {
                                    BlockPos neighbor = p.relative(side);
                                    if (world.hasChunkAt(neighbor) && world.getBlockState(neighbor).is(core)) {
                                        found.add(p);
                                        break;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        if (found.equals(sources)) return;
        sources = found;
        LogUtils.getLogger().info("[SoulFog] Sources={}, magma={}, first={}", found.size(), candidates, found.stream().findFirst().map(BlockPos::toShortString).orElse("none"));
        if (found.isEmpty()) return;
        if (CELLS.isEmpty()) {
            BlockPos first = found.stream().min(Comparator.comparingLong(BlockPos::asLong)).orElseThrow();
            noiseX = first.getX();
            noiseZ = first.getZ();
        }
        CELLS.clear();
        for (BlockPos source : found) {
            int x0 = source.getX() - AREA_SIZE / 2, z0 = source.getZ() - AREA_SIZE / 2;
            for (int x = x0; x < x0 + AREA_SIZE; x++) {
                for (int z = z0; z < z0 + AREA_SIZE; z++) {
                    float edge = Math.min(Math.min(x - x0 + .5F, x0 + AREA_SIZE - x - .5F), Math.min(z - z0 + .5F, z0 + AREA_SIZE - z - .5F));
                    float weight = Mth.clamp(edge / 3F, 0F, 1F);
                    weight = weight * weight * (3 - 2 * weight);
                    CELLS.merge(new BlockPos(x, source.getY(), z), weight, Math::max);
                }
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void render(RenderLevelStageEvent e) {
        if (e.getStage() != RenderLevelStageEvent.Stage.AFTER_WEATHER || shader == null || CELLS.isEmpty()) return;
        if (world == null || Minecraft.getInstance().level != world) return;
        float visibility = Mth.lerp(e.getPartialTick(), previous, fade);
        if (visibility <= 0) return;
        Vec3 eye = e.getCamera().getPosition();
        float time = (ticks + e.getPartialTick()) / 20F;
        try (SoulRenderState ignored = new SoulRenderState()) {
            RenderSystem.enableDepthTest();
            RenderSystem.depthFunc(515);
            RenderSystem.depthMask(false);
            RenderSystem.disableCull();
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.colorMask(true, true, true, false);
            RenderSystem.setShaderColor(1, 1, 1, 1);
            RenderSystem.setShader(() -> shader);
            // AFTER_WEATHER can have a different global RenderSystem matrix.
            // Camera-relative vertices need the world render event's view, exactly once.
            shader.safeGetUniform("CameraView").set(new Matrix4f(e.getPoseStack().last().pose()));
            shader.safeGetUniform("CameraProjection").set(e.getProjectionMatrix());
            shader.safeGetUniform("DebugMode").set(debug ? 1F : 0F);
            shader.safeGetUniform("FogTime").set(time);
            drawCalls++;
            if (drawCalls == 1)
                LogUtils.getLogger().info("[SoulFog] First draw: sources={}, cells={}", sources.size(), CELLS.size());
            shader.safeGetUniform("Opacity").set(OPACITY * visibility);
            for (int layer = LAYERS - 1; layer >= 0; layer--) {
                shader.safeGetUniform("Layer").set((float) layer);
                BufferBuilder b = Tesselator.getInstance().getBuilder();
                b.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR_TEX);
                for (var cell : CELLS.entrySet()) {
                    BlockPos p = cell.getKey();
                    float vertical = Mth.clamp((16F - (float) Math.abs(eye.y - (p.getY() + 1))) / 8F, 0F, 1F);
                    float a = cell.getValue() * vertical;
                    if (a <= 0) continue;
                    double y = p.getY() + 1 + BASE_HEIGHT + layer * LAYER_GAP;
                    vertex(b, p.getX(), y, p.getZ(), eye, a, time, layer);
                    vertex(b, p.getX(), y, p.getZ() + 1, eye, a, time, layer);
                    vertex(b, p.getX() + 1, y, p.getZ() + 1, eye, a, time, layer);
                    vertex(b, p.getX() + 1, y, p.getZ(), eye, a, time, layer);
                }
                BufferUploader.drawWithShader(b.end());
            }
        }
    }

    @SubscribeEvent
    public static void commands(RegisterClientCommandsEvent e) {
        e.getDispatcher().register(Commands.literal("oasiso_fog_debug").executes(context -> {
            Minecraft mc = Minecraft.getInstance();
            debug = !debug;
            if (mc.player != null) {
                if (world != null) discover(mc.player.blockPosition());
                mc.player.displayClientMessage(Component.literal("[SoulFog] debug=" + debug + " | " + status()), false);
                mc.player.displayClientMessage(Component.literal(debug ? "Diagnostic cyan plane ON. Run /oasiso_fog_debug again for normal smoke." : "Normal smoke restored."), false);
            }
            LogUtils.getLogger().info("[SoulFog] {}", status());
            return 1;
        }));
    }

    private static String status() {
        Minecraft mc = Minecraft.getInstance();
        String first = sources.stream().findFirst().map(p -> p.toShortString() + " fogY=" + (p.getY() + 1 + BASE_HEIGHT)).orElse("none");
        return "shader=" + (shader == null ? "MISSING" : "READY") + ", magma=" + candidates + ", sources=" + sources.size() + ", cells=" + CELLS.size() + ", draws=" + drawCalls + ", fade=" + fade + ", first=" + first + ", playerY=" + (mc.player == null ? "none" : String.format(java.util.Locale.ROOT, "%.2f", mc.player.getY()));
    }

    private static void vertex(BufferBuilder b, double x, double y, double z, Vec3 eye, float a, float t, int layer) {
        double u = x - noiseX, v = z - noiseZ;
        double lift = .035 * Math.sin(u * .55 + t * .6 + layer) * Math.cos(v * .48 - t * .4);
        b.vertex(x - eye.x, y + lift - eye.y, z - eye.z).color(0F, .80F, .60F, a).uv((float) u, (float) v).endVertex();
    }
}
