package com.benji.oasiso.common.event;

import com.benji.oasiso.Oasiso;
import com.benji.oasiso.common.entity.ChaosBombEntity;
import com.benji.oasiso.common.entity.projectile.CactoProjEntity;
import com.benji.oasiso.common.item.EntropyChestplateItem;
import com.benji.oasiso.common.util.EntropyAmmoStorage;
import com.benji.oasiso.common.util.EntropyTurretHelper;
import com.benji.oasiso.registry.ModEntities;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.ArrowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Oasiso.MODID)
public final class EntropyChestplateTurretHandler {

    private static final String NEXT_SHOT_TICK = "EntropyTurretNextShotTick";
    private static final int FIRE_COOLDOWN_TICKS = 20; // 1 second

    private static final float ARROW_SPEED = 2.6F;
    private static final float ARROW_INACCURACY = 0.35F;
    private static final double ARROW_DAMAGE = 4.0D;

    private static final float CACTUS_SPEED = 2.75F;
    private static final float CACTUS_INACCURACY = 0.20F;

    private static final double CHAOS_BOMB_SPEED = 1.15D;

    private EntropyChestplateTurretHandler() {
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        Player player = event.player;
        ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);

        if (!(chest.getItem() instanceof EntropyChestplateItem)) {
            clearClientFireState(player);
            return;
        }

        long gameTime = player.level().getGameTime();

        if (!EntropyChestplateItem.areTurretsOperational(chest, gameTime)) {
            clearClientFireState(player);
            return;
        }

        long nextShot = player.getPersistentData().getLong(NEXT_SHOT_TICK);
        if (gameTime < nextShot) {
            return;
        }

        EntropyTurretHelper.TargetPair targets = EntropyTurretHelper.findTargets(player);

        LivingEntity leftTarget = targets.left();
        LivingEntity rightTarget = targets.right();

        int requestedMask = 0;
        if (leftTarget != null) {
            requestedMask |= EntropyChestplateItem.FIRE_LEFT_MASK;
        }
        if (rightTarget != null) {
            requestedMask |= EntropyChestplateItem.FIRE_RIGHT_MASK;
        }

        if (requestedMask == 0) {
            return;
        }

