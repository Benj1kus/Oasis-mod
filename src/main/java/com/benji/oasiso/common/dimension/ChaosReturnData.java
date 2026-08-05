package com.benji.oasiso.common.dimension;

import com.benji.oasiso.Oasiso;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

public final class ChaosReturnData {

    private static final String RETURN_X =
            Oasiso.MODID + ":chaos_return_x";

    private static final String RETURN_Y =
            Oasiso.MODID + ":chaos_return_y";

    private static final String RETURN_Z =
            Oasiso.MODID + ":chaos_return_z";

    private static final String RETURN_YAW =
            Oasiso.MODID + ":chaos_return_yaw";

    private static final String RETURN_PITCH =
            Oasiso.MODID + ":chaos_return_pitch";

    private static final double PORTAL_EXIT_OFFSET = 2.5D;

    private ChaosReturnData() {
    }

    public static void save(
            ServerPlayer player,
            BlockPos portalPos,
            Direction.Axis portalAxis
    ) {
        double returnX = player.getX();
        double returnY = player.getY();
        double returnZ = player.getZ();

        if (portalAxis == Direction.Axis.X) {

            double side =
                    player.getZ()
                            - (portalPos.getZ() + 0.5D);

            if (Math.abs(side) < 0.05D) {
                side = player.getLookAngle().z;
            }

            returnZ += side >= 0.0D
                    ? PORTAL_EXIT_OFFSET
                    : -PORTAL_EXIT_OFFSET;
        } else {

            double side =
                    player.getX()
                            - (portalPos.getX() + 0.5D);

            if (Math.abs(side) < 0.05D) {
                side = player.getLookAngle().x;
            }

            returnX += side >= 0.0D
                    ? PORTAL_EXIT_OFFSET
                    : -PORTAL_EXIT_OFFSET;
        }

        CompoundTag data =
                player.getPersistentData();

        data.putDouble(RETURN_X, returnX);
        data.putDouble(RETURN_Y, returnY);
        data.putDouble(RETURN_Z, returnZ);

        data.putFloat(
                RETURN_YAW,
                player.getYRot()
        );

        data.putFloat(
                RETURN_PITCH,
                player.getXRot()
        );
    }

    public static ReturnLocation load(
            ServerPlayer player,
            ServerLevel overworld
    ) {
        CompoundTag data =
                player.getPersistentData();

        if (data.contains(RETURN_X, Tag.TAG_DOUBLE)
                && data.contains(RETURN_Y, Tag.TAG_DOUBLE)
                && data.contains(RETURN_Z, Tag.TAG_DOUBLE)) {

            return new ReturnLocation(
                    data.getDouble(RETURN_X),
                    data.getDouble(RETURN_Y),
                    data.getDouble(RETURN_Z),
                    data.getFloat(RETURN_YAW),
                    data.getFloat(RETURN_PITCH)
            );
        }


        BlockPos spawnPos =
                overworld.getSharedSpawnPos();

        return new ReturnLocation(
                spawnPos.getX() + 0.5D,
                spawnPos.getY() + 1.0D,
                spawnPos.getZ() + 0.5D,
                player.getYRot(),
                player.getXRot()
        );
    }

    public static void clear(
            ServerPlayer player
    ) {
        CompoundTag data =
                player.getPersistentData();

        data.remove(RETURN_X);
        data.remove(RETURN_Y);
        data.remove(RETURN_Z);
        data.remove(RETURN_YAW);
        data.remove(RETURN_PITCH);
    }

    public record ReturnLocation(
            double x,
            double y,
            double z,
            float yaw,
            float pitch
    ) {
    }
}