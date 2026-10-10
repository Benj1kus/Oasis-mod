package com.benji.oasiso.common.geyser;

import com.benji.oasiso.Oasiso;
import com.benji.oasiso.common.block.GeyserSmallBlock;
import com.benji.oasiso.registry.ModEffects;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Oasiso.MODID)
public final class SmallGeyserExposure {
    public static final int RANGE = 3, WAIT_TICKS = 60, EFFECT_TICKS = 60;
    private static final String SINCE = "OasisoSmallGeyserSince";

    @SubscribeEvent
    public static void tick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer p)) return;
        var data = p.getPersistentData();
        if (!p.isAlive() || p.isSpectator() || p.isCreative()) {
            data.remove(SINCE);
            return;
        }
        if (p.tickCount % 10 != 0) return;
        long now = p.level().getGameTime();
        if (!nearGeyser(p)) {
            data.remove(SINCE);
            return;
        }
        if (!data.contains(SINCE) || data.getLong(SINCE) > now) {
            data.putLong(SINCE, now);
            return;
        }
        if (now - data.getLong(SINCE) >= WAIT_TICKS && !p.hasEffect(ModEffects.ENTROPY_EFFECT.get()))
            p.addEffect(new MobEffectInstance(ModEffects.ENTROPY_EFFECT.get(), EFFECT_TICKS, 0));
    }

    private static boolean nearGeyser(ServerPlayer player) {
        var level = player.serverLevel();
        BlockPos origin = player.blockPosition();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int y = -RANGE - 1; y <= RANGE; y++)
            for (int z = -RANGE; z <= RANGE; z++)
                for (int x = -RANGE; x <= RANGE; x++) {
                    pos.set(origin.getX() + x, origin.getY() + y, origin.getZ() + z);
                    if (player.distanceToSqr(pos.getX() + .5, pos.getY() + 13.0 / 16.0, pos.getZ() + .5) > RANGE * RANGE)
                        continue;
                    if (level.hasChunkAt(pos) && level.getBlockState(pos).getBlock() instanceof GeyserSmallBlock && GeyserSmallBlock.open(level, pos))
                        return true;
                }
        return false;
    }

    @SubscribeEvent
    public static void login(PlayerEvent.PlayerLoggedInEvent e) {
        e.getEntity().getPersistentData().remove(SINCE);
    }

    @SubscribeEvent
    public static void logout(PlayerEvent.PlayerLoggedOutEvent e) {
        e.getEntity().getPersistentData().remove(SINCE);
    }

    @SubscribeEvent
    public static void dimension(PlayerEvent.PlayerChangedDimensionEvent e) {
        e.getEntity().getPersistentData().remove(SINCE);
    }

    @SubscribeEvent
    public static void clone(PlayerEvent.Clone e) {
        e.getEntity().getPersistentData().remove(SINCE);
    }

    private SmallGeyserExposure() {
    }
}
