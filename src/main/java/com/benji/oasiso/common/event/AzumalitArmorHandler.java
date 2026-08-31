package com.benji.oasiso.common.event;

import com.benji.oasiso.Oasiso;
import com.benji.oasiso.common.item.AzumalitArmorItem;
import com.benji.oasiso.common.waypoint.AzumalitWaypointManager;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.ProjectileImpactEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = Oasiso.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class AzumalitArmorHandler {


    private static final float LOW_HEALTH_THRESHOLD = 0.50F;
    private static final float CRITICAL_HEALTH_THRESHOLD = 0.20F;

    private static final int LOW_HEALTH_HITS_REQUIRED = 10;
    private static final int CRITICAL_HEALTH_HITS_REQUIRED = 5;

    private static final int DEFENSE_DURATION_TICKS = 2 * 20;

    private static final int MAX_DEFENSES_BEFORE_COOLDOWN = 3;
    private static final int DEFENSE_COOLDOWN_TICKS = 60 * 20;

    private static final float PROJECTILE_REFLECT_CHANCE = 0.50F;
    private static final double PROJECTILE_REFLECT_SPEED_MULTIPLIER = 1.05D;
    private static final double PROJECTILE_PUSH_OUT_DISTANCE = 0.70D;

    private static final String DATA_HIT_COUNT = "OasisoAzumalitHitCount";
    private static final String DATA_DEFENSE_USES = "OasisoAzumalitDefenseUses";
    private static final String DATA_DEFENSE_UNTIL = "OasisoAzumalitDefenseUntil";
    private static final String DATA_COOLDOWN_UNTIL = "OasisoAzumalitCooldownUntil";

    private static final double MELEE_ATTACK_RADIUS = 3.0D;
    private static final double STRONG_ATTACK_RADIUS = 8.0D;

    private static final float ARM_ATTACK_DAMAGE = 20.0F;
    private static final int ARM_ATTACK_COOLDOWN_TICKS = 3 * 20;
    private static final int ARM_ATTACK_DAMAGE_TICK = AzumalitArmorItem.ATTACK_DAMAGE_KEY_TICK;
    private static final int ARM_ATTACK_DURATION_TICKS = AzumalitArmorItem.ATTACK_ANIMATION_TICKS;

    private static final float STRONG_ATTACK_CHANCE = 0.30F;
    private static final double STRONG_ATTACK_VERTICAL_SPEED = 1.0D;

    private static final int ATTACK_NONE = 0;
    private static final int ATTACK_LEFT = 1;
    private static final int ATTACK_RIGHT = 2;
    private static final int ATTACK_BOTH = 3;

    private static final String DATA_ATTACK_TYPE = "OasisoAzumalitArmAttackType";
    private static final String DATA_ATTACK_DAMAGE_AT = "OasisoAzumalitArmAttackDamageAt";
    private static final String DATA_ATTACK_END_AT = "OasisoAzumalitArmAttackEndAt";
    private static final String DATA_ATTACK_NEXT_AT = "OasisoAzumalitArmAttackNextAt";
    private static final String DATA_ATTACK_DAMAGE_DONE = "OasisoAzumalitArmAttackDamageDone";
    private static final String DATA_ATTACK_MANUAL_TARGET = "OasisoAzumalitArmAttackTarget";

    private AzumalitArmorHandler() {
    }

    @SubscribeEvent
    public static void onLivingAttack(LivingAttackEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        if (!AzumalitArmorItem.isWearingFullSet(player)) {
            return;
        }

        ServerLevel level = player.serverLevel();
        long gameTime = level.getGameTime();

        refreshExpiredCooldown(player, gameTime);

        if (event.getSource().is(DamageTypeTags.IS_EXPLOSION)) {
            event.setCanceled(true);
            return;
        }

        if (isDefenseActive(player, gameTime)) {
            event.setCanceled(true);

            if (isCombatHit(player, event.getSource())) {
                playShieldBlockSound(level, player);
            }

            return;
        }

        if (!isCombatHit(player, event.getSource())) {
            return;
        }

        if (isCooldownActive(player, gameTime)) {
            return;
        }

        float maxHealth = player.getMaxHealth();

        if (maxHealth <= 0.0F) {
            return;
        }

        float healthRatio = player.getHealth() / maxHealth;

        if (healthRatio >= LOW_HEALTH_THRESHOLD) {
            setHitCount(player, 0);
            return;
        }

        int requiredHits = healthRatio < CRITICAL_HEALTH_THRESHOLD ? CRITICAL_HEALTH_HITS_REQUIRED : LOW_HEALTH_HITS_REQUIRED;

        int hitCount = getHitCount(player) + 1;
        setHitCount(player, hitCount);

        if (hitCount < requiredHits) {
            return;
        }

        activateDefense(player, level, gameTime);

        event.setCanceled(true);
        playShieldBlockSound(level, player);
    }

    @SubscribeEvent
    public static void onProjectileImpact(ProjectileImpactEvent event) {
        if (!(event.getRayTraceResult() instanceof EntityHitResult entityHit)) {
            return;
        }

        if (!(entityHit.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        if (!AzumalitArmorItem.isWearingFullSet(player)) {
            return;
        }

        ServerLevel level = player.serverLevel();
        long gameTime = level.getGameTime();

        if (isDefenseActive(player, gameTime)) {
            return;
        }

        if (player.getRandom().nextFloat() >= PROJECTILE_REFLECT_CHANCE) {
            return;
        }

        Projectile projectile = event.getProjectile();
        Vec3 incoming = projectile.getDeltaMovement();

        if (incoming.lengthSqr() < 0.000001D) {
            return;
        }

        Vec3 reflected = incoming.scale(-PROJECTILE_REFLECT_SPEED_MULTIPLIER);
        Vec3 reflectedDirection = reflected.normalize();

        Vec3 hitPosition = entityHit.getLocation();
        Vec3 newPosition = hitPosition.add(reflectedDirection.scale(PROJECTILE_PUSH_OUT_DISTANCE));

        projectile.setPos(newPosition.x, newPosition.y, newPosition.z);
        projectile.setDeltaMovement(reflected);

        event.setImpactResult(ProjectileImpactEvent.ImpactResult.SKIP_ENTITY);

        cancelActiveArmAttack(player);
        AzumalitWaypointManager.cancelCast(player);

        AzumalitArmorItem.triggerChestAnimation(player, AzumalitArmorItem.TRIGGER_DEFEND_PROJECTILE);

        playProjectileRicochetSound(level, player);
    }

    private static void activateDefense(ServerPlayer player, ServerLevel level, long gameTime) {
        cancelActiveArmAttack(player);
        AzumalitWaypointManager.cancelCast(player);

        CompoundTag data = player.getPersistentData();

        long defenseUntil = gameTime + DEFENSE_DURATION_TICKS;

        data.putLong(DATA_DEFENSE_UNTIL, defenseUntil);
        data.putInt(DATA_HIT_COUNT, 0);

        int uses = data.getInt(DATA_DEFENSE_USES) + 1;
        data.putInt(DATA_DEFENSE_USES, uses);

        if (uses >= MAX_DEFENSES_BEFORE_COOLDOWN) {
            data.putLong(DATA_COOLDOWN_UNTIL, defenseUntil + DEFENSE_COOLDOWN_TICKS);
        }

        AzumalitArmorItem.triggerChestAnimation(player, AzumalitArmorItem.TRIGGER_DEFEND);
    }

    @SubscribeEvent
    public static void onPlayerAttackEntity(AttackEntityEvent event) {
        if (event.getEntity().level().isClientSide) {
            return;
        }

        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        if (!(event.getTarget() instanceof LivingEntity target)) {
            return;
        }

        if (!AzumalitArmorItem.isWearingFullSet(player)) {
            return;
        }

        if (!isValidManualTarget(player, target)) {
            return;
        }

        if (player.distanceToSqr(target) > MELEE_ATTACK_RADIUS * MELEE_ATTACK_RADIUS) {
            return;
        }

        tryStartArmAttack(player, target.getUUID());
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide) {
            return;
        }

        if (!(event.player instanceof ServerPlayer player)) {
            return;
        }

        if (!AzumalitArmorItem.isWearingFullSet(player)) {
            cancelActiveArmAttack(player);
            return;
        }

        ServerLevel level = player.serverLevel();
        long gameTime = level.getGameTime();

        if (AzumalitWaypointManager.isCasting(player) || AzumalitArmorItem.isWaypointAnimationActive(player)) {
            cancelActiveArmAttack(player);
            return;
        }

        if (isDefenseActive(player, gameTime) || AzumalitArmorItem.isGuardAnimationActive(player)) {

            cancelActiveArmAttack(player);
            return;
        }

        if (tickActiveArmAttack(level, player, gameTime)) {
            return;
        }

        if (gameTime < player.getPersistentData().getLong(DATA_ATTACK_NEXT_AT)) {
            return;
        }

        Monster nearbyMonster = findNearestAutomaticMonster(level, player);

        if (nearbyMonster != null) {
            startArmAttack(player, null, gameTime);
        }
    }

    private static void tryStartArmAttack(ServerPlayer player, UUID manualTargetId) {
        ServerLevel level = player.serverLevel();
        long gameTime = level.getGameTime();

        if (AzumalitWaypointManager.isCasting(player) || AzumalitArmorItem.isWaypointAnimationActive(player) || isDefenseActive(player, gameTime) || AzumalitArmorItem.isGuardAnimationActive(player)) {
            return;
        }

        CompoundTag data = player.getPersistentData();

        if (data.getInt(DATA_ATTACK_TYPE) != ATTACK_NONE || gameTime < data.getLong(DATA_ATTACK_NEXT_AT)) {
            return;
        }

        startArmAttack(player, manualTargetId, gameTime);
    }

    private static void startArmAttack(ServerPlayer player, UUID manualTargetId, long gameTime) {
        CompoundTag data = player.getPersistentData();

        int attackType;

        if (player.getRandom().nextFloat() < STRONG_ATTACK_CHANCE) {
            attackType = ATTACK_BOTH;
        } else {
            attackType = player.getRandom().nextBoolean() ? ATTACK_LEFT : ATTACK_RIGHT;
        }

        data.putInt(DATA_ATTACK_TYPE, attackType);
        data.putLong(DATA_ATTACK_DAMAGE_AT, gameTime + ARM_ATTACK_DAMAGE_TICK);
        data.putLong(DATA_ATTACK_END_AT, gameTime + ARM_ATTACK_DURATION_TICKS);
        data.putLong(DATA_ATTACK_NEXT_AT, gameTime + ARM_ATTACK_COOLDOWN_TICKS);
        data.putBoolean(DATA_ATTACK_DAMAGE_DONE, false);

        if (manualTargetId != null) {
            data.putUUID(DATA_ATTACK_MANUAL_TARGET, manualTargetId);
        } else {
            data.remove(DATA_ATTACK_MANUAL_TARGET);
        }

        String animation = switch (attackType) {
            case ATTACK_LEFT -> AzumalitArmorItem.TRIGGER_ATTACK_LEFT;
            case ATTACK_RIGHT -> AzumalitArmorItem.TRIGGER_ATTACK_RIGHT;
            case ATTACK_BOTH -> AzumalitArmorItem.TRIGGER_ATTACK_BOTH;
            default -> null;
        };

        if (animation != null) {
            AzumalitArmorItem.triggerChestAnimation(player, animation);
        }
    }

    private static boolean tickActiveArmAttack(ServerLevel level, ServerPlayer player, long gameTime) {
        CompoundTag data = player.getPersistentData();
        int attackType = data.getInt(DATA_ATTACK_TYPE);

        if (attackType == ATTACK_NONE) {
            return false;
        }

        if (!data.getBoolean(DATA_ATTACK_DAMAGE_DONE) && gameTime >= data.getLong(DATA_ATTACK_DAMAGE_AT)) {

            data.putBoolean(DATA_ATTACK_DAMAGE_DONE, true);

            if (attackType == ATTACK_BOTH) {
                performStrongArmAttack(level, player);
            } else {
                performRegularArmAttack(level, player);
            }
        }

        if (gameTime >= data.getLong(DATA_ATTACK_END_AT)) {
            clearActiveArmAttackData(data);
            return false;
        }

        return true;
    }

    private static void performRegularArmAttack(ServerLevel level, ServerPlayer player) {
        AABB area = player.getBoundingBox().inflate(MELEE_ATTACK_RADIUS);

        Set<UUID> alreadyHit = new HashSet<>();

        List<Monster> monsters = level.getEntitiesOfClass(Monster.class, area, monster -> monster.isAlive() && !player.isAlliedTo(monster));

        for (Monster monster : monsters) {
            if (player.distanceToSqr(monster) > MELEE_ATTACK_RADIUS * MELEE_ATTACK_RADIUS) {
                continue;
            }

            if (damageWithLivingArms(level, player, monster)) {
                alreadyHit.add(monster.getUUID());
            }
        }

        CompoundTag data = player.getPersistentData();

        if (!data.hasUUID(DATA_ATTACK_MANUAL_TARGET)) {
            return;
        }

        Entity rawTarget = level.getEntity(data.getUUID(DATA_ATTACK_MANUAL_TARGET));

        if (!(rawTarget instanceof LivingEntity target) || alreadyHit.contains(target.getUUID()) || !isValidManualTarget(player, target) || player.distanceToSqr(target) > MELEE_ATTACK_RADIUS * MELEE_ATTACK_RADIUS) {
            return;
        }

        damageWithLivingArms(level, player, target);
    }

    private static void performStrongArmAttack(ServerLevel level, ServerPlayer player) {
        AABB area = player.getBoundingBox().inflate(STRONG_ATTACK_RADIUS);

        List<LivingEntity> entities = level.getEntitiesOfClass(LivingEntity.class, area, entity -> canStrongAttackHit(player, entity));

        for (LivingEntity entity : entities) {
            if (player.distanceToSqr(entity) > STRONG_ATTACK_RADIUS * STRONG_ATTACK_RADIUS) {
                continue;
            }

            damageWithLivingArms(level, player, entity);
            Vec3 movement = entity.getDeltaMovement();
            double requiredPush = Math.max(0.0D, STRONG_ATTACK_VERTICAL_SPEED - movement.y);

            if (requiredPush > 0.0D) {
                entity.push(0.0D, requiredPush, 0.0D);
            }
        }
    }

    private static boolean damageWithLivingArms(ServerLevel level, ServerPlayer player, LivingEntity target) {
        return target.hurt(level.damageSources().playerAttack(player), ARM_ATTACK_DAMAGE);
    }

    private static Monster findNearestAutomaticMonster(ServerLevel level, ServerPlayer player) {
        AABB area = player.getBoundingBox().inflate(MELEE_ATTACK_RADIUS);

        Monster nearest = null;
        double nearestDistance = Double.MAX_VALUE;

        for (Monster monster : level.getEntitiesOfClass(Monster.class, area, candidate -> candidate.isAlive() && !player.isAlliedTo(candidate))) {

            double distance = player.distanceToSqr(monster);

            if (distance > MELEE_ATTACK_RADIUS * MELEE_ATTACK_RADIUS) {
                continue;
            }

            if (distance < nearestDistance) {
                nearest = monster;
                nearestDistance = distance;
            }
        }

        return nearest;
    }

    private static boolean isValidManualTarget(ServerPlayer wearer, LivingEntity target) {
        if (target == wearer || !target.isAlive()) {
            return false;
        }

        if (target instanceof Player player) {
            return !player.isCreative() && !player.isSpectator();
        }

        return true;
    }

    private static boolean canStrongAttackHit(ServerPlayer wearer, LivingEntity entity) {
        if (entity == wearer || !entity.isAlive()) {
            return false;
        }

        if (entity instanceof Player player) {
            return !player.isCreative() && !player.isSpectator();
        }

        return true;
    }

    public static void cancelActiveArmAttack(ServerPlayer player) {
        clearActiveArmAttackData(player.getPersistentData());
    }

    private static void clearActiveArmAttackData(CompoundTag data) {
        data.putInt(DATA_ATTACK_TYPE, ATTACK_NONE);
        data.putLong(DATA_ATTACK_DAMAGE_AT, 0L);
        data.putLong(DATA_ATTACK_END_AT, 0L);
        data.putBoolean(DATA_ATTACK_DAMAGE_DONE, false);
        data.remove(DATA_ATTACK_MANUAL_TARGET);
    }

    private static boolean isDefenseActive(ServerPlayer player, long gameTime) {
        return player.getPersistentData().getLong(DATA_DEFENSE_UNTIL) > gameTime;
    }

    private static boolean isCooldownActive(ServerPlayer player, long gameTime) {
        return player.getPersistentData().getLong(DATA_COOLDOWN_UNTIL) > gameTime;
    }

    private static void refreshExpiredCooldown(ServerPlayer player, long gameTime) {
        CompoundTag data = player.getPersistentData();
        long cooldownUntil = data.getLong(DATA_COOLDOWN_UNTIL);

        if (cooldownUntil <= 0L || gameTime < cooldownUntil) {
            return;
        }

        data.putLong(DATA_COOLDOWN_UNTIL, 0L);
        data.putLong(DATA_DEFENSE_UNTIL, 0L);
        data.putInt(DATA_DEFENSE_USES, 0);
        data.putInt(DATA_HIT_COUNT, 0);
    }

    private static boolean isCombatHit(ServerPlayer player, DamageSource source) {
        Entity attacker = source.getEntity();

        return attacker instanceof LivingEntity && attacker != player;
    }

    private static int getHitCount(ServerPlayer player) {
        return player.getPersistentData().getInt(DATA_HIT_COUNT);
    }

    private static void setHitCount(ServerPlayer player, int value) {
        player.getPersistentData().putInt(DATA_HIT_COUNT, Math.max(0, value));
    }

    private static void playShieldBlockSound(ServerLevel level, ServerPlayer player) {
        level.playSound(null, player.getX(), player.getY() + 1.0D, player.getZ(), SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 1.15F, 0.92F + level.random.nextFloat() * 0.12F);
    }

    private static void playProjectileRicochetSound(ServerLevel level, ServerPlayer player) {
        double x = player.getX();
        double y = player.getY() + 1.0D;
        double z = player.getZ();

        float brightPitch = 1.72F + level.random.nextFloat() * 0.14F;
        float highPitch = 1.88F + level.random.nextFloat() * 0.10F;

        level.playSound(null, x, y, z, SoundEvents.ANVIL_PLACE, SoundSource.MASTER, 2.10F, brightPitch);
        level.playSound(null, x, y, z, SoundEvents.ANVIL_HIT, SoundSource.MASTER, 1.35F, highPitch);
    }
}
