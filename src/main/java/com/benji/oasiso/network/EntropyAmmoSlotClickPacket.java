package com.benji.oasiso.network;

import com.benji.oasiso.common.item.EntropyChestplateItem;
import com.benji.oasiso.common.util.EntropyAmmoStorage;
import net.minecraft.core.NonNullList;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class EntropyAmmoSlotClickPacket {

    private static final int LEFT_BUTTON = 0;
    private static final int RIGHT_BUTTON = 1;

    private final int chestInventorySlot;
    private final int ammoSlot;
    private final int mouseButton;
    private final boolean shiftDown;

    public EntropyAmmoSlotClickPacket(int chestInventorySlot, int ammoSlot, int mouseButton, boolean shiftDown) {
        this.chestInventorySlot = chestInventorySlot;
        this.ammoSlot = ammoSlot;
        this.mouseButton = mouseButton;
        this.shiftDown = shiftDown;
    }

    public EntropyAmmoSlotClickPacket(FriendlyByteBuf buffer) {
        this.chestInventorySlot = buffer.readVarInt();
        this.ammoSlot = buffer.readVarInt();
        this.mouseButton = buffer.readVarInt();
        this.shiftDown = buffer.readBoolean();
    }

    public void toBytes(FriendlyByteBuf buffer) {
        buffer.writeVarInt(this.chestInventorySlot);
        buffer.writeVarInt(this.ammoSlot);
        buffer.writeVarInt(this.mouseButton);
        buffer.writeBoolean(this.shiftDown);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();

        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null || this.ammoSlot < 0 || this.ammoSlot >= EntropyAmmoStorage.SLOT_COUNT || (this.mouseButton != LEFT_BUTTON && this.mouseButton != RIGHT_BUTTON)) {
                return;
            }

            ItemStack chest = resolveChestplate(player, this.chestInventorySlot);
            if (chest.isEmpty()) {
                return;
            }

            NonNullList<ItemStack> items = EntropyAmmoStorage.getItems(chest);
            ItemStack internal = items.get(this.ammoSlot);

            if (this.shiftDown) {
                if (internal.isEmpty()) {
                    return;
                }

                ItemStack extracted = internal.copy();
                items.set(this.ammoSlot, ItemStack.EMPTY);
                EntropyAmmoStorage.setItems(chest, items);

                if (!player.getInventory().add(extracted)) {
                    player.drop(extracted, false);
                }

                sync(player);
                return;
            }

            ItemStack carried = player.containerMenu.getCarried();
            boolean rightClick = this.mouseButton == RIGHT_BUTTON;

            if (carried.isEmpty()) {
                if (internal.isEmpty()) {
                    return;
                }

                int amount = rightClick ? (internal.getCount() + 1) / 2 : internal.getCount();

                ItemStack pickedUp = internal.copy();
                pickedUp.setCount(amount);

                internal.shrink(amount);
                if (internal.isEmpty()) {
                    items.set(this.ammoSlot, ItemStack.EMPTY);
                }

                EntropyAmmoStorage.setItems(chest, items);
                player.containerMenu.setCarried(pickedUp);
                sync(player);
                return;
            }

            if (!EntropyAmmoStorage.isAllowedAmmo(carried)) {
                return;
            }

            if (internal.isEmpty()) {
                int amount = rightClick ? 1 : carried.getCount();

                ItemStack placed = carried.copy();
                placed.setCount(amount);
                items.set(this.ammoSlot, placed);

                carried.shrink(amount);
                EntropyAmmoStorage.setItems(chest, items);
                player.containerMenu.setCarried(carried.isEmpty() ? ItemStack.EMPTY : carried);
                sync(player);
                return;
            }

            if (ItemStack.isSameItemSameTags(internal, carried)) {
                int maxStack = Math.min(internal.getMaxStackSize(), carried.getMaxStackSize());
                int room = maxStack - internal.getCount();

                if (room <= 0) {
                    return;
                }

                int amount = rightClick ? Math.min(1, room) : Math.min(carried.getCount(), room);

                internal.grow(amount);
                carried.shrink(amount);

                EntropyAmmoStorage.setItems(chest, items);
                player.containerMenu.setCarried(carried.isEmpty() ? ItemStack.EMPTY : carried);
                sync(player);
                return;
            }

            if (!rightClick && EntropyAmmoStorage.isAllowedAmmo(internal)) {
                ItemStack oldInternal = internal.copy();
                ItemStack newInternal = carried.copy();

                items.set(this.ammoSlot, newInternal);
                EntropyAmmoStorage.setItems(chest, items);
                player.containerMenu.setCarried(oldInternal);
                sync(player);
            }
        });

        context.setPacketHandled(true);
        return true;
    }

    private static void sync(ServerPlayer player) {
        player.getInventory().setChanged();
        player.containerMenu.broadcastChanges();
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
