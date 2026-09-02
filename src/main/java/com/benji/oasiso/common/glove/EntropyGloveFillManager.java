package com.benji.oasiso.common.glove;

import com.benji.oasiso.Oasiso;
import com.benji.oasiso.common.entity.EntropyPhysicsBlockEntity;
import com.benji.oasiso.common.item.EntropyChestplateGloveItem;
import com.benji.oasiso.network.EntropyGloveFillAnimationPacket;
import com.benji.oasiso.network.EntropyGloveFillSelectionSyncPacket;
import com.benji.oasiso.network.ModMessages;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.SoundType;

import java.util.*;

@Mod.EventBusSubscriber(modid = Oasiso.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class EntropyGloveFillManager {

    public static final int MAX_FILL_BLOCKS = 512;
    private static final int START_INTERVAL_TICKS = 1;
    public static final int INSERT_ANIMATION_TICKS = 10;
    private static final Map<UUID, Selection> SELECTIONS = new HashMap<>();
    private static final List<ScheduledPlacement> PENDING = new ArrayList<>();
    private static final Set<ReservedPos> RESERVED = new HashSet<>();

    private EntropyGloveFillManager() {
    }

    public static void toggleMode(ServerPlayer player, InteractionHand hand) {
        ItemStack glove = player.getItemInHand(hand);

        if (!(glove.getItem() instanceof EntropyChestplateGloveItem)) {
            return;
        }

        boolean enabled = !EntropyChestplateGloveItem.isFillMode(glove);

        if (enabled && player.level() instanceof ServerLevel level) {
            EntropyPhysicsBlockEntity held = EntropyChestplateGloveItem.resolveHeldBlock(level, glove);


            if (held != null && held.isHeldBy(player)) {
                held.releaseHolder(player);
            }
            EntropyChestplateGloveItem.clearHeldBlock(glove);
        }

        EntropyChestplateGloveItem.setFillMode(glove, enabled);
        SELECTIONS.remove(player.getUUID());
        syncInventory(player);
        syncSelection(player, hand);
    }

    public static void startSelection(ServerPlayer player, InteractionHand hand, BlockPos point, Direction.Axis axis) {
        ItemStack glove = player.getItemInHand(hand);

        if (!isValidFillGlove(glove)) {
            return;
        }

        if (!isWithinReach(player, point)) {
            return;
        }

        Selection selection = new Selection(player.level().dimension(),
                hand,
                point.immutable(),
                null,
                axis,
                player.getRandom().nextLong());

        SELECTIONS.put(player.getUUID(), selection);
        syncSelection(player, hand);
    }

    public static void finishSelection(ServerPlayer player, InteractionHand hand, BlockPos requestedPoint) {
        Selection current = SELECTIONS.get(player.getUUID());


        if (current == null || current.second() != null
                || current.dimension() != player.level().dimension()
                || current.hand() != hand) {

            return;
        }

        ItemStack glove = player.getItemInHand(hand);

        if (!isValidFillGlove(glove)) {
            return;
        }

        BlockPos point = projectToPlane(requestedPoint, current.first(), current.axis());

        if (!isWithinReach(player, point)) {
            return;
        }

        long blockCount = getSelectionSize(current.first(), point);

        if (blockCount <= 0L || blockCount > MAX_FILL_BLOCKS) {
            return;
        }


        Selection completed = new Selection(current.dimension(), current.hand(),
                current.first(), point.immutable(),
                current.axis(),
                current.seed());

        SELECTIONS.put(player.getUUID(), completed);
        syncSelection(player, hand);
    }

    public static void startFill(ServerPlayer player, InteractionHand gloveHand, BlockPos clickedCell) {
        if (!(player.level() instanceof ServerLevel level)) {

            return;
        }

        ItemStack glove = player.getItemInHand(gloveHand);

        if (!isValidFillGlove(glove)) {
            return;
        }

        Selection selection = SELECTIONS.get(player.getUUID());

        if (selection == null || selection.second() == null
                || selection.dimension() != level.dimension()
                || selection.hand() != gloveHand) {
            return;
        }


        if (!selectionContains(selection, clickedCell)) {
            return;
        }


        if (!isWithinReach(player, clickedCell)) {
            return;
        }

        InteractionHand materialHand = gloveHand == InteractionHand.MAIN_HAND
                ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;

        ItemStack material = player.getItemInHand(materialHand);


        if (!(material.getItem() instanceof BlockItem blockItem)) {
            return;
        }

        BlockState placementState = blockItem.getBlock().defaultBlockState();

        if (!EntropyChestplateGloveItem.canAttachBlockState(level, selection.first(), placementState)) {
            return;
        }

        List<BlockPos> targets = getOrderedTargets(level, selection, placementState);

        if (targets.isEmpty()) {
            return;
        }

        int available = player.getAbilities().instabuild
                ? targets.size() : material.getCount();

        int amount = Math.min(available, targets.size());

        if (amount <= 0) {
            return;
        }

        if (!player.getAbilities().instabuild) {
            material.shrink(amount);
            syncInventory(player);
        }

        long now = level.getGameTime();

        for (int i = 0; i < amount; i++) {

            BlockPos pos = targets.get(i).immutable();
            ReservedPos key = new ReservedPos(level.dimension(), pos.asLong());

            RESERVED.add(key);

            ItemStack refund = material.copy();
            refund.setCount(1);

            long animationStart = now + (long) i * START_INTERVAL_TICKS;

            PENDING.add(new ScheduledPlacement(level.dimension(),
                    player.getUUID(),
                    pos,
                    placementState,
                    refund,
                    !player.getAbilities().instabuild,
                    animationStart,
                    animationStart + INSERT_ANIMATION_TICKS,
                    false));
        }
    }

    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END
                || event.level.isClientSide
                || !(event.level instanceof ServerLevel level)) {
            return;
        }
        long now = level.getGameTime();

        Iterator<ScheduledPlacement> iterator = PENDING.iterator();


        while (iterator.hasNext()) {
            ScheduledPlacement placement = iterator.next();

            if (placement.dimension() != level.dimension()) {
                continue;
            }


            if (!placement.animationSent() && now >= placement.animationStart()) {
                broadcastAnimation(level, placement);
                placement.animationSent = true;
            }

            if (now < placement.placeAt()) {
                continue;
            }

            ReservedPos reserved = new ReservedPos(placement.dimension(), placement.pos().asLong());

            RESERVED.remove(reserved);

            if (canFillTarget(level,
                    placement.pos(),
                    placement.state())) {

                boolean placed = level.setBlock(placement.pos(),
                        placement.state(),
                        Block.UPDATE_ALL);


                if (!placed) {
                    refund(level, placement);
                }

            } else {
                refund(level, placement);
            }
            iterator.remove();
        }
    }

    private static List<BlockPos> getOrderedTargets(ServerLevel level, Selection selection, BlockState state) {
        List<BlockPos> result = new ArrayList<>();


        for (BlockPos pos : BlockPos.betweenClosed(selection.first(), selection.second())) {

            BlockPos immutable = pos.immutable();
            ReservedPos reserved = new ReservedPos(level.dimension(), immutable.asLong());


            if (RESERVED.contains(reserved)) {

                continue;
            }


            if (!canFillTarget(level, immutable, state)) {

                continue;
            }

            result.add(immutable);
        }

        boolean highA = (selection.seed() & 1L) != 0L;
        boolean highB = (selection.seed() & 2L) != 0L;

        int minA;
        int maxA;

        int minB;
        int maxB;

        switch (selection.axis()) {

            case X -> {
                minA = Math.min(selection.first().getY(), selection.second().getY());
                maxA = Math.max(selection.first().getY(), selection.second().getY());

                minB = Math.min(selection.first().getZ(), selection.second().getZ());
                maxB = Math.max(selection.first().getZ(), selection.second().getZ());
            }


            case Y -> {
                minA = Math.min(selection.first().getX(), selection.second().getX());
                maxA = Math.max(selection.first().getX(), selection.second().getX());

                minB = Math.min(selection.first().getZ(), selection.second().getZ());
                maxB = Math.max(selection.first().getZ(), selection.second().getZ());
            }


            case Z -> {
                minA = Math.min(selection.first().getX(), selection.second().getX());
                maxA = Math.max(selection.first().getX(), selection.second().getX());

                minB = Math.min(selection.first().getY(), selection.second().getY());
                maxB = Math.max(selection.first().getY(), selection.second().getY());
            }

            default -> throw new IllegalStateException();
        }

        int cornerA = highA ? maxA : minA;
        int cornerB = highB ? maxB : minB;

        result.sort(Comparator.comparingDouble(pos -> getOrderDistance(pos,
                selection.axis(),
                cornerA, cornerB,
                selection.seed())));

        return result;
    }

    private static double getOrderDistance(BlockPos pos, Direction.Axis axis, int cornerA, int cornerB, long seed) {
        int a;
        int b;


        switch (axis) {

            case X -> {
                a = pos.getY();
                b = pos.getZ();
            }

            case Y -> {
                a = pos.getX();
                b = pos.getZ();
            }

            case Z -> {
                a = pos.getX();
                b = pos.getY();
            }

            default -> throw new IllegalStateException();
        }

        double distance = Math.abs(a - cornerA) + Math.abs(b - cornerB);
        long hash = pos.asLong() ^ seed * 0x9E3779B97F4A7C15L;

        hash ^= hash >>> 33;
        hash *= 0xff51afd7ed558ccdL;
        hash ^= hash >>> 33;

        double jitter = (hash & 1023L) / 1023.0D * 0.35D;

        return distance + jitter;
    }

    private static boolean canFillTarget(ServerLevel level, BlockPos pos, BlockState state) {
        if (pos.getY() < level.getMinBuildHeight()
                || pos.getY() >= level.getMaxBuildHeight()) {
            return false;
        }

        if (!level.getWorldBorder().isWithinBounds(pos)) {
            return false;
        }

        BlockState current = level.getBlockState(pos);

        if (!current.canBeReplaced()) {
            return false;
        }

        if (!level.getFluidState(pos).isEmpty()) {

            return false;
        }
        return state.canSurvive(level, pos);
    }

    private static void broadcastAnimation(ServerLevel level, ScheduledPlacement placement) {
        Vec3 center = Vec3.atCenterOf(placement.pos());

        EntropyGloveFillAnimationPacket packet = new EntropyGloveFillAnimationPacket(placement.pos(),
                Block.getId(placement.state()),
                INSERT_ANIMATION_TICKS);


        for (ServerPlayer watcher : level.players()) {
            if (watcher.position().distanceToSqr(center) > 96.0D * 96.0D) {
                continue;
            }

            ModMessages.sendToPlayer(watcher, packet);
        }

        SoundType soundType = placement.state().getSoundType();
        float volume = Math.min(0.55F,
                0.28F + soundType.getVolume() * 0.18F);

        float pitch = soundType.getPitch() * (0.96F + level.random.nextFloat() * 0.10F);

        level.playSound(null, placement.pos(), soundType.getPlaceSound(), SoundSource.BLOCKS, volume, pitch);
    }

    private static void refund(ServerLevel level, ScheduledPlacement placement) {
        if (!placement.paid()) {
            return;
        }


        ServerPlayer owner = level.getServer().getPlayerList().getPlayer(placement.owner());
        ItemStack refund = placement.refund().copy();

        if (owner != null && owner.getInventory().add(refund)) {
            return;
        }


        Block.popResource(level, placement.pos(), refund);
    }

    private static BlockPos projectToPlane(BlockPos point, BlockPos anchor, Direction.Axis axis) {
        return switch (axis) {
            case X -> new BlockPos(anchor.getX(), point.getY(), point.getZ());
            case Y -> new BlockPos(point.getX(), anchor.getY(), point.getZ());
            case Z -> new BlockPos(point.getX(), point.getY(), anchor.getZ());
        };
    }

    private static long getSelectionSize(BlockPos first, BlockPos second) {
        long x = Math.abs((long) first.getX() - second.getX()) + 1L;
        long y = Math.abs((long) first.getY() - second.getY()) + 1L;
        long z = Math.abs((long) first.getZ() - second.getZ()) + 1L;

        return x * y * z;
    }


    private static boolean selectionContains(Selection selection, BlockPos pos) {
        if (selection.second() == null) {
            return false;
        }

        int minX = Math.min(selection.first().getX(), selection.second().getX());
        int maxX = Math.max(selection.first().getX(), selection.second().getX());

        int minY = Math.min(selection.first().getY(), selection.second().getY());
        int maxY = Math.max(selection.first().getY(), selection.second().getY());

        int minZ = Math.min(selection.first().getZ(), selection.second().getZ());
        int maxZ = Math.max(selection.first().getZ(), selection.second().getZ());

        return pos.getX() >= minX && pos.getX() <= maxX && pos.getY() >= minY && pos.getY() <= maxY && pos.getZ() >= minZ && pos.getZ() <= maxZ;
    }


    private static boolean isWithinReach(ServerPlayer player, BlockPos pos) {
        double reach = player.getAttributeValue(ForgeMod.BLOCK_REACH.get());
        reach += 1.25D;
        return player.getEyePosition().distanceToSqr(Vec3.atCenterOf(pos)) <= reach * reach;
    }

    private static boolean isValidFillGlove(ItemStack stack) {
        return stack.getItem() instanceof EntropyChestplateGloveItem

                && EntropyChestplateGloveItem.isFillMode(stack);
    }

    private static void syncSelection(ServerPlayer player, InteractionHand hand) {
        ItemStack glove = player.getItemInHand(hand);
        boolean mode = isValidFillGlove(glove);

        Selection selection = SELECTIONS.get(player.getUUID());

        ModMessages.sendToPlayer(player, new EntropyGloveFillSelectionSyncPacket(mode, hand, selection != null, selection != null && selection.second() != null, selection == null ? BlockPos.ZERO : selection.first(), selection == null || selection.second() == null ? BlockPos.ZERO : selection.second(), selection == null ? Direction.Axis.Y : selection.axis()));
    }


    private static void syncInventory(ServerPlayer player) {
        player.getInventory().setChanged();

        player.containerMenu.broadcastChanges();
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        SELECTIONS.remove(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void onDimensionChanged(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        Selection old = SELECTIONS.remove(player.getUUID());

        if (old != null) {
            syncSelection(player, old.hand());
        }
    }

    private record Selection(ResourceKey<Level> dimension, InteractionHand hand, BlockPos first, BlockPos second,
                             Direction.Axis axis, long seed) {
    }

    private record ReservedPos(ResourceKey<Level> dimension, long pos) {
    }


    private static final class ScheduledPlacement {

        private final ResourceKey<Level> dimension;
        private final UUID owner;
        private final BlockPos pos;
        private final BlockState state;
        private final ItemStack refund;
        private final boolean paid;
        private final long animationStart;
        private final long placeAt;
        private boolean animationSent;


        private ScheduledPlacement(ResourceKey<Level> dimension, UUID owner, BlockPos pos, BlockState state, ItemStack refund, boolean paid, long animationStart, long placeAt, boolean animationSent) {
            this.dimension = dimension;
            this.owner = owner;
            this.pos = pos;
            this.state = state;
            this.refund = refund;
            this.paid = paid;
            this.animationStart = animationStart;
            this.placeAt = placeAt;
            this.animationSent = animationSent;
        }


        private ResourceKey<Level> dimension() {
            return dimension;
        }

        private UUID owner() {
            return owner;
        }

        private BlockPos pos() {
            return pos;
        }

        private BlockState state() {
            return state;
        }

        private ItemStack refund() {
            return refund;
        }

        private boolean paid() {
            return paid;
        }

        private long animationStart() {
            return animationStart;
        }

        private long placeAt() {
            return placeAt;
        }

        private boolean animationSent() {
            return animationSent;
        }
    }
}