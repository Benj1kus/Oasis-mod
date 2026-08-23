package com.benji.oasiso.common.event;

import com.benji.oasiso.Oasiso;
import com.benji.oasiso.common.entity.AzumaalEntity;
import com.benji.oasiso.common.entity.ChaosBombEntity;
import com.benji.oasiso.common.entity.ai.AzumaalDamageScaler;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.EventPriority;

@Mod.EventBusSubscriber(modid = Oasiso.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class AzumaalAdaptiveDamageEvents {

    private AzumaalAdaptiveDamageEvents() {
    }


    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onLivingHurt(LivingHurtEvent event) {

        if (!(event.getEntity() instanceof ServerPlayer player)) {

            return;
        }

        ServerLevel level = player.serverLevel();
        DamageSource source = event.getSource();
        AzumaalEntity boss = resolveAzumaalSource(level, source);

        if (boss == null || !boss.isAlive()) {

            return;
        }


        float scaledDamage = AzumaalDamageScaler.scaleDamage(player, source, event.getAmount());


        event.setAmount(scaledDamage);
    }


    private static AzumaalEntity resolveAzumaalSource(ServerLevel level, DamageSource source) {
        Entity attacker = source.getEntity();

        Entity direct = source.getDirectEntity();

        if (attacker instanceof AzumaalEntity attackerBoss) {
            return attackerBoss;
        }


        if (direct instanceof AzumaalEntity directBoss) {
            return directBoss;
        }

        if (attacker instanceof ChaosBombEntity bomb) {
            AzumaalEntity owner = bomb.getAzumaalOwner(level);
            if (owner != null) {
                return owner;
            }
        }

        if (direct instanceof ChaosBombEntity bomb) {
            return bomb.getAzumaalOwner(level);
        }


        return null;
    }
}