package com.benji.oasiso.common.entity;

import com.benji.oasiso.ModSounds;
import com.benji.oasiso.Oasiso;
import net.minecraft.core.BlockPos;
import com.benji.oasiso.common.world.EntropyCreatureSpawns;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.core.Direction;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

public class EntropyCreatureEntity extends Monster implements GeoEntity, GlowmaskEntity  {
    private static final EntityDataAccessor<Boolean> SPAWNING = SynchedEntityData.defineId(
            EntropyCreatureEntity.class, EntityDataSerializers.BOOLEAN);
    private static final RawAnimation SPAWN = RawAnimation.begin().thenPlay("spawn");
    private int spawnTicks;
    private UUID entropyWave;

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    private static final double MELEE_RANGE = 5.0;
    private static final double SPECIAL_RANGE = 15.0;
    private static final double LIGHTNING_RADIUS = 4.0;
    private static final int LIGHTNING_COUNT = 16;

    private static final int MELEE_HIT_TICK = 13;
    private static final int MELEE_DURATION = 22;
    private static final int SPECIAL_HIT_TICK = 18;
    private static final int SPECIAL_DURATION = 40;
    private static final int MELEE_RECOVERY = 20;
    private static final int SPECIAL_COOLDOWN = 160;

    private static final double LAUNCH_SPEED = 0.80;
    private static final int PULL_DELAY = 10;
    private static final int PULL_DURATION = 16;
    private static final double PULL_SPEED = 0.65;
    private static final double PULL_STOP_DISTANCE = 1.4;

    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation WALK = RawAnimation.begin().thenLoop("walk");
    private static final RawAnimation MELEE = RawAnimation.begin().thenPlay("attack");
    private static final RawAnimation SPECIAL = RawAnimation.begin().thenPlay("attack_sup");

    private int attackKind;
    private long attackStarted;
    private long nextMelee;
    private long nextSpecial;
    private LivingEntity meleeVictim;
    private final Map<UUID, Long> liftedPlayers = new HashMap<>();

