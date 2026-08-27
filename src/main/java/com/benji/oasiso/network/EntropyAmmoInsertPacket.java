package com.benji.oasiso.network;

import com.benji.oasiso.common.item.EntropyChestplateItem;
import com.benji.oasiso.common.util.EntropyAmmoStorage;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class EntropyAmmoInsertPacket {

    private final int chestInventorySlot;
    private final int sourceInventorySlot;
    private final ItemStack ammoTemplate;

    public EntropyAmmoInsertPacket(int chestInventorySlot, int sourceInventorySlot, ItemStack ammoTemplate) {
        this.chestInventorySlot = chestInventorySlot;
        this.sourceInventorySlot = sourceInventorySlot;
        this.ammoTemplate = ammoTemplate.copyWithCount(1);
    }

    public EntropyAmmoInsertPacket(FriendlyByteBuf buffer) {
        this.chestInventorySlot = buffer.readVarInt();
        this.sourceInventorySlot = buffer.readInt();
        this.ammoTemplate = buffer.readItem();
    }

    public void toBytes(FriendlyByteBuf buffer) {
        buffer.writeVarInt(this.chestInventorySlot);
        buffer.writeInt(this.sourceInventorySlot);
        buffer.writeItem(this.ammoTemplate);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();

        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) {
                return;
            }

            ItemStack chest = resolveChestplate(player, this.chestInventorySlot);
            if (chest.isEmpty()) {
                return;
            }

            ItemStack source = resolveSource(player);
            if (source.isEmpty() || !EntropyAmmoStorage.isAllowedAmmo(source)) {
                return;
            }

            if (EntropyAmmoStorage.insert(chest, source) <= 0) {
                return;
            }

            if (this.sourceInventorySlot == -1) {
                player.containerMenu.setCarried(source.isEmpty() ? ItemStack.EMPTY : source);
            }

            player.getInventory().setChanged();
            player.containerMenu.broadcastChanges();
        });

        context.setPacketHandled(true);
        return true;
    }

    private ItemStack resolveSource(ServerPlayer player) {
        if (this.sourceInventorySlot == -1) {
            ItemStack carried = player.containerMenu.getCarried();
            if (isMatchingAllowedAmmo(carried)) {
                return carried;
            }
        } else if (isValidInventorySlot(player, this.sourceInventorySlot)) {
            ItemStack direct = player.getInventory().getItem(this.sourceInventorySlot);
            if (isMatchingAllowedAmmo(direct)) {
                return direct;
            }
        }

        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (isMatchingAllowedAmmo(stack)) {
                return stack;
            }
        }

        return ItemStack.EMPTY;
    }

    private boolean isMatchingAllowedAmmo(ItemStack stack) {
        return !stack.isEmpty() && EntropyAmmoStorage.isAllowedAmmo(stack) && !this.ammoTemplate.isEmpty() && ItemStack.isSameItemSameTags(stack, this.ammoTemplate);
    }

    private static ItemStack resolveChestplate(ServerPlayer player, int requestedSlot) {
        if (isValidInventorySlot(player, requestedSlot)) {
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

    private static boolean isValidInventorySlot(ServerPlayer player, int slot) {
        return slot >= 0 && slot < player.getInventory().getContainerSize();
    }
}
