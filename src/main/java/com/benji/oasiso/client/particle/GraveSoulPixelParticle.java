package com.benji.oasiso.client.particle;

import com.benji.oasiso.Oasiso;
import com.benji.oasiso.common.gravesoul.GraveSoulParticles;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

@Mod.EventBusSubscriber(modid = Oasiso.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class GraveSoulPixelParticle extends TextureSheetParticle {
    private static SpriteSet captureSprites;
    private UUID capturePlayer;
    private Vec3 captureStart, captureControl, captureEnd;

    public static void spawnCapture(ClientLevel level, double x, double y, double z, UUID playerId) {
        if (captureSprites == null) return;
        Player player = level.getPlayerByUUID(playerId);
        if (player == null) return;
        GraveSoulPixelParticle particle = new GraveSoulPixelParticle(level, x, y, z, 0, 0, 0, captureSprites);
        particle.capturePlayer = playerId;
        particle.captureStart = new Vec3(x, y, z);
        particle.captureControl = particle.captureStart.add((particle.random.nextDouble() - .5) * .35, .15 + particle.random.nextDouble() * .25, (particle.random.nextDouble() - .5) * .35);
        particle.captureEnd = targetPoint(player);
        particle.lifetime = 20 + particle.random.nextInt(9);
        Minecraft.getInstance().particleEngine.add(particle);
    }

    private static Vec3 targetPoint(Player player) {
        double yaw = Math.toRadians(player.getYRot());
        return new Vec3(player.getX() - Math.sin(yaw) * .25, player.getY() + player.getBbHeight() * .55, player.getZ() + Math.cos(yaw) * .25);
    }

    private GraveSoulPixelParticle(ClientLevel level, double x, double y, double z, double dx, double dy, double dz, SpriteSet sprites) {
        super(level, x, y, z);
        pickSprite(sprites);
        xd = dx;
        yd = dy;
        zd = dz;
        lifetime = 26 + random.nextInt(20);
        quadSize = .018F + random.nextFloat() * .022F;
        gravity = 0;
        hasPhysics = false;
        friction = .98F;
        if (random.nextBoolean()) setColor(1, 1, 1);
        else setColor(95F / 255F, 1, 204F / 255F);
    }

    @Override
    public void tick() {
        if (capturePlayer != null) {
            xo = x;
            yo = y;
            zo = z;
            Player player = level.getPlayerByUUID(capturePlayer);
            if (player != null && player.isAlive()) captureEnd = targetPoint(player);
            float t = Math.min(1F, ++age / (float) lifetime);
            double u = t * t, v = 1 - u;
            Vec3 point = captureStart.scale(v * v).add(captureControl.scale(2 * v * u)).add(captureEnd.scale(u * u));
            setPos(point.x, point.y, point.z);
            float fade = Math.max(0F, (t - .75F) / .25F);
            alpha = 1F - fade * fade * (3F - 2F * fade);
            if (age >= lifetime) remove();
            return;
        }
        super.tick();
        yd += .0015;
        alpha = Math.min(1F, Math.max(0F, (lifetime - age) / 12F));
    }

    @Override
    public int getLightColor(float partial) {
        return 0xF000F0;
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    @SubscribeEvent
    public static void register(RegisterParticleProvidersEvent e) {
        e.registerSpriteSet(GraveSoulParticles.PIXEL, sprites -> {
            captureSprites = sprites;
            return new Provider(sprites);
        });
    }

    private record Provider(SpriteSet sprites) implements ParticleProvider<SimpleParticleType> {
        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z, double dx, double dy, double dz) {
            return new GraveSoulPixelParticle(level, x, y, z, dx, dy, dz, sprites);
        }
    }
}
