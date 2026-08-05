package com.benji.oasiso.common.dimension;

import com.benji.oasiso.Oasiso;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(
        modid = Oasiso.MODID,
        bus = Mod.EventBusSubscriber.Bus.FORGE
)
public final class ChaosDimensionPlayerHandler {

    private static final String FLOATING_MARKER =
            Oasiso.MODID + ":chaos_floating";

    private static final double SAFE_HEIGHT = 80.0D;

    private ChaosDimensionPlayerHandler() {
    }

    @SubscribeEvent
    public static void onPlayerTick(
            TickEvent.PlayerTickEvent event
    ) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        Player player = event.player;

        if (player.level().isClientSide) {
            return;
        }

        boolean insideChaos =
                player.level().dimension()
                        .equals(Oasiso.CHAOS_DIMENSION);

        if (!insideChaos) {
            restoreGravity(player);
            return;
        }

        player.getPersistentData()
                .putBoolean(FLOATING_MARKER, true);


        player.setNoGravity(true);
        player.fallDistance = 0.0F;

        Vec3 movement =
                player.getDeltaMovement();


        double bob =
                Math.sin(
                        (player.tickCount
                                + player.getId() * 7)
                                * 0.07D
                ) * 0.0025D;

        double verticalMovement =
                Mth.clamp(
                        movement.y * 0.82D + bob,
                        -0.075D,
                        0.075D
                );

        player.setDeltaMovement(
                movement.x,
                verticalMovement,
                movement.z
        );

        player.hurtMarked = true;


        if (player.getY()
                < player.level().getMinBuildHeight() + 8
                && player instanceof ServerPlayer serverPlayer
                && player.level()
                instanceof ServerLevel serverLevel) {

            serverPlayer.teleportTo(
                    serverLevel,
                    serverPlayer.getX(),
                    SAFE_HEIGHT,
                    serverPlayer.getZ(),
                    serverPlayer.getYRot(),
                    serverPlayer.getXRot()
            );

            serverPlayer.setDeltaMovement(Vec3.ZERO);
        }
    }

    private static void restoreGravity(
            Player player
    ) {
        if (!player.getPersistentData()
                .getBoolean(FLOATING_MARKER)) {
            return;
        }

        player.setNoGravity(false);

        player.getPersistentData()
                .remove(FLOATING_MARKER);
    }


    @SubscribeEvent
    public static void onLivingHurt(
            LivingHurtEvent event
    ) {
        if (!(event.getEntity()
                instanceof Player player)) {
            return;
        }

        if (!player.level().dimension()
                .equals(Oasiso.CHAOS_DIMENSION)) {
            return;
        }

        if (event.getSource().is(
                DamageTypes.FELL_OUT_OF_WORLD
        )) {
            event.setCanceled(true);
        }
    }
}