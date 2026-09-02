package com.benji.oasiso.client.event;

import com.benji.oasiso.Oasiso;
import com.benji.oasiso.client.key.ModKeyMappings;
import com.benji.oasiso.common.item.EntropyChestplateGloveItem;
import com.benji.oasiso.network.EntropyGloveFillActionPacket;
import com.benji.oasiso.network.ModMessages;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Oasiso.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class EntropyGloveFillInput {

    private static boolean previousUseDown;
    private static BlockPos lastDragSoundCell;

    private EntropyGloveFillInput() {
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;

        if (player == null) {
            previousUseDown = false;
            lastDragSoundCell = null;
            return;
        }

        if (minecraft.screen == null) {

            while (ModKeyMappings.GLOVE_FILL_MODE.consumeClick()) {
                InteractionHand gloveHand = EntropyChestplateGloveItem.findGloveHand(player);
                if (gloveHand == null) {
                    continue;
                }
                ModMessages.sendToServer(EntropyGloveFillActionPacket.toggle(gloveHand));
            }
        }

        boolean useDown = minecraft.options.keyUse.isDown();

        if (EntropyGloveFillClientState.isFillMode() && EntropyGloveFillClientState.hasSelection() && !EntropyGloveFillClientState.isComplete() && useDown) {

            BlockPos projected = getProjectedPoint(player);

            if (projected != null) {

                BlockPos previous = EntropyGloveFillClientState.preview();
                boolean changed = previous == null || !previous.equals(projected);

                if (changed) {

                    EntropyGloveFillClientState.setPreview(projected);
                    playDragTick(minecraft, projected);
                }
            }
        }

        if (previousUseDown && !useDown && EntropyGloveFillClientState.isFillMode() && EntropyGloveFillClientState.hasSelection() && !EntropyGloveFillClientState.isComplete()) {

            BlockPos preview = EntropyGloveFillClientState.preview();
            BlockPos first = EntropyGloveFillClientState.first();

            if (preview != null && first != null && !preview.equals(first)) {
                ModMessages.sendToServer(EntropyGloveFillActionPacket.finish(EntropyGloveFillClientState.hand(), preview));
            }
        }

        if (!useDown || !EntropyGloveFillClientState.isFillMode() || EntropyGloveFillClientState.isComplete()) {

            lastDragSoundCell = null;
        }
        previousUseDown = useDown;
    }

    @SubscribeEvent
    public static void onInteractionKey(InputEvent.InteractionKeyMappingTriggered event) {
        if (!event.isUseItem()) {
            return;
        }

        if (!EntropyGloveFillClientState.isFillMode()) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;

        if (player == null || minecraft.screen != null) {
            return;
        }

        InteractionHand gloveHand = EntropyChestplateGloveItem.findGloveHand(player);

        if (gloveHand == null) {
            return;
        }

        event.setCanceled(true);
        event.setSwingHand(false);

        if (player.isShiftKeyDown()) {
            startSelectionFromHit(minecraft, gloveHand);
            lastDragSoundCell = null;
            return;
        }

        if (!EntropyGloveFillClientState.hasSelection()) {
            startSelectionFromHit(minecraft, gloveHand);
            lastDragSoundCell = null;
            return;
        }

        if (!EntropyGloveFillClientState.isComplete()) {
            BlockPos projected = getProjectedPoint(player);

            if (projected != null) {
                EntropyGloveFillClientState.setPreview(projected);
            }
            return;
        }

        BlockPos cell = getProjectedPoint(player);
        if (cell == null || !EntropyGloveFillClientState.contains(cell)) {
            return;
        }

        InteractionHand materialHand = gloveHand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;

        ItemStack material = player.getItemInHand(materialHand);

        if (!(material.getItem() instanceof BlockItem)) {
            return;
        }

        ModMessages.sendToServer(EntropyGloveFillActionPacket.fill(gloveHand, cell));
    }

    private static void playDragTick(Minecraft minecraft, BlockPos cell) {
        if (minecraft.level == null) {
            return;
        }

        if (cell.equals(lastDragSoundCell)) {

            return;
        }

        lastDragSoundCell = cell.immutable();

        BlockPos first = EntropyGloveFillClientState.first();
        int distance = first == null ? 0 : Math.abs(cell.getX() - first.getX()) + Math.abs(cell.getY() - first.getY()) + Math.abs(cell.getZ() - first.getZ());

        float pitch = Math.min(1.95F, 1.42F + distance * 0.035F);

        Vec3 position = Vec3.atCenterOf(cell);
        minecraft.level.playLocalSound(position.x, position.y, position.z, SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.PLAYERS, 0.18F, pitch, false);
    }

    private static void startSelectionFromHit(Minecraft minecraft, InteractionHand gloveHand) {
        HitResult raw = minecraft.hitResult;

        if (!(raw instanceof BlockHitResult hit) || hit.getType() != HitResult.Type.BLOCK) {
            return;
        }

        BlockPos point = hit.getBlockPos().relative(hit.getDirection());
        ModMessages.sendToServer(EntropyGloveFillActionPacket.start(gloveHand, point, hit.getDirection().getAxis()));
    }

    private static BlockPos getProjectedPoint(LocalPlayer player) {
        BlockPos anchor = EntropyGloveFillClientState.first();
        if (anchor == null) {
            return null;
        }

        Direction.Axis axis = EntropyGloveFillClientState.axis();
        Vec3 eye = player.getEyePosition(1.0F);
        Vec3 direction = player.getViewVector(1.0F).normalize();

        double denominator = switch (axis) {
            case X -> direction.x;
            case Y -> direction.y;
            case Z -> direction.z;
        };

        if (Math.abs(denominator) < 0.00001D) {
            return null;
        }

        double plane = switch (axis) {
            case X -> anchor.getX() + 0.5D;
            case Y -> anchor.getY() + 0.5D;
            case Z -> anchor.getZ() + 0.5D;
        };

        double eyeAxis = switch (axis) {
            case X -> eye.x;
            case Y -> eye.y;
            case Z -> eye.z;
        };

        double distance = (plane - eyeAxis) / denominator;
        double reach = player.getAttributeValue(ForgeMod.BLOCK_REACH.get()) + 0.75D;

        if (distance < 0.0D || distance > reach) {
            return null;
        }

        Vec3 point = eye.add(direction.scale(distance));

        int x = Mth.floor(point.x);
        int y = Mth.floor(point.y);
        int z = Mth.floor(point.z);

        return switch (axis) {
            case X -> new BlockPos(anchor.getX(), y, z);
            case Y -> new BlockPos(x, anchor.getY(), z);
            case Z -> new BlockPos(x, y, anchor.getZ());
        };
    }
}