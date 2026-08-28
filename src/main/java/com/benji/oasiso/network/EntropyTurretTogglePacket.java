package com.benji.oasiso.network;

import com.benji.oasiso.ModSounds;
import com.benji.oasiso.common.item.EntropyChestplateItem;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
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

            boolean wasEnabled = EntropyChestplateItem.isTurretsOn(chest);

            EntropyChestplateItem.toggleTurrets(chest, player.level().getGameTime());

            boolean nowEnabled = !wasEnabled;
            playToggleSound(player, nowEnabled);

            player.getPersistentData().putLong("EntropyTurretNextShotTick", 0L);
            player.getInventory().setChanged();
            player.containerMenu.broadcastChanges();
        });

        context.setPacketHandled(true);
        return true;
    }

    private static void playToggleSound(ServerPlayer player, boolean enabled) {
        boolean easterEgg = player.getRandom().nextFloat() < 0.20F;

        SoundEvent sound;

        if (enabled) {
            sound = easterEgg ? ModSounds.ON_EA.get() : ModSounds.TURRET_MODE_ON.get();
        } else {
            sound = easterEgg ? ModSounds.OFF_EA.get() : ModSounds.TURRET_MODE_OFF.get();
        }

        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), sound, SoundSource.PLAYERS, 1.0F, 1.0F);
    }

}
