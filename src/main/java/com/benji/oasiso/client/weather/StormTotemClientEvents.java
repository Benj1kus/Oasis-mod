package com.benji.oasiso.client.weather;

import com.benji.oasiso.Oasiso;
import com.benji.oasiso.common.block.entity.StormTotemBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.material.FogType;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Vector3f;

import java.util.List;

@Mod.EventBusSubscriber(modid = Oasiso.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class StormTotemClientEvents {

    private static StormTotemWindSound windSound;

    private static final DustParticleOptions SAND_DUST = new DustParticleOptions(new Vector3f(0.82F, 0.63F, 0.30F), 1.0F);
    private static final DustParticleOptions GOLD_DUST = new DustParticleOptions(new Vector3f(1.0F, 0.76F, 0.20F), 0.85F);

    private static final RandomSource RANDOM = RandomSource.create();

    private static final float FULL_FOG_END = 3.75F;
    private static final float FULL_FOG_START = 0.35F;

    private StormTotemClientEvents() {
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();

        if (minecraft.level == null || minecraft.player == null) {
            stopWindSound();
            StormTotemBlockEntity.clearClientSources();
            return;
        }

        if (minecraft.isPaused()) {
            return;
        }

        ClientLevel level = minecraft.level;

        StormTotemBlockEntity.tickClientSources(level.dimension(), level.getGameTime());
        updateWindSound(minecraft);
        spawnStormParticles(level, minecraft.player);
    }

    private static void updateWindSound(Minecraft minecraft) {
        if (minecraft.level == null || minecraft.player == null) {
            stopWindSound();
            return;
        }

        if (windSound != null && windSound.isStopped()) {
            windSound = null;
        }

        float strength = StormTotemBlockEntity.getClientStormStrength(minecraft.level.dimension(),
                minecraft.player.getEyePosition());

        if (strength <= 0.001F) {
            stopWindSound();
            return;
        }

        if (windSound != null) {
            return;
        }

        windSound = new StormTotemWindSound();
        minecraft.getSoundManager().play(windSound);
    }

    private static void stopWindSound() {
        if (windSound == null) {
            return;
        }

        windSound.requestStop();
        windSound = null;
    }

    private static void spawnStormParticles(ClientLevel level, LocalPlayer player) {
        List<StormTotemBlockEntity.ClientStormSource> sources = StormTotemBlockEntity.getClientSources(level.dimension());


        for (StormTotemBlockEntity.ClientStormSource source : sources) {
            float intensity = source.intensity();

            if (intensity <= 0.03F) {

                continue;
            }


            BlockPos pos = source.pos();

            double centerX = pos.getX() + 0.5D;
            double centerY = pos.getY() + 0.9D;
            double centerZ = pos.getZ() + 0.5D;

            if (player.distanceToSqr(centerX, centerY, centerZ) > 34.0D * 34.0D) {

                continue;
            }


            int count = 2 + Mth.floor(intensity * 5.0F);

            double time = level.getGameTime() / 20.0D;

            for (int i = 0; i < count; i++) {

                double radius = 2.0D + RANDOM.nextDouble() * 12.5D;
                double angle = RANDOM.nextDouble() * Math.PI * 2.0D;

                double y = centerY + RANDOM.nextDouble() * 4.8D;
                double x = centerX + Math.cos(angle) * radius;
                double z = centerZ + Math.sin(angle) * radius;


                double speed = 0.11D + intensity * 0.17D;

                double velocityX = -Math.sin(angle) * speed;
                double velocityZ = Math.cos(angle) * speed;

                velocityX += (RANDOM.nextDouble() - 0.5D) * 0.055D;
                velocityZ += (RANDOM.nextDouble() - 0.5D) * 0.055D;


                double velocityY = Math.sin(time * 4.0D + angle) * 0.018D + (RANDOM.nextDouble() - 0.5D) * 0.025D;

                DustParticleOptions particle = RANDOM.nextFloat() < 0.11F ? GOLD_DUST : SAND_DUST;

                level.addParticle(particle, x, y, z, velocityX, velocityY, velocityZ);
            }
        }
    }


    @SubscribeEvent
    public static void onRenderFog(ViewportEvent.RenderFog event) {
        Minecraft minecraft = Minecraft.getInstance();


        if (minecraft.level == null || minecraft.player == null) {
            return;
        }

        if (event.getType() != FogType.NONE) {
            return;
        }

        float strength = StormTotemBlockEntity.getClientStormStrength(minecraft.level.dimension(),
                event.getCamera().getPosition());

        if (strength <= 0.001F) {
            return;
        }


        float near = Mth.lerp(strength, event.getNearPlaneDistance(), FULL_FOG_START);
        float far = Mth.lerp(strength, event.getFarPlaneDistance(), FULL_FOG_END);


        event.setNearPlaneDistance(near);
        event.setFarPlaneDistance(far);
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onFogColor(ViewportEvent.ComputeFogColor event) {
        Minecraft minecraft = Minecraft.getInstance();

        if (minecraft.level == null || minecraft.player == null) {
            return;
        }


        float strength = StormTotemBlockEntity.getClientStormStrength(minecraft.level.dimension(),

                event.getCamera().getPosition());

        if (strength <= 0.001F) {
            return;
        }


        float stormRed = 0.66F;
        float stormGreen = 0.49F;
        float stormBlue = 0.25F;


        event.setRed(Mth.lerp(strength, event.getRed(), stormRed));
        event.setGreen(Mth.lerp(strength, event.getGreen(), stormGreen));
        event.setBlue(Mth.lerp(strength, event.getBlue(), stormBlue));
    }
}