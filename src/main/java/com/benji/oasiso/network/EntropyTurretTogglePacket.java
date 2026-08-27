package com.benji.oasiso.network;

import com.benji.oasiso.common.item.EntropyChestplateItem;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class EntropyTurretTogglePacket {

    public EntropyTurretTogglePacket() {
    }

    public EntropyTurretTogglePacket(FriendlyByteBuf buffer) {
    }

    public void toBytes(FriendlyByteBuf buffer) {
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();

        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) {
                return;
            }

            ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
            if (!(chest.getItem() instanceof EntropyChestplateItem)) {
                return;
            }

            EntropyChestplateItem.toggleTurrets(chest, player.level().getGameTime());

            player.getPersistentData().putLong("EntropyTurretNextShotTick", 0L);
            player.getInventory().setChanged();
            player.containerMenu.broadcastChanges();
        });

        context.setPacketHandled(true);
        return true;
    }
}
