package com.benji.oasiso.common.entity;

import com.benji.oasiso.ModSounds;
import com.benji.oasiso.Oasiso;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import com.benji.oasiso.common.combat.ScienceCombatNetwork;
import com.benji.oasiso.common.combat.ScienceShieldGeometry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.EnumSet;
import java.util.UUID;

public class CrusaderScienceEntity extends Monster implements GeoEntity, GlowmaskEntity {
    private static final int NORMAL = 0, HELLO = 1, STOP = 2, STOP_IDLE = 3, ANGRY = 4;

    private static final int HELLO_TICKS = 130;
    private static final int STOP_TICKS = 40;
    private static final int STOP_IDLE_TICKS = 196;
    private static final int HELLO_COOLDOWN = 200;
    private static final double NOTICE_RADIUS = 15, APPROACH_RADIUS = 5;

    private static final EntityDataAccessor<Integer> PHASE = SynchedEntityData.defineId(
            CrusaderScienceEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> NO_HELMET = SynchedEntityData.defineId(
            CrusaderScienceEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> SECOND_IDLE = SynchedEntityData.defineId(
            CrusaderScienceEntity.class, EntityDataSerializers.BOOLEAN);

    public static final int SHIELD_TICKS = 200, VULNERABLE_TICKS = 300;
    public static final int BOTS_PER_WAVE = 6;
    private static final EntityDataAccessor<Boolean> SHIELDED = SynchedEntityData.defineId(
            CrusaderScienceEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Long> SHIELD_CHANGED = SynchedEntityData.defineId(
            CrusaderScienceEntity.class, EntityDataSerializers.LONG);
    private int combatTicks = SHIELD_TICKS, pendingBots;
    private long lastHitFxTick = Long.MIN_VALUE;
    private static final ResourceLocation BOT_ID = ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "bombo_bot");

    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation IDLE2 = RawAnimation.begin().thenLoop("idle2");
    private static final RawAnimation WALK = RawAnimation.begin().thenLoop("walk");
    private static final RawAnimation HELLO_ANIM = RawAnimation.begin().thenLoop("hello");
    private static final RawAnimation STOP_ANIM = RawAnimation.begin().thenPlayAndHold("stop");
    private static final RawAnimation STOP_IDLE_ANIM = RawAnimation.begin().thenLoop("stop_idle");
    private static final RawAnimation IDLE_BARE = RawAnimation.begin().thenLoop("idle_nohelmet");
    private static final RawAnimation IDLE2_BARE = RawAnimation.begin().thenLoop("idle2_nohelmet");
    private static final RawAnimation WALK_BARE = RawAnimation.begin().thenLoop("walk_nohelmet");
    private static final RawAnimation ATTACK = RawAnimation.begin().thenLoop("attack_pose");
    private static final RawAnimation ATTACK_BARE = RawAnimation.begin().thenLoop("attack_pose_nohelmet");

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private int phaseTicks, helloCooldown, idleTicks;
    private UUID focusPlayer;
    private UUID angryPlayer;

    public CrusaderScienceEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(PHASE, NORMAL);
        entityData.define(NO_HELMET, false);
        entityData.define(SECOND_IDLE, false);
        entityData.define(SHIELDED, false);
        entityData.define(SHIELD_CHANGED, -1000L);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 150.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.25D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D)
                .add(Attributes.ATTACK_DAMAGE, 0.0D)
                .add(Attributes.FOLLOW_RANGE, 20.0D);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new ScriptedPoseGoal());
        goalSelector.addGoal(3, new WaterAvoidingRandomStrollGoal(this, 0.8D));
    }

    public boolean hasRemovedHelmet() {
        return entityData.get(NO_HELMET);
    }

    public boolean isProvoked() {
        return entityData.get(PHASE) == ANGRY;
    }

    private boolean scriptedPose() {
        return entityData.get(PHASE) != NORMAL;
    }

    private boolean validPlayer(Player player) {
        return player != null && player.isAlive() && !player.isSpectator() && !player.isCreative() && player.level() == level();
    }

    private Player resolvePlayer(UUID uuid) {
        Player player = uuid == null ? null : level().getPlayerByUUID(uuid);
        return validPlayer(player) ? player : null;
    }

    private Player nearestVisiblePlayer(double radius) {
        Player nearest = null;
        double best = radius * radius;
        for (Player player : level().players()) {
            double distance = distanceToSqr(player);
            if (validPlayer(player) && distance <= best && getSensing().hasLineOfSight(player)) {
                nearest = player;
                best = distance;
            }
        }
        return nearest;
    }

    private void phase(int next, int ticks) {
        entityData.set(PHASE, next);
        phaseTicks = ticks;
        if (next != NORMAL) holdPosition();
        else idleTicks = 0;
    }

    private void holdPosition() {
        getNavigation().stop();
        setSpeed(0);
        setXxa(0);
        setZza(0);
        Vec3 motion = getDeltaMovement();
        setDeltaMovement(0, motion.y, 0);
    }

    private void face(Player player) {
        if (!validPlayer(player)) return;
        getLookControl().setLookAt(player, 30.0F, 30.0F);
        double dx = player.getX() - getX(), dz = player.getZ() - getZ();
        if (dx * dx + dz * dz > 1.0E-6) {
            float yaw = (float) (Math.atan2(dz, dx) * 180.0 / Math.PI) - 90.0F;
            setYRot(net.minecraft.util.Mth.rotLerp(.3F, getYRot(), yaw));
            setYBodyRot(getYRot());
            setYHeadRot(getYRot());
        }
    }

    private void approach(Player player) {
        focusPlayer = player.getUUID();
        phase(STOP, STOP_TICKS);
    }

    private void finishHello() {
        focusPlayer = null;
        helloCooldown = HELLO_COOLDOWN;
        phase(NORMAL, 0);
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (level().isClientSide || !isAlive() || isNoAi()) return;
        if (helloCooldown > 0) helloCooldown--;
        int current = entityData.get(PHASE);
        if (current == ANGRY) {
            holdPosition();
            Player enemy = resolvePlayer(angryPlayer);
            face(enemy);
            tickCombat(enemy);
            return;
        }
        if (current == STOP || current == STOP_IDLE) {
            holdPosition();
            face(resolvePlayer(focusPlayer));
            if (--phaseTicks <= 0) {
                if (current == STOP) {
                    entityData.set(NO_HELMET, true);
                    phase(STOP_IDLE, STOP_IDLE_TICKS);
                } else {
                    focusPlayer = null;
                    phase(NORMAL, 0);
                }
            }
            return;
        }
        if (!hasRemovedHelmet()) {
            Player close = nearestVisiblePlayer(APPROACH_RADIUS);
            if (close != null) {
                approach(close);
                face(close);
                return;
            }
            if (current == HELLO) {
                Player player = resolvePlayer(focusPlayer);
                if (player == null || distanceToSqr(player) > NOTICE_RADIUS * NOTICE_RADIUS || !getSensing().hasLineOfSight(player)) {
                    finishHello();
                    return;
                }
                holdPosition();
                face(player);
                if (--phaseTicks <= 0) finishHello();
                return;
            }
            if (helloCooldown == 0) {
                Player player = nearestVisiblePlayer(NOTICE_RADIUS);
                if (player != null) {
                    focusPlayer = player.getUUID();
                    phase(HELLO, HELLO_TICKS);
                    face(player);
                    return;
                }
            }
        }
        if (getDeltaMovement().horizontalDistanceSqr() > 1.0E-4) idleTicks = 0;
        else if (--idleTicks <= 0) {
            boolean second = random.nextBoolean();
            entityData.set(SECOND_IDLE, second);
            idleTicks = second ? 45 : 40;
        }
    }

    public void provoke(Player player) {
        if (level().isClientSide || !isAlive() || !validPlayer(player)) return;
        boolean alreadyAngry = isProvoked();
        angryPlayer = player.getUUID();
        focusPlayer = null;
        setTarget(null);
        phase(ANGRY, 0);
        if (!alreadyAngry) {
            combatTicks = SHIELD_TICKS;
            pendingBots = 0;
            setShield(true);
        }
        face(player);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (isShieldActive() && !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            if (!level().isClientSide && amount > 0) {
                Vec3 from = source.getDirectEntity() instanceof Projectile projectile ? projectile.position().subtract(projectile.getDeltaMovement()) : source.getSourcePosition();
                if (source.getDirectEntity() instanceof Player player) from = player.getEyePosition();
                if (from == null) from = shieldCenter().add(0, 0, 2);
                Vec3 point = ScienceShieldGeometry.intersect(from, shieldCenter(), shieldCenter(), shieldRadii());
                shieldHit(point == null ? from : point);
                if (source.getEntity() instanceof Player player) provoke(player);
            }
            amount = Math.min(.1F, Math.max(0, amount) * .01F);
        }
        boolean accepted = super.hurt(source, amount);
        if (accepted && !level().isClientSide && source.getEntity() instanceof Player player) provoke(player);
        return accepted;
    }

    public boolean isShieldActive() {
        return isAlive() && isProvoked() && entityData.get(SHIELDED);
    }

    public Vec3 shieldRadii() {
        return ScienceShieldGeometry.radii(getBbWidth(), getBbHeight());
    }

    public Vec3 shieldCenter() {
        return position().add(0, shieldRadii().y - .04, 0);
    }

    public long shieldChangedAt() {
        return entityData.get(SHIELD_CHANGED);
    }

    private void setShield(boolean enabled) {
        if (entityData.get(SHIELDED) == enabled) return;
        entityData.set(SHIELDED, enabled);
        entityData.set(SHIELD_CHANGED, level().getGameTime());
    }

    public void shieldHit(Vec3 point) {
        if (level().isClientSide || !isShieldActive()) return;
        long now = level().getGameTime();
        if (lastHitFxTick == now) return;
        lastHitFxTick = now;
        Vec3 normal = ScienceShieldGeometry.unit(point, shieldCenter(), shieldRadii()).normalize();
        if (normal.lengthSqr() < .5) normal = new Vec3(0, 0, 1);
        ScienceCombatNetwork.hit(this, normal);
    }

    private void interceptProjectiles() {
        Vec3 center = shieldCenter(), r = shieldRadii();
        for (Projectile projectile : level().getEntitiesOfClass(Projectile.class, new AABB(center, center).inflate(r.x + 4, r.y + 4, r.z + 4), p -> p.isAlive() && p.getOwner() != this && !(p.getOwner() instanceof BomboBotEntity))) {
            Vec3 velocity = projectile.getDeltaMovement();
            if (velocity.lengthSqr() < 1.0E-5) continue;
            Vec3 from = projectile.position(), to = from.add(velocity);
            Vec3 point = ScienceShieldGeometry.intersect(from, to, center, r);
            if (point == null) continue;
            var block = level().clip(new ClipContext(from, point, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, projectile));
            if (block.getType() != HitResult.Type.MISS) continue;
            shieldHit(point);

            if (projectile instanceof net.minecraft.world.entity.projectile.ThrownTrident) {
                Vec3 normal = point.subtract(center).normalize();
                projectile.setPos(point.add(normal.scale(.15)));
                projectile.setDeltaMovement(velocity.subtract(normal.scale(2 * velocity.dot(normal))).scale(.35));
                projectile.hurtMarked = true;
            } else projectile.discard();
        }
    }

    private void tickCombat(Player enemy) {
        if (isShieldActive()) interceptProjectiles();

        if (!isShieldActive()) {
            if (pendingBots > 0 && enemy != null && distanceToSqr(enemy) <= 64 * 64 && tickCount % 5 == 0)
                spawnWave(enemy);
            if (--combatTicks <= 0) {
                pendingBots = 0;
                setShield(true);
                combatTicks = SHIELD_TICKS;
            }
        } else if (enemy != null && distanceToSqr(enemy) <= 64 * 64 && --combatTicks <= 0) {
            setShield(false);
            combatTicks = VULNERABLE_TICKS;
            pendingBots = BOTS_PER_WAVE;
            spawnWave(enemy);
        }
    }

    private void spawnWave(Player enemy) {
        if (!(level() instanceof ServerLevel server)) return;
        var type = ForgeRegistries.ENTITY_TYPES.getValue(BOT_ID);
        if (type == null) {
            com.mojang.logging.LogUtils.getLogger().error("Missing Bombo Bot entity type: {}", BOT_ID);
            pendingBots = 0;
            return;
        }
        double offset = random.nextDouble() * Math.PI * 2;

        for (int slot = 0; slot < 48 && pendingBots > 0; slot++) {
            double angle = offset + slot * Math.PI / 6;
            double radius = 1.8 + (slot / 12) * .8;
            double x = getX() + Math.cos(angle) * radius, z = getZ() + Math.sin(angle) * radius;
            BlockPos base = BlockPos.containing(x, getY(), z);
            for (int dy = 2; dy >= -2; dy--) {
                BlockPos pos = base.offset(0, dy, 0);
                if (!server.hasChunkAt(pos) || !server.getWorldBorder().isWithinBounds(pos) || !server.getBlockState(pos.below()).isFaceSturdy(server, pos.below(), Direction.UP) || !server.getFluidState(pos).isEmpty())
                    continue;
                var entity = type.create(server);
                if (!(entity instanceof BomboBotEntity bot)) {
                    com.mojang.logging.LogUtils.getLogger().error("Entity {} is not BomboBotEntity", BOT_ID);
                    pendingBots = 0;
                    return;
                }
                bot.moveTo(x, pos.getY() + .02, z, getYRot(), 0);
                if (!server.noCollision(bot, bot.getBoundingBox()) || !server.getEntities(bot, bot.getBoundingBox(), e -> e.isAlive() && e.isPickable()).isEmpty())
                    continue;
                if (server.clip(new ClipContext(getEyePosition(), bot.position().add(0, .3, 0), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this)).getType() != HitResult.Type.MISS)
                    continue;
                bot.setScientistOwner(this, enemy);
                if (server.addFreshEntity(bot)) pendingBots--;
                break;
            }
        }
    }

    public static void onCoreDetonationStarted(ServerLevel level, Vec3 coreCenter, Player culprit) {
        if (culprit == null || culprit.level() != level) return;
        for (CrusaderScienceEntity scientist : level.getEntitiesOfClass(CrusaderScienceEntity.class, new AABB(coreCenter, coreCenter).inflate(40), e -> e.isAlive() && e.position().distanceToSqr(coreCenter) <= 40 * 40)) {
            scientist.provoke(culprit);
        }
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }

    @Override
    protected boolean shouldDespawnInPeaceful() {
        return false;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("ScienceShielded", entityData.get(SHIELDED));
        tag.putInt("ScienceCombatTicks", combatTicks);
        tag.putInt("SciencePendingBots", pendingBots);
        tag.putBoolean("ScienceNoHelmet", hasRemovedHelmet());
        tag.putInt("SciencePhase", entityData.get(PHASE));
        tag.putInt("SciencePhaseTicks", phaseTicks);
        tag.putInt("ScienceHelloCooldown", helloCooldown);
        tag.putBoolean("ScienceSecondIdle", entityData.get(SECOND_IDLE));
        if (focusPlayer != null) tag.putUUID("ScienceFocus", focusPlayer);
        if (angryPlayer != null) tag.putUUID("ScienceAngryPlayer", angryPlayer);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        entityData.set(NO_HELMET, tag.getBoolean("ScienceNoHelmet"));
        entityData.set(SECOND_IDLE, tag.getBoolean("ScienceSecondIdle"));
        angryPlayer = tag.hasUUID("ScienceAngryPlayer") ? tag.getUUID("ScienceAngryPlayer") : null;
        focusPlayer = tag.hasUUID("ScienceFocus") ? tag.getUUID("ScienceFocus") : null;
        int saved = tag.getInt("SciencePhase");
        int current = saved >= NORMAL && saved <= ANGRY ? saved : NORMAL;
        if (angryPlayer != null) current = ANGRY;

        int duration = switch (current) {
            case HELLO -> HELLO_TICKS;
            case STOP -> STOP_TICKS;
            case STOP_IDLE -> STOP_IDLE_TICKS;
            default -> 0;
        };
        if (current == STOP_IDLE) entityData.set(NO_HELMET, true);
        phase(current, duration);
        boolean shield = current == ANGRY && (!tag.contains("ScienceShielded") || tag.getBoolean("ScienceShielded"));
        entityData.set(SHIELDED, shield);
        entityData.set(SHIELD_CHANGED, level().getGameTime() - 20);
        int maxTicks = shield ? SHIELD_TICKS : VULNERABLE_TICKS;
        combatTicks = tag.contains("ScienceCombatTicks") ? Math.max(1, Math.min(maxTicks, tag.getInt("ScienceCombatTicks"))) : maxTicks;
        pendingBots = shield ? 0 : Math.max(0, Math.min(BOTS_PER_WAVE, tag.getInt("SciencePendingBots")));
        helloCooldown = Math.max(0, Math.min(HELLO_COOLDOWN, tag.getInt("ScienceHelloCooldown")));
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 0, event -> {
            RawAnimation animation = switch (entityData.get(PHASE)) {
                case HELLO -> HELLO_ANIM;
                case STOP -> STOP_ANIM;
                case STOP_IDLE -> STOP_IDLE_ANIM;
                case ANGRY -> hasRemovedHelmet() ? ATTACK_BARE : ATTACK;
                default ->
                        event.isMoving() ? (hasRemovedHelmet() ? WALK_BARE : WALK) : entityData.get(SECOND_IDLE) ? (hasRemovedHelmet() ? IDLE2_BARE : IDLE2) : (hasRemovedHelmet() ? IDLE_BARE : IDLE);
            };
            return event.setAndContinue(animation);
        }));
    }

    private final class ScriptedPoseGoal extends Goal {
        ScriptedPoseGoal() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
        }

        @Override
        public boolean canUse() {
            return scriptedPose();
        }

        @Override
        public boolean canContinueToUse() {
            return scriptedPose();
        }

        @Override
        public void start() {
            holdPosition();
        }

        @Override
        public void tick() {
            holdPosition();
        }
    }

    @Override
    protected SoundEvent getAmbientSound() {

        SoundEvent[] sounds = {ModSounds.SCIENCE_IDLE1.get(), ModSounds.SCIENCE_IDLE2.get(), ModSounds.SCIENCE_IDLE3.get()};

        return sounds[this.random.nextInt(sounds.length)];
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSourceIn) {
        return ModSounds.SCIENCE_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.SCIENCE_DEATH.get();
    }


    @Override
    public ResourceLocation getGlowmaskTexture() {
        return ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "textures/entity/emissive/crusader_science_emissive.png");
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
