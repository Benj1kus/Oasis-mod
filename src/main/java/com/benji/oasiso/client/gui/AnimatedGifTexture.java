package com.benji.oasiso.client.gui;

import com.benji.oasiso.Oasiso;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Mod.EventBusSubscriber(modid = Oasiso.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class AnimatedGifTexture implements AutoCloseable {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Map<ResourceLocation, AnimatedGifTexture> CACHE = new HashMap<>();

    private final ResourceLocation source;
    private boolean loadAttempted;
    private List<NativeImage> frames = List.of();
    private int[] frameEndTimes = new int[0];
    private int durationMillis;
    private int canvasWidth = 1;
    private int canvasHeight = 1;
    private int cropX;
    private int cropY;
    private int cropWidth = 1;
    private int cropHeight = 1;
    private DynamicTexture texture;
    private ResourceLocation textureId;
    private int uploadedFrame = -1;

    private AnimatedGifTexture(ResourceLocation source) {
        this.source = source;
    }

    public static AnimatedGifTexture get(ResourceLocation source) {
        return CACHE.computeIfAbsent(source, AnimatedGifTexture::new);
    }

    @SubscribeEvent
    public static void registerReloadListener(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener((ResourceManagerReloadListener) resourceManager -> clearCache());
    }

    private static void clearCache() {
        for (AnimatedGifTexture gif : CACHE.values()) {
            gif.close();
        }
        CACHE.clear();
    }

    public long durationMillis() {
        ensureLoaded();
        return durationMillis;
    }

    public boolean isAvailable() {
        ensureLoaded();
        return texture != null && !frames.isEmpty();
    }

    public void render(GuiGraphics graphics, long animationMillis, boolean loop) {
        ensureLoaded();
        if (texture == null || frames.isEmpty() || durationMillis <= 0) {
            return;
        }

        int frameIndex = findFrame(animationMillis, loop);
        uploadFrame(frameIndex);

        float scale = Math.min(graphics.guiWidth() / (float) Math.max(1, canvasWidth), graphics.guiHeight() / (float) Math.max(1, canvasHeight));
        int x = Math.round(cropX * scale);
        int y = Math.round(cropY * scale);
        int width = Math.max(1, Math.round(cropWidth * scale));
        int height = Math.max(1, Math.round(cropHeight * scale));

        graphics.blit(textureId, x, y, width, height, 0.0F, 0.0F, cropWidth, cropHeight, cropWidth, cropHeight);
    }

    private int findFrame(long animationMillis, boolean loop) {
        long time = loop ? Math.floorMod(animationMillis, durationMillis) : Math.min(Math.max(0L, animationMillis), durationMillis - 1L);

        int low = 0;
        int high = frameEndTimes.length - 1;
        while (low < high) {
            int middle = (low + high) >>> 1;
            if (time < frameEndTimes[middle]) {
                high = middle;
            } else {
                low = middle + 1;
            }
        }
        return low;
    }

    private void uploadFrame(int frameIndex) {
        if (frameIndex == uploadedFrame) {
            return;
        }

        NativeImage pixels = texture.getPixels();
        if (pixels != null) {
            pixels.copyFrom(frames.get(frameIndex));
            texture.upload();
            uploadedFrame = frameIndex;
        }
    }

    private void ensureLoaded() {
        if (loadAttempted) {
            return;
        }
        loadAttempted = true;

        try (InputStream stream = Minecraft.getInstance().getResourceManager().getResourceOrThrow(source).open()) {
            GifFrameDecoder.Decoded decoded = GifFrameDecoder.decode(stream);
            this.canvasWidth = decoded.canvasWidth();
            this.canvasHeight = decoded.canvasHeight();
            this.cropX = decoded.crop().x;
            this.cropY = decoded.crop().y;
            this.cropWidth = decoded.crop().width;
            this.cropHeight = decoded.crop().height;

            List<NativeImage> loadedFrames = new ArrayList<>(decoded.frames().size());
            this.frameEndTimes = new int[decoded.frames().size()];
            int elapsed = 0;

            try {
                for (int index = 0; index < decoded.frames().size(); index++) {
                    GifFrameDecoder.RawFrame raw = decoded.frames().get(index);
                    NativeImage image = new NativeImage(cropWidth, cropHeight, true);
                    image.fillRect(0, 0, cropWidth, cropHeight, 0);
                    copyFrame(raw, decoded.crop(), image);
                    loadedFrames.add(image);
                    elapsed += raw.delayMillis();
                    frameEndTimes[index] = elapsed;
                }
            } catch (RuntimeException exception) {
                for (NativeImage frame : loadedFrames) {
                    frame.close();
                }
                throw exception;
            }

            this.frames = List.copyOf(loadedFrames);
            this.durationMillis = Math.max(1, elapsed);
            this.texture = new DynamicTexture(cropWidth, cropHeight, true);
            this.texture.setFilter(false, false);
            this.textureId = Minecraft.getInstance().getTextureManager().register("oasiso_glove_gif", texture);
        } catch (Exception exception) {
            LOGGER.error("OOPS my bad {}", source, exception);
            close();
        }
    }

    private static void copyFrame(GifFrameDecoder.RawFrame frame, java.awt.Rectangle crop, NativeImage target) {
        if (frame.bounds() == null) {
            return;
        }

        int targetX = frame.bounds().x - crop.x;
        int targetY = frame.bounds().y - crop.y;
        for (int y = 0; y < frame.bounds().height; y++) {
            for (int x = 0; x < frame.bounds().width; x++) {
                int argb = frame.argb()[y * frame.bounds().width + x];
                int abgr = (argb & 0xFF00FF00) | ((argb & 0x00FF0000) >>> 16) | ((argb & 0x000000FF) << 16);
                target.setPixelRGBA(targetX + x, targetY + y, abgr);
            }
        }
    }

    @Override
    public void close() {
        for (NativeImage frame : frames) {
            frame.close();
        }
        frames = List.of();
        frameEndTimes = new int[0];
        durationMillis = 0;

        if (textureId != null) {
            Minecraft.getInstance().getTextureManager().release(textureId);
        } else if (texture != null) {
            texture.close();
        }
        texture = null;
        textureId = null;
        uploadedFrame = -1;
    }
}
