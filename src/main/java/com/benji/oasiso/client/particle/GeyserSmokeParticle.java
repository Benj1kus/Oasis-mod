package com.benji.oasiso.client.particle;

import com.benji.oasiso.Oasiso;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

@Mod.EventBusSubscriber(modid = Oasiso.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class GeyserSmokeParticle extends TextureSheetParticle {
    private final SpriteSet sprites;
    private final float baseSize;

    private GeyserSmokeParticle(ClientLevel level, double x, double y, double z, SpriteSet sprites) {
        super(level, x, y, z);
        this.sprites = sprites;
        lifetime = 48 + random.nextInt(25);
        baseSize = (0.10F + random.nextFloat() * 0.09F) * 2.0F;
        quadSize = baseSize;
        double angle = random.nextDouble() * Math.PI * 2;
        double speed = .028 + random.nextDouble() * .025;
        xd = Math.cos(angle) * speed;
        zd = Math.sin(angle) * speed;
        yd = .115 + random.nextDouble() * .02;
        setSize(0.16F, 0.16F);
        alpha = 0;
        setSprite(sprites.get(0, 2));
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
        float t = age / (float) lifetime;
        yd = Math.max(-.12, yd - .016);
        move(xd, yd, zd);
        xd *= .991;
        zd *= .991;
        quadSize = baseSize * (1 + 1.7F * t);
        setSprite(sprites.get(Math.min(2, age * 3 / lifetime), 2));
        alpha = .65F * Math.min(1F, age / 5F) * (float) Math.pow(1 - t, 1.25);
        if (!level.hasChunkAt(net.minecraft.core.BlockPos.containing(x, y, z))) remove();
    }

    @Override
    public void move(double dx, double dy, double dz) {
        Vec3 movement = Entity.collideBoundingBox(null, new Vec3(dx, dy, dz), getBoundingBox(), level, List.of());
        setBoundingBox(getBoundingBox().move(movement));
        setLocationFromBoundingbox();
        onGround = dy < 0 && Math.abs(movement.y - dy) > 1.0e-7;
        if (Math.abs(movement.x - dx) > 1.0e-7) xd = 0;
        if (Math.abs(movement.z - dz) > 1.0e-7) zd = 0;
        if (Math.abs(movement.y - dy) > 1.0e-7) yd = 0;
    }

    @Override
    protected int getLightColor(float partialTick) {
        return 0xF000F0;
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    @SubscribeEvent
    public static void register(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(Oasiso.GEYSER_SMOKE.get(), Provider::new);
    }

    public static final class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z, double vx, double vy, double vz) {
            return new GeyserSmokeParticle(level, x, y, z, sprites);
        }
    }
}
