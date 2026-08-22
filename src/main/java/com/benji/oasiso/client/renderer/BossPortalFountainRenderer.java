package com.benji.oasiso.client.renderer;

import com.benji.oasiso.Oasiso;
import com.benji.oasiso.common.block.entity.NephritisLampBlockEntity;
import com.benji.oasiso.common.entity.BossPortalEntity;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;


@Mod.EventBusSubscriber(modid = Oasiso.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class BossPortalFountainRenderer {

    private static final double FOUNTAIN_HEIGHT = 10.5D;
    private static final double HALF_WIDTH = 1.85D;

    private static final double PORTAL_BASE_Y = 0.12D;
    private static final double LAMP_BASE_Y = 1.02D;

    private static final int GRID_X = 38;
    private static final int GRID_Y = 94;


    private static final int BLOB_COUNT = 30;
    private static final int MESH_UPDATE_INTERVAL_TICKS = 1;
    private static final int LAMP_PATTERN_VARIANTS = 4;
    private static final double LAMP_RENDER_DISTANCE = 36.0D;
    private static final int MAX_VISIBLE_LAMPS = 24;
    private static final double PORTAL_RENDER_DISTANCE = 64.0D;
    private static final long MESH_CACHE_LIFETIME = 20L * 4L;

    private static final double OUTLINE_THRESHOLD = 0.70D;
    private static final double FILL_THRESHOLD = 1.00D;

    private static final int FILL_RED = 18;
    private static final int FILL_GREEN = 118;
    private static final int FILL_BLUE = 72;
    private static final int FILL_ALPHA = 218;


    private static final int OUTLINE_RED = 90;
    private static final int OUTLINE_GREEN = 255;
    private static final int OUTLINE_BLUE = 170;
    private static final int OUTLINE_ALPHA = 250;


    private static final long REVEAL_TIME_NS = 1_000_000_000L;
    private static final long DESPAWN_FADE_TIME_NS = 1_500_000_000L;

    private static final Map<BossPortalEntity, Long> IDLE_START_TIMES = new WeakHashMap<>();
    private static final Map<BossPortalEntity, Long> DESPAWN_START_TIMES = new WeakHashMap<>();
    private static final Map<FountainKey, MeshCache> MESH_CACHE = new HashMap<>();

    private static ClientLevel lastLevel;
    private static ResourceKey<Level> lastDimension;

    private static long lastClientTick = Long.MIN_VALUE;

    private BossPortalFountainRenderer() {
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_WEATHER) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();

        if (minecraft.level == null || minecraft.player == null) {
            return;
        }

        if (lastLevel != minecraft.level) {
            MESH_CACHE.clear();
            IDLE_START_TIMES.clear();
            DESPAWN_START_TIMES.clear();

            NephritisLampBlockEntity.clearClientSources();

            lastLevel = minecraft.level;
            lastDimension = null;
            lastClientTick = Long.MIN_VALUE;
        }


        ResourceKey<Level> dimension = minecraft.level.dimension();

        long gameTick = minecraft.level.getGameTime();

        if (!dimension.equals(lastDimension) || gameTick != lastClientTick) {
            NephritisLampBlockEntity.tickClientSources(dimension, gameTick);
            pruneMeshCache(dimension, gameTick);

            lastDimension = dimension;
            lastClientTick = gameTick;
        }


        Vec3 camera = event.getCamera().getPosition();

        List<BossPortalEntity> portals = minecraft.level.getEntitiesOfClass(BossPortalEntity.class, minecraft.player.getBoundingBox().inflate(PORTAL_RENDER_DISTANCE), portal -> portal.isAlive() && portal.isChaosPortal() && (portal.getAnimState() == BossPortalEntity.STATE_IDLE || portal.getAnimState() == BossPortalEntity.STATE_DESPAWN));
        List<NephritisLampBlockEntity.ClientLampSource> lamps = getVisibleLamps(dimension, camera);

        if (portals.isEmpty() && lamps.isEmpty()) {
            return;
        }

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);


        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        BufferBuilder buffer = Tesselator.getInstance().getBuilder();
        buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

        Matrix4f matrix = new Matrix4f().identity();

        long now = System.nanoTime();

        for (BossPortalEntity portal : portals) {
            renderPortal(buffer, matrix, portal, dimension, camera, gameTick, now);
        }

        for (NephritisLampBlockEntity.ClientLampSource lamp : lamps) {
            renderLamp(buffer, matrix, lamp, dimension, camera, gameTick);
        }

        BufferUploader.drawWithShader(buffer.end());
        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }


    private static List<NephritisLampBlockEntity.ClientLampSource> getVisibleLamps(ResourceKey<Level> dimension, Vec3 camera) {
        List<NephritisLampBlockEntity.ClientLampSource> result = new ArrayList<>();
        double maxDistanceSqr = LAMP_RENDER_DISTANCE * LAMP_RENDER_DISTANCE;

        for (NephritisLampBlockEntity.ClientLampSource source : NephritisLampBlockEntity.getClientSources(dimension)) {
            if (distanceToLampSqr(source.pos(), camera) > maxDistanceSqr) {
                continue;
            }
            result.add(source);
        }
        result.sort(Comparator.comparingDouble(source -> distanceToLampSqr(source.pos(), camera)));

        if (result.size() > MAX_VISIBLE_LAMPS) {
            return new ArrayList<>(result.subList(0, MAX_VISIBLE_LAMPS));
        }
        return result;
    }


    private static double distanceToLampSqr(BlockPos pos, Vec3 camera) {
        double x = pos.getX() + 0.5D - camera.x;
        double y = pos.getY() + 1.0D - camera.y;
        double z = pos.getZ() + 0.5D - camera.z;

        return x * x + y * y + z * z;
    }


    private static void renderPortal(BufferBuilder buffer, Matrix4f matrix, BossPortalEntity portal, ResourceKey<Level> dimension, Vec3 camera, long gameTick, long now) {

        long idleStart = IDLE_START_TIMES.computeIfAbsent(portal, ignored -> now);

        float reveal = Mth.clamp((now - idleStart) / (float) REVEAL_TIME_NS, 0.0F, 1.0F);

        reveal = smoothstep(reveal);

        if (portal.getAnimState() == BossPortalEntity.STATE_DESPAWN) {

            long despawnStart = DESPAWN_START_TIMES.computeIfAbsent(portal, ignored -> now);
            float progress = Mth.clamp((now - despawnStart) / (float) DESPAWN_FADE_TIME_NS, 0.0F, 1.0F);

            reveal *= 1.0F - smoothstep(progress);

        } else {
            DESPAWN_START_TIMES.remove(portal);
        }

        if (reveal <= 0.001F) {
            return;
        }

        UUID uuid = portal.getUUID();

        long seed = uuid.getMostSignificantBits() ^ uuid.getLeastSignificantBits();

        FountainKey key = FountainKey.portal(dimension, uuid);

        renderSource(buffer, matrix, key, seed, portal.getX(), portal.getY() + PORTAL_BASE_Y, portal.getZ(), camera, gameTick, reveal);
    }

    private static void renderLamp(BufferBuilder buffer, Matrix4f matrix, NephritisLampBlockEntity.ClientLampSource source, ResourceKey<Level> dimension, Vec3 camera, long gameTick) {

        float reveal = source.intensity();

        if (reveal <= 0.001F) {
            return;
        }

        BlockPos pos = source.pos();

        int variant = Math.floorMod(Long.hashCode(pos.asLong()), LAMP_PATTERN_VARIANTS);

        long seed = 0x6A09E667F3BCC909L ^ (variant * 0x9E3779B97F4A7C15L);

        FountainKey key = FountainKey.lamp(dimension, variant);

        renderSource(buffer, matrix, key, seed, pos.getX() + 0.5D, pos.getY() + LAMP_BASE_Y, pos.getZ() + 0.5D, camera, gameTick, reveal);
    }

    private static void renderSource(BufferBuilder buffer, Matrix4f matrix, FountainKey key, long seed, double worldX, double worldY, double worldZ, Vec3 camera, long gameTick, float reveal) {

        double toCameraX = camera.x - worldX;
        double toCameraZ = camera.z - worldZ;
        double horizontalLength = Math.sqrt(toCameraX * toCameraX + toCameraZ * toCameraZ);

        double rightX;
        double rightZ;

        if (horizontalLength < 0.0001D) {
            rightX = 1.0D;
            rightZ = 0.0D;

        } else {

            rightX = -toCameraZ / horizontalLength;
            rightZ = toCameraX / horizontalLength;
        }

        double centerX = worldX - camera.x;
        double centerY = worldY - camera.y;
        double centerZ = worldZ - camera.z;


        MeshCache mesh = getOrBuildMesh(key, seed, gameTick);
        drawMesh(buffer, matrix, mesh, centerX, centerY, centerZ, rightX, rightZ, reveal);
    }

    private static MeshCache getOrBuildMesh(FountainKey key, long seed, long gameTick) {
        MeshCache cache = MESH_CACHE.computeIfAbsent(key, ignored -> new MeshCache());
        cache.lastSeenTick = gameTick;

        boolean needsUpdate = cache.builtTick == Long.MIN_VALUE || gameTick < cache.builtTick || gameTick - cache.builtTick >= MESH_UPDATE_INTERVAL_TICKS;

        if (needsUpdate) {
            cache.cells = buildMesh(seed, gameTick / 20.0D);
            cache.builtTick = gameTick;
        }
        return cache;
    }

    private static List<MeshCell> buildMesh(long seed, double time) {

        BlobState[] blobs = buildBlobStates(seed, time);


        List<MeshCell> cells = new ArrayList<>();

        double cellWidth = (HALF_WIDTH * 2.0D) / GRID_X;
        double cellHeight = FOUNTAIN_HEIGHT / GRID_Y;

        for (int yIndex = 0; yIndex < GRID_Y; yIndex++) {

            double y0 = yIndex * cellHeight;
            double y1 = (yIndex + 1) * cellHeight;

            double sampleY = (y0 + y1) * 0.5D;

            float topFade = getTopFade(sampleY);


            for (int xIndex = 0; xIndex < GRID_X; xIndex++) {

                double x0 = -HALF_WIDTH + xIndex * cellWidth;
                double x1 = x0 + cellWidth;

                double sampleX = (x0 + x1) * 0.5D;
                double field = getMetaballField(sampleX, sampleY, blobs);

                if (field < OUTLINE_THRESHOLD) {
                    continue;
                }

                cells.add(new MeshCell(x0, x1, y0, y1, field >= FILL_THRESHOLD, topFade));
            }
        }
        return cells;
    }

    private static BlobState[] buildBlobStates(long seed, double time) {
        BlobState[] blobs = new BlobState[BLOB_COUNT];


        for (int i = 0; i < BLOB_COUNT; i++) {


            double phase = hash01(seed, i, 1);
            double speed = 0.11D + hash01(seed, i, 2) * 0.07D;
            double progress = fract(time * speed + phase);

            double blobY = -0.35D + progress * (FOUNTAIN_HEIGHT + 0.85D);

            double appear = smoothstepDouble(Mth.clamp(progress / 0.10D, 0.0D, 1.0D));
            double disappear = smoothstepDouble(Mth.clamp((1.0D - progress) / 0.16D, 0.0D, 1.0D));


            double life = appear * disappear;
            double radius = 0.10D + hash01(seed, i, 3) * 0.12D;

            radius *= 0.32D + life * 0.68D;

            double baseOffset = (hash01(seed, i, 4) * 2.0D - 1.0D) * 0.38D;

            double waveSpeed = 0.7D + hash01(seed, i, 5) * 0.9D;
            double wavePhase = hash01(seed, i, 6) * Math.PI * 2.0D;

            double spread = 0.45D + progress * 0.55D;
            double blobX = baseOffset * spread + Math.sin(time * waveSpeed + wavePhase) * 0.34D * spread + Math.sin(progress * Math.PI * 5.0D + wavePhase) * 0.15D;

            blobs[i] = new BlobState(blobX, blobY, radius);
        }
        return blobs;
    }

    private static double getMetaballField(double sampleX, double sampleY, BlobState[] blobs) {
        double field = 0.0D;
        field += metaball(sampleX, sampleY, 0.0D, 0.12D, 0.28D);
        field += metaball(sampleX, sampleY, 0.0D, 0.48D, 0.24D);
        field += metaball(sampleX, sampleY, 0.0D, 0.82D, 0.20D);

        for (BlobState blob : blobs) {
            field += metaball(sampleX, sampleY, blob.x(), blob.y(), blob.radius());
        }
        return field;
    }

    private static void drawMesh(BufferBuilder buffer, Matrix4f matrix, MeshCache mesh, double centerX, double centerY, double centerZ, double rightX, double rightZ, float reveal) {

        for (MeshCell cell : mesh.cells) {

            int red = cell.fill() ? FILL_RED : OUTLINE_RED;
            int green = cell.fill() ? FILL_GREEN : OUTLINE_GREEN;
            int blue = cell.fill() ? FILL_BLUE : OUTLINE_BLUE;

            int baseAlpha = cell.fill() ? FILL_ALPHA : OUTLINE_ALPHA;
            int alpha = Mth.clamp(Math.round(baseAlpha * reveal * cell.topFade()), 0, 255);

            if (alpha <= 0) {
                continue;
            }
            addCell(buffer, matrix, centerX, centerY, centerZ, rightX, rightZ, cell.x0(), cell.x1(), cell.y0(), cell.y1(), red, green, blue, alpha);
        }
    }

    private static void addCell(BufferBuilder buffer, Matrix4f matrix, double centerX, double centerY, double centerZ, double rightX, double rightZ, double x0, double x1, double y0, double y1, int red, int green, int blue, int alpha) {

        double leftX = centerX + rightX * x0;
        double leftZ = centerZ + rightZ * x0;

        double rightWorldX = centerX + rightX * x1;
        double rightWorldZ = centerZ + rightZ * x1;

        addVertex(buffer, matrix, leftX, centerY + y0, leftZ, red, green, blue, alpha);

        addVertex(buffer, matrix, leftX, centerY + y1, leftZ, red, green, blue, alpha);

        addVertex(buffer, matrix, rightWorldX, centerY + y1, rightWorldZ, red, green, blue, alpha);

        addVertex(buffer, matrix, rightWorldX, centerY + y0, rightWorldZ, red, green, blue, alpha);
    }


    private static void addVertex(BufferBuilder buffer, Matrix4f matrix, double x, double y, double z, int red, int green, int blue, int alpha) {
        buffer.vertex(matrix, (float) x, (float) y, (float) z).color(red, green, blue, alpha).endVertex();
    }

    private static float getTopFade(double sampleY) {
        double fadeStart = FOUNTAIN_HEIGHT - 1.1D;


        if (sampleY <= fadeStart) {
            return 1.0F;
        }
        return 1.0F - smoothstep(Mth.clamp((float) ((sampleY - fadeStart) / 1.1D), 0.0F, 1.0F));
    }

    private static double metaball(double sampleX, double sampleY, double centerX, double centerY, double radius) {
        double deltaX = sampleX - centerX;
        double deltaY = sampleY - centerY;
        double distanceSqr = deltaX * deltaX + deltaY * deltaY;

        return (radius * radius) / (distanceSqr + 0.035D);
    }

    private static void pruneMeshCache(ResourceKey<Level> dimension, long gameTick) {
        if (gameTick % 20L != 0L) {
            return;
        }

        MESH_CACHE.entrySet().removeIf(entry ->
                !entry.getKey().dimension().equals(dimension) || gameTick - entry.getValue().lastSeenTick > MESH_CACHE_LIFETIME);
    }

    private static float smoothstep(float value) {
        value = Mth.clamp(value, 0.0F, 1.0F);
        return value * value * (3.0F - 2.0F * value);
    }

    private static double smoothstepDouble(double value) {
        value = Mth.clamp(value, 0.0D, 1.0D);
        return value * value * (3.0D - 2.0D * value);
    }

    private static double fract(double value) {
        return value - Math.floor(value);
    }

    private static double hash01(long seed, int index, int salt) {
        long value = seed + index * 0x9E3779B97F4A7C15L + salt * 0xBF58476D1CE4E5B9L;

        value = (value ^ (value >>> 30)) * 0xBF58476D1CE4E5B9L;
        value = (value ^ (value >>> 27)) * 0x94D049BB133111EBL;
        value = value ^ (value >>> 31);

        return (value >>> 11) * 0x1.0p-53;
    }

    private record FountainKey(ResourceKey<Level> dimension, int type, long first, long second) {

        private static FountainKey portal(ResourceKey<Level> dimension, UUID uuid) {
            return new FountainKey(dimension, 0, uuid.getMostSignificantBits(), uuid.getLeastSignificantBits());
        }

        private static FountainKey lamp(ResourceKey<Level> dimension, int variant) {
            return new FountainKey(dimension, 1, variant, 0L);
        }
    }


    private static final class MeshCache {
        private long builtTick = Long.MIN_VALUE;
        private long lastSeenTick;
        private List<MeshCell> cells = List.of();
    }

    private record MeshCell(double x0, double x1, double y0, double y1, boolean fill, float topFade) {
    }
    private record BlobState(double x, double y, double radius) {
    }
}