        if (player.level().isClientSide) {
            int requestedShots = Integer.bitCount(requestedMask);
            int possibleShots = EntropyAmmoStorage.countPossibleShots(chest, requestedShots);
            int predictedMask = buildAllowedMask(leftTarget, rightTarget, possibleShots);

            if (predictedMask == 0) {
                clearClientFireState(player);
                return;
            }

            player.getPersistentData().putLong(NEXT_SHOT_TICK, gameTime + FIRE_COOLDOWN_TICKS);
            player.getPersistentData().putInt(EntropyChestplateItem.CLIENT_FIRE_MASK, predictedMask);
            player.getPersistentData().putLong(EntropyChestplateItem.CLIENT_FIRE_UNTIL, gameTime + EntropyChestplateItem.FIRE_ANIMATION_TICKS);
            return;
        }

        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }

        int firedMask = 0;
        if (leftTarget != null && fireNextAmmo(level, player, chest, leftTarget, EntropyTurretHelper.Side.LEFT)) {
            firedMask |= EntropyChestplateItem.FIRE_LEFT_MASK;
        }

        if (rightTarget != null && fireNextAmmo(level, player, chest, rightTarget, EntropyTurretHelper.Side.RIGHT)) {
            firedMask |= EntropyChestplateItem.FIRE_RIGHT_MASK;
        }

        if (firedMask == 0) {
            return;
        }

        player.getPersistentData().putLong(NEXT_SHOT_TICK, gameTime + FIRE_COOLDOWN_TICKS);
        player.getInventory().setChanged();
        player.containerMenu.broadcastChanges();
    }

    private static int buildAllowedMask(LivingEntity leftTarget, LivingEntity rightTarget, int possibleShots) {
        if (possibleShots <= 0) {
            return 0;
        }

        int mask = 0;
        if (leftTarget != null) {
            mask |= EntropyChestplateItem.FIRE_LEFT_MASK;
            possibleShots--;
        }

        if (possibleShots > 0 && rightTarget != null) {
            mask |= EntropyChestplateItem.FIRE_RIGHT_MASK;
        }

        return mask;
    }

    private static boolean fireNextAmmo(ServerLevel level, Player player, ItemStack chest, LivingEntity target, EntropyTurretHelper.Side side) {
        EntropyAmmoStorage.ShotAmmo ammo = EntropyAmmoStorage.takeNextShot(chest);
        if (ammo == null) {
            return false;
        }

        Vec3 muzzle = EntropyTurretHelper.getMuzzlePosition(player, side);
        Vec3 direction = EntropyTurretHelper.getAimDirection(muzzle, target);

        switch (ammo.type()) {
            case ARROW -> fireArrow(level, player, ammo.stack(), muzzle, direction);
            case BLAZE -> fireFlamethrower(level, target, muzzle);
            case CACTUS_SPIKE -> fireCactusSpike(level, player, muzzle, direction);
            case CHAOS_BOMB -> fireChaosBomb(level, muzzle, direction);
        }

        playShotSound(level, player, muzzle, side, ammo.type());

        // Flamethrower deliberately has NO Chaos Bomb muzzle particles.
        if (ammo.type() != EntropyAmmoStorage.AmmoType.BLAZE) {
            spawnMuzzleParticles(level, muzzle);
        }

        return true;
    }

    private static void fireArrow(ServerLevel level, Player player, ItemStack ammoStack, Vec3 muzzle, Vec3 direction) {
        AbstractArrow arrow;

        if (ammoStack.getItem() instanceof ArrowItem arrowItem) {
            arrow = arrowItem.createArrow(level, ammoStack, player);
        } else {
            arrow = new Arrow(level, player);
        }
        arrow.setPos(muzzle.x, muzzle.y, muzzle.z);
        arrow.setBaseDamage(ARROW_DAMAGE);
        arrow.pickup = AbstractArrow.Pickup.DISALLOWED;
        arrow.shoot(direction.x, direction.y, direction.z, ARROW_SPEED, ARROW_INACCURACY);
        level.addFreshEntity(arrow);
    }

    private static void fireCactusSpike(ServerLevel level, Player player, Vec3 muzzle, Vec3 direction) {
        CactoProjEntity spike = new CactoProjEntity(level, player);
        spike.setPos(muzzle.x, muzzle.y, muzzle.z);
        spike.pickup = AbstractArrow.Pickup.DISALLOWED;
        spike.shoot(direction.x, direction.y, direction.z, CACTUS_SPEED, CACTUS_INACCURACY);
        level.addFreshEntity(spike);
    }

    private static void fireChaosBomb(ServerLevel level, Vec3 muzzle, Vec3 direction) {
        ChaosBombEntity bomb = ModEntities.CHAOS_BOMB.get().create(level);
        if (bomb == null) {
            return;
        }

        bomb.setPos(muzzle.x, muzzle.y, muzzle.z);
        bomb.setDeltaMovement(direction.scale(CHAOS_BOMB_SPEED));
        bomb.hasImpulse = true;
        bomb.setOnGround(false);
        level.addFreshEntity(bomb);
    }

    private static void fireFlamethrower(ServerLevel level, LivingEntity target, Vec3 muzzle) {
        Vec3 aimPoint = new Vec3(target.getX(), target.getY() + target.getBbHeight() * 0.55D, target.getZ());

        Vec3 line = aimPoint.subtract(muzzle);
        double length = line.length();
        if (length < 0.001D) {
            return;
        }

        Vec3 direction = line.scale(1.0D / length);
        int steps = Math.max(5, Math.min(18, (int) Math.ceil(length * 1.35D)));

        for (int i = 1; i <= steps; i++) {
            double distance = length * (i / (double) steps);
            Vec3 point = muzzle.add(direction.scale(distance));

            level.sendParticles(ParticleTypes.FLAME, point.x, point.y, point.z, 2, 0.045D, 0.045D, 0.045D, 0.008D);
        }
        target.setSecondsOnFire(4);
    }

    private static void playShotSound(ServerLevel level, Player player, Vec3 muzzle, EntropyTurretHelper.Side side, EntropyAmmoStorage.AmmoType ammoType) {
        float pitch = side == EntropyTurretHelper.Side.LEFT ? 1.22F : 1.30F;

        if (ammoType == EntropyAmmoStorage.AmmoType.BLAZE) {
            level.playSound(null, muzzle.x, muzzle.y, muzzle.z, SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 0.72F, 1.20F);
            return;
        }

        level.playSound(null, muzzle.x, muzzle.y, muzzle.z, SoundEvents.CROSSBOW_SHOOT, SoundSource.PLAYERS, 0.85F, pitch);
    }

    private static void spawnMuzzleParticles(ServerLevel level, Vec3 muzzle) {
        level.sendParticles(Oasiso.CHAOS_BOMB_CENTER_SMOKE.get(), muzzle.x, muzzle.y, muzzle.z, 1, 0.025D, 0.025D, 0.025D, 0.01D);

        level.sendParticles(Oasiso.CHAOS_BOMB_FIRE_SMOKE.get(), muzzle.x, muzzle.y, muzzle.z, 1, 0.03D, 0.03D, 0.03D, 0.02D);

        level.sendParticles(Oasiso.CHAOS_BOMB_SPARKS.get(), muzzle.x, muzzle.y, muzzle.z, 3, 0.04D, 0.04D, 0.04D, 0.07D);
    }

    private static void clearClientFireState(Player player) {
        if (!player.level().isClientSide) {
            return;
        }

        player.getPersistentData().putInt(EntropyChestplateItem.CLIENT_FIRE_MASK, 0);

        player.getPersistentData().putLong(EntropyChestplateItem.CLIENT_FIRE_UNTIL, 0L);
    }
}
