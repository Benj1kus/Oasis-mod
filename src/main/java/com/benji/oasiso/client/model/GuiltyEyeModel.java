package com.benji.oasiso.client.model;

import com.benji.oasiso.Oasiso;
import com.benji.oasiso.common.block.entity.CleanGeoBlockEntity;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.model.GeoModel;

import java.util.Map;
import java.util.WeakHashMap;

public class GuiltyEyeModel extends GeoModel<CleanGeoBlockEntity> {
    public static final double TRACKING_RADIUS = 10;
    private static final String[] PUPILS = {"eball_front", "eball_back", "eball_left", "eball_right"};
    private final Map<CleanGeoBlockEntity, Pose> poses = new WeakHashMap<>();
    private Level currentLevel;

    private static final class Pose {
        final GuiltyEyeLook look = new GuiltyEyeLook();
        double lastTime = Double.NaN;
    }

    @Override
    public ResourceLocation getModelResource(CleanGeoBlockEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "geo/guilty_eye.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(CleanGeoBlockEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "textures/block/guilty_eye.png");
    }

    @Override
    public ResourceLocation getAnimationResource(CleanGeoBlockEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "animations/guilty_eye.animation.json");
    }

    @Override
    public void setCustomAnimations(CleanGeoBlockEntity block, long instanceId, AnimationState<CleanGeoBlockEntity> animationState) {
        super.setCustomAnimations(block, instanceId, animationState);
        Level level = block.getLevel();
        if (level != currentLevel) {
            poses.clear();
            currentLevel = level;
        }
        Pose pose = poses.computeIfAbsent(block, key -> new Pose());
        if (level != null) {
            float partial = animationState.getPartialTick();
            double now = level.getGameTime() + partial;
            double dt = Double.isNaN(pose.lastTime) ? 1 : now - pose.lastTime;
            pose.lastTime = now;
            Vec3 centre = Vec3.atCenterOf(block.getBlockPos());
            Player target = null;
            double nearest = TRACKING_RADIUS * TRACKING_RADIUS;
            for (Player player : level.players()) {
                if (!player.isAlive() || player.isSpectator()) continue;
                double distance = player.distanceToSqr(centre);
                if (distance <= nearest) {
                    target = player;
                    nearest = distance;
                }
            }
            Vec3 relative = Vec3.ZERO;
            if (target != null) {
                relative = target.getEyePosition(partial).subtract(block.getBlockPos().getX() + .5, block.getBlockPos().getY(), block.getBlockPos().getZ() + .5);
                relative = toModel(relative, facing(block.getBlockState()));
            }
            pose.look.update(relative.x, relative.y, relative.z, target != null, dt);
        }
        var head = getAnimationProcessor().getBone("head");
        if (head != null) {
            head.setRotY((float) pose.look.yaw);
            head.setRotX((float) pose.look.pitch);
        }
        for (int i = 0; i < PUPILS.length; i++) {
            var bone = getAnimationProcessor().getBone(PUPILS[i]);
            if (bone != null) {
                bone.setPosX((float) pose.look.pupilX[i]);
                bone.setPosY((float) pose.look.pupilY[i]);
                bone.setPosZ(0);
            }
        }
    }

    private static Direction facing(BlockState state) {
        if (state.hasProperty(HorizontalDirectionalBlock.FACING))
            return state.getValue(HorizontalDirectionalBlock.FACING);
        if (state.hasProperty(DirectionalBlock.FACING)) return state.getValue(DirectionalBlock.FACING);
        return Direction.NORTH;
    }

    private static Vec3 toModel(Vec3 p, Direction facing) {
        return switch (facing) {
            case NORTH -> p;
            case SOUTH -> new Vec3(-p.x, p.y, -p.z);
            case WEST -> new Vec3(-p.z, p.y, p.x);
            case EAST -> new Vec3(p.z, p.y, -p.x);
            case UP -> new Vec3(p.x, p.z, -p.y);
            case DOWN -> new Vec3(p.x, -p.z, p.y);
        };
    }
}
