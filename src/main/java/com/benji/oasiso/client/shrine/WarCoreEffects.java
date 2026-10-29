package com.benji.oasiso.client.shrine;

import com.benji.oasiso.Oasiso;
import com.benji.oasiso.ModSounds;
import com.benji.oasiso.common.item.WarCoreItem;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Oasiso.MODID, value = Dist.CLIENT)
public final class WarCoreEffects {
    public static final int FLASH_TICKS = 30;
    public static final float SHAKE_STRENGTH = 1F;
    private static ClientLevel flashWorld;
    private static int flashAge = -1;

    public static void flash() {
        flash(ModSounds.SOUL_DIE.get());
    }

    public static void flash(net.minecraft.sounds.SoundEvent sound) {
        var mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;
        flashWorld = mc.level;
        flashAge = 0;
        mc.getSoundManager().play(SimpleSoundInstance.forUI(sound, 1F, 1F));
    }

    @SubscribeEvent
    public static void logout(ClientPlayerNetworkEvent.LoggingOut event) {
        clear();
    }

    private static void clear() {
        flashWorld = null;
        flashAge = -1;
    }

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        var mc = Minecraft.getInstance();
        if (mc.level != flashWorld || mc.player == null || !mc.player.isAlive()) {
            clear();
            return;
        }
        if (!mc.isPaused() && flashAge >= 0 && ++flashAge >= FLASH_TICKS) clear();
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void camera(ViewportEvent.ComputeCameraAngles event) {
        var mc = Minecraft.getInstance();
        var p = mc.player;
        if (p == null || !p.isAlive() || !p.isUsingItem() || mc.getCameraEntity() != p || !(p.getUseItem().getItem() instanceof WarCoreItem))
            return;
        float ticks = p.getTicksUsingItem() + (float) event.getPartialTick();
        float charge = Mth.clamp(ticks / WarCoreItem.CHARGE_TICKS, 0, 1);
        float strength = charge * charge * SHAKE_STRENGTH;
        event.setYaw(event.getYaw() + strength * (float) (Math.sin(ticks * 2.17) * 1.2 + Math.sin(ticks * .83) * .6));
        event.setPitch(event.getPitch() + strength * (float) (Math.sin(ticks * 1.73 + 1) * 1.4));
        event.setRoll(event.getRoll() + strength * (float) (Math.sin(ticks * 1.31) * 2.2));
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void overlay(RenderGuiEvent.Post event) {
        var mc = Minecraft.getInstance();
        if (flashAge < 0 || mc.level != flashWorld || mc.player == null) return;
        float age = flashAge + event.getPartialTick();
        float strength;
        if (age < 2) strength = age / 2F;
        else {
            float t = Mth.clamp((age - 2) / (FLASH_TICKS - 2F), 0, 1);
            strength = 1 - t * t * (3 - 2 * t);
        }
        int alpha = (int) (Mth.clamp(strength, 0, 1) * 225);
        if (alpha <= 0) return;
        var graphics = event.getGuiGraphics();
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
                graphics.pose().translate(0, 0, 1000);
                graphics.fill(0, 0, graphics.guiWidth(), graphics.guiHeight(), alpha << 24 | 0x5FFFCC);
                graphics.flush();
            } finally {
                graphics.pose().popPose();
            }
        }
    }
}
