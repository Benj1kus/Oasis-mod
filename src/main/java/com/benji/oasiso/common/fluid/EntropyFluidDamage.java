package com.benji.oasiso.common.fluid;

import com.benji.oasiso.Oasiso;
import com.benji.oasiso.common.entity.*;
import com.benji.oasiso.registry.ModKarakFluids;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Zombie;
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

        if ((entity instanceof Zombie || entity instanceof EntZombieEntity) && entity.isAlive() && entity instanceof net.minecraft.world.entity.Mob mob && !mob.isNoAi() && !entity.isPassenger() && entity.getFluidTypeHeight(ModKarakFluids.ENTROPY_WATER_TYPE.get()) > 0.4D) {

            var motion = entity.getDeltaMovement();

            if (motion.y < 0.10D) {
                entity.setDeltaMovement(motion.x, Math.min(0.10D, motion.y + 0.10D), motion.z);
            }
        }

        if (!entity.isAlive()
                || entity.isInvulnerable()
                || (entity instanceof Player player && (player.isCreative() || player.isSpectator()))
                || entity instanceof Creeper
                || entity instanceof Zombie
                || entity instanceof EntropyCreatureEntity
                || entity instanceof EntropyWormEntity
                || entity instanceof EntropySpiderEntity
                || entity instanceof ApollyonEntity
                || entity instanceof EntCreeperEntity
                || entity instanceof EntZombieEntity) {

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