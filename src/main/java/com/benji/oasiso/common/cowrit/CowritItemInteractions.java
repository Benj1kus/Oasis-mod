package com.benji.oasiso.common.cowrit;

import com.benji.oasiso.Oasiso;
import com.benji.oasiso.registry.ModItems;
import com.benji.oasiso.registry.ModKarakFluids;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.common.ToolActions;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Oasiso.MODID)
public final class CowritItemInteractions {
    @SubscribeEvent
    public static void useBlock(PlayerInteractEvent.RightClickBlock event) {
        if (fillBottle(event)) return;
        var player = event.getEntity();
        var stack = event.getItemStack();
        boolean applying = stack.is(ModItems.COWRIT_FAT.get());
        if (!applying && !stack.canPerformAction(ToolActions.AXE_WAX_OFF)) return;
        var level = event.getLevel();
        var pos = event.getPos();
        if (player.isSpectator() || !player.mayBuild() || !level.mayInteract(player, pos)) return;
        if (FatCoatings.has(level, pos) == applying) return;
        if (applying && level.getBlockState(pos).getLightEmission(level, pos) <= 0) return;
        if (level instanceof ServerLevel server) {
            FatCoatings.change(server, pos, applying);
            if (!player.getAbilities().instabuild) {
                if (applying) stack.shrink(1);
                else stack.hurtAndBreak(1, player, p -> p.broadcastBreakEvent(event.getHand()));
            }
            server.playSound(null, pos, applying ? SoundEvents.HONEYCOMB_WAX_ON : SoundEvents.AXE_WAX_OFF, SoundSource.BLOCKS, 1, 1);
            var point = event.getHitVec().getLocation().add(net.minecraft.world.phys.Vec3.atLowerCornerOf(event.getHitVec().getDirection().getNormal()).scale(0.08));
            server.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.SAND.defaultBlockState()), point.x, point.y, point.z, 14, 0.12, 0.12, 0.12, 0.06);
        }
        finish(event);
    }

    @SubscribeEvent
    public static void useAir(PlayerInteractEvent.RightClickItem event) {
        fillBottle(event);
    }

    private static boolean fillBottle(PlayerInteractEvent event) {
        if (!event.getItemStack().is(Items.GLASS_BOTTLE)) return false;
        var level = event.getLevel();
        var player = event.getEntity();
        if (player.isSpectator()) return false;
        var eyes = player.getEyePosition();
        var end = eyes.add(player.getLookAngle().scale(player.getBlockReach()));
        var hit = level.clip(new ClipContext(eyes, end, ClipContext.Block.OUTLINE, ClipContext.Fluid.ANY, player));
        if (hit.getType() != HitResult.Type.BLOCK) return false;
        var pos = hit.getBlockPos();
        var fluid = level.getFluidState(pos);
        if (!fluid.getType().isSame(ModKarakFluids.RELAX_WATER.get()) || !level.mayInteract(player, pos)) return false;
        if (!level.isClientSide) {
            var result = ItemUtils.createFilledResult(event.getItemStack(), player, new ItemStack(ModItems.RELAX_BOTTLE.get()));
            player.setItemInHand(event.getHand(), result);
            level.playSound(null, pos, SoundEvents.BUCKET_FILL, SoundSource.PLAYERS, 1, 1);
        }
        finish(event);
        return true;
    }

    private static void finish(PlayerInteractEvent event) {
        event.setCancellationResult(InteractionResult.sidedSuccess(event.getLevel().isClientSide));
        event.setCanceled(true);
    }
}
