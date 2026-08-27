package com.benji.oasiso.network;

import com.benji.oasiso.common.item.EntropyChestplateItem;
import com.benji.oasiso.common.util.EntropyAmmoStorage;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class EntropyAmmoExtractPacket {

    private final int chestInventorySlot;
    private final int ammoSlot;

    public EntropyAmmoExtractPacket(int chestInventorySlot, int ammoSlot) {
        this.chestInventorySlot = chestInventorySlot;
        this.ammoSlot = ammoSlot;
    }

    public EntropyAmmoExtractPacket(FriendlyByteBuf buffer) {
        this.chestInventorySlot = buffer.readVarInt();
        this.ammoSlot = buffer.readVarInt();
    }

    public void toBytes(FriendlyByteBuf buffer) {
        buffer.writeVarInt(this.chestInventorySlot);
        buffer.writeVarInt(this.ammoSlot);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();

        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null || this.ammoSlot < 0 || this.ammoSlot >= EntropyAmmoStorage.SLOT_COUNT) {
                return;
            }

            ItemStack chest = resolveChestplate(player, this.chestInventorySlot);
            if (chest.isEmpty()) {
                return;
            }

            ItemStack extracted = EntropyAmmoStorage.extract(chest, this.ammoSlot);
            if (extracted.isEmpty()) {
                return;
            }

            if (!player.getInventory().add(extracted)) {
                player.drop(extracted, false);
            }

            player.getInventory().setChanged();
            player.containerMenu.broadcastChanges();
        });

        context.setPacketHandled(true);
        return true;
    }

    private static ItemStack resolveChestplate(ServerPlayer player, int requestedSlot) {
        if (requestedSlot >= 0 && requestedSlot < player.getInventory().getContainerSize()) {
            ItemStack direct = player.getInventory().getItem(requestedSlot);
            if (direct.getItem() instanceof EntropyChestplateItem) {
                return direct;
            }
        }

        ItemStack equipped = player.getItemBySlot(EquipmentSlot.CHEST);
        if (equipped.getItem() instanceof EntropyChestplateItem) {
            return equipped;
        }

        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.getItem() instanceof EntropyChestplateItem) {
                return stack;
            }
        }

        return ItemStack.EMPTY;
    }
}
