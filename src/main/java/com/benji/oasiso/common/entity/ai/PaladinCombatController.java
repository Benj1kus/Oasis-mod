package com.benji.oasiso.common.entity.ai;

import com.benji.oasiso.common.entity.PaladinEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import com.benji.oasiso.Oasiso;
import com.benji.oasiso.common.entity.SwordHeartEntity;
import com.benji.oasiso.common.util.DamageNumberSpawner;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import com.benji.oasiso.ModSounds;
import net.minecraft.sounds.SoundEvent;

import java.util.List;
import java.util.UUID;

public final class PaladinCombatController {

    private static final int ATTACK_1_DURATION = 42;
    private static final int ATTACK_1_DAMAGE_TICK = 20;

    private static final int ATTACK_2_DURATION = 30;
    private static final int ATTACK_2_DAMAGE_TICK = 15;

    private static final int SPIN_ATTACK_DURATION = 50;
    private static final int SPIN_START_TICK = 13;
    private static final int SPIN_END_TICK = 40;

    private static final int MIN_SPECIAL_COOLDOWN = 160;
    private static final int MAX_SPECIAL_COOLDOWN = 280;

    private static final int TELEPORT_HOP_INTERVAL = 20;
    private static final int MIN_TELEPORT_HOPS = 1;
    private static final int MAX_TELEPORT_HOPS = 4;

    private static final double TELEPORT_RADIUS = 8.0D;
    private static final double TELEPORT_MIN_RADIUS = 2.0D;

    private static final int SHIELD_DURATION = 114;

    private static final int QTE_START_TICK = 17;
    private static final int QTE_END_TICK = 101;

    private static final int MIN_HEART_SHUFFLE = 12;
    private static final int MAX_HEART_SHUFFLE = 28;

    private static final float QTE_FAILURE_HEAL = 100.0F;
    private static final float QTE_SUCCESS_DAMAGE = 60.0F;

    private static final int SPIN_DAMAGE_INTERVAL = 20;
    private static final double SPIN_DAMAGE_RADIUS = 5.0D;

    private static final double SPIN_FORWARD_SPEED = 0.52D;
    private static final double SPIN_SIDE_SPEED = 0.34D;
    private static final double SPIN_ZIGZAG_FREQUENCY = 0.72D;

    private static final int GRAB_DURATION = 20;
    private static final double GRAB_PULL_DISTANCE = 5.0D;
    private static final double GRAB_PULL_STEP = 0.5D;
    private static final double GRAB_MIN_DISTANCE = 2.5D;

    private static final double MELEE_START_RANGE = 5.5D;

    private static final double ATTACK_2_HIT_RANGE = 6.0D;

    private static final double SHOCKWAVE_RADIUS = 10.0D;
    private static final double SHOCKWAVE_VERTICAL_SPEED = 0.8D;

    private static final int MIN_MELEE_COOLDOWN = 22;
    private static final int MAX_MELEE_COOLDOWN = 38;

    private static final String DATA_TAG = "PaladinCombat";

    private final PaladinEntity boss;

    private int attackTick = -1;
    private int attackCooldown = 25;
    private int pathRefresh;

    private UUID targetId;
    private double grabRemainingDistance;

    private int specialCooldown = 180;

    private int teleportHopsRemaining;
    private int teleportHopTimer;

    private boolean qteActive;

    private int heartsDestroyed;
    private int heartShuffleTimer;

    public PaladinCombatController(PaladinEntity boss) {
        this.boss = boss;
    }

    public boolean isAttacking() {
        return this.attackTick >= 0;
    }
    public void prepareForDeath(ServerLevel level) {

        this.qteActive = false;
        this.heartsDestroyed = 0;
        this.heartShuffleTimer = 0;
        cleanupQteHearts(level);

        this.attackTick = -1;
        this.attackCooldown = 0;
        this.pathRefresh = 0;
        this.grabRemainingDistance = 0.0D;

        this.teleportHopsRemaining = 0;
        this.teleportHopTimer = 0;

        this.targetId = null;

        boss.setTarget(null);
        boss.getNavigation().stop();
        boss.setDeltaMovement(Vec3.ZERO);
    }

