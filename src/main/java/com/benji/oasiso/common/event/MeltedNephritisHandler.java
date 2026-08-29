package com.benji.oasiso.common.event;

import com.benji.oasiso.Oasiso;
import com.benji.oasiso.common.entity.EntropyPhysicsBlockEntity;
import com.benji.oasiso.common.item.EntropyChestplateGloveItem;
import com.benji.oasiso.common.util.MeltedNephritisEffects;
import com.benji.oasiso.common.world.MeltedNephritisSavedData;
import com.benji.oasiso.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;

@Mod.EventBusSubscriber(modid = Oasiso.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class MeltedNephritisHandler {

    private MeltedNephritisHandler() {
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        ItemStack stack = event.getItemStack();
        Level level = event.getLevel();
        BlockPos pos = event.getPos();
        BlockState state = level.getBlockState(pos);

        boolean applying = stack.is(ModItems.MELTED_NEPHRITIS.get());
        boolean axe = stack.getItem() instanceof AxeItem;

        if (!applying && !axe) {
            return;
        }

        if (applying && !EntropyChestplateGloveItem.canLiftBlock(level, pos, state)) {
            return;
        }

        if (level.isClientSide) {
            if (applying) {
                event.setCanceled(true);
                event.setCancellationResult(InteractionResult.SUCCESS);
            }
            return;
        }

        if (!(level instanceof ServerLevel serverLevel) || !(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        MeltedNephritisSavedData data = MeltedNephritisSavedData.get(serverLevel);

        if (applying) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);

            boolean newlyCoated = data.add(pos);

            MeltedNephritisEffects.spawnBurst(serverLevel, Vec3.atCenterOf(pos));
            serverLevel.playSound(null, pos, SoundEvents.HONEYCOMB_WAX_ON, SoundSource.PLAYERS, 1.0F, 1.0F);

            if (newlyCoated && !player.getAbilities().instabuild) {
                stack.shrink(1);
            }

            player.swing(event.getHand(), true);
            player.getInventory().setChanged();
            player.containerMenu.broadcastChanges();
            return;
        }

        if (axe && data.isCoated(pos)) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);

            data.remove(pos);
            MeltedNephritisEffects.spawnBurst(serverLevel, Vec3.atCenterOf(pos));

            serverLevel.playSound(null, pos, SoundEvents.AXE_WAX_OFF, SoundSource.PLAYERS, 1.0F, 1.0F);

            stack.hurtAndBreak(1, player, broken -> broken.broadcastBreakEvent(event.getHand()));
        }
    }

    @SubscribeEvent
    public static void onEntityInteractSpecific(PlayerInteractEvent.EntityInteractSpecific event) {
        if (!(event.getTarget() instanceof EntropyPhysicsBlockEntity physics)) {
            return;
        }

        ItemStack stack = event.getItemStack();
        boolean glove = stack.getItem() instanceof EntropyChestplateGloveItem;
        boolean settledCoated = physics.isNephritisCoated() && physics.isSettledPhysical();
        if (glove) {
            boolean detach = event.getEntity().isShiftKeyDown() && settledCoated;

            if (!detach && !physics.canBeGrabbedWithGlove()) {
                return;
            }

            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);

            if (event.getLevel().isClientSide) {
                return;
            }

            if (!(event.getLevel() instanceof ServerLevel level) || !(event.getEntity() instanceof ServerPlayer player)) {
                return;
            }

            boolean releasingCurrent = !detach && physics.isHeldBy(player) && physics.getUUID().equals(EntropyChestplateGloveItem.getHeldBlockId(stack));

            InteractionResult result;

            if (releasingCurrent) {
                physics.releaseHolder(player);
                result = InteractionResult.CONSUME;
            } else if (detach) {
                result = physics.detachAttachedBlock(level, player, event.getHand(), stack);
            } else {
                result = physics.grabWithGlove(player, event.getHand(), stack);
            }

            event.setCancellationResult(result);
            return;
        }

        if (!settledCoated) {
            return;
        }

        boolean supported = stack.is(ModItems.MELTED_NEPHRITIS.get()) || stack.getItem() instanceof AxeItem || stack.getItem() instanceof BlockItem;

        if (!supported) {
            return;
        }

        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);

        if (event.getLevel().isClientSide) {
            return;
        }

        if (!(event.getLevel() instanceof ServerLevel level) || !(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        InteractionResult result;

        if (stack.is(ModItems.MELTED_NEPHRITIS.get())) {
            result = physics.bondCurrentStructure(level, player, stack);
        } else if (stack.getItem() instanceof AxeItem) {
            result = physics.removeNephritis(level, player, event.getHand(), stack);
        } else {
            result = physics.attachBlock(level, player, event.getHand(), stack, (BlockItem) stack.getItem(), event.getLocalPos());
        }

        event.setCancellationResult(result);
    }

    @SubscribeEvent
    public static void onBlockBroken(BlockEvent.BreakEvent event) {
        if (event.getLevel() instanceof ServerLevel level) {
            MeltedNephritisSavedData.get(level).remove(event.getPos());
        }
    }

    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.level.isClientSide || !(event.level instanceof ServerLevel level) || level.getGameTime() % 20L != 0L) {
            return;
        }

        MeltedNephritisSavedData data = MeltedNephritisSavedData.get(level);
        if (data.getPackedPositions().isEmpty()) {
            return;
        }

        List<BlockPos> stale = new ArrayList<>();
        int emitted = 0;

        for (long packed : data.getPackedPositions()) {
            BlockPos pos = BlockPos.of(packed);

            if (!level.hasChunkAt(pos)) {
                continue;
            }

            BlockState state = level.getBlockState(pos);
            if (!EntropyChestplateGloveItem.canLiftBlock(level, pos, state)) {
                stale.add(pos);
                continue;
            }

            if (emitted < 8 && level.random.nextInt(5) == 0) {
                MeltedNephritisEffects.spawnIdle(level, Vec3.atCenterOf(pos).add((level.random.nextDouble() - 0.5D) * 0.62D, (level.random.nextDouble() - 0.5D) * 0.45D, (level.random.nextDouble() - 0.5D) * 0.62D));
                emitted++;
            }
        }

        for (BlockPos pos : stale) {
            data.remove(pos);
        }
    }
}
