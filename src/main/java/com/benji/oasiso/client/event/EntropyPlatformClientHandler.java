package com.benji.oasiso.client.event;

import com.benji.oasiso.Oasiso;
import com.benji.oasiso.common.entity.EntropyPhysicsBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

@Mod.EventBusSubscriber(modid = Oasiso.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class EntropyPlatformClientHandler {

    private static final double PLATFORM_SEARCH_RADIUS = 36.0D;
    private EntropyPlatformClientHandler() {
    }

    @SubscribeEvent
    public static void clientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {

            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null || minecraft.isPaused()) {

            return;
        }

        LocalPlayer player = minecraft.player;
        if (player.getDeltaMovement().y > 0.12D) {
            return;
        }

        AABB searchBox = player.getBoundingBox().inflate(PLATFORM_SEARCH_RADIUS, 2.0D, PLATFORM_SEARCH_RADIUS);
        List<EntropyPhysicsBlockEntity> platforms = minecraft.level.getEntitiesOfClass(EntropyPhysicsBlockEntity.class, searchBox, EntropyPhysicsBlockEntity::isPlatformMode);
        EntropyPhysicsBlockEntity bestPlatform = null;

        double bestTop = Double.NaN;

        for (EntropyPhysicsBlockEntity platform : platforms) {

            double top = platform.getPlatformTopUnderPlayer(player);

            if (Double.isNaN(top)) {
                continue;
            }
            if (bestPlatform == null || top > bestTop) {
                bestPlatform = platform;
                bestTop = top;
            }
        }

        if (bestPlatform == null) {
            return;
        }

        stabilizeLocalPlayer(player, bestTop);
    }

    private static void stabilizeLocalPlayer(LocalPlayer player, double platformTop) {
        if (player.getY() < platformTop) {
            player.setPos(player.getX(), platformTop, player.getZ());
        }

        Vec3 velocity = player.getDeltaMovement();

        if (velocity.y < 0.0D) {
            player.setDeltaMovement(velocity.x, 0.0D, velocity.z);
        }

        player.setOnGround(true);
        player.fallDistance = 0.0F;
    }
}