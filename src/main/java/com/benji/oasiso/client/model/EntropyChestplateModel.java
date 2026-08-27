package com.benji.oasiso.client.model;

import com.benji.oasiso.Oasiso;
import com.benji.oasiso.common.item.EntropyChestplateItem;
import com.benji.oasiso.common.util.EntropyTurretHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.core.animatable.model.CoreGeoBone;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.model.GeoModel;

import java.util.Map;
import java.util.WeakHashMap;

public class EntropyChestplateModel extends GeoModel<EntropyChestplateItem> {

    private static final float MAX_UP_PITCH = 42.0F;

    private static final float YAW_SPEED = 6.0F;
    private static final float PITCH_SPEED = 3.5F;
    private static final float FIRE_RECOIL_DEGREES = -12.5F;

    private final Map<Player, TurretAimState> aimStates = new WeakHashMap<>();

    @Override
    public ResourceLocation getModelResource(EntropyChestplateItem animatable) {
        return ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "geo/entropy_chestplate.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(EntropyChestplateItem animatable) {
        return ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "textures/models/armor/entropy_chestplate.png");
    }

    @Override
    public ResourceLocation getAnimationResource(EntropyChestplateItem animatable) {
        return ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "animations/entropy_chestplate.animation.json");
    }

    @Override
    public void setCustomAnimations(EntropyChestplateItem animatable, long instanceId, AnimationState<EntropyChestplateItem> animationState) {
        super.setCustomAnimations(animatable, instanceId, animationState);

        Entity rawEntity = animationState.getData(DataTickets.ENTITY);
        if (!(rawEntity instanceof Player player)) {
            return;
        }

        ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
        if (!(chest.getItem() instanceof EntropyChestplateItem)) {
            return;
        }

        TurretAimState state = this.aimStates.computeIfAbsent(player, ignored -> new TurretAimState());

        long gameTime = player.level().getGameTime();
        if (!EntropyChestplateItem.areTurretsOperational(chest, gameTime)) {
            state.reset(gameTime);
            return;
        }

        updateAimState(player, state, gameTime);

        float partialTick = Minecraft.getInstance().getFrameTime();

        float leftYaw = Mth.rotLerp(partialTick, state.previousLeftYaw, state.leftYaw);
        float rightYaw = Mth.rotLerp(partialTick, state.previousRightYaw, state.rightYaw);
        float leftPitch = Mth.lerp(partialTick, state.previousLeftPitch, state.leftPitch);
        float rightPitch = Mth.lerp(partialTick, state.previousRightPitch, state.rightPitch);

        applyTurretAim(player, EntropyTurretHelper.Side.LEFT, "turret_left", "turret_left_head", leftYaw, leftPitch, gameTime, partialTick);

        applyTurretAim(player, EntropyTurretHelper.Side.RIGHT, "turret_right", "turret_right_head", rightYaw, rightPitch, gameTime, partialTick);
    }

    private void updateAimState(Player player, TurretAimState state, long gameTime) {
        if (state.lastUpdateTick == gameTime) {
            return;
        }

        long elapsedTicks = state.lastUpdateTick == Long.MIN_VALUE ? 1L : Math.max(1L, Math.min(5L, gameTime - state.lastUpdateTick));

        state.lastUpdateTick = gameTime;

        state.previousLeftYaw = state.leftYaw;
        state.previousRightYaw = state.rightYaw;
        state.previousLeftPitch = state.leftPitch;
        state.previousRightPitch = state.rightPitch;

        EntropyTurretHelper.TargetPair targets = EntropyTurretHelper.findTargets(player);

        LivingEntity leftTarget = targets.left();
        LivingEntity rightTarget = targets.right();

        float targetLeftYaw = getTargetYaw(player, leftTarget);
        float targetRightYaw = getTargetYaw(player, rightTarget);

        float targetLeftPitch = getTargetUpPitch(player, leftTarget);
        float targetRightPitch = getTargetUpPitch(player, rightTarget);

        float yawStep = YAW_SPEED * elapsedTicks;
        float pitchStep = PITCH_SPEED * elapsedTicks;

        state.leftYaw = approachAngle(state.leftYaw, targetLeftYaw, yawStep);
        state.rightYaw = approachAngle(state.rightYaw, targetRightYaw, yawStep);

        state.leftPitch = Mth.approach(state.leftPitch, targetLeftPitch, pitchStep);
        state.rightPitch = Mth.approach(state.rightPitch, targetRightPitch, pitchStep);
    }

    private float getTargetYaw(Player player, LivingEntity target) {
        if (target == null) {
            return 0.0F;
        }
        return -EntropyTurretHelper.getRelativeYawDegrees(player, target);
    }

    private float getTargetUpPitch(Player player, LivingEntity target) {
        if (target == null) {
            return 0.0F;
        }
        float modelPitch = -EntropyTurretHelper.getPitchDegrees(player, target);
        return Mth.clamp(modelPitch, 0.0F, MAX_UP_PITCH);
    }

    private void applyTurretAim(Player player, EntropyTurretHelper.Side side, String baseBoneName, String headBoneName, float yawDegrees, float upPitchDegrees, long gameTime, float partialTick) {
        CoreGeoBone base = getAnimationProcessor().getBone(baseBoneName);
        CoreGeoBone head = getAnimationProcessor().getBone(headBoneName);

        if (base == null || head == null) {
            return;
        }
        base.setRotX(0.0F);
        base.setRotZ(0.0F);
        base.setRotY(yawDegrees * Mth.DEG_TO_RAD);
        float recoilDegrees = getFireRecoilDegrees(player, side, gameTime, partialTick);

        head.setRotY(0.0F);
        head.setRotZ(0.0F);
        head.setRotX((upPitchDegrees + recoilDegrees) * Mth.DEG_TO_RAD);
    }

    private float getFireRecoilDegrees(Player player, EntropyTurretHelper.Side side, long gameTime, float partialTick) {
        long fireUntil = player.getPersistentData().getLong(EntropyChestplateItem.CLIENT_FIRE_UNTIL);

        if (fireUntil <= gameTime) {
            return 0.0F;
        }

        int fireMask = player.getPersistentData().getInt(EntropyChestplateItem.CLIENT_FIRE_MASK);
        int requiredMask = side == EntropyTurretHelper.Side.LEFT ? EntropyChestplateItem.FIRE_LEFT_MASK : EntropyChestplateItem.FIRE_RIGHT_MASK;

        if ((fireMask & requiredMask) == 0) {
            return 0.0F;
        }

        double startTime = fireUntil - EntropyChestplateItem.FIRE_ANIMATION_TICKS;
        double now = gameTime + partialTick;

        float progress = Mth.clamp((float) ((now - startTime) / EntropyChestplateItem.FIRE_ANIMATION_TICKS), 0.0F, 1.0F);
        float pulse = progress <= 0.5F ? progress * 2.0F : (1.0F - progress) * 2.0F;

        return FIRE_RECOIL_DEGREES * Mth.clamp(pulse, 0.0F, 1.0F);
    }

    private static float approachAngle(float current, float target, float maxStep) {
        float difference = Mth.wrapDegrees(target - current);
        difference = Mth.clamp(difference, -maxStep, maxStep);
        return Mth.wrapDegrees(current + difference);
    }

    private static final class TurretAimState {
        private long lastUpdateTick = Long.MIN_VALUE;

        private float previousLeftYaw;
        private float previousRightYaw;
        private float previousLeftPitch;
        private float previousRightPitch;

        private float leftYaw;
        private float rightYaw;
        private float leftPitch;
        private float rightPitch;

        private void reset(long gameTime) {
            this.lastUpdateTick = gameTime;

            this.previousLeftYaw = 0.0F;
            this.previousRightYaw = 0.0F;
            this.previousLeftPitch = 0.0F;
            this.previousRightPitch = 0.0F;

            this.leftYaw = 0.0F;
            this.rightYaw = 0.0F;
            this.leftPitch = 0.0F;
            this.rightPitch = 0.0F;
        }
    }
}
