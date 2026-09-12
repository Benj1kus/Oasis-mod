package com.benji.oasiso.common.entity.ai;

import com.benji.oasiso.Oasiso;
import com.benji.oasiso.common.entity.AzumaalEntity;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import com.benji.oasiso.config.OsirisRealmConfig;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class AzumaalStageTwoAI {

    private static final String DATA_TAG = "AzumaalStageTwoAI";

    private static final int INITIAL_ATTACK_COOLDOWN = 30;

    private static final int ATTACK_COOLDOWN_MIN = 20;
    private static final int ATTACK_COOLDOWN_MAX = 35;

    private static final double TARGET_RANGE = 64.0D;

    private static final float BITE_SELECTION_CHANCE = 0.40F;
    private static final int BITE_ANIMATION_TICKS = 10;
    private static final int BITE_COOLDOWN_TICKS = 50;
    private static final double BITE_EXTRA_REACH = 1.35D;

    private static final int DIG_ANIMATION_TICKS = 34;
    private static final int DIG_WAIT_TICKS = 40;

    private static final int EMERGE_ANIMATION_TICKS = 25;
    private static final int EMERGE_DAMAGE_TICK = 7;
    private static final double EMERGE_DAMAGE_EXTRA_REACH = 1.2D;
    private static final double SAFE_EMERGE_MIN_RADIUS = 4.0D;
    private static final double SAFE_EMERGE_MAX_RADIUS = 5.0D;

    private static final double CHARGE_SPEED = 0.72D;
    private static final int CHARGE_MAX_TICKS = 20 * 8;
    private static final int CHARGE_DAMAGE_COOLDOWN = 10;
    private static final double CHARGE_WALL_PROBE = 0.80D;
    private static final int CHARGE_IMPACT_ANIMATION_TICKS = 25;
    private static final double CHARGE_PUSH_SPEED = 0.65D;

    private final AzumaalEntity boss;

    private Phase phase = Phase.NONE;

    private int attackTick;
    private int attackCooldown = INITIAL_ATTACK_COOLDOWN;
    private int biteCooldown;

    private UUID targetId;
    private Vec3 chargeDirection = Vec3.ZERO;

    private boolean damagingEmerge;
    private boolean emergeDamageDone;
    private double digGroundY;
    private final Map<UUID, Long> chargeHitTimes = new HashMap<>();


    public AzumaalStageTwoAI(AzumaalEntity boss) {
        this.boss = boss;
    }

    public void beginStageTwo() {
        reset();

        this.attackCooldown = INITIAL_ATTACK_COOLDOWN;
        boss.setAnimState(AzumaalEntity.STATE_IDLE);
        boss.setDeltaMovement(Vec3.ZERO);
    }

    public void reset() {
        this.phase = Phase.NONE;

        this.attackTick = 0;
        this.attackCooldown = INITIAL_ATTACK_COOLDOWN;
        this.biteCooldown = 0;

        this.targetId = null;

        this.chargeDirection = Vec3.ZERO;

        this.damagingEmerge = false;
        this.emergeDamageDone = false;

        this.chargeHitTimes.clear();

        boss.setDeltaMovement(Vec3.ZERO);
    }

    public boolean isAttacking() {
        return this.phase != Phase.NONE;
    }

    public boolean isCharging() {
        return this.phase == Phase.CHARGE;
    }

    public void tick(ServerLevel level) {
        if (this.biteCooldown > 0) {
            this.biteCooldown--;
        }

        switch (this.phase) {
            case NONE -> tickIdle(level);
            case BITE -> tickBite(level);
            case DIG_ANIMATION -> tickDigAnimation(level);
            case DIG_WAIT -> tickDigWait(level);
            case EMERGE -> tickEmerge(level);
            case CHARGE -> tickCharge(level);
            case CHARGE_IMPACT -> tickChargeImpact();
        }
    }

    private void tickIdle(ServerLevel level) {
        boss.setDeltaMovement(0.0D, boss.getDeltaMovement().y, 0.0D);

        ServerPlayer target = findNearestTarget(level);

        if (target != null) {
            boss.lookAtPlayer(target, 5.0F);
        }

        if (this.attackCooldown > 0) {
            this.attackCooldown--;
            return;
        }

        if (target == null) {
            this.attackCooldown = 20;
            return;
        }

        startRandomAttack(target);
    }

    private void startRandomAttack(ServerPlayer target) {

        boolean canBite = this.biteCooldown <= 0 && isInBiteRange(target);

        if (canBite && boss.getRandom().nextFloat() < BITE_SELECTION_CHANCE) {
            startBite(target);
            return;
        }

        if (boss.getRandom().nextBoolean()) {
            startDig(target);
        } else {
            startCharge(target);
        }
    }


    private void startBite(ServerPlayer target) {
        this.phase = Phase.BITE;
        this.attackTick = 0;
        this.targetId = target.getUUID();

        boss.setDeltaMovement(Vec3.ZERO);
        boss.setAnimState(AzumaalEntity.STATE_STAGE_TWO_BITE);
    }

    private void tickBite(ServerLevel level) {
        this.attackTick++;

        ServerPlayer target = resolveTarget(level);

        if (target != null) {
            boss.lookAtPlayer(target, 8.0F);
        }

        if (this.attackTick < BITE_ANIMATION_TICKS) {
            return;
        }

        if (target != null && isInBiteRange(target)) {
            damagePlayer(target);
        }

        this.biteCooldown = BITE_COOLDOWN_TICKS;

        finishAttack();
    }

    private boolean isInBiteRange(ServerPlayer player) {
        return boss.getBoundingBox().inflate(BITE_EXTRA_REACH).intersects(player.getBoundingBox());
    }

    private void startDig(ServerPlayer target) {
        this.phase = Phase.DIG_ANIMATION;
        this.attackTick = 0;
        this.targetId = target.getUUID();
        this.digGroundY = boss.getY();

        boss.setDeltaMovement(Vec3.ZERO);
        boss.setAnimState(AzumaalEntity.STATE_STAGE_TWO_DIG);
    }

    private void tickDigAnimation(ServerLevel level) {
        this.attackTick++;

        boss.setDeltaMovement(Vec3.ZERO);

        if (this.attackTick % 2 == 0) {
            spawnDigParticles(level, boss.getX(), boss.getY(), boss.getZ(), 4.0D, 12);
        }

        if (this.attackTick < DIG_ANIMATION_TICKS) {
            return;
        }

        this.phase = Phase.DIG_WAIT;
        this.attackTick = 0;
    }

    private void tickDigWait(ServerLevel level) {
        this.attackTick++;

        boss.setDeltaMovement(Vec3.ZERO);

        if (this.attackTick < DIG_WAIT_TICKS) {
            return;
        }

        ServerPlayer target = resolveTarget(level);

        if (target == null) {
            target = findNearestTarget(level);
        }

        if (target == null) {
            finishAttack();
            return;
        }

        this.damagingEmerge = boss.getRandom().nextBoolean();
        this.emergeDamageDone = false;

        Vec3 emergePosition;

        if (this.damagingEmerge) {
            emergePosition = new Vec3(target.getX(), this.digGroundY, target.getZ());

        } else {
            emergePosition = findSafeEmergePosition(level, target);
        }

        boss.setPos(emergePosition.x, emergePosition.y, emergePosition.z);
        boss.setDeltaMovement(Vec3.ZERO);

        spawnDigParticles(level, emergePosition.x, emergePosition.y, emergePosition.z, 4.0D, 55);

        this.targetId = target.getUUID();
        this.phase = Phase.EMERGE;
        this.attackTick = 0;

        boss.setAnimState(AzumaalEntity.STATE_STAGE_TWO_JUMP);
    }

    private void tickEmerge(ServerLevel level) {
        this.attackTick++;

        boss.setDeltaMovement(Vec3.ZERO);

        ServerPlayer target = resolveTarget(level);

        if (this.damagingEmerge && !this.emergeDamageDone && this.attackTick >= EMERGE_DAMAGE_TICK) {
            this.emergeDamageDone = true;

            if (target != null && boss.getBoundingBox().inflate(EMERGE_DAMAGE_EXTRA_REACH).intersects(target.getBoundingBox())) {
                damagePlayer(target, OsirisRealmConfig.AZUMAAL_STAGE_TWO_EMERGE_DAMAGE.get().floatValue());
            }
        }

        if (this.attackTick >= EMERGE_ANIMATION_TICKS) {
            finishAttack();
        }
    }

    private Vec3 findSafeEmergePosition(ServerLevel level, ServerPlayer target) {
        for (int attempt = 0; attempt < 16; attempt++) {

            double angle = boss.getRandom().nextDouble() * Math.PI * 2.0D;
            double radius = Mth.lerp(boss.getRandom().nextDouble(), SAFE_EMERGE_MIN_RADIUS, SAFE_EMERGE_MAX_RADIUS);

            double x = target.getX() + Math.cos(angle) * radius;
            double z = target.getZ() + Math.sin(angle) * radius;

            Vec3 candidate = new Vec3(x, this.digGroundY, z);

            AABB movedBox = boss.getBoundingBox().move(candidate.x - boss.getX(), candidate.y - boss.getY(), candidate.z - boss.getZ());

            if (level.noCollision(boss, movedBox)) {
                return candidate;
            }
        }

        double angle = boss.getRandom().nextDouble() * Math.PI * 2.0D;

        return new Vec3(target.getX() + Math.cos(angle) * SAFE_EMERGE_MAX_RADIUS, this.digGroundY, target.getZ() + Math.sin(angle) * SAFE_EMERGE_MAX_RADIUS);
    }

    private void spawnDigParticles(ServerLevel level, double centerX, double groundY, double centerZ, double radius, int count) {
        BlockParticleOption particle = new BlockParticleOption(ParticleTypes.BLOCK, Oasiso.NEPHRITIS_COMPRESSED.get().defaultBlockState());

        for (int i = 0; i < count; i++) {

            double x = centerX + (boss.getRandom().nextDouble() * 2.0D - 1.0D) * radius;
            double z = centerZ + (boss.getRandom().nextDouble() * 2.0D - 1.0D) * radius;

            level.sendParticles(particle, x, groundY + 0.08D, z, 1, 0.04D, 0.15D, 0.04D, 0.09D);
        }
    }

    private void startCharge(ServerPlayer target) {
        Vec3 horizontal = new Vec3(target.getX() - boss.getX(), 0.0D, target.getZ() - boss.getZ());

        if (horizontal.lengthSqr() < 1.0E-6D) {
            Vec3 look = boss.getLookAngle();
            horizontal = new Vec3(look.x, 0.0D, look.z);
        }

        if (horizontal.lengthSqr() < 1.0E-6D) {
            horizontal = new Vec3(0.0D, 0.0D, 1.0D);
        }

        this.chargeDirection = horizontal.normalize();
        this.targetId = target.getUUID();
        this.phase = Phase.CHARGE;
        this.attackTick = 0;
        this.chargeHitTimes.clear();

        faceChargeDirection();

        boss.setAnimState(AzumaalEntity.STATE_STAGE_TWO_RUN);
        boss.setDeltaMovement(Vec3.ZERO);
    }

    private void tickCharge(ServerLevel level) {
        this.attackTick++;

        faceChargeDirection();

        if (hasWallAhead(level)) {
            beginChargeImpact(level);
            return;
        }

        Vec3 step = this.chargeDirection.scale(CHARGE_SPEED);

        boss.move(MoverType.SELF, step);

        if (boss.horizontalCollision) {
            beginChargeImpact(level);
            return;
        }

        damageAndPushChargeTargets(level);

        if (this.attackTick >= CHARGE_MAX_TICKS) {

            finishAttack();
        }
    }

    private boolean hasWallAhead(ServerLevel level) {
        Vec3 probe = this.chargeDirection.scale(CHARGE_SPEED + CHARGE_WALL_PROBE);

        AABB futureBox = boss.getBoundingBox().move(probe.x, 0.0D, probe.z);

        return !level.noCollision(boss, futureBox);
    }

    private void damageAndPushChargeTargets(ServerLevel level) {
        long now = level.getGameTime();

        AABB hitBox = boss.getBoundingBox().inflate(0.30D, 0.10D, 0.30D);

        for (ServerPlayer player : level.players()) {

            if (!isValidTarget(player)) {
                continue;
            }

            if (!hitBox.intersects(player.getBoundingBox())) {
                continue;
            }

            Vec3 oldMovement = player.getDeltaMovement();

            player.setDeltaMovement(this.chargeDirection.x * CHARGE_PUSH_SPEED, Math.max(oldMovement.y, 0.08D), this.chargeDirection.z * CHARGE_PUSH_SPEED);

            player.hurtMarked = true;
            Long lastHit = this.chargeHitTimes.get(player.getUUID());

            if (lastHit != null && now - lastHit < CHARGE_DAMAGE_COOLDOWN) {
                continue;
            }

            this.chargeHitTimes.put(player.getUUID(), now);
            damagePlayer(player, OsirisRealmConfig.AZUMAAL_STAGE_TWO_CHARGE_DAMAGE.get().floatValue());
        }
    }

    private void beginChargeImpact(ServerLevel level) {
        this.phase = Phase.CHARGE_IMPACT;

        this.attackTick = 0;

        boss.setDeltaMovement(Vec3.ZERO);
        boss.setAnimState(AzumaalEntity.STATE_STAGE_TWO_TENTACLE);
        spawnChargeImpactSmoke(level);
    }

    private void tickChargeImpact() {
        this.attackTick++;
        boss.setDeltaMovement(Vec3.ZERO);

        if (this.attackTick >= CHARGE_IMPACT_ANIMATION_TICKS) {
            finishAttack();
        }
    }

    private void spawnChargeImpactSmoke(ServerLevel level) {
        Vec3 impactCenter = boss.position().add(this.chargeDirection.scale(boss.getBbWidth() * 0.5D + 0.25D));

        for (int i = 0; i < 46; i++) {

            double angle = boss.getRandom().nextDouble() * Math.PI * 2.0D;
            double horizontalSpeed = 0.035D + boss.getRandom().nextDouble() * 0.095D;

            double vx = Math.cos(angle) * horizontalSpeed;
            double vz = Math.sin(angle) * horizontalSpeed;
            double vy = -0.025D - boss.getRandom().nextDouble() * 0.055D;

            double x = impactCenter.x + (boss.getRandom().nextDouble() - 0.5D) * 1.4D;
            double y = boss.getY() + 0.25D + boss.getRandom().nextDouble() * 1.45D;
            double z = impactCenter.z + (boss.getRandom().nextDouble() - 0.5D) * 1.4D;

            level.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, x, y, z, 0, vx, vy, vz, 1.0D);
        }
    }

    private void faceChargeDirection() {
        float yaw = (float) (Mth.atan2(this.chargeDirection.z, this.chargeDirection.x) * 180.0D / Math.PI) - 90.0F;
        boss.setYRot(yaw);
        boss.setYHeadRot(yaw);
        boss.yBodyRot = yaw;
        boss.yBodyRotO = yaw;
    }

    private void damagePlayer(ServerPlayer player) {
        damagePlayer(player, (float) boss.getAttributeValue(Attributes.ATTACK_DAMAGE));
    }

    private void damagePlayer(ServerPlayer player, float baseDamage) {
        player.hurt(boss.damageSources().mobAttack(boss), baseDamage);
    }

    private ServerPlayer findNearestTarget(ServerLevel level) {
        double maxDistanceSqr = TARGET_RANGE * TARGET_RANGE;

        ServerPlayer nearest = null;
        double nearestDistance = Double.MAX_VALUE;

        for (ServerPlayer player : level.players()) {

            if (!isValidTarget(player)) {
                continue;
            }

            double distance = boss.distanceToSqr(player);

            if (distance > maxDistanceSqr) {
                continue;
            }
            if (distance >= nearestDistance) {
                continue;
            }

            nearest = player;
            nearestDistance = distance;
        }
        return nearest;
    }

    private ServerPlayer resolveTarget(ServerLevel level) {
        if (this.targetId == null) {
            return null;
        }
        ServerPlayer player = level.getServer().getPlayerList().getPlayer(this.targetId);

        if (!isValidTarget(player)) {
            return null;
        }
        return player;
    }

    private boolean isValidTarget(ServerPlayer player) {
        return player != null && player.serverLevel() == boss.level() && player.isAlive() && !player.isSpectator() && !player.isCreative() && boss.isEncounterParticipant(player);
    }

    private void finishAttack() {
        this.phase = Phase.NONE;

        this.attackTick = 0;
        this.targetId = null;
        this.chargeDirection = Vec3.ZERO;
        this.damagingEmerge = false;
        this.emergeDamageDone = false;
        this.chargeHitTimes.clear();

        boss.setDeltaMovement(Vec3.ZERO);
        boss.setAnimState(AzumaalEntity.STATE_IDLE);

        this.attackCooldown = randomBetween(ATTACK_COOLDOWN_MIN, ATTACK_COOLDOWN_MAX);
    }

    private int randomBetween(int minimum, int maximum) {
        int min = Math.min(minimum, maximum);
        int max = Math.max(minimum, maximum);
        return min + boss.getRandom().nextInt(max - min + 1);
    }

    public void save(CompoundTag parent) {
        CompoundTag tag = new CompoundTag();

        tag.putString("Phase", this.phase.name());
        tag.putInt("AttackTick", this.attackTick);
        tag.putInt("AttackCooldown", this.attackCooldown);
        tag.putInt("BiteCooldown", this.biteCooldown);

        if (this.targetId != null) {
            tag.putUUID("Target", this.targetId);
        }

        tag.putDouble("ChargeX", this.chargeDirection.x);
        tag.putDouble("ChargeZ", this.chargeDirection.z);
        tag.putBoolean("DamagingEmerge", this.damagingEmerge);
        tag.putBoolean("EmergeDamageDone", this.emergeDamageDone);
        tag.putDouble("DigGroundY", this.digGroundY);

        parent.put(DATA_TAG, tag);
    }

    public void load(CompoundTag parent) {
        if (!parent.contains(DATA_TAG)) {
            return;
        }

        CompoundTag tag = parent.getCompound(DATA_TAG);

        try {
            this.phase = Phase.valueOf(tag.getString("Phase"));

        } catch (IllegalArgumentException ignored) {
            this.phase = Phase.NONE;
        }

        this.attackTick = tag.getInt("AttackTick");
        this.attackCooldown = tag.getInt("AttackCooldown");
        this.biteCooldown = tag.getInt("BiteCooldown");
        this.targetId = tag.hasUUID("Target") ? tag.getUUID("Target") : null;
        this.chargeDirection = new Vec3(tag.getDouble("ChargeX"), 0.0D, tag.getDouble("ChargeZ"));
        this.damagingEmerge = tag.getBoolean("DamagingEmerge");
        this.emergeDamageDone = tag.getBoolean("EmergeDamageDone");
        this.digGroundY = tag.getDouble("DigGroundY");
    }

    private enum Phase {
        NONE, BITE, DIG_ANIMATION, DIG_WAIT, EMERGE, CHARGE, CHARGE_IMPACT
    }
}