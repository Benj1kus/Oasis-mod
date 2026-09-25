package com.benji.oasiso.common.entity;

import com.benji.oasiso.Oasiso;
import com.benji.oasiso.common.entity.ai.ApollyonTeleportController;
import com.benji.oasiso.common.entity.ai.ApollyonSpearAttack;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

public class ApollyonEntity extends Monster implements GeoEntity, GlowmaskEntity, MiniBossHealthBar {

    private static final EntityDataAccessor<Long> TELEPORT_START = SynchedEntityData.defineId(
            ApollyonEntity.class, EntityDataSerializers.LONG);
    public static final int TELEPORT_OUT = 8;
    public static final int TELEPORT_HIDDEN = 4;
    public static final int TELEPORT_IN = 8;
    public static final int TELEPORT_TOTAL = TELEPORT_OUT + TELEPORT_HIDDEN + TELEPORT_IN;

    private final ApollyonTeleportController teleportController = new ApollyonTeleportController(this);
    private static final EntityDataAccessor<Integer> COMBAT_MODE = SynchedEntityData.defineId(ApollyonEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> PUSHED_PLAYER = SynchedEntityData.defineId(ApollyonEntity.class, EntityDataSerializers.INT);

    private final ApollyonSpearAttack spearAttack = new ApollyonSpearAttack(this);
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation ATTACK = RawAnimation.begin().thenPlay("attack");
    private static final RawAnimation JUMP = RawAnimation.begin().thenPlayAndHold("jump");
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public ApollyonEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        setNoGravity(true);
    }

    @Override
    public boolean showMiniBossHealthBar() {
        return this.isAlive() && !isTeleporting();
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(TELEPORT_START, -1L);
        entityData.define(COMBAT_MODE, 0);
        entityData.define(PUSHED_PLAYER, -1);
    }

    public int getCombatMode() {
        return entityData.get(COMBAT_MODE);
    }

    public void setCombatMode(int mode) {
        entityData.set(COMBAT_MODE, mode);
    }

    public int getPushedPlayerId() {
        return entityData.get(PUSHED_PLAYER);
    }

    public void setPushedPlayerId(int id) {
        entityData.set(PUSHED_PLAYER, id);
    }

    public boolean isPushingPlayer() {
        return isAlive() && getCombatMode() == 1 && getPushedPlayerId() >= 0;
    }

    public long getTeleportStart() {
        return entityData.get(TELEPORT_START);
    }

    public boolean isTeleporting() {
        return isAlive() && getTeleportStart() >= 0;
    }

    public boolean isAttackControllingMovement() {
        return attackControlsMovement;
    }

    public float teleportAge(float partial) {
        return isTeleporting() ? (float) (level().getGameTime() - getTeleportStart()) + partial : -1;
    }

    public float teleportDissolve(float partial) {
        float age = teleportAge(partial);
        if (age < 0) return 0;
        if (age < TELEPORT_OUT) return Mth.clamp(age / TELEPORT_OUT, 0, 1);
        return 1 - Mth.clamp((age - TELEPORT_OUT - TELEPORT_HIDDEN) / TELEPORT_IN, 0, 1);
    }

    public void beginTeleport() {
        entityData.set(TELEPORT_START, level().getGameTime());
        getNavigation().stop();
        setDeltaMovement(Vec3.ZERO);
    }

    public void endTeleport() {
        entityData.set(TELEPORT_START, -1L);
        hoverY = Double.NaN;
        setDeltaMovement(Vec3.ZERO);
    }

    @Override
    public void lerpTo(double x, double y, double z, float yaw, float pitch, int steps, boolean teleport) {
        if (isTeleporting()) {
            super.lerpTo(x, y, z, yaw, pitch, 0, teleport);
            setPos(x, y, z);
            xo = x;
            yo = y;
            zo = z;
            setYRot(yaw);
            setXRot(pitch);
            yRotO = yaw;
            xRotO = pitch;
        } else {
            super.lerpTo(x, y, z, yaw, pitch, steps, teleport);
        }
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 650.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D)
                .add(Attributes.ATTACK_DAMAGE, 15.0D)
                .add(Attributes.FOLLOW_RANGE, 30.0D);
    }

    @Override
    protected void registerGoals() {
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, false, false));
    }

    public static final double HOVER_HEIGHT = 6.0;
    private double hoverY = Double.NaN;
    private boolean attackControlsMovement;

    public void setAttackControlsMovement(boolean active) {
        attackControlsMovement = active;
        if (!active) hoverY = Double.NaN;
    }

    @Override
    public boolean isNoGravity() {
        return true;
    }

    @Override
    public boolean onClimbable() {
        return false;
    }

    @Override
    protected void checkFallDamage(double dy, boolean ground, BlockState state, BlockPos pos) {
        fallDistance = 0;
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (!isAlive()) {
            spearAttack.cancel();
            return;
        }
        faceTarget();
        if (spearAttack.tick()) return;
        if (teleportController.tick()) return;
        if (attackControlsMovement) return;
        updateHover(.30, .12, .18);
    }

    public void resetHover() {
        hoverY = Double.NaN;
    }

    public boolean atHoverHeight(double tolerance) {
        return !Double.isNaN(hoverY) && Math.abs(getY() - hoverY) < tolerance;
    }

    public void updateHover(double maxSpeed, double response, double smoothing) {
        if (tickCount % 4 == 0 || Double.isNaN(hoverY)) {
            Vec3 from = position().add(0, .5, 0);
            Vec3 to = new Vec3(getX(), level().getMinBuildHeight(), getZ());
            var hit = level().clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
            hoverY = hit.getType() == HitResult.Type.BLOCK ? hit.getLocation().y + HOVER_HEIGHT : getY();
        }
        double bob = Math.sin((tickCount + getId() * 7) * .045) * .09;
        double desiredSpeed = Mth.clamp((hoverY + bob - getY()) * response, -maxSpeed, maxSpeed);
        double speed = Mth.lerp(smoothing, getDeltaMovement().y, desiredSpeed);
        setDeltaMovement(0, speed, 0);
    }

    private void faceTarget() {
        var target = getTarget();
        if (target == null || !target.isAlive()) return;
        double dx = target.getX() - getX(), dz = target.getZ() - getZ();
        if (dx * dx + dz * dz < .0001) return;
        float yaw = (float) (Math.atan2(dz, dx) * 180 / Math.PI) - 90;
        setYRot(Mth.approachDegrees(getYRot(), yaw, 20));
        yBodyRot = yHeadRot = getYRot();
    }

    @Override
    public void tick() {
        super.tick();
        if (isAlive()) yBodyRot = yHeadRot = getYRot();
        else if (!level().isClientSide) spearAttack.cancel();
    }

    @Override
    public void travel(Vec3 input) {
        if (isEffectiveAi()) {
            if (isTeleporting()) setDeltaMovement(Vec3.ZERO);
            move(MoverType.SELF, getDeltaMovement());
            setDeltaMovement(getDeltaMovement().scale(.96));
        }
        calculateEntityAnimation(false);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<ApollyonEntity>(this, "controller", 0, event ->
                event.setAndContinue(switch (getCombatMode()) {
                    case 1 -> ATTACK;
                    case 2 -> JUMP;
                    default -> IDLE;
                })).setCustomInstructionKeyframeHandler(event -> {
        }));
    }

    @Override
    public ResourceLocation getGlowmaskTexture() {
        int frame = (this.tickCount / 6) % 6;
        String textureName = frame == 0 ? "apollyon_emissive" : "apollyon_emissive_frame" + (frame + 1);
        return ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "textures/entity/emissive/" + textureName + ".png");
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }
}
