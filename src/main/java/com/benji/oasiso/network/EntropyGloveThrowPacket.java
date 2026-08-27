package com.benji.oasiso.network;

import com.benji.oasiso.common.entity.EntropyPhysicsBlockEntity;
import com.benji.oasiso.common.item.EntropyChestplateGloveItem;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class EntropyGloveThrowPacket {

    public EntropyGloveThrowPacket() {
    }

    public EntropyGloveThrowPacket(FriendlyByteBuf buffer) {
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

            ItemStack glove = EntropyChestplateGloveItem.findActiveGlove(player);
            if (glove.isEmpty()) {
                return;
            }

            if (!(player.level() instanceof ServerLevel level)) {
                return;
            }

            EntropyPhysicsBlockEntity block = EntropyChestplateGloveItem.resolveHeldBlock(level, glove);

            if (block == null) {
                EntropyChestplateGloveItem.clearHeldBlock(glove);
                player.getInventory().setChanged();
                player.containerMenu.broadcastChanges();
                return;
            }

            block.throwFrom(player);
            EntropyChestplateGloveItem.clearHeldBlock(glove);

            player.getInventory().setChanged();
            player.containerMenu.broadcastChanges();
        });

        context.setPacketHandled(true);
        return true;
    }
}
