package com.benji.oasiso.common.entity;

import com.benji.oasiso.ModSounds;
import com.benji.oasiso.Oasiso;
import com.benji.oasiso.common.entity.ai.KrombulRandomFlyGoal;
import com.benji.oasiso.common.entity.ai.KrombulTeleportPlayerGoal;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

public class KrombulEntity extends PathfinderMob
        implements GeoEntity, GlowmaskEntity {

    public static final int STATE_NORMAL = 0;
    public static final int STATE_TP_START = 1;
    public static final int STATE_TP_END = 2;

    private static final net.minecraft.network.syncher.EntityDataAccessor<Integer> STATE =
            net.minecraft.network.syncher.SynchedEntityData.defineId(
                    KrombulEntity.class,
                    net.minecraft.network.syncher.EntityDataSerializers.INT
            );

    private static final double HOVER_HEIGHT = 1.5D;
    private static final double HOVER_BOB_AMOUNT = 0.12D;

    private final AnimatableInstanceCache cache =
            GeckoLibUtil.createInstanceCache(this);

    public KrombulEntity(
            EntityType<? extends KrombulEntity> type,
            Level level
    ) {
        super(type, level);

        this.moveControl =
                new FlyingMoveControl(this, 20, true);

        this.setNoGravity(true);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();

        this.entityData.define(
                STATE,
                STATE_NORMAL
        );
    }

    public int getAnimState() {
        return this.entityData.get(STATE);
    }

    public void setAnimState(int state) {
        this.entityData.set(STATE, state);
    }

    public boolean isTeleporting() {
        return this.getAnimState() != STATE_NORMAL;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.28D)
                .add(Attributes.FLYING_SPEED, 0.35D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.2D)
                .add(Attributes.ATTACK_DAMAGE, 10.0D)
                .add(Attributes.FOLLOW_RANGE, 30.0D);
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        return new FlyingPathNavigation(this, level);
    }

    @Override
    protected void registerGoals() {

        this.goalSelector.addGoal(
                1,
                new MeleeAttackGoal(
                        this,
                        1.15D,
                        true
                )
        );

        this.goalSelector.addGoal(
                2,
                new KrombulTeleportPlayerGoal(this)
        );

        this.goalSelector.addGoal(
                3,
                new KrombulRandomFlyGoal(
                        this,
                        1.0D
                )
        );

        this.goalSelector.addGoal(
                4,
                new LookAtPlayerGoal(
                        this,
                        Player.class,
                        8.0F
                )
        );

        this.goalSelector.addGoal(
                5,
                new RandomLookAroundGoal(this)
        );

        /*
         * Единственный способ получить боевую цель —
         * быть атакованным другой сущностью.
         */
        this.targetSelector.addGoal(
                1,
                new HurtByTargetGoal(this)
        );
    }

    @Override
    public void tick() {
        super.tick();

        this.setNoGravity(true);
        this.fallDistance = 0.0F;

        if (this.level().isClientSide) {
            return;
        }


        if (!this.isTeleporting()) {
            maintainHoverHeight();
        }


        if (this.tickCount % 2 == 0
                && this.level() instanceof ServerLevel serverLevel) {
            spawnThrusterParticles(serverLevel);
        }
    }

    private void maintainHoverHeight() {
        double desiredY =
                findHoverY(this.getX(), this.getZ());

        double bob =
                Math.sin(
                        (this.tickCount + this.getId() * 7)
                                * 0.15D
                ) * HOVER_BOB_AMOUNT;

        desiredY += bob;

        double difference = desiredY - this.getY();

        Vec3 movement = this.getDeltaMovement();

        double correction = Mth.clamp(
                difference * 0.08D,
                -0.12D,
                0.12D
        );

        this.setDeltaMovement(
                movement.x,
                movement.y * 0.75D + correction,
                movement.z
        );
    }


    public double findHoverY(double x, double z) {
        int blockX = Mth.floor(x);
        int blockZ = Mth.floor(z);

        int startY =
                Math.min(
                        this.level().getMaxBuildHeight() - 3,
                        Mth.floor(this.getY()) + 4
                );

        int minimumY =
                Math.max(
                        this.level().getMinBuildHeight(),
                        Mth.floor(this.getY()) - 14
                );

        BlockPos.MutableBlockPos mutablePos =
                new BlockPos.MutableBlockPos();

        for (int y = startY; y >= minimumY; y--) {
            mutablePos.set(blockX, y, blockZ);

            if (!this.level()
                    .getBlockState(mutablePos)
                    .isFaceSturdy(
                            this.level(),
                            mutablePos,
                            Direction.UP
                    )) {
                continue;
            }

            BlockPos firstAir =
                    mutablePos.above();

            BlockPos secondAir =
                    mutablePos.above(2);

            if (!this.level().getBlockState(firstAir)
                    .getCollisionShape(this.level(), firstAir)
                    .isEmpty()) {
                continue;
            }

            if (!this.level().getBlockState(secondAir)
                    .getCollisionShape(this.level(), secondAir)
                    .isEmpty()) {
                continue;
            }


            return y + 1.0D + HOVER_HEIGHT;
        }

        return this.getY();
    }

    private void spawnThrusterParticles(
            ServerLevel level
    ) {
        RandomSource random = this.getRandom();

        for (int i = 0; i < 3; i++) {
            double x = this.getX()
                    + (random.nextDouble() - 0.5D)
                    * this.getBbWidth() * 0.45D;

            double y = this.getY() + 0.08D;

            double z = this.getZ()
                    + (random.nextDouble() - 0.5D)
                    * this.getBbWidth() * 0.45D;

            double velocityX =
                    (random.nextDouble() - 0.5D) * 0.025D;

            double velocityY =
                    -0.10D - random.nextDouble() * 0.08D;

            double velocityZ =
                    (random.nextDouble() - 0.5D) * 0.025D;

            level.sendParticles(
                    Oasiso.PURPLE_STARS.get(),
                    x,
                    y,
                    z,
                    0,
                    velocityX,
                    velocityY,
                    velocityZ,
                    1.0D
            );
        }
    }

    @Override
    public boolean causeFallDamage(
            float fallDistance,
            float damageMultiplier,
            DamageSource source
    ) {
        return false;
    }

    @Override
    public void registerControllers(
            AnimatableManager.ControllerRegistrar controllers
    ) {
        controllers.add(
                new AnimationController<>(
                        this,
                        "controller",
                        0,
                        event -> {
                            int state = this.getAnimState();

                            if (state == STATE_TP_START) {
                                return event.setAndContinue(
                                        RawAnimation.begin()
                                                .thenPlayAndHold("tp_start")
                                );
                            }

                            if (state == STATE_TP_END) {
                                return event.setAndContinue(
                                        RawAnimation.begin()
                                                .thenPlayAndHold("tp_end")
                                );
                            }

                            if (event.isMoving()) {
                                return event.setAndContinue(
                                        RawAnimation.begin()
                                                .thenLoop("walk")
                                );
                            }

                            return event.setAndContinue(
                                    RawAnimation.begin()
                                            .thenLoop("idle")
                            );
                        }
                )
        );
    }

    @Override
    public ResourceLocation getGlowmaskTexture() {
        int frame =
                (this.tickCount / 4) % 4;

        String textureName;

        if (frame == 0) {
            textureName = "krombul_emissive";
        } else {
            textureName =
                    "krombul_frame"
                            + (frame + 1)
                            + "_emissive";
        }

        return ResourceLocation.fromNamespaceAndPath(
                Oasiso.MODID,
                "textures/entity/emissive/"
                        + textureName
                        + ".png"
        );
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSourceIn) {
        return ModSounds.MONKI_DEATH.get();
    }

    @Override
    public AnimatableInstanceCache
    getAnimatableInstanceCache() {
        return this.cache;
    }
}