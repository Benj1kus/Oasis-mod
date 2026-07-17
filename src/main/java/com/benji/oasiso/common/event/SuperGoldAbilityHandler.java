package com.benji.oasiso.common.event;

import com.benji.oasiso.ModSounds;
import com.benji.oasiso.Oasiso;
import com.benji.oasiso.client.renderer.SuperGoldShockwaveRenderer;
import com.benji.oasiso.common.item.SuperGoldArmorItem;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Oasiso.MODID)
public class SuperGoldAbilityHandler {

    @SubscribeEvent
    public static void onRightClickEmpty(PlayerInteractEvent.RightClickEmpty event) {
        handleAbilityTrigger(event.getEntity(), event.getHand());
    }

    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        handleAbilityTrigger(event.getEntity(), event.getHand());
    }

    private static void handleAbilityTrigger(Player player, net.minecraft.world.InteractionHand hand) {
        if (player.isCrouching() && hand == net.minecraft.world.InteractionHand.MAIN_HAND) {
            if (hasFullSuperGoldSet(player)) {

                ItemStack chestplate = player.getItemBySlot(EquipmentSlot.CHEST);
                if (!player.getCooldowns().isOnCooldown(chestplate.getItem())) {

                    if (player.level().isClientSide) {
                        SuperGoldShockwaveRenderer.spawnSandShockwave(player);

                        com.benji.oasiso.network.ModMessages.sendToServer(new com.benji.oasiso.network.SuperGoldShockwavePacket());
                    } else {
                        executeServerShockwave(player);
                    }

                    player.getCooldowns().addCooldown(chestplate.getItem(), 200);
                }
            }
        }
    }

    public static boolean hasFullSuperGoldSet(Player player) {
        for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            if (!(player.getItemBySlot(slot).getItem() instanceof SuperGoldArmorItem)) {
                return false;
            }
        }
        return true;
    }

    public static void executeServerShockwave(Player player) {
        if (player.level() instanceof ServerLevel sl) {

            sl.playSound(null, player.blockPosition(), ModSounds.TITANA_STEP.get(), SoundSource.PLAYERS, 30.0F, 1.8F);

            double px = player.getX();
            double py = player.getY() + 0.1;
            double pz = player.getZ();

            for (int i = 0; i < 360; i += 10) {
                double rad = Math.toRadians(i);
                double dx = Math.cos(rad) * 4.5;
                double dz = Math.sin(rad) * 4.5;

                sl.sendParticles(
                        new BlockParticleOption(ParticleTypes.FALLING_DUST, Blocks.SAND.defaultBlockState()),
                        px + dx, py, pz + dz, 2, dx * 0.1, 0, dz * 0.1, 0.05
                );
            }

            for (LivingEntity target : sl.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(5.0D))) {
                if (target != player) {
                    target.setDeltaMovement(target.getDeltaMovement().add(0, 3.0D, 0));
                    target.hasImpulse = true;
                    target.hurtMarked = true;

                    target.hurt(sl.damageSources().playerAttack(player), 30.0F);
                }
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(net.minecraftforge.event.TickEvent.PlayerTickEvent event) {
        if (event.phase == net.minecraftforge.event.TickEvent.Phase.END && event.player.level().isClientSide) {
            Player player = event.player;

            if (hasFullSuperGoldSet(player)) {
                net.minecraft.world.item.Item chestItem = player.getItemBySlot(EquipmentSlot.CHEST).getItem();

                boolean isCurrentlyOnCooldown = player.getCooldowns().isOnCooldown(chestItem);
                boolean wasOnCooldown = player.getPersistentData().getBoolean("SuperGoldAbilityCooldown");

                if (wasOnCooldown && !isCurrentlyOnCooldown) {
                    player.playSound(ModSounds.DASHER3.get(), 1.0F, 1.0F);
                    SuperGoldShockwaveRenderer.spawnSmallSandShockwave(player);
                }

                if (isCurrentlyOnCooldown != wasOnCooldown) {
                    player.getPersistentData().putBoolean("SuperGoldAbilityCooldown", isCurrentlyOnCooldown);
                }
            } else {
                if (player.getPersistentData().getBoolean("SuperGoldAbilityCooldown")) {
                    player.getPersistentData().putBoolean("SuperGoldAbilityCooldown", false);
                }
            }
        }
    }
}