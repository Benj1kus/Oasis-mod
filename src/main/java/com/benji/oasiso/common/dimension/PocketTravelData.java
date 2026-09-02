package com.benji.oasiso.common.dimension;

import com.benji.oasiso.Oasiso;
import com.benji.oasiso.common.entity.ChaosPortalEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import javax.annotation.Nullable;
import java.util.UUID;

public final class PocketTravelData {

    private static final String ACTIVE = Oasiso.MODID + ":pocket_active";
    private static final String SOURCE_DIMENSION = Oasiso.MODID + ":pocket_source_dimension";
    private static final String SOURCE_PORTAL = Oasiso.MODID + ":pocket_source_portal";
    private static final String RETURN_PORTAL = Oasiso.MODID + ":pocket_return_portal";
    private static final String RETURN_X = Oasiso.MODID + ":pocket_return_x";
    private static final String RETURN_Y = Oasiso.MODID + ":pocket_return_y";
    private static final String RETURN_Z = Oasiso.MODID + ":pocket_return_z";
    private static final String RETURN_YAW = Oasiso.MODID + ":pocket_return_yaw";
    private static final String RETURN_PITCH = Oasiso.MODID + ":pocket_return_pitch";


    private static final double EXIT_OFFSET = 2.6D;


    private PocketTravelData() {
    }


    public static void saveSource(ServerPlayer player, ChaosPortalEntity portal) {
        CompoundTag data = player.getPersistentData();

        double angle = Math.toRadians(portal.getPortalYaw());

        double directionX = Math.sin(angle);
        double directionZ = Math.cos(angle);

        double returnX = portal.getX() + directionX * EXIT_OFFSET;
        double returnY = portal.getY();
        double returnZ = portal.getZ() + directionZ * EXIT_OFFSET;


        data.putBoolean(ACTIVE, true);

        data.putString(SOURCE_DIMENSION, portal.level().dimension().location().toString());

        data.putUUID(SOURCE_PORTAL, portal.getUUID());

        data.putDouble(RETURN_X, returnX);
        data.putDouble(RETURN_Y, returnY);
        data.putDouble(RETURN_Z, returnZ);

        data.putFloat(RETURN_YAW, player.getYRot());
        data.putFloat(RETURN_PITCH, player.getXRot());
    }


    public static void setReturnPortal(ServerPlayer player, UUID portal) {
        player.getPersistentData().putUUID(RETURN_PORTAL, portal);
    }

    public static boolean isActive(ServerPlayer player) {
        return player.getPersistentData().getBoolean(ACTIVE);
    }

    @Nullable
    public static ReturnTarget loadReturnTarget(ServerPlayer player) {
        CompoundTag data = player.getPersistentData();

        if (!data.contains(SOURCE_DIMENSION, Tag.TAG_STRING)) {
            return null;
        }

        ResourceLocation id = ResourceLocation.tryParse(data.getString(SOURCE_DIMENSION));

        if (id == null) {
            return null;
        }

        MinecraftServer server = player.getServer();
        if (server == null) {
            return null;
        }

        ResourceKey<net.minecraft.world.level.Level> key = ResourceKey.create(Registries.DIMENSION, id);
        ServerLevel destination = server.getLevel(key);

        if (destination == null) {
            return null;
        }

        UUID sourcePortal = data.hasUUID(SOURCE_PORTAL) ? data.getUUID(SOURCE_PORTAL) : null;

        return new ReturnTarget(destination, data.getDouble(RETURN_X), data.getDouble(RETURN_Y), data.getDouble(RETURN_Z), data.getFloat(RETURN_YAW), data.getFloat(RETURN_PITCH), sourcePortal);
    }

    public static void clear(ServerPlayer player) {
        CompoundTag data = player.getPersistentData();


        data.remove(ACTIVE);
        data.remove(SOURCE_DIMENSION);
        data.remove(SOURCE_PORTAL);
        data.remove(RETURN_PORTAL);
        data.remove(RETURN_X);
        data.remove(RETURN_Y);
        data.remove(RETURN_Z);
        data.remove(RETURN_YAW);
        data.remove(RETURN_PITCH);
    }


    public record ReturnTarget(ServerLevel level, double x, double y, double z, float yaw, float pitch,
                               @Nullable UUID sourcePortal) {
    }
}