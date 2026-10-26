package com.benji.oasiso.common.effect;

import com.benji.oasiso.Oasiso;
import net.minecraft.network.protocol.game.ClientboundRemoveMobEffectPacket;
import net.minecraft.network.protocol.game.ClientboundUpdateMobEffectPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Mob;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Map;
import java.util.WeakHashMap;

/** Sends the actual effect to tracking clients, enabling both existing visual handlers. */
@Mod.EventBusSubscriber(modid = Oasiso.MODID)
public final class SadnessStunSync {
    private static final Map<Mob, Integer> LAST_DURATION = new WeakHashMap<>();

    private SadnessStunSync() {}

    private static MobEffectInstance activeEffect(Mob mob) {
        return mob.isAlive() && mob.getMaxHealth() < 200
                ? mob.getEffect(SadnessStun.INSTANCE) : null;
    }

    @SubscribeEvent
    public static void tick(LivingEvent.LivingTickEvent event) {
        if (!(event.getEntity() instanceof Mob mob)
                || !(mob.level() instanceof ServerLevel level)) return;

        MobEffectInstance effect = activeEffect(mob);
        if (effect == null) {
            if (LAST_DURATION.remove(mob) != null) {
                level.getChunkSource().broadcastAndSend(mob,
                        new ClientboundRemoveMobEffectPacket(mob.getId(), SadnessStun.INSTANCE));
            }
            return;
        }

        int duration = effect.getDuration();
        Integer previous = LAST_DURATION.put(mob, duration);
        // First application, refresh/change, plus a small periodic correction.
        // A copy keeps the packet independent of subsequent server effect ticking.
        if (previous == null || duration != previous - 1 || mob.tickCount % 20 == 0) {
            level.getChunkSource().broadcastAndSend(mob,
                    new ClientboundUpdateMobEffectPacket(mob.getId(), new MobEffectInstance(effect)));
        }
    }

    @SubscribeEvent
    public static void startTracking(PlayerEvent.StartTracking event) {
        if (!(event.getEntity() instanceof ServerPlayer player)
                || !(event.getTarget() instanceof Mob mob)) return;
        MobEffectInstance effect = activeEffect(mob);
        if (effect != null) {
            player.connection.send(new ClientboundUpdateMobEffectPacket(
                    mob.getId(), new MobEffectInstance(effect)));
        }
    }

    @SubscribeEvent
    public static void stopServer(ServerStoppedEvent event) {
        LAST_DURATION.clear();
    }
}