    public EntropyCreatureEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(SPAWNING, false);
    }

    public void setEntropyWave(UUID id) {
        entropyWave = id;
    }

    public void beginSpawnAnimation() {
        spawnTicks = 60;
        entityData.set(SPAWNING, true);
        attackKind = 0;
        meleeVictim = null;
        stopWalking();
    }

    private boolean isSpawning() {
        return entityData.get(SPAWNING);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("EntropySpawnTicks", spawnTicks);
        if (entropyWave != null) tag.putUUID("EntropySpawnWave", entropyWave);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        spawnTicks = Math.max(0, Math.min(60, tag.getInt("EntropySpawnTicks")));
        entityData.set(SPAWNING, spawnTicks > 0);
        entropyWave = tag.hasUUID("EntropySpawnWave") ? tag.getUUID("EntropySpawnWave") : null;
    }

    private void finishSpawnWave() {
        if (entropyWave != null && level() instanceof ServerLevel server) {
            EntropyCreatureSpawns.get(server).removed(server, entropyWave, getUUID());
            entropyWave = null;
        }
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        if (isDeadOrDying()) finishSpawnWave();
    }

    @Override
    public void remove(RemovalReason reason) {
        if (reason.shouldDestroy()) finishSpawnWave();
        super.remove(reason);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 80.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.25D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D)
                .add(Attributes.ATTACK_DAMAGE, 20.0D)
                .add(Attributes.FOLLOW_RANGE, 30.0D);
    }

    @Override
    protected void registerGoals() {
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, false, false));
        this.goalSelector.addGoal(0, new SpawnAnimationGoal());
        this.goalSelector.addGoal(2, new CreatureCombatGoal());
        this.goalSelector.addGoal(3, new WaterAvoidingRandomStrollGoal(this, 0.8D));
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        AnimationController<EntropyCreatureEntity> controller = new AnimationController<>(this, "controller", 0, event -> event.setAndContinue(isSpawning() ? SPAWN : (event.isMoving() ? WALK : IDLE)));
        controller.triggerableAnim("melee", MELEE);
        controller.triggerableAnim("special", SPECIAL);
        controller.setCustomInstructionKeyframeHandler(event -> {
        });
        controllers.add(controller);
    }

    @Override
    public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel server)) return;

        if (spawnTicks > 0) {
            stopWalking();
            if (--spawnTicks == 0) entityData.set(SPAWNING, false);
            return;
        }
        tickLiftedPlayers(server);
        if (!isAlive() || isNoAi()) {
            attackKind = 0;
            meleeVictim = null;
            return;
        }
        tickAttack(server);
    }

    private boolean validTarget(LivingEntity target) {
        return target != null && target.isAlive() && target.level() == level() && (!(target instanceof Player player) || (!player.isCreative() && !player.isSpectator()));
    }

    private void beginAttack(int kind, LivingEntity target) {
        attackKind = kind;
        attackStarted = level().getGameTime();
        meleeVictim = kind == 1 ? target : null;
        if (kind == 2) nextSpecial = attackStarted + SPECIAL_COOLDOWN;
        stopWalking();
        triggerAnim("controller", kind == 1 ? "melee" : "special");
    }

    private void stopWalking() {
        getNavigation().stop();
        setSpeed(0);
        setZza(0);
        setXxa(0);
        Vec3 motion = getDeltaMovement();
        setDeltaMovement(0, motion.y, 0);
    }

    private void tickAttack(ServerLevel server) {
        if (attackKind == 0) return;
        stopWalking();
        long age = server.getGameTime() - attackStarted;
        if (attackKind == 1 && age == MELEE_HIT_TICK) {
            playSound(ModSounds.CREATURE_DEATH.get(), 1.0F, 1.0F);
            if (validTarget(meleeVictim) && distanceToSqr(meleeVictim) <= MELEE_RANGE * MELEE_RANGE && getSensing().hasLineOfSight(meleeVictim)) {
                doHurtTarget(meleeVictim);
            }
        } else if (attackKind == 2 && age == SPECIAL_HIT_TICK) {
            releaseSpecial(server);
        }
        int duration = attackKind == 1 ? MELEE_DURATION : SPECIAL_DURATION;
        if (age >= duration) {
            nextMelee = server.getGameTime() + MELEE_RECOVERY;
            attackKind = 0;
            meleeVictim = null;
        }
    }

    private void releaseSpecial(ServerLevel server) {
        playSound(ModSounds.ENTROPY_LIGHTNING.get(), 1.0F, 1.0F);
        spawnGroundLightning(server);
        for (ServerPlayer player : server.getEntitiesOfClass(ServerPlayer.class, getBoundingBox().inflate(SPECIAL_RANGE), p -> validTarget(p) && distanceToSqr(p) <= SPECIAL_RANGE * SPECIAL_RANGE)) {
            if (player.isPassenger()) player.stopRiding();
            if (player.isFallFlying()) player.stopFallFlying();
            player.fallDistance = 0;
            setPlayerMotion(player, new Vec3(0, LAUNCH_SPEED, 0));
            liftedPlayers.put(player.getUUID(), server.getGameTime());
        }
    }

    private void tickLiftedPlayers(ServerLevel server) {
        Iterator<Map.Entry<UUID, Long>> iterator = liftedPlayers.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, Long> entry = iterator.next();
            ServerPlayer player = server.getServer().getPlayerList().getPlayer(entry.getKey());
            if (!validTarget(player)) {
                iterator.remove();
                continue;
            }
            long age = server.getGameTime() - entry.getValue();
            player.fallDistance = 0;
            if (age > 100 || (age > 2 && player.onGround())) {
                iterator.remove();
                continue;
            }
            if (!isAlive() || isNoAi() || age < PULL_DELAY || age >= PULL_DELAY + PULL_DURATION) continue;
            Vec3 toward = position().subtract(player.position());
            double horizontal = Math.sqrt(toward.x * toward.x + toward.z * toward.z);
            Vec3 old = player.getDeltaMovement();
            if (horizontal <= PULL_STOP_DISTANCE) {
                setPlayerMotion(player, new Vec3(0, old.y, 0));
                continue;
            }
            double speed = Math.min(PULL_SPEED, (horizontal - PULL_STOP_DISTANCE) * 0.25);
            setPlayerMotion(player, new Vec3(toward.x / horizontal * speed, Math.max(old.y, -0.08), toward.z / horizontal * speed));
        }
    }

    private static void setPlayerMotion(ServerPlayer player, Vec3 motion) {
        player.setDeltaMovement(motion);
        player.hurtMarked = true;
        player.connection.send(new ClientboundSetEntityMotionPacket(player));
    }

    private void spawnGroundLightning(ServerLevel server) {
        Set<BlockPos> used = new HashSet<>();
        for (int attempt = 0; attempt < LIGHTNING_COUNT * 3 && used.size() < LIGHTNING_COUNT; attempt++) {
            double angle = random.nextDouble() * Math.PI * 2;
            double radius = Math.sqrt(random.nextDouble()) * LIGHTNING_RADIUS;
            double x = getX() + Math.cos(angle) * radius;
            double z = getZ() + Math.sin(angle) * radius;
            BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
            for (int y = blockPosition().getY() + 2; y >= blockPosition().getY() - 4; y--) {
                pos.set(net.minecraft.util.Mth.floor(x), y, net.minecraft.util.Mth.floor(z));
                if (!server.hasChunkAt(pos)) break;
                VoxelShape shape = server.getBlockState(pos).getCollisionShape(server, pos);
                if (shape.isEmpty()) continue;
                if (server.getBlockState(pos.above()).getCollisionShape(server, pos.above()).isEmpty() && used.add(pos.immutable())) {
                    server.sendParticles(Oasiso.ENTROPY_LIGHTNING.get(), x, y + shape.max(Direction.Axis.Y) + 0.45, z, 1, 0, 0, 0, 0);
                }
                break;
            }
        }
    }

    private final class SpawnAnimationGoal extends Goal {
        SpawnAnimationGoal() { setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP)); }
        @Override public boolean canUse() { return isSpawning(); }
        @Override public boolean canContinueToUse() { return isSpawning(); }
        @Override public boolean requiresUpdateEveryTick() { return true; }
        @Override public void start() { stopWalking(); }
        @Override public void tick() { stopWalking(); }
    }

    private final class CreatureCombatGoal extends Goal {
        private int pathDelay;

        CreatureCombatGoal() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            return !isSpawning() && (attackKind != 0 || validTarget(getTarget()));
        }

        @Override
        public boolean canContinueToUse() {
            return canUse();
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void stop() {
            getNavigation().stop();
        }

        @Override
        public void tick() {
            LivingEntity target = attackKind == 1 ? meleeVictim : getTarget();
            if (validTarget(target)) getLookControl().setLookAt(target, 30.0F, 30.0F);
            if (attackKind != 0) {
                stopWalking();
                return;
            }
            if (!validTarget(target)) return;
            double distance = distanceToSqr(target);
            long now = level().getGameTime();
            boolean visible = getSensing().hasLineOfSight(target);
            if (visible && distance <= MELEE_RANGE * MELEE_RANGE) {
                getNavigation().stop();
                if (now >= nextMelee) beginAttack(1, target);
            } else if (visible && distance <= SPECIAL_RANGE * SPECIAL_RANGE && now >= nextSpecial) {
                beginAttack(2, target);
            } else if (--pathDelay <= 0) {
                pathDelay = 10;
                getNavigation().moveTo(target, 1.0D);
            }
        }
    }

    @Override
    protected SoundEvent getAmbientSound() {
        if (isSpawning()) return null;

        SoundEvent[] sounds = {ModSounds.CREATURE_IDLE1.get(), ModSounds.CREATURE_IDLE2.get()};

        return sounds[this.random.nextInt(sounds.length)];
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSourceIn) {
        return ModSounds.CREATURE_HIT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.CREATURE_DEATH.get();
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState blockIn) {
        this.playSound(ModSounds.TITANA_STEP.get(), 1.0F, 1.0F);
    }

    @Override
    public ResourceLocation getGlowmaskTexture() {
        return ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "textures/entity/emissive/entropy_creature_emissive.png");
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }
}