    public void tick(ServerLevel level) {

        if (this.teleportHopsRemaining > 0) {
            tickTeleportSequence(level);
            return;
        }

        if (isAttacking()) {
            tickAttack(level);
            return;
        }

        ServerPlayer target = getCurrentTarget();

        if (target != null) {
            if (this.specialCooldown > 0) {
                this.specialCooldown--;
            }
            if (this.specialCooldown <= 0) {
                startRandomSpecialAttack(level, target);
                return;
            }
        }
        tickIdle(level);
    }

    private void startRandomSpecialAttack(ServerLevel level, ServerPlayer target) {
        this.specialCooldown = randomBetween(MIN_SPECIAL_COOLDOWN, MAX_SPECIAL_COOLDOWN);
        if (boss.getRandom().nextBoolean()) {
            startTeleportSequence(level, target);
        } else {
            startShieldAttack(level, target);
        }
    }

    private void startTeleportSequence(ServerLevel level, ServerPlayer target) {
        this.targetId = target.getUUID();
        this.teleportHopsRemaining = randomBetween(MIN_TELEPORT_HOPS, MAX_TELEPORT_HOPS);

        this.teleportHopTimer = 0;

        boss.getNavigation().stop();

        performTeleportHop(level, target);

        this.teleportHopsRemaining--;
        if (this.teleportHopsRemaining > 0) {
            this.teleportHopTimer = TELEPORT_HOP_INTERVAL;
        } else {
            finishTeleportSequence();
        }
    }

    private void tickTeleportSequence(ServerLevel level) {
        boss.getNavigation().stop();

        ServerPlayer target = resolveTarget(level);

        if (target == null) {
            finishTeleportSequence();
            return;
        }

        boss.lookAtPlayer(target, 180.0F);

        if (this.teleportHopTimer > 0) {
            this.teleportHopTimer--;
            return;
        }

        performTeleportHop(level, target);

        this.teleportHopsRemaining--;
        if (this.teleportHopsRemaining <= 0) {
            finishTeleportSequence();
            return;
        }
        this.teleportHopTimer = TELEPORT_HOP_INTERVAL;
    }

    private void performTeleportHop(ServerLevel level, ServerPlayer target) {
        Vec3 destination = findTeleportDestination(level, target);
        if (destination == null) {
            return;
        }
        level.sendParticles(Oasiso.PURPLE_STARS.get(), boss.getX(), boss.getY() + boss.getBbHeight() * 0.5D, boss.getZ(), 55, 0.7D, boss.getBbHeight() * 0.45D, 0.7D, 0.08D);
        boss.teleportTo(destination.x, destination.y, destination.z);
        boss.getNavigation().stop();
        level.sendParticles(Oasiso.PURPLE_STARS.get(), destination.x, destination.y + boss.getBbHeight() * 0.5D, destination.z, 55, 0.7D, boss.getBbHeight() * 0.45D, 0.7D, 0.08D);

        level.playSound(null, destination.x, destination.y, destination.z, SoundEvents.ENDERMAN_TELEPORT, SoundSource.HOSTILE, 1.2F, 0.90F + boss.getRandom().nextFloat() * 0.20F);
        boss.lookAtPlayer(target, 180.0F);
    }

    private Vec3 findTeleportDestination(ServerLevel level, ServerPlayer target) {
        for (int attempt = 0; attempt < 24; attempt++) {

            double angle = boss.getRandom().nextDouble() * Math.PI * 2.0D;
            double distance = TELEPORT_MIN_RADIUS + boss.getRandom().nextDouble() * (TELEPORT_RADIUS - TELEPORT_MIN_RADIUS);

            double x = target.getX() + Math.cos(angle) * distance;
            double z = target.getZ() + Math.sin(angle) * distance;
            int centerY = Mth.floor(target.getY());

            for (int dy = 4; dy >= -4; dy--) {
                int y = centerY + dy;
                BlockPos feet = BlockPos.containing(x, y, z);
                BlockPos support = feet.below();
                if (!level.getBlockState(support).isFaceSturdy(level, support, Direction.UP)) {
                    continue;
                }
                double moveX = x - boss.getX();
                double moveY = y - boss.getY();
                double moveZ = z - boss.getZ();

                AABB movedBox = boss.getBoundingBox().move(moveX, moveY, moveZ);
                if (!level.noCollision(boss, movedBox)) {
                    continue;
                }
                return new Vec3(x, y, z);
            }
        }
        return null;
    }


