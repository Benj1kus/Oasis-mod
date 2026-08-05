package com.benji.oasiso.client.dimension;

import com.benji.oasiso.Oasiso;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(
        modid = Oasiso.MODID,
        bus = Mod.EventBusSubscriber.Bus.FORGE,
        value = Dist.CLIENT
)
public final class ChaosDimensionClientEvents {

    private static final RandomSource RANDOM =
            RandomSource.create();

    private static int dimensionTicks;

    private ChaosDimensionClientEvents() {
    }

    public static int getDimensionTicks() {
        return dimensionTicks;
    }

    public static int getMoonStage() {
        if (dimensionTicks < 200) {
            return 1;
        }

        return Math.min(
                6,
                2 + (dimensionTicks - 200) / 100
        );
    }

    @SubscribeEvent
    public static void onClientTick(
            TickEvent.ClientTickEvent event
    ) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        Minecraft minecraft =
                Minecraft.getInstance();

        if (minecraft.isPaused()) {
            return;
        }

        if (!isInsideChaosDimension(minecraft)) {
            dimensionTicks = 0;
            return;
        }

        dimensionTicks++;

        spawnAtmosphere(
                minecraft.level,
                minecraft.player
        );
    }


    private static boolean isInsideChaosDimension(
            Minecraft minecraft
    ) {
        return minecraft.level != null
                && minecraft.player != null
                && minecraft.level.dimension()
                .equals(Oasiso.CHAOS_DIMENSION);
    }

    private static void spawnAtmosphere(
            ClientLevel level,
            Player player
    ) {
        for (int i = 0; i < 2; i++) {
            double angle =
                    RANDOM.nextDouble()
                            * Math.PI
                            * 2.0D;

            double distance =
                    3.0D
                            + RANDOM.nextDouble()
                            * 12.0D;

            double x =
                    player.getX()
                            + Math.cos(angle) * distance;

            double y =
                    player.getY()
                            - 4.0D
                            + RANDOM.nextDouble() * 10.0D;

            double z =
                    player.getZ()
                            + Math.sin(angle) * distance;

            level.addParticle(
                    ParticleTypes.ASH,
                    x,
                    y,
                    z,
                    (RANDOM.nextDouble() - 0.5D)
                            * 0.006D,
                    0.002D
                            + RANDOM.nextDouble()
                            * 0.004D,
                    (RANDOM.nextDouble() - 0.5D)
                            * 0.006D
            );
        }

        if (RANDOM.nextInt(8) != 0) {
            return;
        }

        double angle =
                RANDOM.nextDouble()
                        * Math.PI
                        * 2.0D;

        double distance =
                2.0D
                        + RANDOM.nextDouble()
                        * 9.0D;

        double x =
                player.getX()
                        + Math.cos(angle) * distance;

        double y =
                player.getY()
                        - 3.0D
                        + RANDOM.nextDouble() * 8.0D;

        double z =
                player.getZ()
                        + Math.sin(angle) * distance;

        level.addParticle(
                Oasiso.PURPLE_STARS.get(),
                x,
                y,
                z,
                (RANDOM.nextDouble() - 0.5D)
                        * 0.015D,
                (RANDOM.nextDouble() - 0.5D)
                        * 0.01D,
                (RANDOM.nextDouble() - 0.5D)
                        * 0.015D
        );
    }
}