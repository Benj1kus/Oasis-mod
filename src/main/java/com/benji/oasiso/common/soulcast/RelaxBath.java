package com.benji.oasiso.common.soulcast;

import com.benji.oasiso.registry.ModKarakFluids;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import com.benji.oasiso.ModSounds;
import net.minecraft.sounds.SoundSource;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = "oasiso")
public final class RelaxBath {
    public static final int POSE_TICKS = 90;
    private static final int OPEN_AT = POSE_TICKS + 60;
    public static final int DURATION = OPEN_AT + 18;
    private static final int PRICE = 100;
    private static final Map<UUID, Visit> visits = new HashMap<>();

    private static final class Visit {
        int soaking;
        boolean finished;
        SoulCastNetwork.State scene;
    }

    static boolean isActive(ServerPlayer player) {
        Visit visit = visits.get(player.getUUID());
        return visit != null && visit.scene != null;
    }

    public static float closure(float age) {
        if (age < 58 || age >= DURATION) return 0;
        if (age < 66) return ease(age, 58, 66);
        if (age < 74) return 1 - ease(age, 66, 74);
        if (age < 81) return ease(age, 74, 81);
        if (age < 85) return 1 - ease(age, 81, 85);
        if (age < POSE_TICKS) return ease(age, 85, POSE_TICKS);
        if (age < OPEN_AT) return 1;
        return 1 - ease(age, OPEN_AT, DURATION);
    }

    private static float ease(float age, int from, int to) {
        float t = (age - from) / (to - from);
        return t * t * (3 - 2 * t);
    }

    @SubscribeEvent
    public static void tick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)) return;
        if (!player.isAlive() || player.isSpectator() || player.isPassenger() || player.isSleeping() || player.getFluidTypeHeight(ModKarakFluids.RELAX_WATER_TYPE.get()) <= 0) {
            leave(player);
            return;
        }
        Visit visit = visits.computeIfAbsent(player.getUUID(), ignored -> new Visit());
        if (visit.finished) return;
        long now = player.serverLevel().getGameTime();
        if (visit.scene == null) {
            if (SoulCast.isCasting(player)) {
                visit.soaking = 0;
                return;
            }
            if (++visit.soaking <= 60) return;
            visit.scene = new SoulCastNetwork.State(player.getUUID(), now, player.getYRot(), true, true);
            SoulCastNetwork.broadcast(player, visit.scene);
        } else if (now - visit.scene.start() >= DURATION) {
            visit.finished = true;
            finishScene(player, visit);
            recover(player);
        }
    }

    private static void recover(ServerPlayer player) {
        var food = player.getFoodData();
        if (player.getHealth() >= player.getMaxHealth() && food.getFoodLevel() >= 20) return;
        if (player.experienceLevel == 0 && Math.round(player.experienceProgress * player.getXpNeededForNextLevel()) < PRICE)
            return;
        player.giveExperiencePoints(-PRICE);
        player.heal(player.getMaxHealth() * .30F);
        food.setFoodLevel(Math.min(20, food.getFoodLevel() + 6));
        player.serverLevel().playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.HEAL.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
    }


    private static void finishScene(ServerPlayer player, Visit visit) {
        if (visit.scene == null) return;
        var scene = visit.scene;
        visit.scene = null;
        SoulCastNetwork.broadcast(player, new SoulCastNetwork.State(player.getUUID(), scene.start(), scene.yaw(), false, true));
    }

    private static void leave(ServerPlayer player) {
        Visit visit = visits.remove(player.getUUID());
        if (visit != null) finishScene(player, visit);
    }

    @SubscribeEvent
    public static void track(PlayerEvent.StartTracking event) {
        if (event.getTarget() instanceof ServerPlayer target && event.getEntity() instanceof ServerPlayer observer) {
            Visit visit = visits.get(target.getUUID());
            if (visit != null && visit.scene != null) SoulCastNetwork.send(observer, visit.scene);
        }
    }

    @SubscribeEvent
    public static void dimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) leave(player);
    }

    @SubscribeEvent
    public static void logout(PlayerEvent.PlayerLoggedOutEvent event) {
        visits.remove(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void respawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) leave(player);
    }

    @SubscribeEvent
    public static void stop(ServerStoppedEvent event) {
        visits.clear();
    }
}
