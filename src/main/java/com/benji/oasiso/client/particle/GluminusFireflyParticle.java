package com.benji.oasiso.client.particle;

import com.benji.oasiso.Oasiso;
import com.benji.oasiso.common.block.GlumBlock;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.core.BlockPos;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Oasiso.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class GluminusFireflyParticle extends TextureSheetParticle {
    private final BlockPos flower;
    private final double baseY, radius, speed, phase, bobPhase;

    private GluminusFireflyParticle(ClientLevel level, double x, double y, double z, SpriteSet sprites) {
        super(level, x, y, z);
        flower = BlockPos.containing(x, y, z);
        baseY = y + .20 + random.nextDouble() * .42;
        radius = .15 + random.nextDouble() * .35;
        speed = (.028 + random.nextDouble() * .035) * (random.nextBoolean() ? 1 : -1);
        phase = random.nextDouble() * Math.PI * 2;
        bobPhase = random.nextDouble() * Math.PI * 2;
        lifetime = 90 + random.nextInt(51);
        quadSize = .014F + random.nextFloat() * .026F;
        setColor(.82F + .18F * random.nextFloat(), 1F, .52F + .25F * random.nextFloat());
        pickSprite(sprites);
        alpha = 0;
        updatePosition();
        xo = this.x;
        yo = this.y;
        zo = this.z;
    }

    private void updatePosition() {
        double angle = phase + speed * age;
        double orbit = radius + .045 * Math.sin(bobPhase + age * .046);
        setPos(flower.getX() + .5 + Math.cos(angle) * orbit, baseY + .105 * Math.sin(bobPhase + age * .069), flower.getZ() + .5 + Math.sin(angle) * orbit);
    }

    @Override
    public void tick() {
        xo = x;
        yo = y;
        zo = z;
        if (++age >= lifetime || (age % 10 == 0 && !(level.getBlockState(flower).getBlock() instanceof GlumBlock))) {
            remove();
            return;
        }
        updatePosition();
        float fade = Math.min(1F, Math.min(age, lifetime - age) / 12F);
        alpha = fade * (.72F + .28F * (float) Math.sin(bobPhase + age * .095));
    }

    @Override
    public int getLightColor(float partialTick) {
        return 0xF000F0;
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    @SubscribeEvent
    public static void register(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(Oasiso.GLUMINUS_FIREFLY.get(), sprites -> (type, level, x, y, z, vx, vy, vz) -> new GluminusFireflyParticle(level, x, y, z, sprites));
    }
}
