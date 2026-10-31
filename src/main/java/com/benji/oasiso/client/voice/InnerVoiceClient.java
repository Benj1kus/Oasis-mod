package com.benji.oasiso.client.voice;

import com.benji.oasiso.ModSounds;
import com.benji.oasiso.common.voice.InnerVoiceNetwork;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.*;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.*;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(modid = "oasiso", value = Dist.CLIENT)
public final class InnerVoiceClient {
    private static final KeyMapping SETTINGS = new KeyMapping("key.oasiso_voice.settings", GLFW.GLFW_KEY_Y, "key.categories.oasiso_voice");
    private static final ResourceLocation OVERLAY = ResourceLocation.fromNamespaceAndPath("oasiso", "textures/gui/voice_overlay.png");
    private static final int LETTER_TICKS = 2, HOLD_TICKS = 60, FADE_TICKS = 30;
    private static String phrase = "";
    private static int age = -1, count, shown;
    private static long token;
    private static float pulse = .35F, oldPulse = .35F;
    private static ClientLevel world;

    @Mod.EventBusSubscriber(modid = "oasiso", value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static final class Keys {
        @SubscribeEvent
        public static void keys(RegisterKeyMappingsEvent e) {
            e.register(SETTINGS);
        }
    }

    public static void show(String key, long id) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            InnerVoiceNetwork.done(id);
            return;
        }
        InnerVoicePreferences.load();
        phrase = Component.translatable(key).getString();
        int total = phrase.codePointCount(0, phrase.length());
        if (total > 300) phrase = phrase.substring(0, phrase.offsetByCodePoints(0, 300));
        count = phrase.codePointCount(0, phrase.length());
        age = 0;
        shown = 0;
        token = id;
        world = mc.level;
        pulse = oldPulse = .35F;
    }

    public static void preview(int voice) {
        Minecraft mc = Minecraft.getInstance();
        mc.getSoundManager().play(SimpleSoundInstance.forUI(sound(voice), 1F, .55F));
    }

    private static SoundEvent sound(int voice) {
        return switch (voice) {
            case 0 -> ModSounds.PALADIN_VOICE.get();
            case 2 -> ModSounds.SOUL_VOICE.get();
            case 3 -> ModSounds.SCIENCE_TXT.get();
            case 4 -> ModSounds.HAPPY_TXT.get();
            case 5 -> ModSounds.SAD_TXT.get();
            case 6 -> ModSounds.ANGER_TXT.get();
            case 7 -> ModSounds.BEAST.get();
            case 8 -> ModSounds.SPRINGV.get();
            case 9 -> ModSounds.GRANDPA.get();
            case 10 -> ModSounds.EHH.get();
            case 11 -> ModSounds.BOB.get();
            case 12 -> ModSounds.BONG.get();
            case 13 -> ModSounds.MECHANICAL.get();
            case 14 -> ModSounds.TUN.get();
            case 15 -> ModSounds.SOUL_ANGRY.get();
            case 16 -> ModSounds.SOUL_SAD.get();
            case 17 -> ModSounds.SOUL_KID.get();
            default -> ModSounds.OSIRIS_VOICE.get();
        };
    }

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        while (SETTINGS.consumeClick())
            if (mc.player != null && mc.screen == null) mc.setScreen(new InnerVoiceScreen());
        if (age < 0) return;
        if (mc.level != world || mc.player == null) {
            age = -1;
            return;
        }
        if (!mc.player.isAlive()) {
            age = -1;
            InnerVoiceNetwork.done(token);
            return;
        }
        if (mc.isPaused()) return;
        age++;
        int next = Math.min(count, age / LETTER_TICKS);
        if (next > shown) {
            int cp = phrase.codePointAt(phrase.offsetByCodePoints(0, next - 1));
            if (Character.isLetterOrDigit(cp)) preview(InnerVoicePreferences.voice);
            shown = next;
        }
        oldPulse = pulse;
        float target = shown < count ? ((shown & 1) == 0 ? .32F : .70F) : .35F;
        pulse = Mth.lerp(.35F, pulse, target);
        if (age >= count * LETTER_TICKS + HOLD_TICKS + FADE_TICKS) {
            age = -1;
            InnerVoiceNetwork.done(token);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void hud(RenderGuiEvent.Post e) {
        if (Minecraft.getInstance().screen == null) render(e.getGuiGraphics());
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void screen(ScreenEvent.Render.Post e) {
        render(e.getGuiGraphics());
    }

    private static void render(GuiGraphics g) {
        Minecraft mc = Minecraft.getInstance();
        if (age < 0 || mc.level != world) return;
        float t = age + (mc.isPaused() ? 0 : mc.getFrameTime());
        float alpha = Math.min(1, t / 6F) * (1 - Mth.clamp((t - count * LETTER_TICKS - HOLD_TICKS) / FADE_TICKS, 0, 1));
        if (alpha <= 0) return;
        g.flush();
        try (InnerRenderState ignored = new InnerRenderState()) {
            RenderSystem.disableDepthTest();
            RenderSystem.depthMask(false);
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.colorMask(true, true, true, false);
            int color = InnerVoicePreferences.vignette;
            if (mc.getResourceManager().getResource(OVERLAY).isPresent()) {
                g.setColor(((color >> 16) & 255) / 255F, ((color >> 8) & 255) / 255F, (color & 255) / 255F, alpha * Mth.lerp(mc.getFrameTime(), oldPulse, pulse));
                g.blit(OVERLAY, 0, 0, g.guiWidth(), g.guiHeight(), 0, 0, 1920, 1080, 1920, 1080);
            }
            g.setColor(1, 1, 1, 1);
            String visible = phrase.substring(0, phrase.offsetByCodePoints(0, shown));
            var lines = mc.font.split(Component.literal(visible), Math.min(420, g.guiWidth() - 32));
            int size = Math.min(6, lines.size()), height = size * 11;
            int bottom = g.guiHeight() - 64, top = bottom - height;
            int width = 0;
            for (int i = lines.size() - size; i < lines.size(); i++)
                width = Math.max(width, mc.font.width(lines.get(i)));
            if (size > 0) {
                g.fill((g.guiWidth() - width) / 2 - 7, top - 5, (g.guiWidth() + width) / 2 + 7, bottom + 4, ((int) (alpha * 155) << 24));
                int textColor = (Math.max(4, (int) (alpha * 255)) << 24) | InnerVoicePreferences.text;
                for (int i = 0; i < size; i++) {
                    var line = lines.get(lines.size() - size + i);
                    g.drawString(mc.font, line, (g.guiWidth() - mc.font.width(line)) / 2, top + i * 11, textColor, false);
                }
            }
            g.flush();
        }
    }
}
