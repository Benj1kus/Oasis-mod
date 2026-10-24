package com.benji.oasiso.client.gui;

import com.benji.oasiso.Oasiso;
import com.benji.oasiso.ModSounds;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;


@Mod.EventBusSubscriber(modid = Oasiso.MODID, value = Dist.CLIENT)
public final class CutsceneFightOverlay {
    private static final ResourceLocation GIF = ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "textures/gui/fight.gif");
    public static final float DISPLAY_SCALE = 1F;
    public static final float SOUND_VOLUME = 1F;
    public static final long LAST_FRAME_HOLD_MS = 350;
    public static final long FADE_OUT_MS = 650;
    private static java.util.UUID waitingPaladin;
    private static int waitTicks, uploadedFade = -1;
    private static GifFrameDecoder.Decoded decoded;
    private static DynamicTexture texture;
    private static ResourceLocation textureId;
    private static ClientLevel world;
    private static long durationMillis;
    private static int ageTicks, uploadedFrame = -1;
    private static boolean playing;

    private CutsceneFightOverlay() {
    }

    public static void prepare() {
        clear();
        Minecraft mc = Minecraft.getInstance();
        world = mc.level;
        try (var stream = mc.getResourceManager().open(GIF)) {
            decoded = GifFrameDecoder.decode(stream);
            // A HUD animation must remain small even if replaced by a resource pack.
            if (decoded.canvasWidth() > 1024 || decoded.canvasHeight() > 1024 || decoded.frames().size() > 200)
                throw new IllegalArgumentException("Fight GIF exceeds HUD limits");
            durationMillis = decoded.frames().stream().mapToLong(GifFrameDecoder.RawFrame::delayMillis).sum();
            texture = new DynamicTexture(decoded.canvasWidth(), decoded.canvasHeight(), true);
            texture.setFilter(false, false);
            textureId = mc.getTextureManager().register("oasiso_cutscene_fight", texture);
        } catch (Exception ex) {
            clear();
            LogUtils.getLogger().warn("Cannot load {} for cutscene announcement", GIF, ex);
        }
    }

    public static void afterCutscene(net.minecraft.world.entity.Entity boss) {
        if (boss instanceof com.benji.oasiso.common.entity.PaladinEntity paladin) {
            waitingPaladin = paladin.getUUID();
            world = Minecraft.getInstance().level;
            waitTicks = 0;
        } else play();
    }

    public static void play() {
        waitingPaladin = null;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || !mc.player.isAlive()) {
            clear();
            return;
        }
        mc.getSoundManager().play(SimpleSoundInstance.forUI(ModSounds.FIGHT.get(), 1F, SOUND_VOLUME));
        if (decoded == null || world != mc.level) {
            clear();
            return;
        }
        ageTicks = 0;
        uploadedFrame = -1;
        playing = true;
    }

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        if (world != null && (mc.level != world || mc.player == null || !mc.player.isAlive())) {
            clear();
            return;
        }
        if (waitingPaladin != null && mc.level != null && !mc.isPaused()) {
            if (++waitTicks > 20 * 180) {
                clear();
                return;
            }
            for (var entity : mc.level.entitiesForRendering()) {
                if (!entity.getUUID().equals(waitingPaladin)) continue;
                if (!(entity instanceof com.benji.oasiso.common.entity.PaladinEntity paladin) || !paladin.isAlive() || paladin.isDeathSequenceActive()) {
                    clear();
                    return;
                }
                if (paladin.isIntroDialogueDone()) play();
                break;
            }
        }
        if (playing && !mc.isPaused() && ++ageTicks * 50L >= durationMillis + LAST_FRAME_HOLD_MS + FADE_OUT_MS) clear();
    }

    @SubscribeEvent
    public static void hud(RenderGuiEvent.Post event) {
        if (Minecraft.getInstance().screen == null) draw(event.getGuiGraphics());
    }

    @SubscribeEvent
    public static void screen(ScreenEvent.Render.Post event) {
        draw(event.getGuiGraphics());
    }

    private static void draw(GuiGraphics g) {
        if (!playing || decoded == null || texture == null) return;
        Minecraft mc = Minecraft.getInstance();
        long time = (long) ((ageTicks + (mc.isPaused() ? 0 : mc.getFrameTime())) * 50);
        float fade = Math.max(0F, Math.min(1F, (time - durationMillis - LAST_FRAME_HOLD_MS) / (float) FADE_OUT_MS));
        if (fade >= 1F) return;
        int fadeStep = fade == 0 ? 0 : 1 + (int) (fade * 20);
        time = Math.min(time, durationMillis - 1);
        int index = 0;
        while (index < decoded.frames().size() - 1 && time >= decoded.frames().get(index).delayMillis())
            time -= decoded.frames().get(index++).delayMillis();
        if (index != uploadedFrame || fadeStep != uploadedFade) {
            NativeImage image = texture.getPixels();
            if (image == null) return;
            image.fillRect(0, 0, decoded.canvasWidth(), decoded.canvasHeight(), 0);
            var frame = decoded.frames().get(index);
            var bounds = frame.bounds();
            if (bounds != null) for (int y = 0; y < bounds.height; y++)
                for (int x = 0; x < bounds.width; x++) {
                    int argb = frame.argb()[y * bounds.width + x];
                    int abgr = (argb & 0xff00ff00) | ((argb >> 16) & 255) | ((argb & 255) << 16);
                    if (fadeStep > 0) {
                        int hash = ((bounds.x + x) / 4) * 374761393 + ((bounds.y + y) / 4) * 668265263;
                        hash = (hash ^ (hash >>> 13)) * 1274126177;
                        float threshold = (hash & 1023) / 1023F * .75F;
                        float dissolve = Math.max(0F, Math.min(1F, (fade - threshold) / .25F));
                        int alpha = Math.round((abgr >>> 24) * (1F - fade) * (1F - dissolve));
                        abgr = (abgr & 0x00ffffff) | (alpha << 24);
                    }
                    image.setPixelRGBA(bounds.x + x, bounds.y + y, abgr);
                }
            texture.upload();
            uploadedFrame = index;
            uploadedFade = fadeStep;
        }
        float scale = Math.min(DISPLAY_SCALE, Math.min(g.guiWidth() * .85F / decoded.canvasWidth(), g.guiHeight() * .85F / decoded.canvasHeight()));
        g.flush();
        boolean blend = org.lwjgl.opengl.GL11.glIsEnabled(org.lwjgl.opengl.GL11.GL_BLEND);
        float[] color = RenderSystem.getShaderColor().clone();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1, 1, 1, 1);
        g.pose().pushPose();
        try {
            g.pose().translate((g.guiWidth() - decoded.canvasWidth() * scale) / 2F, (g.guiHeight() - decoded.canvasHeight() * scale) / 2F, 1000);
            g.pose().scale(scale, scale, 1);
            g.blit(textureId, 0, 0, 0, 0, decoded.canvasWidth(), decoded.canvasHeight(), decoded.canvasWidth(), decoded.canvasHeight());
            g.flush();
        } finally {
            g.pose().popPose();
            RenderSystem.setShaderColor(color[0], color[1], color[2], color[3]);
            if (!blend) RenderSystem.disableBlend();
        }
    }

    private static void clear() {
        waitingPaladin = null;
        waitTicks = 0;
        uploadedFade = -1;
        playing = false;
        decoded = null;
        world = null;
        durationMillis = 0;
        if (textureId != null) Minecraft.getInstance().getTextureManager().release(textureId);
        else if (texture != null) texture.close();
        textureId = null;
        texture = null;
        uploadedFrame = -1;
    }
}
