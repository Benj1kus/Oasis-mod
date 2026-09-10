package com.benji.oasiso.client.gui;

import com.benji.oasiso.Oasiso;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Mod.EventBusSubscriber(modid = Oasiso.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class SandScreenOverlay {

    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "textures/gui/sand_screen.png");

    private static final int TEXTURE_WIDTH = 40;
    private static final int TEXTURE_HEIGHT = 80;
    private static final float BASE_RENDER_SCALE = 2.5F;
    private static final float LIFETIME = 60.0F;
    private static final int MAX_STAINS = 8;

    private static final List<Stain> STAINS = new ArrayList<>();

    private SandScreenOverlay() {
    }

    public static void spawn() {
        ThreadLocalRandom random = ThreadLocalRandom.current();

        float x = 0.18F + random.nextFloat() * 0.64F;
        float y = 0.18F + random.nextFloat() * 0.34F;

        float rotation = -17.0F + random.nextFloat() * 34.0F;
        float scale = BASE_RENDER_SCALE * (0.90F + random.nextFloat() * 0.20F);

        float slideDistance = 38.0F + random.nextFloat() * 38.0F;
        float sideDrift = -7.0F + random.nextFloat() * 14.0F;
        float phase = random.nextFloat() * Mth.TWO_PI;

        boolean mirrored = random.nextBoolean();

        if (STAINS.size() >= MAX_STAINS) {
            STAINS.remove(0);
        }
        STAINS.add(new Stain(x, y, rotation, scale, slideDistance, sideDrift, phase, mirrored));
    }

    @SubscribeEvent
    public static void clientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {

            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.isPaused()) {

            return;
        }

        Iterator<Stain> iterator = STAINS.iterator();

        while (iterator.hasNext()) {
            Stain stain = iterator.next();
            stain.previousAge = stain.age;
            stain.age += 1.0F;

            if (stain.age >= LIFETIME) {
                iterator.remove();
            }
        }
    }

    @SubscribeEvent
    public static void render(RenderGuiEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || STAINS.isEmpty()) {
            return;
        }

        GuiGraphics graphics = event.getGuiGraphics();
        float partialTick = event.getPartialTick();
        int screenWidth = graphics.guiWidth();
        int screenHeight = graphics.guiHeight();

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        for (Stain stain : STAINS) {
            renderStain(graphics, stain, partialTick, screenWidth, screenHeight);
        }
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.disableBlend();
    }

    private static void renderStain(GuiGraphics graphics, Stain stain, float partialTick, int screenWidth, int screenHeight) {
        float age = Mth.lerp(partialTick, stain.previousAge, stain.age);
        float progress = Mth.clamp(age / LIFETIME, 0.0F, 1.0F);

        float fadeInProgress = Mth.clamp(age / 3.0F, 0.0F, 1.0F);
        float fadeIn = 0.48F + 0.52F * smoothStep(fadeInProgress);

        float fadeOutProgress = Mth.clamp((progress - 0.20F) / 0.80F, 0.0F, 1.0F);
        float fadeOut = 1.0F - smoothStep(fadeOutProgress);
        float alpha = fadeIn * fadeOut * 0.92F;

        if (alpha <= 0.005F) {
            return;
        }

        float slideProgress = smoothStep(progress);
        float slideY = stain.slideDistance * slideProgress;
        float wobble = Mth.sin(progress * 7.0F + stain.phase) * 1.8F * (1.0F - progress);
        float driftX = stain.sideDrift * slideProgress + wobble;
        float pop = age < 6.0F ? 1.0F + 0.15F * (1.0F - age / 6.0F) : 1.0F;

        float rotation = stain.rotation + Mth.sin(age * 0.45F + stain.phase) * 2.5F * (1.0F - progress);
        float scale = stain.scale * pop;

        float x = stain.xFactor * screenWidth + driftX;
        float y = stain.yFactor * screenHeight + slideY;

        float halfWidth = TEXTURE_WIDTH * scale * 0.5F;
        float halfHeight = TEXTURE_HEIGHT * scale * 0.5F;

        float radians = rotation * ((float) Math.PI / 180.0F);
        float cos = Math.abs(Mth.cos(radians));
        float sin = Math.abs(Mth.sin(radians));

        float extentX = cos * halfWidth + sin * halfHeight;
        float extentY = sin * halfWidth + cos * halfHeight;
        float margin = 4.0F;

        if (screenWidth > (extentX + margin) * 2.0F) {
            x = Mth.clamp(x, extentX + margin, screenWidth - extentX - margin);
        }

        if (screenHeight > (extentY + margin) * 2.0F) {
            y = Mth.clamp(y, extentY + margin, screenHeight - extentY - margin);
        }

        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, alpha);

        graphics.pose().pushPose();
        graphics.pose().translate(x, y, 250.0F);
        graphics.pose().mulPose(Axis.ZP.rotationDegrees(rotation));
        graphics.pose().scale(stain.mirrored ? -scale : scale, scale, 1.0F);
        graphics.blit(TEXTURE, -TEXTURE_WIDTH / 2, -TEXTURE_HEIGHT / 2, 0.0F, 0.0F, TEXTURE_WIDTH, TEXTURE_HEIGHT, TEXTURE_WIDTH, TEXTURE_HEIGHT);
        graphics.pose().popPose();
    }

    private static float smoothStep(float value) {
        value = Mth.clamp(value, 0.0F, 1.0F);

        return value * value * (3.0F - 2.0F * value);
    }

    private static final class Stain {

        private final float xFactor;
        private final float yFactor;

        private final float rotation;
        private final float scale;

        private final float slideDistance;
        private final float sideDrift;
        private final float phase;

        private final boolean mirrored;

        private float age;
        private float previousAge;

        private Stain(float xFactor, float yFactor, float rotation, float scale, float slideDistance, float sideDrift, float phase, boolean mirrored) {
            this.xFactor = xFactor;
            this.yFactor = yFactor;
            this.rotation = rotation;
            this.scale = scale;
            this.slideDistance = slideDistance;
            this.sideDrift = sideDrift;
            this.phase = phase;
            this.mirrored = mirrored;
        }
    }
}