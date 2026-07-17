package com.benji.oasiso.network;

import com.benji.oasiso.common.event.SuperGoldAbilityHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class SuperGoldShockwavePacket {

    public SuperGoldShockwavePacket() {
        // Пустой конструктор для создания пакета на клиенте
    }

    public SuperGoldShockwavePacket(FriendlyByteBuf buf) {
        // Чтение из буфера (нам читать нечего, пакет пустой)
    }

    public void toBytes(FriendlyByteBuf buf) {
        // Запись в буфер (нам писать нечего)
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        // ВАЖНО: Весь код на сервере должен выполняться в основном потоке!
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null) {
                // Вызываем нашу логику урона и подбрасывания
                SuperGoldAbilityHandler.executeServerShockwave(player);
            }
        });
        return true;
    }
}