package com.benji.oasiso.network;

import com.benji.oasiso.ModSounds;
import com.benji.oasiso.Oasiso;
import com.benji.oasiso.registry.ModEffects;

import java.util.Optional;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

@Mod.EventBusSubscriber(modid = Oasiso.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class AirRushNetwork {
    private static final String VERSION = "1";
    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "air_rush"), () -> VERSION, VERSION::equals, VERSION::equals);
    private static final int COOLDOWN = 5;
    private static final String NEXT_JUMP = "oasiso_air_rush_next_jump";

    private record Jump() {
    }

    @SubscribeEvent
    public static void setup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            CHANNEL.messageBuilder(Jump.class, 0, NetworkDirection.PLAY_TO_SERVER).encoder((message, buffer) -> {
            }).decoder(buffer -> new Jump()).consumerMainThread((message, supplier) -> {
                ServerPlayer player = supplier.get().getSender();
                if (player != null) {
                    jump(player);
                }
            }).add();
        });
    }

    public static void requestJump() {
        CHANNEL.sendToServer(new Jump());
    }

    private static void jump(ServerPlayer player) {
        if (!player.isAlive() || player.onGround() || player.isPassenger() || player.isInFluidType() || player.isSpectator() || player.getAbilities().flying || player.isFallFlying() || !player.hasEffect(ModEffects.AIR_RUSH.get()))
            return;
        var level = player.serverLevel();
        long now = level.getGameTime();
        if (now < player.getPersistentData().getLong(NEXT_JUMP)) return;
        player.getPersistentData().putLong(NEXT_JUMP, now + COOLDOWN);
        double yaw = Math.toRadians(player.getYRot());
        player.setDeltaMovement(-Math.sin(yaw) * 0.28, 0.54, Math.cos(yaw) * 0.28);
        player.fallDistance = 0;
        player.hurtMarked = true;
        level.sendParticles(ParticleTypes.CLOUD, player.getX(), player.getY() - 0.04, player.getZ(), 12, 0.19, 0.04, 0.19, 0.035);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.LILY_JUMP.get(), SoundSource.PLAYERS, 0.85F, 0.88F + player.getRandom().nextFloat() * 0.34F);
    }
}
