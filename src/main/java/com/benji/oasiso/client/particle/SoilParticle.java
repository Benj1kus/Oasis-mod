package com.benji.oasiso.client.particle;

import com.benji.oasiso.Oasiso;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Oasiso.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class SoilParticle extends TextureSheetParticle {
    private static final double RANGE_SQUARED = 15 * 15;
    private static final int SPAWN_INTERVAL = 4;
    private static java.lang.ref.WeakReference<ClientLevel> spawnWorld = new java.lang.ref.WeakReference<>(null);
    private static long nextSpawn;
    private final SpriteSet sprites;
    private final double riseSpeed, driftX, driftZ;

    private SoilParticle(ClientLevel level, double x, double y, double z, SpriteSet sprites) {
        super(level, x, y, z);
        this.sprites = sprites;
        lifetime = 40 + random.nextInt(21);
        quadSize = .09F + random.nextFloat() * .05F;
        riseSpeed = .012 + random.nextDouble() * .008;
        driftX = (random.nextDouble() - .5) * .003;
        driftZ = (random.nextDouble() - .5) * .003;
        gravity = 0;
        hasPhysics = true;
        alpha = 0;
        setSprite(sprites.get(0, 5));
    }

    @Override
    public void tick() {
        xo = x;
        yo = y;
        zo = z;
        if (++age >= lifetime) {
            remove();
            return;
        }
        double t = age / (double) lifetime;
        move(driftX, riseSpeed * (1 - .25 * t), driftZ);
        setSprite(sprites.get(Math.min(5, age * 6 / lifetime), 5));
        float fadeIn = Math.min(1F, age / 5F);
        float fadeOut = (float) Math.max(0, Math.min(1, ((5.0 / 6.0) - t) / .30));
        alpha = .85F * fadeIn * fadeOut;
    }

    @Override
    protected int getLightColor(float partialTick) {
        return 0xF000F0;
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    public static void trySpawn(Level level, BlockPos pos, RandomSource random) {
        Minecraft mc = Minecraft.getInstance();
        if (!(level instanceof ClientLevel client) || client != mc.level || mc.player == null || mc.isPaused()) return;
        long now = client.getGameTime();
        if (spawnWorld.get() != client) {
            spawnWorld = new java.lang.ref.WeakReference<>(client);
            nextSpawn = now;
        }
        if (nextSpawn > now + SPAWN_INTERVAL) nextSpawn = now;
        if (now < nextSpawn || random.nextInt(8) != 0) return;

        double x = pos.getX() + .15 + random.nextDouble() * .70;
        double y = pos.getY() + 1.02;
        double z = pos.getZ() + .15 + random.nextDouble() * .70;
        if (mc.player.distanceToSqr(x, y, z) > RANGE_SQUARED) return;
        BlockPos above = pos.above();
        if (!client.hasChunkAt(above) || !client.getFluidState(above).isEmpty() || !client.getBlockState(above).getCollisionShape(client, above).isEmpty())
            return;

        nextSpawn = now + SPAWN_INTERVAL;
        client.addParticle(Oasiso.SOIL_PARTICLE.get(), x, y, z, 0, 0, 0);
    }

    @SubscribeEvent
    public static void register(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(Oasiso.SOIL_PARTICLE.get(), Provider::new);
    }

    public static final class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z, double vx, double vy, double vz) {
            return new SoilParticle(level, x, y, z, sprites);
        }
    }
}
