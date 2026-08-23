package com.benji.oasiso.client.dialogue;

import com.benji.oasiso.ModSounds;
import com.benji.oasiso.Oasiso;
import com.benji.oasiso.dialogue.data.DialogueDefinition;
import com.benji.oasiso.network.dialogueengine.DialogueNetwork;
import com.google.gson.Gson;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import java.awt.Color;
import java.util.*;

@Mod.EventBusSubscriber(modid = Oasiso.MODID, value = Dist.CLIENT)
public final class DialogueClient {

    private static final Gson GSON = new Gson();

    private static final ResourceLocation DEFAULT_FRAME = ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "textures/gui/dialogue/frame_dialog.png");

    private static final ResourceLocation DEFAULT_BACKGROUND = ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "textures/gui/dialogue/dithering_gradient.png");

    private static DialogueDefinition definition;

    private static UUID sessionId;
    private static ResourceLocation dialogueId;

    private static int lineIndex;
    private static int revealedChars;

    private static int typingDelay;
    private static int holdTicks;
    private static int totalTicks;

    private static boolean active;
    private static boolean ending;
    private static int endingTicks;

    private static String currentText = "";

    private static int[] revealTicks = new int[0];

    // Sprite transition
    private static String previousSprite;

    private static float spriteMoveFromX;
    private static int spriteMoveAge = 1000;

    private static int spriteTransitionAge = 1000;

    private static int voiceLetterCounter;

    private DialogueClient() {
    }


    public static void start(UUID newSessionId, ResourceLocation newDialogueId, String json) {
        try {
            DialogueDefinition loaded = GSON.fromJson(json, DialogueDefinition.class);

            if (loaded == null || loaded.lines == null || loaded.lines.isEmpty()) {

                DialogueNetwork.finish(newSessionId);

                return;
            }

            definition = loaded;
            sessionId = newSessionId;
            dialogueId = newDialogueId;

            lineIndex = 0;
            revealedChars = 0;

            typingDelay = 0;
            holdTicks = 0;
            totalTicks = 0;

            ending = false;
            endingTicks = 0;

            active = true;

            previousSprite = null;

            spriteTransitionAge = 1000;
            spriteMoveAge = 1000;

            voiceLetterCounter = 0;

            updateCurrentText();

            spriteMoveFromX = resolveSpriteTargetX(currentLine());

        } catch (Exception exception) {
            exception.printStackTrace();

            DialogueNetwork.finish(newSessionId);
        }
    }


    public static void cancel(UUID targetSession) {
        if (!active || sessionId == null || !sessionId.equals(targetSession)) {
            return;
        }

        reset();
    }


    @SubscribeEvent
    public static void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !active) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();

        if (minecraft.level == null || minecraft.player == null) {
            reset();
            return;
        }

        if (minecraft.isPaused()) {
            return;
        }

        totalTicks++;

        spriteMoveAge++;
        spriteTransitionAge++;

        if (ending) {
            endingTicks++;

            if (endingTicks >= getFadeTicks()) {
                finish();
            }

            return;
        }

        if (revealedChars < currentText.length()) {

            if (typingDelay > 0) {
                typingDelay--;
                return;
            }

            char character = currentText.charAt(revealedChars);

            revealTicks[revealedChars] = totalTicks;

            revealedChars++;

            playVoice(character);

            typingDelay = Math.max(0, getCharTicks() - 1) + punctuationPause(character);

            return;
        }

        holdTicks++;

        if (holdTicks >= getHoldTicks()) {
            advanceLine();
        }
    }


    private static void advanceLine() {
        if (lineIndex >= definition.lines.size() - 1) {

            ending = true;
            endingTicks = 0;
            return;
        }

        DialogueDefinition.Line old = currentLine();

        float oldX = currentSpriteX(0.0F);

        String oldSprite = old.sprite;

        lineIndex++;

        DialogueDefinition.Line next = currentLine();

        previousSprite = oldSprite;

        spriteMoveFromX = oldX;
        spriteMoveAge = 0;

        if (!Objects.equals(oldSprite, next.sprite)) {
            spriteTransitionAge = 0;
        } else {
            spriteTransitionAge = 1000;
        }

        revealedChars = 0;
        typingDelay = 0;
        holdTicks = 0;

        voiceLetterCounter = 0;

        updateCurrentText();
    }


    private static void updateCurrentText() {
        DialogueDefinition.Line line = currentLine();

        currentText = line.literal != null ? line.literal : line.text != null ? I18n.get(line.text) : "";

        revealTicks = new int[currentText.length()];

        Arrays.fill(revealTicks, Integer.MIN_VALUE / 2);
    }


    private static void finish() {
        UUID finished = sessionId;

        reset();

        if (finished != null) {
            DialogueNetwork.finish(finished);
        }
    }


    private static void reset() {
        definition = null;

        sessionId = null;
        dialogueId = null;

        lineIndex = 0;
        revealedChars = 0;

        typingDelay = 0;
        holdTicks = 0;

        totalTicks = 0;

        ending = false;
        endingTicks = 0;

        currentText = "";

        revealTicks = new int[0];

        previousSprite = null;

        spriteTransitionAge = 1000;
        spriteMoveAge = 1000;

        voiceLetterCounter = 0;

        active = false;
    }


    private static void playVoice(char character) {
        if (!Character.isLetterOrDigit(character)) {
            return;
        }

        voiceLetterCounter++;

        DialogueDefinition.Line line = currentLine();

        int every = line.voice_every != null ? line.voice_every : definition.voice_every;

        every = Math.max(1, every);

        if ((voiceLetterCounter - 1) % every != 0) {
            return;
        }

        SoundEvent sound = resolveVoice(line);

        if (sound == null) {
            return;
        }

        float pitch = line.voice_pitch != null ? line.voice_pitch : definition.voice_pitch;

        float volume = line.voice_volume != null ? line.voice_volume : definition.voice_volume;

        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(sound, pitch, volume));
    }


    private static SoundEvent resolveVoice(DialogueDefinition.Line line) {
        String value = line.voice != null ? line.voice : definition.voice;

        if (value == null || value.isBlank()) {
            return null;
        }

        if (value.equals("osiris")) {
            return ModSounds.OSIRIS_VOICE.get();
        }

        if (value.equals("paladin")) {
            return ModSounds.PALADIN_VOICE.get();
        }

        ResourceLocation id = ResourceLocation.tryParse(value);

        return id != null ? ForgeRegistries.SOUND_EVENTS.getValue(id) : null;
    }


    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        if (Minecraft.getInstance().screen != null) {
            return;
        }

        render(event.getGuiGraphics());
    }


    @SubscribeEvent
    public static void onRenderScreen(ScreenEvent.Render.Post event) {
        render(event.getGuiGraphics());
    }


    private static void render(GuiGraphics graphics) {
        if (!active || definition == null) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();

        float partialTick = minecraft.getFrameTime();

        float alpha = globalAlpha(partialTick);

        if (alpha <= 0.001F) {
            return;
        }

        float time = (totalTicks + partialTick) / 20.0F;

        renderBackground(graphics, time, alpha);

        renderDialogue(graphics, minecraft.font, time, partialTick, alpha);
    }


    private static void renderBackground(GuiGraphics graphics, float time, float alpha) {
        DialogueDefinition.Layout layout = definition.layout;

        ResourceLocation texture = currentBackground();

        int screenW = graphics.guiWidth();

        int screenH = graphics.guiHeight();

        float scale = Math.max(screenW / (float) layout.canvas_width, screenH / (float) layout.canvas_height) * 1.05F;

        float width = layout.canvas_width * scale;

        float height = layout.canvas_height * scale;

        float bob = (float) Math.sin(time * definition.background_speed) * definition.background_bob * scale;

        float x = (screenW - width) * 0.5F;

        float y = (screenH - height) * 0.5F + bob;

        PoseStack pose = graphics.pose();

        pose.pushPose();

        pose.translate(x, y, 300.0F);

        pose.scale(scale, scale, 1.0F);

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        graphics.setColor(1.0F, 1.0F, 1.0F, definition.background_alpha * alpha);

        graphics.blit(texture, 0, 0, 0, 0, layout.canvas_width, layout.canvas_height, layout.canvas_width, layout.canvas_height);

        graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);

        pose.popPose();
    }


    private static void renderDialogue(GuiGraphics graphics, Font font, float time, float partialTick, float alpha) {
        DialogueDefinition.Layout layout = definition.layout;

        int screenW = graphics.guiWidth();

        int screenH = graphics.guiHeight();

        float scale = Math.min(screenW / (float) layout.canvas_width, screenH / (float) layout.canvas_height);

        float originX = (screenW - layout.canvas_width * scale) * 0.5F;

        float originY = (screenH - layout.canvas_height * scale) * 0.5F;

        PoseStack pose = graphics.pose();

        pose.pushPose();

        pose.translate(originX, originY, 400.0F);

        pose.scale(scale, scale, 1.0F);

        renderSprite(graphics, partialTick, alpha);

        renderFrame(graphics, alpha);

        renderText(graphics, font, time, partialTick, alpha);

        pose.popPose();
    }


    private static void renderSprite(GuiGraphics graphics, float partialTick, float alpha) {
        DialogueDefinition.Line line = currentLine();

        if (line.sprite == null) {
            return;
        }

        ResourceLocation sprite = ResourceLocation.tryParse(line.sprite);

        if (sprite == null) {
            return;
        }

        String transition = spriteTransition(line);

        int transitionTicks = spriteTransitionTicks(line);

        float progress = Mth.clamp((spriteTransitionAge + partialTick) / transitionTicks, 0.0F, 1.0F);

        float x = currentSpriteX(partialTick);

        int width = spriteWidth(line);

        int height = spriteHeight(line);

        int y = definition.layout.sprite_y;


        if ("fade".equals(transition) || "fade_up".equals(transition)) {

            if (previousSprite != null && progress < 1.0F) {

                ResourceLocation old = ResourceLocation.tryParse(previousSprite);

                if (old != null) {
                    int visibleHeight = Math.max(0, Math.round(height * (1.0F - progress)));

                    if (visibleHeight > 0) {
                        graphics.setColor(1.0F, 1.0F, 1.0F, alpha);

                        graphics.blit(old, Math.round(spriteMoveFromX), y, 0, 0, width, visibleHeight, width, height);
                    }
                }
            }
        }

        PoseStack pose = graphics.pose();

        pose.pushPose();

        float centerX = x + width * 0.5F;

        float centerY = y + height * 0.5F;

        float drawAlpha = alpha;

        switch (transition) {

            case "bounce" -> {
                float age = spriteTransitionAge + partialTick;

                float damping = (float) Math.exp(-age * 0.22F);

                float bounceY = -(float) Math.sin(age * 0.92F) * damping * 4.0F;

                float bounceScale = 1.0F + Math.max(0.0F, (float) Math.sin(age * 0.92F)) * damping * 0.035F;

                pose.translate(centerX, centerY + bounceY, 0.0F);

                pose.scale(bounceScale, bounceScale, 1.0F);

                pose.translate(-width * 0.5F, -height * 0.5F, 0.0F);
            }

            case "sway" -> {
                float age = spriteTransitionAge + partialTick;

                float damping = (float) Math.exp(-age * 0.20F);

                float sway = (float) Math.sin(age * 0.95F) * damping;

                pose.translate(centerX + sway * 3.5F, centerY, 0.0F);

                pose.mulPose(Axis.ZP.rotationDegrees(sway * 5.0F));

                pose.translate(-width * 0.5F, -height * 0.5F, 0.0F);
            }

            case "fade", "fade_up" -> {
                drawAlpha *= progress;

                pose.translate(x, y, 0.0F);
            }

            default -> pose.translate(x, y, 0.0F);
        }

        graphics.setColor(1.0F, 1.0F, 1.0F, drawAlpha);

        graphics.blit(sprite, 0, 0, 0, 0, width, height, width, height);

        graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);

        pose.popPose();
    }


    private static void renderFrame(GuiGraphics graphics, float alpha) {
        DialogueDefinition.Layout layout = definition.layout;

        ResourceLocation frame = currentFrame();

        graphics.setColor(1.0F, 1.0F, 1.0F, alpha);

        graphics.blit(frame, layout.frame_x, layout.frame_y, 0, 0, layout.frame_width, layout.frame_height, layout.frame_width, layout.frame_height);

        graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
    }


    private static void renderText(GuiGraphics graphics, Font font, float time, float partialTick, float alpha) {
        DialogueDefinition.Layout layout = definition.layout;

        int maxWidth = Mth.floor(layout.text_width / layout.text_scale);

        List<Glyph> glyphs = layoutGlyphs(font, currentText, maxWidth);

        PoseStack pose = graphics.pose();

        pose.pushPose();

        pose.translate(layout.text_x, layout.text_y, 10.0F);

        pose.scale(layout.text_scale, layout.text_scale, 1.0F);

        int alphaByte = Mth.clamp(Math.round(alpha * 255.0F), 0, 255);

        String effect = textEffect(currentLine());

        for (Glyph glyph : glyphs) {

            if (glyph.index >= revealedChars) {
                continue;
            }

            float age = totalTicks + partialTick - revealTicks[glyph.index];

            float x = glyph.x;
            float y = glyph.y;

            float scale = 1.0F;

            switch (effect) {

                case "wave" -> y += (float) Math.sin(time * 5.0F + glyph.index * 0.55F) * 0.85F;

                case "shake" -> {
                    long seed = glyph.index * 734287L + totalTicks * 912271L;

                    x += hashOffset(seed);
                    y += hashOffset(seed + 19L);
                }

                case "explode" -> {
                    float p = smooth(age / 6.0F);

                    scale = 1.0F + (1.0F - p) * 0.85F;
                }

                case "slide", "linear" -> {
                    float p = smooth(age / 6.0F);

                    x -= (1.0F - p) * 13.0F;
                }
            }

            int rgb = letterColor(currentLine(), glyph, maxWidth, time);

            int color = (alphaByte << 24) | rgb;

            PoseStack glyphPose = graphics.pose();

            glyphPose.pushPose();

            glyphPose.translate(x, y, 0.0F);

            if (scale != 1.0F) {
                int glyphWidth = font.width(String.valueOf(glyph.character));

                glyphPose.translate(glyphWidth * 0.5F, font.lineHeight * 0.5F, 0.0F);

                glyphPose.scale(scale, scale, 1.0F);

                glyphPose.translate(-glyphWidth * 0.5F, -font.lineHeight * 0.5F, 0.0F);
            }

            graphics.drawString(font, String.valueOf(glyph.character), 0, 0, color, false);

            glyphPose.popPose();
        }

        pose.popPose();
    }


    private static int letterColor(DialogueDefinition.Line line, Glyph glyph, int maxWidth, float time) {
        List<String> gradient = line.text_gradient != null ? line.text_gradient : definition.text_gradient;

        if (gradient != null && gradient.size() >= 2) {

            float t = Mth.clamp(glyph.x / (float) Math.max(1, maxWidth), 0.0F, 1.0F);

            return gradientColor(gradient, t);
        }

        String value = line.text_color != null ? line.text_color : definition.text_color;

        if ((value == null || value.equals("white"))) {

            String legacy = line.text_style != null ? line.text_style : definition.text_style;

            if (legacy != null) {
                value = legacy;
            }
        }

        if ("rainbow".equalsIgnoreCase(value)) {
            float hue = (glyph.index * 0.095F + time * 0.055F) % 1.0F;

            return Color.HSBtoRGB(hue, 0.76F, 1.0F) & 0xFFFFFF;
        }

        return parseColor(value);
    }


    private static int parseColor(String value) {
        if (value == null) {
            return 0xFFFFFF;
        }

        value = value.trim().toLowerCase(Locale.ROOT);

        return switch (value) {

            case "blue" -> 0x4AA3FF;

            case "red" -> 0xFF4D55;

            case "gold", "golden" -> 0xFFD45A;

            case "green" -> 0x55E878;

            case "white" -> 0xFFFFFF;

            case "black" -> 0x000000;

            case "purple" -> 0xB76CFF;

            case "cyan" -> 0x42F2E1;

            default -> parseHex(value);
        };
    }


    private static int parseHex(String value) {
        try {
            if (value.startsWith("#")) {
                value = value.substring(1);
            }

            if (value.startsWith("0x")) {
                value = value.substring(2);
            }

            if (value.length() == 3) {
                value = "" + value.charAt(0) + value.charAt(0) + value.charAt(1) + value.charAt(1) + value.charAt(2) + value.charAt(2);
            }

            return Integer.parseInt(value, 16) & 0xFFFFFF;

        } catch (Exception ignored) {
            return 0xFFFFFF;
        }
    }


    private static int gradientColor(List<String> colors, float t) {
        int sections = colors.size() - 1;

        float scaled = t * sections;

        int index = Mth.clamp((int) Math.floor(scaled), 0, sections - 1);

        float local = scaled - index;

        int a = parseColor(colors.get(index));

        int b = parseColor(colors.get(index + 1));

        int ar = a >> 16 & 255;
        int ag = a >> 8 & 255;
        int ab = a & 255;

        int br = b >> 16 & 255;
        int bg = b >> 8 & 255;
        int bb = b & 255;

        int r = Mth.lerpInt(local, ar, br);

        int g = Mth.lerpInt(local, ag, bg);

        int bl = Mth.lerpInt(local, ab, bb);

        return r << 16 | g << 8 | bl;
    }


    private static float currentSpriteX(float partialTick) {
        DialogueDefinition.Line line = currentLine();

        float target = resolveSpriteTargetX(line);

        int ticks = line.sprite_move_ticks != null ? line.sprite_move_ticks : definition.sprite_move_ticks;

        if (ticks <= 0) {
            return target;
        }

        float p = smooth((spriteMoveAge + partialTick) / ticks);

        return Mth.lerp(p, spriteMoveFromX, target);
    }


    private static float resolveSpriteTargetX(DialogueDefinition.Line line) {
        if (line.sprite_x != null) {
            return line.sprite_x;
        }

        String position = line.sprite_position != null ? line.sprite_position : definition.sprite_position;

        DialogueDefinition.Layout layout = definition.layout;

        if (position == null) {
            position = "center";
        }

        return switch (position.toLowerCase(Locale.ROOT)) {
            case "left" -> layout.sprite_left_x;

            case "right" -> layout.sprite_right_x;

            default -> layout.sprite_center_x;
        };
    }


    private static int spriteWidth(DialogueDefinition.Line line) {
        return line.sprite_width != null ? line.sprite_width : definition.layout.sprite_width;
    }


    private static int spriteHeight(DialogueDefinition.Line line) {
        return line.sprite_height != null ? line.sprite_height : definition.layout.sprite_height;
    }


    private static String spriteTransition(DialogueDefinition.Line line) {
        String value = line.sprite_transition != null ? line.sprite_transition : definition.sprite_transition;

        return value != null ? value.toLowerCase(Locale.ROOT) : "none";
    }


    private static int spriteTransitionTicks(DialogueDefinition.Line line) {
        return Math.max(1, line.sprite_transition_ticks != null ? line.sprite_transition_ticks : definition.sprite_transition_ticks);
    }

    private static ResourceLocation currentFrame() {
        DialogueDefinition.Line line = currentLine();

        String value = line.frame != null ? line.frame : definition.frame;

        ResourceLocation parsed = value != null ? ResourceLocation.tryParse(value) : null;

        return parsed != null ? parsed : DEFAULT_FRAME;
    }


    private static ResourceLocation currentBackground() {
        DialogueDefinition.Line line = currentLine();

        String value = line.background != null ? line.background : definition.background;

        ResourceLocation parsed = value != null ? ResourceLocation.tryParse(value) : null;

        return parsed != null ? parsed : DEFAULT_BACKGROUND;
    }

    private static String textEffect(DialogueDefinition.Line line) {
        String value = line.text_effect != null ? line.text_effect : definition.text_effect;

        return value != null ? value.toLowerCase(Locale.ROOT) : "normal";
    }


    private static int getCharTicks() {
        Integer custom = currentLine().char_ticks;

        return Math.max(1, custom != null ? custom : definition.char_ticks);
    }


    private static int getHoldTicks() {
        Integer custom = currentLine().hold_ticks;

        return Math.max(1, custom != null ? custom : definition.hold_ticks);
    }


    private static int getFadeTicks() {
        return Math.max(1, definition.fade_ticks);
    }


    private static int punctuationPause(char character) {
        return switch (character) {
            case '.', '!', '?' -> 3;
            case ',', ';', ':' -> 1;
            default -> 0;
        };
    }


    private static float globalAlpha(float partialTick) {
        float fadeIn = smooth((totalTicks + partialTick) / getFadeTicks());

        if (!ending) {
            return fadeIn;
        }

        float fadeOut = 1.0F - smooth((endingTicks + partialTick) / getFadeTicks());

        return fadeIn * fadeOut;
    }


    private static float smooth(float value) {
        value = Mth.clamp(value, 0.0F, 1.0F);

        return value * value * (3.0F - 2.0F * value);
    }


    private static float hashOffset(long value) {
        value ^= value << 13;
        value ^= value >>> 7;
        value ^= value << 17;

        return ((value & 1023L) / 1023.0F - 0.5F) * 1.6F;
    }


    private static DialogueDefinition.Line currentLine() {
        return definition.lines.get(lineIndex);
    }

    private static List<Glyph> layoutGlyphs(Font font, String text, int maxWidth) {
        List<Glyph> result = new ArrayList<>();

        int x = 0;
        int y = 0;

        int i = 0;

        int lineHeight = definition.layout.line_height;

        while (i < text.length()) {

            char character = text.charAt(i);

            if (character == '\n') {
                x = 0;
                y += lineHeight;
                i++;
                continue;
            }

            if (Character.isWhitespace(character)) {
                int width = font.width(String.valueOf(character));

                if (x + width <= maxWidth) {
                    result.add(new Glyph(i, character, x, y));

                    x += width;
                } else {
                    x = 0;
                    y += lineHeight;
                }

                i++;
                continue;
            }

            int wordEnd = i;

            while (wordEnd < text.length()) {

                char next = text.charAt(wordEnd);

                if (Character.isWhitespace(next) || next == '\n') {
                    break;
                }

                wordEnd++;
            }

            String word = text.substring(i, wordEnd);

            int wordWidth = font.width(word);

            if (x > 0 && x + wordWidth > maxWidth) {

                x = 0;
                y += lineHeight;
            }

            for (int index = i; index < wordEnd; index++) {

                char letter = text.charAt(index);

                int width = font.width(String.valueOf(letter));

                if (x > 0 && x + width > maxWidth) {

                    x = 0;
                    y += lineHeight;
                }

                result.add(new Glyph(index, letter, x, y));

                x += width;
            }
            i = wordEnd;
        }
        return result;
    }


    private record Glyph(int index, char character, int x, int y) {
    }
}