package com.benji.oasiso.client.factory;

import com.benji.oasiso.Oasiso;
import com.benji.oasiso.common.block.entity.FactoryCoreBlockEntity;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashSet;
import java.util.Set;

import com.benji.oasiso.ModSounds;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;

@Mod.EventBusSubscriber(modid = Oasiso.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class FactoryCoreWarningHud {
    private static final ResourceLocation OVERLAY = texture("warning_core");
    private static final ResourceLocation DANGER = texture("warning_danger");
    private static final ResourceLocation MID = texture("warning_mid");
    private static final ResourceLocation SAFE = texture("warning_safe");
    private static final Set<BlockPos> KNOWN = new HashSet<>();
    private static ClientLevel world;
    private static long ticks;
    private static long safeSince;
    private static int zone = -1;

    private FactoryCoreWarningHud() {
    }

    public static void forget(BlockPos pos) {
        KNOWN.remove(pos);
        if (KNOWN.isEmpty()) zone = -1;
    }

    private static ResourceLocation texture(String name) {
        return ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "textures/gui/" + name + ".png");
    }

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        if (world != mc.level) {
            world = mc.level;
            KNOWN.clear();
            ticks = 0;
            zone = -1;
        }
        if (world == null || mc.player == null || mc.isPaused()) return;
        ticks++;
        if (ticks % 5 == 1) {
            BlockPos player = mc.player.blockPosition();
            int cx = player.getX() >> 4, cz = player.getZ() >> 4;
            for (int x = cx - 4; x <= cx + 4; x++) {
                for (int z = cz - 4; z <= cz + 4; z++) {
                    var chunk = world.getChunkSource().getChunk(x, z, ChunkStatus.FULL, false);
                    if (chunk == null) continue;
                    for (var be : chunk.getBlockEntities().values()) {
                        if (be instanceof FactoryCoreBlockEntity core && core.isArmed() && !FactoryCoreBlastClient.hasBlast(core.getBlockPos(), core.getDetonateAt()) && center(core.getBlockPos()).distanceToSqr(mc.player.position()) <= 64 * 64) {
                            KNOWN.add(core.getBlockPos().immutable());
                        }
                    }
                }
            }
        }
        KNOWN.removeIf(pos -> center(pos).distanceToSqr(mc.player.position()) > 96 * 96 || (world.hasChunkAt(pos) && (!(world.getBlockEntity(pos) instanceof FactoryCoreBlockEntity core) || !core.isArmed())));
        double nearest = Double.POSITIVE_INFINITY;
        for (BlockPos pos : KNOWN) nearest = Math.min(nearest, center(pos).distanceToSqr(mc.player.position()));
        int next = KNOWN.isEmpty() ? -1 : nearest <= 100 ? 0 : nearest <= 400 ? 1 : 2;
        if (next == 2 && zone != 2) safeSince = ticks;
        zone = next;
        boolean warningVisible = zone >= 0 && (zone != 2 || ticks - safeSince < 100);

        if (warningVisible && ticks % 32 == 16) {
            mc.getSoundManager().play(SimpleSoundInstance.forUI(ModSounds.WARNING.get(), 2.0F));
        }
    }

    private static Vec3 center(BlockPos pos) {
        return new Vec3(pos.getX() + 0.5, pos.getY() + 1.4375, pos.getZ() + 0.5);
    }

    @SubscribeEvent
    public static void hud(RenderGuiEvent.Post event) {
        if (Minecraft.getInstance().screen == null) draw(event.getGuiGraphics(), event.getPartialTick());
    }

    @SubscribeEvent
    public static void screen(ScreenEvent.Render.Post event) {
        draw(event.getGuiGraphics(), event.getPartialTick());
    }

    private static void draw(GuiGraphics graphics, float partialTick) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.level != world || mc.player == null || zone < 0) return;
        double time = ticks + (mc.isPaused() ? 0 : partialTick);
        float fade = zone == 2 ? (float) Math.max(0, Math.min(1, (100 - (time - safeSince)) / 40.0)) : 1;
        if (fade <= 0) return;
        float blink = (float) (0.5 + 0.5 * Math.cos(time * Math.PI / 16.0));
        int width = mc.getWindow().getGuiScaledWidth(), height = mc.getWindow().getGuiScaledHeight();
        graphics.flush();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();
        graphics.pose().pushPose();
        graphics.pose().translate(0, 0, 1000);
        try {
            graphics.setColor(1, 1, 1, fade * blink);
            graphics.blit(OVERLAY, 0, 0, width, height, 0, 0, 1920, 1080, 1920, 1080);
            graphics.flush();
            graphics.setColor(1, 1, 1, fade);
            graphics.blit(zone == 0 ? DANGER : zone == 1 ? MID : SAFE, (width - 16) / 2, height - 65, 0, 0, 16, 16, 16, 16);
            graphics.flush();
        } finally {
            graphics.setColor(1, 1, 1, 1);
            graphics.pose().popPose();
            RenderSystem.enableDepthTest();
            RenderSystem.disableBlend();
        }
    }
}