    private void finishTeleportSequence() {

        this.teleportHopsRemaining = 0;
        this.teleportHopTimer = 0;
        this.targetId = null;
        this.attackCooldown = Math.max(this.attackCooldown, 20);
    }

    private void startShieldAttack(ServerLevel level, ServerPlayer target) {
        this.attackTick = 0;
        this.targetId = target.getUUID();
        this.qteActive = false;
        this.heartsDestroyed = 0;
        this.heartShuffleTimer = 0;

        boss.getNavigation().stop();
        boss.setDeltaMovement(0.0D, boss.getDeltaMovement().y, 0.0D);
        boss.setAnimState(PaladinEntity.STATE_SHIELD);
        playBossSound(level, ModSounds.SWORD_SHIELD.get(), 1.0F, 1.0F);
        boss.lookAtPlayer(target, 180.0F);
    }

    private void tickIdle(ServerLevel level) {
        if (this.attackCooldown > 0) {
            this.attackCooldown--;
        }
        ServerPlayer target = getCurrentTarget();
        if (target == null) {
            this.targetId = null;
            return;
        }

        boss.lookAtPlayer(target, 8.0F);
        double distanceSqr = boss.distanceToSqr(target);

        if (distanceSqr > MELEE_START_RANGE * MELEE_START_RANGE) {
            if (this.attackCooldown <= 0 && boss.hasLineOfSight(target)) {
                startRandomRangedAttack(level, target);
                return;
            }
            this.pathRefresh--;

            if (this.pathRefresh <= 0) {
                this.pathRefresh = 5;
                boss.getNavigation().moveTo(target, 1.0D);
            }
            return;
        }
        boss.getNavigation().stop();
        if (this.attackCooldown > 0) {
            return;
        }
        startRandomMeleeAttack(target);
    }


    private void startRandomRangedAttack(ServerLevel level, ServerPlayer target) {
        this.attackTick = 0;
        this.targetId = target.getUUID();
        this.grabRemainingDistance = 0.0D;
        boss.getNavigation().stop();
        boss.setDeltaMovement(0.0D, boss.getDeltaMovement().y, 0.0D);

        if (boss.getRandom().nextBoolean()) {
            boss.setAnimState(PaladinEntity.STATE_SPIN_ATTACK);
        } else {
            boss.setAnimState(PaladinEntity.STATE_GRAB);
            beginGrab(level, target);
        }
        boss.lookAtPlayer(target, 180.0F);
    }

    private void startRandomMeleeAttack(ServerPlayer target) {
        this.attackTick = 0;
        this.targetId = target.getUUID();
        boss.getNavigation().stop();
        boss.setDeltaMovement(0.0D, boss.getDeltaMovement().y, 0.0D);

        if (boss.getRandom().nextBoolean()) {
            boss.setAnimState(PaladinEntity.STATE_ATTACK_1);
        } else {
            boss.setAnimState(PaladinEntity.STATE_ATTACK_2);
        }
        boss.lookAtPlayer(target, 180.0F);
    }

