package com.benji.oasiso.client.fluwirussort;

import com.benji.oasiso.Oasiso;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.math.Axis;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;

@Mod.EventBusSubscriber(modid = "oasiso", bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class SortStars {
    private record Star(double born, float x, float y, float vx, float vy, float size, float angle, float spin,
                        int life, int phase) {
    }

    private static final ArrayList<Star> stars = new ArrayList<>();
    private static final RandomSource random = RandomSource.create();
    private static SpriteSet frames;
    private static AbstractContainerMenu owner;
    private static double next;

    @SubscribeEvent
    public static void register(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(Oasiso.SORT_STAR.get(), sprites -> {
            frames = sprites;
            return (type, level, x, y, z, vx, vy, vz) -> new WorldStar(level, x, y, z, vx, vy, vz, sprites);
        });
    }

    static void clear() {
        stars.clear();
        owner = null;
        next = 0;
    }

    static void begin() {
        next = 0;
    }

    static void emit(AbstractContainerMenu menu, double now, float x, float y) {
        if (owner != menu) {
            clear();
            owner = menu;
        }
        if (now < next) return;
        next = now + 1;
        for (int i = 0; i < 2; i++) {
            float a = random.nextFloat() * 2 * (float) Math.PI;
            float dx = Mth.cos(a), dy = Mth.sin(a), speed = .55F + random.nextFloat() * 1.05F;
            stars.add(new Star(now, x + 3 * dx, y + 3 * dy, dx * speed, dy * speed - .18F, 3 + random.nextFloat() * 5, random.nextFloat() * 60 - 30, random.nextFloat() * 5 - 2.5F, 16 + random.nextInt(9), random.nextInt(7)));
        }
    }

    static void draw(GuiGraphics g, AbstractContainerMenu menu, double now) {
        if (owner != menu || frames == null) return;
        stars.removeIf(s -> now - s.born >= s.life);
        if (stars.isEmpty()) {
            owner = null;
            return;
        }
        g.flush();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        var pose = g.pose();
        for (Star s : stars) {
            float age = Math.max(0, (float) (now - s.born)), t = age / s.life;
            float drift = age * (1 - .35F * t);
            var sprite = frames.get(((int) (age / 1.5F) + s.phase) % 7, 6);
            float size = s.size * (1 - .3F * t);
            pose.pushPose();
            pose.translate(s.x + s.vx * drift, s.y + s.vy * drift, 650);
            pose.mulPose(Axis.ZP.rotationDegrees(s.angle + s.spin * age));
            pose.scale(size / 8, size / 8, 1);
            RenderSystem.setShaderColor(1, 1, 1, 1 - t * t);
            g.blit(-4, -4, 0, 8, 8, sprite);
            pose.popPose();
        }
        RenderSystem.setShaderColor(1, 1, 1, 1);
    }

    private static final class WorldStar extends TextureSheetParticle {
        private final SpriteSet sprites;
        private final float spin;

        WorldStar(ClientLevel level, double x, double y, double z, double vx, double vy, double vz, SpriteSet sprites) {
            super(level, x, y, z);
            this.sprites = sprites;
            xd = vx;
            yd = vy;
            zd = vz;
            friction = .95F;
            hasPhysics = false;
            quadSize = .035F + random.nextFloat() * .065F;
            lifetime = 16 + random.nextInt(9);
            spin = (random.nextFloat() - .5F) * .09F;
            setSprite(sprites.get(0, 6));
        }

        @Override
        public void tick() {
            super.tick();
            oRoll = roll;
            roll += spin;
            alpha = Math.max(0, 1 - age / (float) lifetime);
            setSprite(sprites.get((age * 2 / 3) % 7, 6));
        }

        @Override
        public ParticleRenderType getRenderType() {
            return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
        }
    }
}
