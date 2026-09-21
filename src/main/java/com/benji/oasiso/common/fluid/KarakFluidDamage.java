package com.benji.oasiso.common.fluid;

import com.benji.oasiso.Oasiso;
import com.benji.oasiso.registry.ModKarakFluids;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Oasiso.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class KarakFluidDamage {
    private static final String TIMER = "oasiso_kr_water_ticks";
    private static final int DAMAGE_INTERVAL = 40;
    private static final float DAMAGE = 4.0F;

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide) return;
        Player player = event.player;
        CompoundTag data = player.getPersistentData();

        if (!player.isAlive() || player.isCreative() || player.isSpectator() || player.getFluidTypeHeight(ModKarakFluids.KR_WATER_TYPE.get()) <= 0.0D) {
            data.remove(TIMER);
            return;
        }

        int ticks = data.getInt(TIMER) + 1;
        if (ticks >= DAMAGE_INTERVAL) {
            ticks = 0;
            player.hurt(player.damageSources().magic(), DAMAGE);
        }
        data.putInt(TIMER, ticks);
    }

    private KarakFluidDamage() {
    }
}
