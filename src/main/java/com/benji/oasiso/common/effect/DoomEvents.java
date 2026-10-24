package com.benji.oasiso.common.effect;

import com.benji.oasiso.Oasiso;
import com.benji.oasiso.common.entity.ApollyonEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Oasiso.MODID)
public final class DoomEvents {
    public static final double JOIN_RADIUS = 10;

    private DoomEvents() {
    }

    @SubscribeEvent
    public static void playerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)) return;
        DoomManager.maintainPlayer(player);
        if (!player.isAlive() || player.isSpectator() || player.tickCount % 10 != 0 || DoomManager.getBoundBossId(player) != null)
            return;
        ApollyonEntity nearest = null;
        double best = JOIN_RADIUS * JOIN_RADIUS;
        for (ApollyonEntity boss : player.serverLevel().getEntitiesOfClass(ApollyonEntity.class, player.getBoundingBox().inflate(JOIN_RADIUS))) {
            if (!boss.isAlive() || boss.isDissolvingDeath() || boss.inCutscene() || boss.isNoAi()) continue;
            double distance = boss.distanceToSqr(player);
            if (distance <= best) {
                best = distance;
                nearest = boss;
            }
        }
        if (nearest != null) DoomManager.bindPlayer(player, nearest);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void damage(LivingDamageEvent event) {
        if (event.getAmount() <= 0 || !(event.getEntity() instanceof ApollyonEntity boss) || !(boss.level() instanceof ServerLevel level) || boss.inCutscene() || boss.isDissolvingDeath())
            return;
        if (event.getSource().getEntity() instanceof ServerPlayer player) DoomManager.bindPlayer(player, boss);
        var random = boss.getRandom();
        for (int i = 0; i < 8; i++) {
            double x = boss.getX() + (random.nextDouble() - .5) * boss.getBbWidth();
            double y = boss.getY() + boss.getBbHeight() * (.25 + random.nextDouble() * .6);
            double z = boss.getZ() + (random.nextDouble() - .5) * boss.getBbWidth();
            level.sendParticles(Oasiso.PURPLE_HEART.get(), x, y, z, 0, (random.nextDouble() - .5) * .2, .12 + random.nextDouble() * .12, (random.nextDouble() - .5) * .2, 1);
        }
    }

    @SubscribeEvent
    public static void death(LivingDeathEvent event) {
        if (event.getEntity() instanceof ApollyonEntity boss && boss.level() instanceof ServerLevel level)
            DoomManager.releasePlayers(level.getServer(), boss.getUUID());
    }
}
