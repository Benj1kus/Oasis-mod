package com.benji.oasiso.client.event;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;

public final class EntropyGloveFillClientState {

    private static boolean mode;
    private static InteractionHand hand = InteractionHand.MAIN_HAND;
    private static BlockPos first;
    private static BlockPos second;
    private static BlockPos preview;
    private static Direction.Axis axis = Direction.Axis.Y;
    private static boolean complete;


    private EntropyGloveFillClientState() {
    }

    public static void apply(boolean newMode, InteractionHand newHand, boolean hasSelection, boolean newComplete, BlockPos newFirst, BlockPos newSecond, Direction.Axis newAxis) {
        mode = newMode;
        hand = newHand;

        if (!newMode || !hasSelection) {
            first = null;
            second = null;
            preview = null;
            complete = false;
            axis = Direction.Axis.Y;
            return;
        }
        first = newFirst.immutable();
        second = newComplete ? newSecond.immutable() : null;
        preview = newComplete ? null : newFirst.immutable();
        complete = newComplete;
        axis = newAxis;
    }

    public static boolean isFillMode() {
        return mode;
    }

    public static boolean hasSelection() {
        return first != null;
    }

    public static boolean isComplete() {
        return complete && first != null && second != null;
    }

    public static InteractionHand hand() {
        return hand;
    }

    public static BlockPos first() {
        return first;
    }

    public static BlockPos second() {
        return second;
    }

    public static Direction.Axis axis() {
        return axis;
    }

    public static BlockPos preview() {
        return preview;
    }

    public static void setPreview(BlockPos value) {
        preview = value == null ? null : value.immutable();
    }

    public static BlockPos visibleEnd() {
        if (isComplete()) {
            return second;
        }
        if (preview != null) {
            return preview;
        }
        return first;
    }

    public static boolean contains(BlockPos pos) {
        if (!isComplete()) {
            return false;
        }

        int minX = Math.min(first.getX(), second.getX());
        int maxX = Math.max(first.getX(), second.getX());

        int minY = Math.min(first.getY(), second.getY());
        int maxY = Math.max(first.getY(), second.getY());

        int minZ = Math.min(first.getZ(), second.getZ());
        int maxZ = Math.max(first.getZ(), second.getZ());

        return pos.getX() >= minX && pos.getX() <= maxX && pos.getY() >= minY && pos.getY() <= maxY && pos.getZ() >= minZ && pos.getZ() <= maxZ;
    }
}