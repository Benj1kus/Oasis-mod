package com.benji.oasiso.common.event;

import com.benji.oasiso.Oasiso;
import com.benji.oasiso.common.block.entity.EntropyConnectorBlockEntity;
import com.benji.oasiso.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Oasiso.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class EntropyConnectorPlacementHandler {

    private EntropyConnectorPlacementHandler() {
    }

    @SubscribeEvent
    public static void onBlockPlaced(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        BlockPos placedPos = event.getPos();
        BlockState placedState = event.getPlacedBlock();
        if (placedState.isAir()) {
            return;
        }
        if (placedState.is(ModBlocks.ENTROPY_CONNECTOR.get())) {
            tryPullAroundNewConnector(level, placedPos);
            return;
        }
        tryConnectPlacedBlockToNearestConnector(level, placedPos);
    }

    private static void tryConnectPlacedBlockToNearestConnector(ServerLevel level, BlockPos placedPos) {
        for (int distance = EntropyConnectorBlockEntity.MIN_DISTANCE; distance <= EntropyConnectorBlockEntity.MAX_DISTANCE; distance++) {

            for (Direction dir : Direction.Plane.HORIZONTAL) {
                BlockPos connectorPos = placedPos.relative(dir, distance);
                BlockState connectorState = level.getBlockState(connectorPos);
                if (!connectorState.is(ModBlocks.ENTROPY_CONNECTOR.get())) {
                    continue;
                }
                if (level.getBlockEntity(connectorPos) instanceof EntropyConnectorBlockEntity connectorBe) {
                    Direction sideFromConnectorToPlaced = dir.getOpposite();
                    if (connectorBe.tryStartPull(sideFromConnectorToPlaced, placedPos)) {
                        return;
                    }
                }
            }
        }
    }

    private static void tryPullAroundNewConnector(ServerLevel level, BlockPos connectorPos) {
        if (!(level.getBlockEntity(connectorPos) instanceof EntropyConnectorBlockEntity connectorBe)) {
            return;
        }

        for (Direction side : Direction.Plane.HORIZONTAL) {
            for (int distance = EntropyConnectorBlockEntity.MIN_DISTANCE; distance <= EntropyConnectorBlockEntity.MAX_DISTANCE; distance++) {
                BlockPos candidate = connectorPos.relative(side, distance);
                BlockState state = level.getBlockState(candidate);
                if (state.isAir()) {
                    continue;
                }
                if (connectorBe.tryStartPull(side, candidate)) {
                    break;
                }
            }
        }
    }
}