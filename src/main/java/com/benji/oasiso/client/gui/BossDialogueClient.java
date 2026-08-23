package com.benji.oasiso.client.gui;

import com.benji.oasiso.ModSounds;
import com.benji.oasiso.Oasiso;
import com.benji.oasiso.network.dialogue.BossDialogueNetwork;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.awt.Color;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = Oasiso.MODID, value = Dist.CLIENT)
public final class BossDialogueClient {

    private static final Gson GSON = new GsonBuilder().create();

    private static final float VIRTUAL_WIDTH = 192.0F;
    private static final float VIRTUAL_HEIGHT = 108.0F;

    private static final int FRAME_Y = 63;

    private static final int SPRITE_X = 33;
    private static final int SPRITE_Y = 0;
    private static final int SPRITE_WIDTH = 126;
    private static final int SPRITE_HEIGHT = 78;

    private static final int TEXT_X = 23;
    private static final int TEXT_Y = FRAME_Y + 12;
    private static final int TEXT_WIDTH = 146;

    private static final float TEXT_SCALE = 0.72F;
    private static final int LINE_HEIGHT = 10;

    private static DialogueDefinition definition;

    private static UUID bossId;
    private static String dialogueId;

    private static int lineIndex;
    private static int revealedChars;

    private static int typingDelay;
    private static int holdTicks;

    private static int totalTicks;

    private static int spriteBounceAge = 1000;
    private static int voiceLetterCounter;

    private static boolean active;
    private static boolean ending;
    private static int endingTicks;

    private static String currentText = "";

    private BossDialogueClient() {
    }

    public static void start(UUID newBossId, String newDialogueId) {
        DialogueDefinition loaded = loadDefinition(newDialogueId);

        if (loaded == null || loaded.lines == null || loaded.lines.isEmpty()) {

            BossDialogueNetwork.dialogueFinished(newBossId, newDialogueId);

            return;
        }

        definition = loaded;
        bossId = newBossId;
        dialogueId = newDialogueId;

        lineIndex = 0;
        revealedChars = 0;

        typingDelay = 0;
        holdTicks = 0;

        totalTicks = 0;

        spriteBounceAge = 0;
        voiceLetterCounter = 0;

        ending = false;
        endingTicks = 0;

        active = true;

        updateCurrentText();
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();

        if (!active) {
            return;
        }

        if (minecraft.level == null || minecraft.player == null) {

            reset();
            return;
        }

        if (minecraft.isPaused()) {
            return;
        }

        totalTicks++;

        if (spriteBounceAge < 1000) {
            spriteBounceAge++;
        }
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
            revealedChars++;

            playVoice(character);

            typingDelay = Math.max(0, getCharTicks() - 1) + punctuationPause(character);

            return;
        }

        holdTicks++;

        if (holdTicks < getCurrentHoldTicks()) {
            return;
        }

