package com.benji.oasiso.client.shrine;

import com.benji.oasiso.ModSounds;
import com.benji.oasiso.Oasiso;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Oasiso.MODID, value = Dist.CLIENT)
public final class SoulBottleEffects {
    public static final int HOLD_TICKS = 60, FADE_TICKS = 30;
    public static final float TEXT_SCALE = 3F;
    private static ClientLevel world;
    private static int age = -1, number = -1;

    private static boolean finalCountdown;
    private static int countdownIndex;

    private static final int[] COUNTDOWN_TIMES = {0, 10, 19, 27, 34, 40, 45, 49, 52};

    private static int countdownDuration() {
        return finalCountdown ? COUNTDOWN_TIMES[COUNTDOWN_TIMES.length - 1] : 0;
    }

    private static void playTick() {
        Minecraft.getInstance().getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(ModSounds.SOUL_VOICE.get(), 1F, .7F));
    }

    public static void show(int remaining) {
        var mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        WarCoreEffects.flash(ModSounds.AZUMAAL_HIT.get());

        finalCountdown = remaining == 0;
        countdownIndex = 0;
        number = finalCountdown ? 8 : remaining;
        age = remaining >= 0 ? 0 : -1;
        world = mc.level;

        if (finalCountdown) playTick();
    }

    private static void clear() {
        age = number = -1;
        world = null;
        finalCountdown = false;
        countdownIndex = 0;
    }

    @SubscribeEvent
    public static void logout(ClientPlayerNetworkEvent.LoggingOut event) {
        clear();
    }

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        var mc = Minecraft.getInstance();
        if (mc.level != world || mc.player == null || !mc.player.isAlive()) {
            clear();
            return;
        }

        if (mc.isPaused() || age < 0) return;
        age++;

        if (finalCountdown && countdownIndex < COUNTDOWN_TIMES.length - 1 && age >= COUNTDOWN_TIMES[countdownIndex + 1]) {
            countdownIndex++;
            number = 8 - countdownIndex;
            playTick();
        }

        if (age >= countdownDuration() + HOLD_TICKS + FADE_TICKS) {
            clear();
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void render(RenderGuiEvent.Post event) {
        var mc = Minecraft.getInstance();
        if (age < 0 || number < 0 || mc.level != world || mc.player == null) return;
        float t = age + event.getPartialTick();
        float fade = Mth.clamp((t - countdownDuration() - HOLD_TICKS) / FADE_TICKS, 0, 1);
        float opacity = Mth.clamp(t / 4F, 0, 1) * (1 - fade * fade * (3 - 2 * fade));
        int alpha = (int) (255 * opacity);
        if (alpha < 4) return;
        var graphics = event.getGuiGraphics();
        String text = Integer.toString(number);
        graphics.flush();
        try (SoulRenderState ignored = new SoulRenderState()) {
            RenderSystem.colorMask(true, true, true, false);
            RenderSystem.disableDepthTest();
            RenderSystem.depthMask(false);
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.setShaderColor(1, 1, 1, 1);
            graphics.pose().pushPose();
            try {
                graphics.pose().translate(graphics.guiWidth() * .5F, graphics.guiHeight() * .5F, 1100);
                graphics.pose().scale(TEXT_SCALE, TEXT_SCALE, 1);
                graphics.drawString(mc.font, text, -mc.font.width(text) / 2, -mc.font.lineHeight / 2, (alpha << 24) | 0x5FFFCC, false);
                graphics.flush();
            } finally {
                graphics.pose().popPose();
            }
        }
    }
}
