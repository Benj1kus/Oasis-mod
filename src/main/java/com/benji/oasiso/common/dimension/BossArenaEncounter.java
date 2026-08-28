package com.benji.oasiso.common.dimension;

import com.benji.oasiso.Oasiso;
import com.benji.oasiso.common.entity.AzumaalEntity;
import com.benji.oasiso.common.entity.BossPortalEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public final class BossArenaEncounter {

    private static final ResourceLocation BOSS_ARENA = ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "arena_boss");

    private static final int BASE_X = -12_000_000;
    private static final int STRUCTURE_Y = 80;
    private static final int CELL_SPACING = 512;
    private static final int CELL_MIN = -2048;
    private static final int CELL_MAX = 2047;

    private static final double ARENA_REGION_RADIUS = 220.0D;

    private static final String SESSION_TAG = Oasiso.MODID + ":boss_arena_session";

    private static final String SESSION_ID_TAG = Oasiso.MODID + ":boss_arena_session_id";

    private static final String RETURN_DIMENSION = Oasiso.MODID + ":boss_arena_return_dimension";

    private static final String RETURN_X = Oasiso.MODID + ":boss_arena_return_x";

    private static final String RETURN_Y = Oasiso.MODID + ":boss_arena_return_y";

    private static final String RETURN_Z = Oasiso.MODID + ":boss_arena_return_z";

    private static final String RETURN_YAW = Oasiso.MODID + ":boss_arena_return_yaw";

    private static final String RETURN_PITCH = Oasiso.MODID + ":boss_arena_return_pitch";

    private static final String RETURN_PORTAL = Oasiso.MODID + ":boss_arena_return_portal";

    private static final double RETURN_PORTAL_OFFSET = 2.6D;

    private BossArenaEncounter() {
    }

    public static boolean prepareArenaForPortal(MinecraftServer server, BossPortalEntity entrancePortal) {
        ServerLevel destination = server.getLevel(Oasiso.CHAOS_DIMENSION);

        if (destination == null) {
            return false;
        }

        UUID sessionId = entrancePortal.getOrCreateArenaSessionId();

        return prepareArena(destination, entrancePortal, sessionId) != null;
    }

    static boolean enterArenaNow(ServerPlayer player, BossPortalEntity entrancePortal) {
        return enterArenaNow(player, entrancePortal, entrancePortal.getOrCreateArenaSessionId());
    }

    static boolean enterArenaNow(ServerPlayer player, BossPortalEntity entrancePortal, UUID sessionId) {
        MinecraftServer server = player.getServer();

        if (server == null) {
            return false;
        }

        ServerLevel destination = server.getLevel(Oasiso.CHAOS_DIMENSION);

        if (destination == null) {
            return false;
        }

        if (sessionId == null) {
            return false;
        }

        Vec3 arenaSpawn = prepareArena(destination, entrancePortal, sessionId);

        if (arenaSpawn == null) {
            return false;
        }

        keepDestinationReady(destination, arenaSpawn, player);

        saveEntrancePortal(player, entrancePortal);

        ChaosReturnData.clear(player);

        CompoundTag playerData = player.getPersistentData();

        playerData.putBoolean(SESSION_TAG, true);
        playerData.putUUID(SESSION_ID_TAG, sessionId);

        spawnBurst(player.serverLevel(), player.getX(), player.getY() + player.getBbHeight() * 0.5D, player.getZ());

        player.stopRiding();

        player.teleportTo(destination, arenaSpawn.x, arenaSpawn.y, arenaSpawn.z, player.getYRot(), player.getXRot());

        player.setDeltaMovement(Vec3.ZERO);

        player.fallDistance = 0.0F;
        player.hurtMarked = true;

        spawnBurst(destination, arenaSpawn.x, arenaSpawn.y + 1.0D, arenaSpawn.z);

        return true;
    }

    @Nullable
    private static Vec3 prepareArena(ServerLevel level, BossPortalEntity entrancePortal, UUID sessionId) {
        StructureTemplate template = level.getStructureManager().get(BOSS_ARENA).orElse(null);

        if (template == null) {
            return null;
        }

        BlockPos origin = getSessionOrigin(sessionId);

        Vec3i size = template.getSize();
        loadArenaChunks(level, origin, size);
        BlockPos spawnMarker = findLieBlock(level, origin, size);

        if (spawnMarker != null) {
            entrancePortal.markArenaPrepared();
        }

        if (spawnMarker == null) {
            removeOldArenaEntities(level, origin, size);

            StructurePlaceSettings settings = new StructurePlaceSettings().setMirror(Mirror.NONE).setRotation(Rotation.NONE).setIgnoreEntities(false);

            boolean placed = template.placeInWorld(level, origin, origin, settings, level.random, 3);

            if (!placed) {
                return null;
            }

            entrancePortal.markArenaPrepared();
            spawnMarker = findLieBlock(level, origin, size);
        }

        if (spawnMarker == null) {
            return null;
        }

        return findPartySpawn(level, spawnMarker, playerOffsetSeed(sessionId));
    }

    private static void loadArenaChunks(ServerLevel level, BlockPos origin, Vec3i size) {
        BlockPos max = origin.offset(Math.max(0, size.getX() - 1), 0, Math.max(0, size.getZ() - 1));

        ChunkPos minChunk = new ChunkPos(origin);

        ChunkPos maxChunk = new ChunkPos(max);
        for (int chunkX = minChunk.x - 1; chunkX <= maxChunk.x + 1; chunkX++) {

            for (int chunkZ = minChunk.z - 1; chunkZ <= maxChunk.z + 1; chunkZ++) {

                level.getChunk(chunkX, chunkZ);
            }
        }
    }

    private static void keepDestinationReady(ServerLevel level, Vec3 spawn, ServerPlayer player) {
        BlockPos spawnPos = BlockPos.containing(spawn);

        ChunkPos spawnChunk = new ChunkPos(spawnPos);
        level.getChunkSource().addRegionTicket(TicketType.POST_TELEPORT, spawnChunk, 1, player.getId());

        level.getChunk(spawnChunk.x, spawnChunk.z);
    }

    private static Vec3 findPartySpawn(ServerLevel level, BlockPos marker, int ignoredSeed) {
        return new Vec3(marker.getX() + 0.5D, marker.getY() + 1.05D, marker.getZ() + 0.5D);
    }

    private static int playerOffsetSeed(UUID sessionId) {
        return sessionId.hashCode();
    }

    public static BlockPos getSessionOrigin(UUID sessionId) {
        int cellX = (int) (sessionId.getMostSignificantBits() & 0xFFFL) - 2048;

        int cellZ = (int) (sessionId.getLeastSignificantBits() & 0xFFFL) - 2048;

        return new BlockPos(BASE_X + cellX * CELL_SPACING, STRUCTURE_Y, cellZ * CELL_SPACING);
    }
    public static BlockPos getPersonalOrigin(UUID uuid) {
        return getSessionOrigin(uuid);
    }

    public static boolean isInsideAnyArenaArea(double x, double z) {
        long cellX = Math.round((x - BASE_X) / CELL_SPACING);

        long cellZ = Math.round(z / CELL_SPACING);

        if (cellX < CELL_MIN || cellX > CELL_MAX || cellZ < CELL_MIN || cellZ > CELL_MAX) {

            return false;
        }

        double originX = BASE_X + cellX * CELL_SPACING;

        double originZ = cellZ * CELL_SPACING;

        return Math.abs(x - originX) <= ARENA_REGION_RADIUS && Math.abs(z - originZ) <= ARENA_REGION_RADIUS;
    }
    public static boolean isInsideArenaArea(UUID ignoredPlayerId, double x, double z) {
        return isInsideAnyArenaArea(x, z);
    }

    @Nullable
    private static BlockPos findLieBlock(ServerLevel level, BlockPos origin, Vec3i size) {
        BlockPos max = origin.offset(size.getX() - 1, size.getY() - 1, size.getZ() - 1);

        for (BlockPos pos : BlockPos.betweenClosed(origin, max)) {

            if (level.getBlockState(pos).is(Oasiso.LIE_BLOCK.get())) {

                return pos.immutable();
            }
        }

        return null;
    }

    private static void removeOldArenaEntities(ServerLevel level, BlockPos origin, Vec3i size) {
        AABB area = new AABB(origin.getX(), origin.getY(), origin.getZ(), origin.getX() + size.getX(), origin.getY() + size.getY(), origin.getZ() + size.getZ()).inflate(8.0D);

        for (AzumaalEntity azumaal : level.getEntitiesOfClass(AzumaalEntity.class, area)) {

            azumaal.discard();
        }

        for (BossPortalEntity portal : level.getEntitiesOfClass(BossPortalEntity.class, area)) {

            portal.discard();
        }
    }

    private static void saveEntrancePortal(ServerPlayer player, BossPortalEntity portal) {
        CompoundTag data = player.getPersistentData();

        data.putString(RETURN_DIMENSION, portal.level().dimension().location().toString());
        data.putUUID(RETURN_PORTAL, portal.getUUID());

        double deltaX = player.getX() - portal.getX();
        double deltaZ = player.getZ() - portal.getZ();

        double length = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);

        if (length < 0.05D) {
            Vec3 look = player.getLookAngle();

            deltaX = -look.x;
            deltaZ = -look.z;

            length = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);
        }

        if (length < 0.001D) {
            deltaX = 1.0D;
            deltaZ = 0.0D;
            length = 1.0D;
        }

        deltaX /= length;
        deltaZ /= length;

        data.putDouble(RETURN_X, portal.getX() + deltaX * RETURN_PORTAL_OFFSET);
        data.putDouble(RETURN_Y, portal.getY() + 1.0D);
        data.putDouble(RETURN_Z, portal.getZ() + deltaZ * RETURN_PORTAL_OFFSET);
        data.putFloat(RETURN_YAW, player.getYRot());
        data.putFloat(RETURN_PITCH, player.getXRot());
    }

    public static boolean returnToEntranceNow(ServerPlayer player, BossPortalEntity returnPortal) {
        if (!isArenaSession(player)) {
            return false;
        }

        UUID sessionId = getArenaSessionId(player);

        if (sessionId == null) {
            return false;
        }

        UUID portalSession = returnPortal.getArenaSessionId();

        if (portalSession != null && !portalSession.equals(sessionId)) {

            return false;
        }

        CompoundTag data = player.getPersistentData();

        if (!data.contains(RETURN_DIMENSION, Tag.TAG_STRING)) {

            return false;
        }

        ResourceLocation dimensionId = ResourceLocation.tryParse(data.getString(RETURN_DIMENSION));

        if (dimensionId == null) {
            return false;
        }

        MinecraftServer server = player.getServer();

        if (server == null) {
            return false;
        }

        ResourceKey<Level> dimension = ResourceKey.create(Registries.DIMENSION, dimensionId);

        ServerLevel destination = server.getLevel(dimension);

        if (destination == null) {
            return false;
        }

        double x = data.getDouble(RETURN_X);

        double y = data.getDouble(RETURN_Y);

        double z = data.getDouble(RETURN_Z);

        float yaw = data.getFloat(RETURN_YAW);

        float pitch = data.getFloat(RETURN_PITCH);

        spawnBurst(player.serverLevel(), player.getX(), player.getY() + 1.0D, player.getZ());

        player.stopRiding();

        player.teleportTo(destination, x, y, z, yaw, pitch);

        player.setDeltaMovement(Vec3.ZERO);

        player.fallDistance = 0.0F;
        player.hurtMarked = true;

        spawnBurst(destination, x, y + 1.0D, z);

        boolean lastOnlineMember = !hasOtherOnlineSessionPlayers(server, sessionId, player.getUUID());

        if (lastOnlineMember) {
            returnPortal.startDespawning();
            closeEntrancePortal(destination, data, x, y, z);
        }

        clearSession(player);

        return true;
    }

    private static boolean hasOtherOnlineSessionPlayers(MinecraftServer server, UUID sessionId, UUID excludingPlayer) {
        for (ServerPlayer candidate : server.getPlayerList().getPlayers()) {

            if (candidate.getUUID().equals(excludingPlayer)) {

                continue;
            }

            if (!candidate.level().dimension().equals(Oasiso.CHAOS_DIMENSION)) {

                continue;
            }

            if (isPlayerInSession(candidate, sessionId)) {

                return true;
            }
        }

        return false;
    }

    private static void closeEntrancePortal(ServerLevel level, CompoundTag data, double x, double y, double z) {
        if (data.hasUUID(RETURN_PORTAL)) {

            UUID portalId = data.getUUID(RETURN_PORTAL);

            Entity entity = level.getEntity(portalId);

            if (entity instanceof BossPortalEntity portal && portal.isChaosEntryPortal()) {

                portal.startDespawning();
                return;
            }
        }

        AABB area = new AABB(x - 5.0D, y - 4.0D, z - 5.0D, x + 5.0D, y + 4.0D, z + 5.0D);

        for (BossPortalEntity portal : level.getEntitiesOfClass(BossPortalEntity.class, area, BossPortalEntity::isChaosEntryPortal)) {

            portal.startDespawning();
            return;
        }
    }

    public static boolean isArenaSession(ServerPlayer player) {
        CompoundTag data = player.getPersistentData();

        return data.getBoolean(SESSION_TAG) && data.hasUUID(SESSION_ID_TAG);
    }

    @Nullable
    public static UUID getArenaSessionId(ServerPlayer player) {
        CompoundTag data = player.getPersistentData();

        if (!data.getBoolean(SESSION_TAG) || !data.hasUUID(SESSION_ID_TAG)) {

            return null;
        }

        return data.getUUID(SESSION_ID_TAG);
    }

    public static boolean isPlayerInSession(ServerPlayer player, @Nullable UUID sessionId) {
        if (sessionId == null) {
            return false;
        }

        UUID playerSession = getArenaSessionId(player);

        return sessionId.equals(playerSession);
    }

    @Nullable
    public static UUID findNearbySessionId(ServerLevel level, Vec3 position, double range) {
        UUID result = null;
        double bestDistance = range * range;

        for (ServerPlayer player : level.players()) {

            if (!player.isAlive() || player.isSpectator() || !isArenaSession(player)) {

                continue;
            }

            double distance = player.position().distanceToSqr(position);

            if (distance >= bestDistance) {
                continue;
            }

            UUID session = getArenaSessionId(player);

            if (session == null) {
                continue;
            }

            result = session;
            bestDistance = distance;
        }

        return result;
    }

    public static void clearSession(ServerPlayer player) {
        CompoundTag data = player.getPersistentData();

        data.remove(SESSION_TAG);

        data.remove(SESSION_ID_TAG);

        data.remove(RETURN_DIMENSION);

        data.remove(RETURN_X);

        data.remove(RETURN_Y);

        data.remove(RETURN_Z);

        data.remove(RETURN_YAW);

        data.remove(RETURN_PITCH);

        data.remove(RETURN_PORTAL);
    }

    private static void spawnBurst(ServerLevel level, double x, double y, double z) {
        level.sendParticles(Oasiso.PURPLE_STARS.get(), x, y, z, 70, 0.8D, 1.0D, 0.8D, 0.10D);
    }
}
