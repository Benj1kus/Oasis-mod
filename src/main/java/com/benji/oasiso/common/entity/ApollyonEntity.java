package com.benji.oasiso.common.entity;

import com.benji.oasiso.Oasiso;
import com.benji.oasiso.common.entity.ai.ApollyonTeleportController;
import com.benji.oasiso.common.entity.ai.ApollyonCombatController;
import org.joml.Vector3f;
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

    private static final EntityDataAccessor<Long> TELEPORT_START = SynchedEntityData.defineId(ApollyonEntity.class, EntityDataSerializers.LONG);
    public static final int TELEPORT_OUT = 8;
    public static final int TELEPORT_HIDDEN = 4;
    public static final int TELEPORT_IN = 8;
    public static final int TELEPORT_TOTAL = TELEPORT_OUT + TELEPORT_HIDDEN + TELEPORT_IN;

    private final ApollyonTeleportController teleportController = new ApollyonTeleportController(this);
    private static final EntityDataAccessor<Integer> COMBAT_MODE = SynchedEntityData.defineId(ApollyonEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> PUSHED_PLAYER = SynchedEntityData.defineId(ApollyonEntity.class, EntityDataSerializers.INT);

    private static final EntityDataAccessor<Integer> RESTRAINED_PLAYER = SynchedEntityData.defineId(ApollyonEntity.class, EntityDataSerializers.INT);
    private static final RawAnimation SUMMON = RawAnimation.begin().thenPlay("summon");

    private static final EntityDataAccessor<Integer> SPEAR_TARGET = SynchedEntityData.defineId(ApollyonEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Long> SPEAR_MARK_TIME = SynchedEntityData.defineId(ApollyonEntity.class, EntityDataSerializers.LONG);
    private final ApollyonCombatController combat = new ApollyonCombatController(this);
    private static final RawAnimation DRILL = RawAnimation.begin().thenPlay("attack_drill");
    private static final EntityDataAccessor<Long> DRILL_BEGAN = SynchedEntityData.defineId(ApollyonEntity.class, EntityDataSerializers.LONG);
    private static final EntityDataAccessor<Vector3f> DRILL_AXIS = SynchedEntityData.defineId(ApollyonEntity.class, EntityDataSerializers.VECTOR3);
    private static final RawAnimation COMMON = RawAnimation.begin().thenPlay("attack_common");
    private static final EntityDataAccessor<Long> SHOCK_START = SynchedEntityData.defineId(ApollyonEntity.class, EntityDataSerializers.LONG);
    private static final EntityDataAccessor<BlockPos> SHOCK_ORIGIN = SynchedEntityData.defineId(ApollyonEntity.class, EntityDataSerializers.BLOCK_POS);
    private static final EntityDataAccessor<Vector3f> SHOCK_OFFSET = SynchedEntityData.defineId(ApollyonEntity.class, EntityDataSerializers.VECTOR3);
    public static final int DEFENCE_TICKS = 40;
    public static final float DEFENCE_CHANCE = .20F;
    private static final RawAnimation DEFENCE = RawAnimation.begin().thenPlayAndHold("defence");
    private long defenceUntil = -1;

    public boolean isDefending() {
        return isAlive() && getCombatMode() == 8 && (level().isClientSide || level().getGameTime() < defenceUntil);
    }

    public boolean tryBeginDefence() {
        if (level().isClientSide || !isAlive() || isRemoved()) return false;
        if (isDefending()) return true;
        if (isTeleporting() || getRandom().nextFloat() >= DEFENCE_CHANCE) return false;
        combat.cancel();
        defenceUntil = level().getGameTime() + DEFENCE_TICKS;
        setCombatMode(8);
        setAttackControlsMovement(true);
        getNavigation().stop();
        setDeltaMovement(Vec3.ZERO);
        return true;
    }

    @Override
    public boolean hurt(net.minecraft.world.damagesource.DamageSource source, float amount) {
        if (source.getDirectEntity() instanceof net.minecraft.world.entity.projectile.Projectile projectile && com.benji.oasiso.common.entity.ai.ApollyonProjectileDefence.tryDeflect(this, projectile)) {
            return false;
        }
        return super.hurt(source, amount);
    }

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
        entityData.define(RESTRAINED_PLAYER, -1);
        entityData.define(SPEAR_TARGET, -1);
        entityData.define(SPEAR_MARK_TIME, -1L);
        entityData.define(SHOCK_START, -1L);
        entityData.define(DRILL_BEGAN, -1L);
        entityData.define(DRILL_AXIS, new Vector3f(0, 0, 1));
        entityData.define(SHOCK_ORIGIN, BlockPos.ZERO);
        entityData.define(SHOCK_OFFSET, new Vector3f());
    }

    public void beginShockwave(Vec3 origin) {
        BlockPos base = BlockPos.containing(origin);
        entityData.set(SHOCK_ORIGIN, base);
        entityData.set(SHOCK_OFFSET, new Vector3f((float) (origin.x - base.getX()), (float) (origin.y - base.getY()), (float) (origin.z - base.getZ())));
        entityData.set(SHOCK_START, level().getGameTime());
    }

    public long getShockwaveStart() {
        return entityData.get(SHOCK_START);
    }

    public Vec3 getShockwaveOrigin() {
        BlockPos base = entityData.get(SHOCK_ORIGIN);
        Vector3f offset = entityData.get(SHOCK_OFFSET);
        return new Vec3(base.getX() + offset.x, base.getY() + offset.y, base.getZ() + offset.z);
    }

    public void beginDrill(Vec3 axis) {
        setDrillAxis(axis);
        entityData.set(DRILL_BEGAN, level().getGameTime());
    }

    public void setDrillAxis(Vec3 axis) {
        entityData.set(DRILL_AXIS, new Vector3f((float) axis.x, (float) axis.y, (float) axis.z));
    }

    public Vec3 getDrillAxis() {
        Vector3f v = entityData.get(DRILL_AXIS);
        return new Vec3(v.x, v.y, v.z);
    }

    public float getDrillAge(float partial) {
        long start = entityData.get(DRILL_BEGAN);
        return start < 0 ? -1 : (float) (level().getGameTime() - start) + partial;
    }

    public boolean isDrilling() {
        return isAlive() && !isRemoved() && getCombatMode() == 5;
    }

    public void setSpearTarget(int id) {
        entityData.set(SPEAR_TARGET, id);
        entityData.set(SPEAR_MARK_TIME, id < 0 ? -1L : level().getGameTime());
    }

    public int getSpearTargetId() {
        return entityData.get(SPEAR_TARGET);
    }

    public long getSpearMarkTime() {
        return entityData.get(SPEAR_MARK_TIME);
    }

    public int getRestrainedPlayerId() {
        return entityData.get(RESTRAINED_PLAYER);
    }

    public void setRestrainedPlayerId(int id) {
        entityData.set(RESTRAINED_PLAYER, id);
    }

    public boolean isRestrainingPlayer(int playerId) {
        return isAlive() && !isRemoved() && getRestrainedPlayerId() == playerId && (getCombatMode() == 3 || getCombatMode() == 1);
    }

    @Override
    public void remove(RemovalReason reason) {
        if (!level().isClientSide) combat.cancel();
        super.remove(reason);
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
                .add(Attributes.ATTACK_DAMAGE, 12.0D)
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
            combat.cancel();
            return;
        }
        faceTarget();
        if (getCombatMode() == 8) {
            if (level().getGameTime() < defenceUntil) {
                getNavigation().stop();
                setDeltaMovement(Vec3.ZERO);
                return;
            }
            defenceUntil = -1;
            setCombatMode(0);
            setAttackControlsMovement(false);
        }
        if (combat.tick()) return;
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
        if (getCombatMode() == 5 && getDrillAge(0) >= com.benji.oasiso.common.entity.ai.ApollyonAttackTimeline.DRILL_START)
            return;
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
        else if (!level().isClientSide) combat.cancel();
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
        controllers.add(new AnimationController<ApollyonEntity>(this, "controller", 0, event -> event.setAndContinue(switch (getCombatMode()) {
            case 1 -> ATTACK;
            case 2 -> JUMP;
            case 3 -> SUMMON;
            case 4 -> COMMON;
            case 5 -> DRILL;
            case 6 -> SUMMON;
            case 7 -> IDLE;
            case 8 -> DEFENCE;
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