        advanceLine();
    }

    private static void advanceLine() {
        if (lineIndex >= definition.lines.size() - 1) {
            ending = true;
            endingTicks = 0;
            return;
        }

        String oldSprite = currentLine().sprite;

        lineIndex++;


        String newSprite = currentLine().sprite;


        if (!Objects.equals(oldSprite, newSprite)) {
            spriteBounceAge = 0;
        }


        revealedChars = 0;
        typingDelay = 0;
        holdTicks = 0;
        voiceLetterCounter = 0;

        updateCurrentText();
    }

    private static void updateCurrentText() {
        currentText = I18n.get(currentLine().text);
    }


    private static void finish() {
        UUID finishedBoss = bossId;
        String finishedDialogue = dialogueId;

        reset();

        if (finishedBoss != null && finishedDialogue != null) {
            BossDialogueNetwork.dialogueFinished(finishedBoss, finishedDialogue);
        }
    }


    private static void reset() {
        definition = null;

        bossId = null;
        dialogueId = null;

        lineIndex = 0;
        revealedChars = 0;

        typingDelay = 0;
        holdTicks = 0;

        totalTicks = 0;

        spriteBounceAge = 1000;
        voiceLetterCounter = 0;

        currentText = "";

        active = false;
        ending = false;
        endingTicks = 0;
    }

    private static void playVoice(char character) {
        if (!Character.isLetterOrDigit(character)) {
            return;
        }

        voiceLetterCounter++;
        int voiceEvery = Math.max(1, definition.voice_every);

        if ((voiceLetterCounter - 1) % voiceEvery != 0) {
            return;
        }

        SoundEvent sound = resolveVoice();
        if (sound == null) {
            return;
        }

        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(sound, definition.voice_pitch, definition.voice_volume));
    }


    private static SoundEvent resolveVoice() {
        if (definition == null || definition.voice == null) {
            return null;
        }

        return switch (definition.voice) {
            case "osiris" -> ModSounds.OSIRIS_VOICE.get();

            case "paladin" -> ModSounds.PALADIN_VOICE.get();

            default -> null;
        };
    }


    private static int punctuationPause(char character) {
        return switch (character) {
            case '.', '!', '?' -> 3;
            case ',', ';', ':' -> 1;
            default -> 0;
        };
    }


    /*
     * =========================================================
     * Render events
     * =========================================================
     */

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

        float alpha = getGlobalAlpha(partialTick);

        if (alpha <= 0.001F) {
            return;
        }

        float time = (totalTicks + partialTick) / 20.0F;


        renderBackground(graphics, time, alpha);

        renderDialogueLayer(graphics, minecraft, time, partialTick, alpha);
    }


    /*
     * =========================================================
     * Background
     * =========================================================
     */

    private static void renderBackground(GuiGraphics graphics, float time, float alpha) {
        ResourceLocation texture = parseLocation(definition.background);

        if (texture == null) {
            return;
        }

        int screenWidth = graphics.guiWidth();

        int screenHeight = graphics.guiHeight();


        /*
         * COVER, а не contain.
         *
         * Dithering всегда закрывает весь экран,
         * даже ultrawide.
         */
        float scale = Math.max(screenWidth / VIRTUAL_WIDTH, screenHeight / VIRTUAL_HEIGHT) * 1.05F;


        float width = VIRTUAL_WIDTH * scale;

        float height = VIRTUAL_HEIGHT * scale;


        /*
         * Очень слабое плавание вверх-вниз.
         */
        float bob = (float) Math.sin(time * 1.10F) * 1.6F * scale;


        float x = (screenWidth - width) * 0.5F;

        float y = (screenHeight - height) * 0.5F + bob;


        PoseStack pose = graphics.pose();

        pose.pushPose();

        pose.translate(x, y, 300.0F);

        pose.scale(scale, scale, 1.0F);


        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        graphics.setColor(1.0F, 1.0F, 1.0F, definition.background_alpha * alpha);


        graphics.blit(texture, 0, 0, 0, 0, 192, 108, 192, 108);


        graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);

        pose.popPose();
    }


    /*
     * =========================================================
     * Sprite + frame + text
     * =========================================================
     */

    private static void renderDialogueLayer(GuiGraphics graphics, Minecraft minecraft, float time, float partialTick, float alpha) {
        int screenWidth = graphics.guiWidth();

        int screenHeight = graphics.guiHeight();


        float scale = Math.min(screenWidth / VIRTUAL_WIDTH, screenHeight / VIRTUAL_HEIGHT);


        float originX = (screenWidth - VIRTUAL_WIDTH * scale) * 0.5F;

        float originY = (screenHeight - VIRTUAL_HEIGHT * scale) * 0.5F;


        PoseStack pose = graphics.pose();

        pose.pushPose();

        pose.translate(originX, originY, 400.0F);

        pose.scale(scale, scale, 1.0F);


        renderSprite(graphics, partialTick, alpha);

        renderFrame(graphics, alpha);

        renderText(graphics, minecraft.font, time, alpha);


        pose.popPose();
    }


    private static void renderSprite(GuiGraphics graphics, float partialTick, float alpha) {
        ResourceLocation sprite = parseLocation(currentLine().sprite);

        if (sprite == null) {
            return;
        }


        float age = spriteBounceAge + partialTick;


        /*
         * Небольшой damped bounce.
         */
        float damping = (float) Math.exp(-age * 0.22F);

        float bounceY = -(float) Math.sin(age * 0.92F) * damping * 4.0F;


        float bounceScale = 1.0F + Math.max(0.0F, (float) Math.sin(age * 0.92F)) * damping * 0.035F;


        PoseStack pose = graphics.pose();

        pose.pushPose();

        float centerX = SPRITE_X + SPRITE_WIDTH * 0.5F;

        float centerY = SPRITE_Y + SPRITE_HEIGHT * 0.5F;


        pose.translate(centerX, centerY + bounceY, 0.0F);

        pose.scale(bounceScale, bounceScale, 1.0F);

        pose.translate(-SPRITE_WIDTH * 0.5F, -SPRITE_HEIGHT * 0.5F, 0.0F);


        graphics.setColor(1.0F, 1.0F, 1.0F, alpha);


        graphics.blit(sprite, 0, 0, 0, 0, SPRITE_WIDTH, SPRITE_HEIGHT, SPRITE_WIDTH, SPRITE_HEIGHT);


        graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);


        pose.popPose();
    }


    private static void renderFrame(GuiGraphics graphics, float alpha) {
        ResourceLocation frame = parseLocation(definition.frame);

        if (frame == null) {
            return;
        }

        graphics.setColor(1.0F, 1.0F, 1.0F, alpha);


        graphics.blit(frame, 0, FRAME_Y, 0, 0, 192, 45, 192, 45);


        graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
    }


    /*
     * =========================================================
     * Text
     * =========================================================
     */

    private static void renderText(GuiGraphics graphics, Font font, float time, float alpha) {
        int maxWidth = Mth.floor(TEXT_WIDTH / TEXT_SCALE);


        List<Glyph> glyphs = layoutGlyphs(font, currentText, maxWidth);


        PoseStack pose = graphics.pose();

        pose.pushPose();

        pose.translate(TEXT_X, TEXT_Y, 10.0F);

        pose.scale(TEXT_SCALE, TEXT_SCALE, 1.0F);


        int alphaByte = Mth.clamp(Math.round(alpha * 255.0F), 0, 255);


        for (Glyph glyph : glyphs) {

            if (glyph.index >= revealedChars) {
                continue;
            }


            /*
             * Лёгкая волна каждой буквы.
             */
            int waveY = Math.round((float) Math.sin(time * 5.0F + glyph.index * 0.55F) * 0.85F);


            int rgb = getLetterColor(glyph.index, time);


            int color = (alphaByte << 24) | rgb;


            graphics.drawString(font, String.valueOf(glyph.character), glyph.x, glyph.y + waveY, color, false);
        }


        pose.popPose();
    }


    private static int getLetterColor(int index, float time) {
        if ("gold".equals(definition.text_style)) {
            return 0xFFD45A;
        }


        /*
         * Osiris:
         * каждая буква имеет свой hue,
         * плюс вся радуга очень медленно течёт.
         */
        float hue = (index * 0.095F + time * 0.055F) % 1.0F;


        return Color.HSBtoRGB(hue, 0.76F, 1.0F) & 0xFFFFFF;
    }


    /*
     * =========================================================
     * Word wrapping
     * =========================================================
     */

    private static List<Glyph> layoutGlyphs(Font font, String text, int maxWidth) {
        List<Glyph> result = new ArrayList<>();

        int x = 0;
        int y = 0;

        int i = 0;

        while (i < text.length()) {

            char character = text.charAt(i);


            if (character == '\n') {
                x = 0;
                y += LINE_HEIGHT;
                i++;
                continue;
            }


            /*
             * Space.
             */
            if (Character.isWhitespace(character)) {

                int width = font.width(String.valueOf(character));

                if (x + width <= maxWidth) {

                    result.add(new Glyph(i, character, x, y));

                    x += width;

                } else {
                    x = 0;
                    y += LINE_HEIGHT;
                }

                i++;
                continue;
            }


            /*
             * Измеряем целое слово заранее,
             * чтобы не разрывать его в середине.
             */
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
                y += LINE_HEIGHT;
            }


            for (int index = i; index < wordEnd; index++) {

                char letter = text.charAt(index);

                int width = font.width(String.valueOf(letter));


                /*
                 * Fallback для слова,
                 * которое само длиннее строки.
                 */
                if (x > 0 && x + width > maxWidth) {

                    x = 0;
                    y += LINE_HEIGHT;
                }


                result.add(new Glyph(index, letter, x, y));


                x += width;
            }


            i = wordEnd;
        }


        return result;
    }


    /*
     * =========================================================
     * Definition
     * =========================================================
     */

    private static DialogueDefinition loadDefinition(String id) {
        Minecraft minecraft = Minecraft.getInstance();


        ResourceLocation location = ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "dialogues/" + id + ".json");


        try {
            Resource resource = minecraft.getResourceManager().getResource(location).orElse(null);

            if (resource == null) {
                return null;
            }


            try (InputStreamReader reader = new InputStreamReader(resource.open(), StandardCharsets.UTF_8)) {
                return GSON.fromJson(reader, DialogueDefinition.class);
            }

        } catch (Exception exception) {
            exception.printStackTrace();
            return null;
        }
    }


    private static DialogueLine currentLine() {
        return definition.lines.get(lineIndex);
    }


    private static int getCharTicks() {
        return Math.max(1, definition.char_ticks);
    }


    private static int getCurrentHoldTicks() {
        int custom = currentLine().hold_ticks;

        if (custom > 0) {
            return custom;
        }

        return Math.max(1, definition.hold_ticks);
    }


    private static int getFadeTicks() {
        return Math.max(1, definition.fade_ticks);
    }


    private static float getGlobalAlpha(float partialTick) {
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


    private static ResourceLocation parseLocation(String value) {
        if (value == null) {
            return null;
        }

        return ResourceLocation.tryParse(value);
    }


    private record Glyph(int index, char character, int x, int y) {
    }


    private static final class DialogueDefinition {
        String voice;
        String text_style;

        float voice_pitch = 1.0F;
        float voice_volume = 0.55F;
        int voice_every = 2;

        int char_ticks = 1;
        int hold_ticks = 14;
        int fade_ticks = 8;

        float background_alpha = 0.62F;

        String frame;
        String background;

        List<DialogueLine> lines = new ArrayList<>();
    }


    private static final class DialogueLine {
        String text;
        String sprite;

        int hold_ticks;
    }
}