    private void tickAttack(ServerLevel level) {
        int state = boss.getAnimState();
        if (state != PaladinEntity.STATE_ATTACK_1 && state != PaladinEntity.STATE_ATTACK_2 && state != PaladinEntity.STATE_SPIN_ATTACK && state != PaladinEntity.STATE_GRAB && state != PaladinEntity.STATE_SHIELD) {
            cancelAttack(level);
            return;
        }

        this.attackTick++;
        boss.getNavigation().stop();

        if (state != PaladinEntity.STATE_SPIN_ATTACK) {
            boss.setDeltaMovement(0.0D, boss.getDeltaMovement().y, 0.0D);
        }
        ServerPlayer target = resolveTarget(level);
        if (target != null) {
            boss.lookAtPlayer(target, 10.0F);
        }
        //pal 1

        if (state == PaladinEntity.STATE_ATTACK_1) {
            if (this.attackTick == ATTACK_1_DAMAGE_TICK) {
                performShockwaveAttack(level);
            }
            if (this.attackTick >= ATTACK_1_DURATION) {
                finishAttack();
            }
            return;
        }
        if (state == PaladinEntity.STATE_SHIELD) {
            tickShieldAttack(level);
            return;
        }
        // spin

        if (state == PaladinEntity.STATE_SPIN_ATTACK) {
            tickSpinAttack(level, target);
            return;
        }
        // grab

        if (state == PaladinEntity.STATE_GRAB) {
            tickGrabAttack(level, target);
            return;
        }
        // pal 2
        if (this.attackTick == ATTACK_2_DAMAGE_TICK) {
            SoundEvent attackSound = boss.getRandom().nextBoolean() ? ModSounds.PALADIN_ATTACK1.get() : ModSounds.PALADIN_ATTACK2.get();
            playBossSound(level, attackSound, 1.0F, 1.0F);
            performDirectAttack(target);
        }
        if (this.attackTick >= ATTACK_2_DURATION) {
            finishAttack();
        }
    }


    private void tickSpinAttack(ServerLevel level, ServerPlayer target) {
        if (this.attackTick == SPIN_START_TICK) {
            playBossSound(level, ModSounds.HEART_KILL.get(), 1.0F, 1.0F);
        }
        if (this.attackTick < SPIN_START_TICK) {
            boss.setDeltaMovement(0.0D, boss.getDeltaMovement().y, 0.0D);
            if (target != null) {
                boss.lookAtPlayer(target, 12.0F);
            }
        }
        else if (this.attackTick <= SPIN_END_TICK) {
            tickSpinMovement(target);
            if ((this.attackTick - SPIN_START_TICK) % SPIN_DAMAGE_INTERVAL == 0) {
                performSpinDamage(level);
            }
        }
        else {
            boss.setDeltaMovement(0.0D, boss.getDeltaMovement().y, 0.0D);
        }
        if (this.attackTick >= SPIN_ATTACK_DURATION) {
            finishAttack();
        }
    }

    private void tickShieldAttack(ServerLevel level) {
        boss.getNavigation().stop();
        boss.setDeltaMovement(0.0D, boss.getDeltaMovement().y, 0.0D);
        if (this.attackTick == QTE_START_TICK) {
            beginQte(level);
        }

        if (this.qteActive) {
            this.heartShuffleTimer--;
            if (this.heartShuffleTimer <= 0) {
                shuffleQteHearts(level);
                this.heartShuffleTimer = randomBetween(MIN_HEART_SHUFFLE, MAX_HEART_SHUFFLE);
            }
        }
        if (this.attackTick == QTE_END_TICK && this.qteActive) {
            finishQte(level);
        }

        if (this.attackTick >= SHIELD_DURATION) {
            if (this.qteActive) {
                finishQte(level);
            }
            finishAttack();
        }
    }

    private void beginQte(ServerLevel level) {
        this.qteActive = true;
        this.heartsDestroyed = 0;
        this.heartShuffleTimer = randomBetween(MIN_HEART_SHUFFLE, MAX_HEART_SHUFFLE);
        for (int slot = 0; slot < 3; slot++) {
            SwordHeartEntity heart = Oasiso.SWORD_HEART.get().create(level);
            if (heart == null) {
                continue;
            }
            heart.setOwnerUuid(boss.getUUID());
            heart.setQteSlot(slot);
            Vec3 position = boss.getQteServerAnchor(slot);
            heart.moveTo(position.x, position.y, position.z, boss.getYRot(), 0.0F);
            level.addFreshEntity(heart);
        }
    }

