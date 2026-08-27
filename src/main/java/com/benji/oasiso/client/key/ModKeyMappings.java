package com.benji.oasiso.client.key;

import com.benji.oasiso.Oasiso;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(modid = Oasiso.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class ModKeyMappings {

    public static final KeyMapping TURRET_MODE = new KeyMapping("key.oasiso.turret_mode", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_G, "key.categories.oasiso");

    private ModKeyMappings() {
    }

    @SubscribeEvent
    public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(TURRET_MODE);
    }
}
