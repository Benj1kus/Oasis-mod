package com.benji.oasiso.common.factory;

import com.benji.oasiso.Oasiso;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import com.benji.oasiso.ModSounds;

import java.util.*;

@Mod.EventBusSubscriber(modid = Oasiso.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class FactoryCoreDetonation {
    private record Echo(BlockPos pos, long tick) {
    }

    private static final Map<ServerLevel, List<Echo>> ECHOES = new IdentityHashMap<>();

    public static Vec3 center(BlockPos pos) {
        return new Vec3(pos.getX() + .5, pos.getY() + 1.4375, pos.getZ() + .5);
    }

    public static void explode(ServerLevel level, BlockPos pos) {
        Vec3 center = center(pos);
        Echo echo = new Echo(pos.immutable(), level.getGameTime());
        ECHOES.computeIfAbsent(level, l -> new ArrayList<>()).add(echo);
        send(level, echo);
        level.removeBlock(pos, false);
        level.playSound(null, center.x, center.y, center.z, ModSounds.ENTROPY_EXPLODE.get(), SoundSource.BLOCKS, 6.875F, 1.0F);

        var damage = level.damageSources().explosion(null, null);
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, new AABB(center, center).inflate(20), e -> e.isAlive() && !e.isSpectator())) {
            float amount = FactoryCoreBlastRules.damage(target.position().distanceToSqr(center));
            if (amount > 0) target.hurt(damage, amount);
        }
    }

    private static void send(ServerLevel level, Echo echo) {
        Vec3 center = center(echo.pos);
        for (var player : level.players())
            if (player.position().distanceToSqr(center) <= FactoryCoreBlastRules.VIEW_RADIUS * FactoryCoreBlastRules.VIEW_RADIUS)
                FactoryCoreBlastNetwork.send(player, echo.pos, echo.tick);
    }

    @SubscribeEvent
    public static void tick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.level instanceof ServerLevel level)) return;
        List<Echo> echoes = ECHOES.get(level);
        if (echoes == null) return;
        echoes.removeIf(e -> level.getGameTime() - e.tick >= FactoryCoreBlastRules.EFFECT_TICKS);
        if (level.getGameTime() % 20 == 0) for (Echo echo : echoes) send(level, echo);
        if (echoes.isEmpty()) ECHOES.remove(level);
    }

    @SubscribeEvent
    public static void unload(LevelEvent.Unload event) {
        if (event.getLevel() instanceof ServerLevel level) ECHOES.remove(level);
    }

    private FactoryCoreDetonation() {
    }
}