    private void shuffleQteHearts(ServerLevel level) {
        List<SwordHeartEntity> hearts = getQteHearts(level);
        if (hearts.isEmpty()) {
            return;
        }
        int[] slots = {0, 1, 2};
        for (int i = slots.length - 1; i > 0; i--) {
            int j = boss.getRandom().nextInt(i + 1);
            int temp = slots[i];
            slots[i] = slots[j];
            slots[j] = temp;
        }
        for (int i = 0; i < hearts.size() && i < 3; i++) {
            hearts.get(i).setQteSlot(slots[i]);
        }
    }

    private List<SwordHeartEntity> getQteHearts(ServerLevel level) {
        UUID bossId = boss.getUUID();
        return level.getEntitiesOfClass(SwordHeartEntity.class, boss.getBoundingBox().inflate(12.0D), heart -> heart.isAlive() && bossId.equals(heart.getOwnerUuid()));
    }

    private void tickSpinMovement(ServerPlayer target) {
        if (target == null || !target.isAlive()) {
            boss.setDeltaMovement(0.0D, boss.getDeltaMovement().y, 0.0D);
            return;
        }
        Vec3 toTarget = new Vec3(target.getX() - boss.getX(), 0.0D, target.getZ() - boss.getZ());
        if (toTarget.lengthSqr() < 0.0001D) {
            return;
        }
        Vec3 forward = toTarget.normalize();
        Vec3 side = new Vec3(-forward.z, 0.0D, forward.x);
        double phase = (this.attackTick - SPIN_START_TICK) * SPIN_ZIGZAG_FREQUENCY;
        double sideways = Math.sin(phase) * SPIN_SIDE_SPEED;
        Vec3 movement = forward.scale(SPIN_FORWARD_SPEED).add(side.scale(sideways));
        boss.setDeltaMovement(movement.x, boss.getDeltaMovement().y, movement.z);
        boss.hasImpulse = true;
    }

    private void performSpinDamage(ServerLevel level) {
        float damage = (float) boss.getAttributeValue(Attributes.ATTACK_DAMAGE);
        AABB area = boss.getBoundingBox().inflate(SPIN_DAMAGE_RADIUS);
        List<LivingEntity> entities = level.getEntitiesOfClass(LivingEntity.class, area, entity -> canSpinHit(entity));
        for (LivingEntity entity : entities) {
            if (boss.distanceToSqr(entity) > SPIN_DAMAGE_RADIUS * SPIN_DAMAGE_RADIUS) {
                continue;
            }
            entity.hurt(boss.damageSources().mobAttack(boss),
                    damage);
        }
    }

    private boolean canSpinHit(LivingEntity entity) {
        if (entity == boss || !entity.isAlive()) {
            return false;
        }
        if (entity instanceof Player player) {
            return !player.isCreative() && !player.isSpectator();
        }
        return true;
    }

    private void beginGrab(ServerLevel level, ServerPlayer target) {
        Vec3 horizontal = new Vec3(boss.getX() - target.getX(), 0.0D, boss.getZ() - target.getZ());
        double distance = horizontal.length();
        this.grabRemainingDistance = Math.min(GRAB_PULL_DISTANCE, Math.max(0.0D, distance - GRAB_MIN_DISTANCE));
        spawnGrabParticles(level, target, 35);
        tickGrabPull(level, target);
    }

    private void tickGrabAttack(ServerLevel level, ServerPlayer target) {
        if (target != null && this.grabRemainingDistance > 0.001D) {
            tickGrabPull(level, target);
        }
        if (this.attackTick >= GRAB_DURATION) {
            finishAttack();
        }
    }

