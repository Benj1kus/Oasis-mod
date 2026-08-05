package com.benji.oasiso.common.block;

import com.benji.oasiso.Oasiso;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Optional;

public final class ChaosPortalShape {

    private static final int FRAME_SIZE = 4;


    private static final Direction[] FRAME_DIRECTIONS = {
            Direction.EAST,
            Direction.SOUTH
    };

    private final BlockPos origin;
    private final Direction horizontalDirection;

    private ChaosPortalShape(
            BlockPos origin,
            Direction horizontalDirection
    ) {

        this.origin = origin.immutable();
        this.horizontalDirection =
                horizontalDirection;
    }

    public static Optional<ChaosPortalShape>
    findIgnitableFrame(
            LevelAccessor level,
            BlockPos clickedPos
    ) {

        for (Direction horizontal
                : FRAME_DIRECTIONS) {

            for (int localX = 0;
                 localX < FRAME_SIZE;
                 localX++) {

                for (int localY = 0;
                     localY < FRAME_SIZE;
                     localY++) {

                    BlockPos possibleOrigin =
                            clickedPos
                                    .relative(
                                            horizontal,
                                            -localX
                                    )
                                    .below(localY);

                    ChaosPortalShape shape =
                            new ChaosPortalShape(
                                    possibleOrigin,
                                    horizontal
                            );

                    if (shape.isIgnitable(level)) {
                        return Optional.of(shape);
                    }
                }
            }
        }

        return Optional.empty();
    }


    public static boolean hasValidPortal(
            LevelAccessor level,
            BlockPos portalPos,
            Direction.Axis axis
    ) {
        Direction horizontal =
                axis == Direction.Axis.X
                        ? Direction.EAST
                        : Direction.SOUTH;


        for (int innerX = 1;
             innerX <= 2;
             innerX++) {

            for (int innerY = 1;
                 innerY <= 2;
                 innerY++) {

                BlockPos possibleOrigin =
                        portalPos
                                .relative(
                                        horizontal,
                                        -innerX
                                )
                                .below(innerY);

                ChaosPortalShape shape =
                        new ChaosPortalShape(
                                possibleOrigin,
                                horizontal
                        );

                if (shape.isActivePortal(level)) {
                    return true;
                }
            }
        }

        return false;
    }

    private boolean isIgnitable(
            LevelAccessor level
    ) {
        for (int x = 0; x < FRAME_SIZE; x++) {
            for (int y = 0; y < FRAME_SIZE; y++) {
                BlockPos currentPos =
                        getPosition(x, y);

                BlockState currentState =
                        level.getBlockState(currentPos);

                if (isFramePosition(x, y)) {
                    if (!isCorrectFrameBlock(
                            currentState,
                            x,
                            y
                    )) {
                        return false;
                    }

                    continue;
                }

                /*
                 * Внутреннее пространство до активации
                 * должно быть полностью пустым.
                 */
                if (!currentState.isAir()) {
                    return false;
                }
            }
        }

        return true;
    }

    private boolean isActivePortal(
            LevelAccessor level
    ) {
        Direction.Axis expectedAxis =
                this.horizontalDirection.getAxis();

        for (int x = 0; x < FRAME_SIZE; x++) {
            for (int y = 0; y < FRAME_SIZE; y++) {
                BlockPos currentPos =
                        getPosition(x, y);

                BlockState currentState =
                        level.getBlockState(currentPos);

                if (isFramePosition(x, y)) {
                    if (!isCorrectFrameBlock(
                            currentState,
                            x,
                            y
                    )) {
                        return false;
                    }

                    continue;
                }

                if (!currentState.is(
                        Oasiso.CHAOS_PORTAL.get()
                )) {
                    return false;
                }

                if (currentState.getValue(
                        ChaosPortalBlock.AXIS
                ) != expectedAxis) {
                    return false;
                }
            }
        }

        return true;
    }

    private boolean isCorrectFrameBlock(
            BlockState state,
            int x,
            int y
    ) {
        if (isCorner(x, y)) {
            return state.is(
                    Oasiso.ENTROPY_BLOCK.get()
            );
        }

        return state.is(
                Oasiso.KARAKOLIT_BLOCK.get()
        );
    }

    private boolean isFramePosition(
            int x,
            int y
    ) {
        return x == 0
                || x == FRAME_SIZE - 1
                || y == 0
                || y == FRAME_SIZE - 1;
    }

    private boolean isCorner(
            int x,
            int y
    ) {
        boolean horizontalCorner =
                x == 0
                        || x == FRAME_SIZE - 1;

        boolean verticalCorner =
                y == 0
                        || y == FRAME_SIZE - 1;

        return horizontalCorner
                && verticalCorner;
    }

    private BlockPos getPosition(
            int horizontalOffset,
            int verticalOffset
    ) {
        return this.origin
                .relative(
                        this.horizontalDirection,
                        horizontalOffset
                )
                .above(verticalOffset);
    }

    public void createPortal(
            ServerLevel level
    ) {
        Direction.Axis portalAxis =
                this.horizontalDirection.getAxis();

        BlockState portalState =
                Oasiso.CHAOS_PORTAL.get()
                        .defaultBlockState()
                        .setValue(
                                ChaosPortalBlock.AXIS,
                                portalAxis
                        );


        for (int x = 1; x <= 2; x++) {
            for (int y = 1; y <= 2; y++) {
                level.setBlock(
                        getPosition(x, y),
                        portalState,
                        3
                );
            }
        }

        double centerX =
                this.origin.getX()
                        + 0.5D
                        + this.horizontalDirection
                        .getStepX() * 1.5D;

        double centerY =
                this.origin.getY() + 2.0D;

        double centerZ =
                this.origin.getZ()
                        + 0.5D
                        + this.horizontalDirection
                        .getStepZ() * 1.5D;


        level.playSound(
                null,
                centerX,
                centerY,
                centerZ,
                SoundEvents.AMETHYST_BLOCK_CHIME,
                SoundSource.BLOCKS,
                1.5F,
                0.65F
        );

        level.playSound(
                null,
                centerX,
                centerY,
                centerZ,
                SoundEvents.BEACON_ACTIVATE,
                SoundSource.BLOCKS,
                0.8F,
                1.4F
        );


        level.sendParticles(
                Oasiso.PURPLE_STARS.get(),
                centerX,
                centerY,
                centerZ,
                55,
                0.8D,
                1.1D,
                0.8D,
                0.08D
        );
    }
}