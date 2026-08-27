package com.benji.oasiso.client.event;

import com.benji.oasiso.Oasiso;
import com.benji.oasiso.client.key.ModKeyMappings;
import com.benji.oasiso.common.item.EntropyChestplateItem;
import com.benji.oasiso.network.EntropyTurretTogglePacket;
import com.benji.oasiso.network.ModMessages;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Oasiso.MODID, value = Dist.CLIENT)
public final class EntropyChestplateClientHandler {

    private EntropyChestplateClientHandler() {
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;

        if (player == null || minecraft.screen != null) {
            return;
        }

        while (ModKeyMappings.TURRET_MODE.consumeClick()) {
            ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
            if (!(chest.getItem() instanceof EntropyChestplateItem)) {
                continue;
            }
            EntropyChestplateItem.toggleTurrets(chest, player.level().getGameTime());

            player.getPersistentData().putLong("EntropyTurretNextShotTick", 0L);
            ModMessages.sendToServer(new EntropyTurretTogglePacket());
        }
    }
}