    private void tickGrabPull(ServerLevel level, ServerPlayer target) {
        if (target == null || !target.isAlive() || this.grabRemainingDistance <= 0.001D) {
            return;
        }
        Vec3 horizontal = new Vec3(boss.getX() - target.getX(), 0.0D, boss.getZ() - target.getZ());

        double currentDistance = horizontal.length();
        if (currentDistance <= GRAB_MIN_DISTANCE) {
            this.grabRemainingDistance = 0.0D;
            return;
        }
        Vec3 direction = horizontal.normalize();
        double allowedDistance = currentDistance - GRAB_MIN_DISTANCE;
        double step = Math.min(GRAB_PULL_STEP, Math.min(this.grabRemainingDistance, allowedDistance));

        if (step <= 0.001D) {
            this.grabRemainingDistance = 0.0D;
            return;
        }
        Vec3 movement = direction.scale(step);
        AABB movedBox = target.getBoundingBox().move(movement);

        if (!level.noCollision(target, movedBox)) {
            this.grabRemainingDistance = 0.0D;
            return;
        }
        target.teleportTo(target.getX() + movement.x, target.getY(), target.getZ() + movement.z);
        this.grabRemainingDistance -= step;
        spawnGrabParticles(level, target, 7);
    }

    private void spawnGrabParticles(ServerLevel level, ServerPlayer target, int count) {
        level.sendParticles(Oasiso.WIZARD_PIXELS.get(), target.getX(), target.getY() + target.getBbHeight() * 0.5D, target.getZ(), count, target.getBbWidth() * 0.65D, target.getBbHeight() * 0.55D, target.getBbWidth() * 0.65D, 0.025D);
    }

    private void performShockwaveAttack(ServerLevel level) {
        playBossSound(level, ModSounds.PALADIN_SHOCK.get(), 1.0F, 1.0F);
        float damage = (float) boss.getAttributeValue(Attributes.ATTACK_DAMAGE);
        AABB area = boss.getBoundingBox().inflate(SHOCKWAVE_RADIUS);
        List<LivingEntity> entities = level.getEntitiesOfClass(LivingEntity.class, area, entity -> canShockwaveHit(entity));
        for (LivingEntity entity : entities) {
            if (boss.distanceToSqr(entity) > SHOCKWAVE_RADIUS * SHOCKWAVE_RADIUS) {
                continue;
            }
            entity.hurt(boss.damageSources().mobAttack(boss), damage);
            Vec3 movement = entity.getDeltaMovement();
            double requiredPush = Math.max(0.0D, SHOCKWAVE_VERTICAL_SPEED - movement.y);
            if (requiredPush > 0.0D) {
                entity.push(0.0D, requiredPush, 0.0D);
            }
        }
    }


    private boolean canShockwaveHit(LivingEntity entity) {
        if (entity == boss || !entity.isAlive()) {
            return false;
        }
        if (entity instanceof Player player) {
            return !player.isCreative() && !player.isSpectator();
        }
        return true;
    }


    private void performDirectAttack(ServerPlayer target) {
        if (target == null || !target.isAlive()) {
            return;
        }
        if (boss.distanceToSqr(target) > ATTACK_2_HIT_RANGE * ATTACK_2_HIT_RANGE) {
            return;
        }
        if (!boss.hasLineOfSight(target)) {
            return;
        }
        float damage = (float) boss.getAttributeValue(Attributes.ATTACK_DAMAGE);
        target.hurt(boss.damageSources().mobAttack(boss), damage);
    }
    private void finishAttack() {
        this.attackTick = -1;
        this.targetId = null;
        this.grabRemainingDistance = 0.0D;

        boss.setDeltaMovement(0.0D, boss.getDeltaMovement().y, 0.0D);
        boss.setAnimState(PaladinEntity.STATE_IDLE);

        this.attackCooldown = randomBetween(MIN_MELEE_COOLDOWN, MAX_MELEE_COOLDOWN);
    }

    private void cancelAttack(ServerLevel level) {
        if (this.qteActive) {
            this.qteActive = false;
            cleanupQteHearts(level);
        }

        this.attackTick = -1;
        this.grabRemainingDistance = 0.0D;

        boss.setDeltaMovement(0.0D, boss.getDeltaMovement().y, 0.0D);

        this.targetId = null;
        this.attackCooldown = 15;
    }

    private ServerPlayer getCurrentTarget() {
        if (!(boss.getTarget() instanceof ServerPlayer player)) {
            return null;
        }
        return isValidTarget(player) ? player : null;
    }

