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

    private static final float MAX_YAW = 80.0F;
    private static final float MAX_UP_PITCH = 42.0F;
    // Degrees 20 t ~ 1 s
    private static final float YAW_SPEED = 6.0F;
    private static final float PITCH_SPEED = 3.5F;

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

        applyTurretAim("turret_left", "turret_left_head", leftYaw, leftPitch);

        applyTurretAim("turret_right", "turret_right_head", rightYaw, rightPitch);
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

        LivingEntity leftTarget = EntropyTurretHelper.findTarget(player, EntropyTurretHelper.Side.LEFT);

        LivingEntity rightTarget = EntropyTurretHelper.findTarget(player, EntropyTurretHelper.Side.RIGHT);

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

        return Mth.clamp(EntropyTurretHelper.getRelativeYawDegrees(player, target), -MAX_YAW, MAX_YAW);
    }

    private float getTargetUpPitch(Player player, LivingEntity target) {
        if (target == null) {
            return 0.0F;
        }

        float modelPitch = -EntropyTurretHelper.getPitchDegrees(player, target);
        return Mth.clamp(modelPitch, 0.0F, MAX_UP_PITCH);
    }

    private void applyTurretAim(String baseBoneName, String headBoneName, float yawDegrees, float upPitchDegrees) {
        CoreGeoBone base = getAnimationProcessor().getBone(baseBoneName);
        CoreGeoBone head = getAnimationProcessor().getBone(headBoneName);

        if (base == null || head == null) {
            return;
        }
        base.setRotY(yawDegrees * Mth.DEG_TO_RAD);
        float recoilX = head.getRotX();
        head.setRotX(recoilX + upPitchDegrees * Mth.DEG_TO_RAD);
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
