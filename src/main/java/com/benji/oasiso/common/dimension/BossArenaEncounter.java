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
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;


public final class BossArenaEncounter {

//structure
    private static final ResourceLocation BOSS_ARENA = ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "arena_boss");

    private static final int BASE_X = -12_000_000;
    private static final int STRUCTURE_Y = 80;
    private static final int CELL_SPACING = 512;
    private static final double ARENA_REGION_RADIUS = 220.0D;
    private static final String SESSION_TAG = Oasiso.MODID + ":boss_arena_session";
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

    static boolean enterArenaNow(ServerPlayer player, BossPortalEntity entrancePortal) {
        MinecraftServer server = player.getServer();
        if (server == null) {
            return false;
        }

        ServerLevel destination = server.getLevel(Oasiso.CHAOS_DIMENSION);
        if (destination == null) {
            return false;
        }

        Vec3 arenaSpawn = prepareArena(destination, player);

        if (arenaSpawn == null) {
            return false;
        }
        saveEntrancePortal(player, entrancePortal);

        ChaosReturnData.clear(player);

        player.getPersistentData().putBoolean(SESSION_TAG, true);

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
    private static Vec3 prepareArena(ServerLevel level, ServerPlayer player) {
        StructureTemplate template = level.getStructureManager().get(BOSS_ARENA).orElse(null);

        if (template == null) {
            return null;
        }

        BlockPos origin = getPersonalOrigin(player.getUUID());
        Vec3i size = template.getSize();

        removeOldArenaEntities(level, origin, size);
        StructurePlaceSettings settings = new StructurePlaceSettings().setMirror(Mirror.NONE).setRotation(Rotation.NONE).setIgnoreEntities(false);

        boolean placed = template.placeInWorld(level, origin, origin, settings, level.random, 3);

        if (!placed) {
            return null;
        }

        BlockPos spawnMarker = findLieBlock(level, origin, size);
        if (spawnMarker == null) {
            return null;
        }
        return new Vec3(spawnMarker.getX() + 0.5D, spawnMarker.getY() + 1.05D, spawnMarker.getZ() + 0.5D);
    }


    public static BlockPos getPersonalOrigin(UUID uuid) {

        int cellX = (int) (uuid.getMostSignificantBits() & 0xFFFL) - 2048;
        int cellZ = (int) (uuid.getLeastSignificantBits() & 0xFFFL) - 2048;

        return new BlockPos(BASE_X + cellX * CELL_SPACING, STRUCTURE_Y, cellZ * CELL_SPACING);
    }

    public static boolean isInsideArenaArea(UUID playerId, double x, double z) {
        BlockPos origin = getPersonalOrigin(playerId);

        return Math.abs(x - origin.getX()) <= ARENA_REGION_RADIUS && Math.abs(z - origin.getZ()) <= ARENA_REGION_RADIUS;
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

        returnPortal.startDespawning();

        spawnBurst(player.serverLevel(), player.getX(), player.getY() + 1.0D, player.getZ());

        player.stopRiding();
        player.teleportTo(destination, x, y, z, yaw, pitch);

        player.setDeltaMovement(Vec3.ZERO);

        player.fallDistance = 0.0F;
        player.hurtMarked = true;

        spawnBurst(destination, x, y + 1.0D, z);

        closeEntrancePortal(destination, data, x, y, z);
        clearSession(player);

        return true;
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
        return player.getPersistentData().getBoolean(SESSION_TAG);
    }

    public static void clearSession(ServerPlayer player) {
        CompoundTag data = player.getPersistentData();

        data.remove(SESSION_TAG);
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