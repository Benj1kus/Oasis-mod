package com.benji.oasiso.common.block.entity;

import com.benji.oasiso.registry.ModBlockEntities;
import com.benji.oasiso.common.block.FluwirusBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;

import java.util.UUID;

import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

public final class FluwirusBlockEntity extends BlockEntity implements GeoBlockEntity {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    private static final RawAnimation SLEEP = RawAnimation.begin().thenLoop("idle_sleep");
    private static final RawAnimation AWAKE = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation CUT = RawAnimation.begin().thenPlayAndHold("cut");

    private UUID collector;
    private float previousYaw, yaw, previousPitch, pitch;

    public UUID getCollector() {
        return collector;
    }

    public void setCollector(UUID collector) {
        this.collector = collector;
        setChanged();
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        if (collector != null) tag.putUUID("Collector", collector);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        collector = tag.hasUUID("Collector") ? tag.getUUID("Collector") : null;
    }

    public static void lookTick(Level level, BlockPos pos, BlockState state, FluwirusBlockEntity flower) {
        Player nearest = null;
        double distance = 25;
        if (!state.getValue(FluwirusBlock.CUTTING)) {
            for (Player player : level.players()) {
                if (player.isCreative() || player.isSpectator() || !player.isAlive()) continue;
                double d = player.distanceToSqr(pos.getX() + .5, pos.getY() + .5, pos.getZ() + .5);
                if (d <= distance) {
                    nearest = player;
                    distance = d;
                }
            }
        }
        if (!level.isClientSide) {
            boolean shouldWake = nearest != null;
            if (state.getValue(FluwirusBlock.AWAKE) != shouldWake) {
                level.setBlock(pos, state.setValue(FluwirusBlock.AWAKE, shouldWake),
                        net.minecraft.world.level.block.Block.UPDATE_CLIENTS);
            }
            return;
        }

        if (!state.getValue(FluwirusBlock.AWAKE) || state.getValue(FluwirusBlock.CUTTING)) {
            flower.previousYaw = flower.yaw = 0;
            flower.previousPitch = flower.pitch = 0;
            return;
        }
        float targetYaw = 0, targetPitch = 0;
        if (nearest != null) {
            Direction facing = state.getValue(HorizontalDirectionalBlock.FACING);
            double dx = nearest.getX() - pos.getX() - .5;
            double dz = nearest.getZ() - pos.getZ() - .5;
            double forward = dx * facing.getStepX() + dz * facing.getStepZ();
            double right = dz * facing.getStepX() - dx * facing.getStepZ();
            double dy = nearest.getEyeY() - pos.getY() - 15.0 / 16.0;
            targetYaw = (float) Math.toDegrees(Math.atan2(-right, forward));
            targetPitch = Mth.clamp((float) Math.toDegrees(Math.atan2(dy, Math.hypot(dx, dz))), -55, 55);
        }
        flower.previousYaw = flower.yaw;
        flower.previousPitch = flower.pitch;
        flower.yaw = Mth.wrapDegrees(flower.yaw + .22F * Mth.wrapDegrees(targetYaw - flower.yaw));
        flower.pitch = Mth.lerp(.22F, flower.pitch, targetPitch);
    }

    public float lookYaw(float partial) {
        return Mth.rotLerp(partial, previousYaw, yaw) * Mth.DEG_TO_RAD;
    }

    public float lookPitch(float partial) {
        return Mth.lerp(partial, previousPitch, pitch) * Mth.DEG_TO_RAD;
    }

    public FluwirusBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FLUWIRUS_BE.get(), pos, state);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "flower", 6, state -> {
            BlockState block = getBlockState();
            boolean cutting = block.getValue(FluwirusBlock.CUTTING);
            RawAnimation clip = cutting ? CUT : block.getValue(FluwirusBlock.AWAKE) ? AWAKE : SLEEP;

            state.getController().transitionLength(cutting ? 0 : 6);
            return state.setAndContinue(clip);
        }));
    }

    public boolean isPlayingAwakeIdle(long instanceId) {
        BlockState block = getBlockState();
        if (!block.getValue(FluwirusBlock.AWAKE) || block.getValue(FluwirusBlock.CUTTING)) {
            return false;
        }

        var controller = cache.getManagerForId(instanceId)
                .getAnimationControllers().get("flower");

        return controller != null
                && controller.getAnimationState() == AnimationController.State.RUNNING
                && AWAKE.equals(controller.getCurrentRawAnimation());
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    @Override
    public AABB getRenderBoundingBox() {
        return new AABB(worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), worldPosition.getX() + 1, worldPosition.getY() + 2, worldPosition.getZ() + 1);
    }
}
