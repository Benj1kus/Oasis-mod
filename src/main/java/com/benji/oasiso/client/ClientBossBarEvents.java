package com.benji.oasiso.client;

import com.benji.oasiso.Oasiso;
import com.benji.oasiso.common.entity.AzumaalEntity;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.CustomizeGuiOverlayEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Oasiso.MODID, value = Dist.CLIENT)
public final class ClientBossBarEvents {

    private static final ResourceLocation FRAME_TEXTURE = ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "textures/gui/" + "azumaal_frame.png");
    private static final ResourceLocation PROGRESS_TEXTURE = ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "textures/gui/" + "azumaal_progress.png");
    private static final ResourceLocation FACE_TEXTURE = ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "textures/gui/" + "azumaal_frame_face.png");

    private static final ResourceLocation FRAME_STAGE_TWO_TEXTURE = ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "textures/gui/azumaal_frame_stage2.png");
    private static final ResourceLocation FACE_STAGE_TWO_TEXTURE = ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "textures/gui/azumaal_frame_face_stage2.png");

    private static final int FRAME_WIDTH = 186;
    private static final int FRAME_HEIGHT = 42;


    private static final int PROGRESS_WIDTH = 182;
    private static final int PROGRESS_HEIGHT = 5;

    private static final int PROGRESS_X_OFFSET = 2;
    private static final int PROGRESS_Y_OFFSET = 18;

    private static final int BOSS_BAR_SPACING = 5;


    private ClientBossBarEvents() {
    }
    @SubscribeEvent
    public static void onRenderBossBar(CustomizeGuiOverlayEvent.BossEventProgress event) {
        Component name = event.getBossEvent().getName();

        boolean stageTwo = isAzumaalStageTwoBossBar(name);

        if (!isAzumaalBossBar(name)) {

            return;
        }

        event.setCanceled(true);

        GuiGraphics graphics = event.getGuiGraphics();


        int screenWidth = graphics.guiWidth();

        int frameX = screenWidth / 2 - FRAME_WIDTH / 2;
        int frameY = event.getY();


        ResourceLocation frameTexture = stageTwo ? FRAME_STAGE_TWO_TEXTURE : FRAME_TEXTURE;
        ResourceLocation faceTexture = stageTwo ? FACE_STAGE_TWO_TEXTURE : FACE_TEXTURE;

        graphics.blit(frameTexture, frameX, frameY, 0, 0, FRAME_WIDTH, FRAME_HEIGHT, FRAME_WIDTH, FRAME_HEIGHT);

        float progress = Mth.clamp(event.getBossEvent().getProgress(), 0.0F, 1.0F);
        int currentProgressWidth = Mth.floor(PROGRESS_WIDTH * progress);

        if (currentProgressWidth > 0) {
            graphics.blit(PROGRESS_TEXTURE, frameX + PROGRESS_X_OFFSET, frameY + PROGRESS_Y_OFFSET, 0, 0, currentProgressWidth, PROGRESS_HEIGHT, PROGRESS_WIDTH, PROGRESS_HEIGHT);
        }
        graphics.blit(faceTexture, frameX, frameY, 0, 0, FRAME_WIDTH, FRAME_HEIGHT, FRAME_WIDTH, FRAME_HEIGHT);
        event.setIncrement(FRAME_HEIGHT + BOSS_BAR_SPACING);
    }


    private static boolean isAzumaalBossBar(Component name) {
        if (name.getContents() instanceof TranslatableContents translatable) {
            String key = translatable.getKey();
            return key.equals("entity.oasiso.azumaal") || key.equals(AzumaalEntity.BOSS_BAR_STAGE_TWO_KEY);
        }
        return name.getString().equalsIgnoreCase("Azumaal");
    }

    private static boolean isAzumaalStageTwoBossBar(Component name) {
        return name.getContents() instanceof TranslatableContents translatable && translatable.getKey().equals(AzumaalEntity.BOSS_BAR_STAGE_TWO_KEY);
    }
}