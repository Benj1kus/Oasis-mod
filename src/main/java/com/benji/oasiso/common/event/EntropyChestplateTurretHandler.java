package com.benji.oasiso.common.event;

import com.benji.oasiso.Oasiso;
import com.benji.oasiso.common.item.EntropyChestplateItem;
import com.benji.oasiso.common.util.EntropyTurretHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Arrow;
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

        LivingEntity leftTarget = EntropyTurretHelper.findTarget(player, EntropyTurretHelper.Side.LEFT);
        LivingEntity rightTarget = EntropyTurretHelper.findTarget(player, EntropyTurretHelper.Side.RIGHT);

        int fireMask = 0;
        if (leftTarget != null) {
            fireMask |= EntropyChestplateItem.FIRE_LEFT_MASK;
        }
        if (rightTarget != null) {
            fireMask |= EntropyChestplateItem.FIRE_RIGHT_MASK;
        }

        if (fireMask == 0) {
            return;
        }

        player.getPersistentData().putLong(NEXT_SHOT_TICK, gameTime + FIRE_COOLDOWN_TICKS);

        if (player.level().isClientSide) {
            player.getPersistentData().putInt(EntropyChestplateItem.CLIENT_FIRE_MASK, fireMask);
            player.getPersistentData().putLong(EntropyChestplateItem.CLIENT_FIRE_UNTIL, gameTime + EntropyChestplateItem.FIRE_ANIMATION_TICKS);
            return;
        }

        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }

        if (leftTarget != null) {
            fire(level, player, leftTarget, EntropyTurretHelper.Side.LEFT);
        }

        if (rightTarget != null) {
            fire(level, player, rightTarget, EntropyTurretHelper.Side.RIGHT);
        }
    }

    private static void fire(ServerLevel level, Player player, LivingEntity target, EntropyTurretHelper.Side side) {
        Vec3 muzzle = EntropyTurretHelper.getMuzzlePosition(player, side);
        Vec3 direction = EntropyTurretHelper.getAimDirection(muzzle, target);

        Arrow arrow = new Arrow(level, player);
        arrow.setPos(muzzle.x, muzzle.y, muzzle.z);
        arrow.setBaseDamage(ARROW_DAMAGE);
        arrow.pickup = AbstractArrow.Pickup.DISALLOWED;
        arrow.shoot(direction.x, direction.y, direction.z, ARROW_SPEED, ARROW_INACCURACY);

        level.addFreshEntity(arrow);

        level.playSound(null, muzzle.x, muzzle.y, muzzle.z, SoundEvents.CROSSBOW_SHOOT, SoundSource.PLAYERS, 0.85F, side == EntropyTurretHelper.Side.LEFT ? 1.22F : 1.30F);

        spawnMuzzleParticles(level, muzzle);
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
