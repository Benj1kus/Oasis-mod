package com.benji.oasiso.common.event;

import com.benji.oasiso.Oasiso;
import com.benji.oasiso.common.block.ChaosPortalShape;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Optional;

@Mod.EventBusSubscriber(
        modid = Oasiso.MODID,
        bus = Mod.EventBusSubscriber.Bus.FORGE
)
public final class ChaosPortalIgnitionHandler {

    private ChaosPortalIgnitionHandler() {
    }

    @SubscribeEvent
    public static void onRightClickBlock(
            PlayerInteractEvent.RightClickBlock event
    ) {
        ItemStack heldStack =
                event.getEntity()
                        .getItemInHand(event.getHand());

        if (!heldStack.is(
                Oasiso.NEPHRITIS_CORE.get()
        )) {
            return;
        }

        BlockState clickedState =
                event.getLevel()
                        .getBlockState(event.getPos());


        if (!clickedState.is(
                Oasiso.ENTROPY_BLOCK.get()
        ) && !clickedState.is(
                Oasiso.KARAKOLIT_BLOCK.get()
        )) {
            return;
        }

        Optional<ChaosPortalShape> foundShape =
                ChaosPortalShape.findIgnitableFrame(
                        event.getLevel(),
                        event.getPos()
                );

        if (foundShape.isEmpty()) {
            return;
        }


        event.setCanceled(true);

        event.setCancellationResult(
                InteractionResult.sidedSuccess(
                        event.getLevel().isClientSide
                )
        );

        if (!(event.getLevel()
                instanceof ServerLevel serverLevel)) {
            return;
        }

        foundShape.get().createPortal(
                serverLevel
        );


        if (!event.getEntity()
                .getAbilities()
                .instabuild) {
            heldStack.shrink(1);
        }
    }
}