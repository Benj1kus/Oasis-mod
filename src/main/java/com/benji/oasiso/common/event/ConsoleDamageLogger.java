package com.benji.oasiso.common.event;

import com.benji.oasiso.Oasiso;
import com.mojang.logging.LogUtils;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

@Mod.EventBusSubscriber(modid = Oasiso.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ConsoleDamageLogger {

    private static final Logger LOGGER = LogUtils.getLogger();

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        if (event.getEntity() instanceof Player player && !player.level().isClientSide()) {

            Entity attacker = event.getSource().getEntity();
            String attackerName = attacker != null ? attacker.getName().getString() : "Environment";

            float rawDamage = event.getAmount();

            LOGGER.info("[RAW DAMAGE] Игрок {} получил {} сырого урона от {}",
                    player.getName().getString(),
                    rawDamage,
                    attackerName);
        }
    }
}