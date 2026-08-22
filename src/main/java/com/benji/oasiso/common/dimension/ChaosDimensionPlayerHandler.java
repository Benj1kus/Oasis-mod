package com.benji.oasiso.common.dimension;

import com.benji.oasiso.Oasiso;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Oasiso.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ChaosDimensionPlayerHandler {


    private static final double LOW_GRAVITY_COMPENSATION = 0.01D;
    private static final double MAX_FALL_SPEED = -3.4D;
    private static final float FALL_DAMAGE_MULTIPLIER = 0.85F;
    private static final String FLOATING_MARKER = Oasiso.MODID + ":chaos_floating";
    private static final double SAFE_HEIGHT = 80.0D;

    private ChaosDimensionPlayerHandler() {
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        Player player = event.player;

        boolean insideChaos = player.level().dimension().equals(Oasiso.CHAOS_DIMENSION);

        if (!insideChaos) {
            restoreGravity(player);
            return;
        }


        restoreGravity(player);

        if (canApplyLowGravity(player)) {
            Vec3 movement = player.getDeltaMovement();

            double newVerticalMovement = movement.y + LOW_GRAVITY_COMPENSATION;

            newVerticalMovement = Math.max(newVerticalMovement, MAX_FALL_SPEED);

            player.setDeltaMovement(movement.x, newVerticalMovement, movement.z);

            if (!player.level().isClientSide) {
                player.hurtMarked = true;
            }
        }


        if (!player.level().isClientSide && player.getY() < player.level().getMinBuildHeight() + 8 && player instanceof ServerPlayer serverPlayer && player.level() instanceof ServerLevel serverLevel) {

            serverPlayer.teleportTo(serverLevel, serverPlayer.getX(), SAFE_HEIGHT, serverPlayer.getZ(), serverPlayer.getYRot(), serverPlayer.getXRot());
            serverPlayer.setDeltaMovement(Vec3.ZERO);

            serverPlayer.fallDistance = 0.0F;
        }
    }

    private static boolean canApplyLowGravity(Player player) {
        return !player.onGround() && !player.getAbilities().flying && !player.isFallFlying() && !player.isInWaterOrBubble() && !player.isInLava() && !player.onClimbable();
    }

    private static void restoreGravity(Player player) {
        if (!player.getPersistentData().getBoolean(FLOATING_MARKER)) {
            return;
        }

        player.setNoGravity(false);

        player.getPersistentData().remove(FLOATING_MARKER);
    }

    @SubscribeEvent
    public static void onLivingFall(LivingFallEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        if (!player.level().dimension().equals(Oasiso.CHAOS_DIMENSION)) {
            return;
        }

        event.setDistance(event.getDistance() * FALL_DAMAGE_MULTIPLIER);
    }


    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        if (!player.level().dimension().equals(Oasiso.CHAOS_DIMENSION)) {
            return;
        }

        if (event.getSource().is(DamageTypes.FELL_OUT_OF_WORLD)) {
            event.setCanceled(true);
        }
    }
}