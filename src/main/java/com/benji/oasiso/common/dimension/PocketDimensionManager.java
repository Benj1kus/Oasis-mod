package com.benji.oasiso.common.dimension;

import com.benji.oasiso.Oasiso;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.UUID;

public final class PocketDimensionManager {

    private static final ResourceLocation PLATFORM = ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "platform");

    private static final int BASE_X = 20_000_000;
    private static final int BASE_Z = 20_000_000;
    private static final int STRUCTURE_Y = 80;
    private static final int CELL_SPACING = 1024;
    private static final int GRID_WIDTH = 8000;

    private PocketDimensionManager() {
    }

    @Nullable
    public static PlatformTarget preparePlatform(MinecraftServer server, UUID owner) {
        ServerLevel chaos = server.getLevel(Oasiso.CHAOS_DIMENSION);

        if (chaos == null) {
            return null;
        }

        PocketPlatformSavedData data = PocketPlatformSavedData.get(server);
        PocketPlatformSavedData.PlatformRecord record = data.getPlatform(owner);

        if (record == null) {
            long slot = data.allocateSlot();
            BlockPos origin = getOriginForSlot(slot);
            if (origin == null) {
                return null;
            }
            record = data.createPlatform(owner, slot, origin);
        }

        if (record.generated() && record.marker() != null) {
            loadChunk(chaos, record.marker());
            return new PlatformTarget(record.origin(), record.marker());
        }

        StructureTemplate template = chaos.getStructureManager().get(PLATFORM).orElse(null);

        if (template == null) {
            return null;
        }

        Vec3i size = template.getSize();


        if (size.getX() >= CELL_SPACING - 64
                || size.getZ() >= CELL_SPACING - 64) {
            return null;
        }

        BlockPos origin = record.origin();

        loadStructureChunks(chaos, origin, size);

        StructurePlaceSettings settings = new StructurePlaceSettings().setMirror(Mirror.NONE).setRotation(Rotation.NONE).setIgnoreEntities(false);

        boolean placed = template.placeInWorld(chaos,
                origin, origin,
                settings,
                chaos.random,
                3);


        if (!placed) {
            return null;
        }

        BlockPos marker = findSpawnMarker(chaos, origin, size);
        if (marker == null) {
            return null;
        }

        data.markGenerated(owner, marker);
        return new PlatformTarget(origin, marker);
    }

    @Nullable
    private static BlockPos getOriginForSlot(long slot) {
        long cellX = slot % GRID_WIDTH;
        long cellZ = slot / GRID_WIDTH;

        long x = BASE_X + cellX * CELL_SPACING;
        long z = BASE_Z + cellZ * CELL_SPACING;

        if (x > 29_000_000L || z > 29_000_000L) {
            return null;
        }

        if (BossArenaEncounter.isInsideAnyArenaArea(x, z)) {
            return null;
        }
        return new BlockPos((int) x, STRUCTURE_Y, (int) z);
    }

    @Nullable
    private static BlockPos findSpawnMarker(ServerLevel level, BlockPos origin, Vec3i size) {
        BlockPos max = origin.offset(size.getX() - 1, size.getY() - 1, size.getZ() - 1);

        for (BlockPos pos : BlockPos.betweenClosed(origin, max)) {
            if (level.getBlockState(pos).is(Oasiso.NEPHRITIS_LAMP.get())) {
                return pos.immutable();
            }
        }
        return null;
    }

    public static Vec3 getPlayerSpawn(PlatformTarget platform) {
        BlockPos lamp = platform.marker();
        return new Vec3(lamp.getX() + 0.5D, lamp.getY() + 1.05D, lamp.getZ() + 0.5D);
    }

    public static ReturnPortalPlacement getReturnPortalPlacement(PlatformTarget platform) {
        BlockPos lamp = platform.marker();

        double x = lamp.getX() + 3.5D;
        double y = lamp.getY() + 1.0D;
        double z = lamp.getZ() + 0.5D;

        double dx = lamp.getX() + 0.5D - x;
        double dz = lamp.getZ() + 0.5D - z;

        float yaw = (float) Math.toDegrees(Math.atan2(dx, dz));
        return new ReturnPortalPlacement(new Vec3(x, y, z),
                yaw);
    }

    public static void keepDestinationReady(ServerLevel level, BlockPos marker, ServerPlayer player) {
        ChunkPos chunk = new ChunkPos(marker);
        level.getChunkSource().addRegionTicket(TicketType.POST_TELEPORT, chunk, 1, player.getId());
        level.getChunk(chunk.x, chunk.z);
    }

    private static void loadChunk(ServerLevel level, BlockPos pos) {
        ChunkPos chunk = new ChunkPos(pos);
        level.getChunk(chunk.x, chunk.z);
    }


    private static void loadStructureChunks(ServerLevel level, BlockPos origin, Vec3i size) {
        BlockPos max = origin.offset(Math.max(0, size.getX() - 1), 0, Math.max(0, size.getZ() - 1));

        ChunkPos min = new ChunkPos(origin);
        ChunkPos maxChunk = new ChunkPos(max);

        for (int x = min.x - 1; x <= maxChunk.x + 1; x++) {
            for (int z = min.z - 1; z <= maxChunk.z + 1; z++) {
                level.getChunk(x, z);
            }
        }
    }

    public record PlatformTarget(BlockPos origin, BlockPos marker) {
    }

    public record ReturnPortalPlacement(Vec3 position, float yaw) {
    }
}