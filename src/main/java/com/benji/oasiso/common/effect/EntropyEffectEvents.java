package com.benji.oasiso.common.effect;

import com.benji.oasiso.Oasiso;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Oasiso.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class EntropyEffectEvents {

    private static final String DAMAGE_LEVEL_TAG = Oasiso.MODID + ":entropy_damage_level";
    private static final String LAST_DAMAGE_TICK_TAG = Oasiso.MODID + ":entropy_last_damage_tick";

    private EntropyEffectEvents() {
    }

    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        LivingEntity entity = event.getEntity();

        if (entity.level().isClientSide) {
            return;
        }

        CompoundTag persistentData = entity.getPersistentData();

        if (!entity.hasEffect(Oasiso.ENTROPY_EFFECT.get())) {
            resetEntropyDamage(persistentData);
            return;
        }

        long currentGameTime = entity.level().getGameTime();

        // timer
        if (!persistentData.contains(LAST_DAMAGE_TICK_TAG, Tag.TAG_LONG)) {
            persistentData.putLong(LAST_DAMAGE_TICK_TAG, currentGameTime);
            persistentData.putInt(DAMAGE_LEVEL_TAG, 0);
            return;
        }

        long lastDamageTime = persistentData.getLong(LAST_DAMAGE_TICK_TAG);

        if (currentGameTime < lastDamageTime) {
            persistentData.putLong(LAST_DAMAGE_TICK_TAG, currentGameTime);
            persistentData.putInt(DAMAGE_LEVEL_TAG, 0);
            return;
        }

        if (currentGameTime - lastDamageTime < 20L) {
            return;
        }

        int nextDamage = persistentData.getInt(DAMAGE_LEVEL_TAG) + 1;

        persistentData.putInt(DAMAGE_LEVEL_TAG, nextDamage);
        persistentData.putLong(LAST_DAMAGE_TICK_TAG, currentGameTime);

        entity.hurt(entity.damageSources().magic(), nextDamage);
    }

    private static void resetEntropyDamage(CompoundTag data) {
        data.remove(DAMAGE_LEVEL_TAG);
        data.remove(LAST_DAMAGE_TICK_TAG);
    }
}