    private ServerPlayer resolveTarget(ServerLevel level) {
        if (this.targetId == null) {
            return null;
        }

        ServerPlayer player = level.getServer().getPlayerList().getPlayer(this.targetId);
        if (player == null || player.serverLevel() != level || !isValidTarget(player)) {
            return null;
        }
        return player;
    }

    private boolean isValidTarget(ServerPlayer player) {
        return player.isAlive() && !player.isCreative() && !player.isSpectator();
    }

    private int randomBetween(int minimum, int maximum) {
        return minimum + boss.getRandom().nextInt(maximum - minimum + 1);
    }

    private void finishQte(ServerLevel level) {
        if (!this.qteActive) {
            return;
        }

        this.qteActive = false;

        if (this.heartsDestroyed >= 3) {

            boss.applyQteBacklash(QTE_SUCCESS_DAMAGE);
            level.sendParticles(Oasiso.PURPLE_STARS.get(), boss.getX(), boss.getY() + boss.getBbHeight() * 0.6D, boss.getZ(), 45, 0.9D, 1.5D, 0.9D, 0.08D);

        } else {
            float healthBefore = boss.getHealth();
            boss.heal(QTE_FAILURE_HEAL);
            float actualHeal = boss.getHealth() - healthBefore;

            if (actualHeal > 0.0F) {
                DamageNumberSpawner.spawn(level, boss, actualHeal);
            }
            level.sendParticles(Oasiso.PURPLE_STARS.get(), boss.getX(), boss.getY() + boss.getBbHeight() * 0.6D, boss.getZ(), 55, 0.9D, 1.6D, 0.9D, 0.05D);
        }
        cleanupQteHearts(level);
    }

    private void cleanupQteHearts(ServerLevel level) {
        for (SwordHeartEntity heart : getQteHearts(level)) {
            heart.discard();
        }
    }

    public boolean isQteActive() {
        return this.qteActive;
    }

    public void onSwordHeartDestroyed() {
        if (!this.qteActive) {
            return;
        }
        this.heartsDestroyed = Math.min(3, this.heartsDestroyed + 1);
    }

    private void playBossSound(ServerLevel level, SoundEvent sound, float volume, float pitch) {
        level.playSound(null,
                boss.getX(), boss.getY() + boss.getBbHeight() * 0.5D, boss.getZ(),
                sound, SoundSource.HOSTILE,
                volume, pitch);
    }

    public void save(CompoundTag parent) {
        CompoundTag tag = new CompoundTag();

        tag.putInt("SpecialCooldown", this.specialCooldown);
        tag.putInt("TeleportHopsRemaining", this.teleportHopsRemaining);
        tag.putInt("TeleportHopTimer", this.teleportHopTimer);
        tag.putBoolean("QteActive", this.qteActive);
        tag.putInt("HeartsDestroyed", this.heartsDestroyed);
        tag.putInt("HeartShuffleTimer", this.heartShuffleTimer);
        tag.putInt("AttackTick", this.attackTick);
        tag.putDouble("GrabRemainingDistance", this.grabRemainingDistance);
        tag.putInt("AttackCooldown", this.attackCooldown);

        if (this.targetId != null) {
            tag.putUUID("Target", this.targetId);
        }
        parent.put(DATA_TAG, tag);
    }


    public void load(CompoundTag parent) {
        if (!parent.contains(DATA_TAG, Tag.TAG_COMPOUND)) {
            return;
        }
        CompoundTag tag = parent.getCompound(DATA_TAG);

        this.specialCooldown = tag.getInt("SpecialCooldown");
        this.teleportHopsRemaining = tag.getInt("TeleportHopsRemaining");
        this.teleportHopTimer = tag.getInt("TeleportHopTimer");
        this.qteActive = tag.getBoolean("QteActive");
        this.heartsDestroyed = tag.getInt("HeartsDestroyed");
        this.heartShuffleTimer = tag.getInt("HeartShuffleTimer");
        this.grabRemainingDistance = tag.getDouble("GrabRemainingDistance");
        this.attackTick = tag.getInt("AttackTick");
        this.attackCooldown = tag.getInt("AttackCooldown");
        this.targetId = tag.hasUUID("Target") ? tag.getUUID("Target") : null;
    }
}