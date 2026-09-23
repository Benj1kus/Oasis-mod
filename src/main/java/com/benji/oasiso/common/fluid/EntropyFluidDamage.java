package com.benji.oasiso.common.fluid;

import com.benji.oasiso.Oasiso;
import com.benji.oasiso.registry.ModKarakFluids;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Oasiso.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class EntropyFluidDamage {
    private static final String TIMER = "oasiso_entropy_water_ticks";
    private static final int DAMAGE_INTERVAL = 10;
    private static final float DAMAGE = 2.0F;

    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide) return;

        if (!entity.isAlive() || entity.isInvulnerable() || (entity instanceof Player player && (player.isCreative() || player.isSpectator()))) {
            entity.getPersistentData().remove(TIMER);
            return;
        }
        if (entity.getFluidTypeHeight(ModKarakFluids.ENTROPY_WATER_TYPE.get()) <= 0.0D) {
            entity.getPersistentData().remove(TIMER);
            return;
        }
        CompoundTag data = entity.getPersistentData();
        int ticks = data.getInt(TIMER) + 1;
        if (ticks >= DAMAGE_INTERVAL) {
            ticks = 0;
            entity.hurt(entity.damageSources().magic(), DAMAGE);
        }
        data.putInt(TIMER, ticks);
    }

    private EntropyFluidDamage() {
    }
}
