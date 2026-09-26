package com.benji.oasiso.client.entity;

import com.benji.oasiso.Oasiso;
import com.benji.oasiso.common.entity.ApollyonEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.MovementInputUpdateEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Клиент гасит ввод сразу, сервер отдельно проверяет фактическое положение. */
@Mod.EventBusSubscriber(modid = Oasiso.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ApollyonRestraintClient {
    private static boolean held(Player player) {
        if (player != Minecraft.getInstance().player || !player.isAlive()
                || player.isSpectator() || player.isCreative()) return false;
        return !player.level().getEntitiesOfClass(ApollyonEntity.class,
                player.getBoundingBox().inflate(32),
                boss -> boss.isRestrainingPlayer(player.getId())).isEmpty();
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void input(MovementInputUpdateEvent event) {
        if (!held(event.getEntity())) return;
        var input = event.getInput();
        input.forwardImpulse = 0;
        input.leftImpulse = 0;
        input.up = input.down = input.left = input.right = false;
        input.jumping = false;
        input.shiftKeyDown = false;
        event.getEntity().setSprinting(false);
    }

    @SubscribeEvent
    public static void tick(TickEvent.PlayerTickEvent event) {
        // Обе фазы: убираем остаточный импульс и накопление падения/прыжка.
        if (!event.player.level().isClientSide || !held(event.player)) return;
        event.player.setDeltaMovement(Vec3.ZERO);
        event.player.fallDistance = 0;
        event.player.setSprinting(false);
    }

    private ApollyonRestraintClient() {}
}
