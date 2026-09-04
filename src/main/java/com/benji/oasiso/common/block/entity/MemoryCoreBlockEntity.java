package com.benji.oasiso.common.block.entity;

import com.benji.oasiso.ModSounds;
import com.benji.oasiso.registry.ModBlockEntities;
import com.benji.oasiso.registry.ModBlocks;
import com.benji.oasiso.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import com.benji.oasiso.Oasiso;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class MemoryCoreBlockEntity extends BlockEntity {

    public enum Phase {
        IDLE, REVEALING, MEMORIZE, HIDING, INPUT, RESOLVING, WRONG_FEEDBACK, SOLVED
    }

    private static final int MAX_TILES = 12;
    private static final int SYMBOL_COUNT = 6;

    private static final int REVEAL_STAGGER = 2;
    private static final int REVEAL_ANIMATION_TICKS = 10;
    private static final int MEMORIZE_TICKS = 100; // 5 секунд.
    private static final int HIDE_STAGGER = 1;
    private static final int HIDE_ANIMATION_TICKS = 9;

    private static final int SECOND_CARD_PAUSE = 7;
    private static final int WRONG_SHAKE_TICKS = 11;
    private static final int SOLVED_PAIR_STAGGER_TICKS = 6;
    private static final int SOLVED_MEMBER_STAGGER_TICKS = 2;

    private static final int SOLVED_PUSH_TICKS = 7;
    private static final int SOLVED_PUSH_HOLD_TICKS = 4;

    private static final int SOLVED_JUMP_TICKS = 24;

    private Phase phase = Phase.IDLE;

    private final List<BlockPos> tiles = new ArrayList<>();

    @Nullable
    private BlockPos firstSelection;

    @Nullable
    private BlockPos secondSelection;

    private int timer;

    private boolean coreOn;

    private MemoryPuzzleBlockEntity.VisualAnimation animation = MemoryPuzzleBlockEntity.VisualAnimation.NONE;

    private long animationStart = Long.MIN_VALUE;
    private Direction solvedPushDirection = Direction.SOUTH;

    public MemoryCoreBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MEMORY_CORE_BE.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, MemoryCoreBlockEntity core) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        core.serverTick(serverLevel);
    }

    public void activate(ServerPlayer player) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        ItemStack keyStack = findKarakolitKey(player);

        if (!keyStack.isEmpty()) {
            if (skipWithKey(serverLevel, player)) {
                if (!player.getAbilities().instabuild) {
                    keyStack.shrink(1);
                }
            }

            return;
        }

        if (phase != Phase.IDLE) {
            return;
        }


        Discovery discovery = discoverConnectedTiles(serverLevel);

        if (discovery.overflow || discovery.positions.size() < 2 || (discovery.positions.size() & 1) != 0) {

            invalidPuzzleFeedback(serverLevel);
            return;
        }

        for (BlockPos tilePos : discovery.positions) {
            if (hasForeignActiveOwner(serverLevel, tilePos)) {
                invalidPuzzleFeedback(serverLevel);
                return;
            }
        }
        resetStoredTiles();

        tiles.clear();
        tiles.addAll(discovery.positions);
        solvedPushDirection = resolveSolvedPushDirection(player);

        List<Integer> shuffledSymbols = createPairLayout(serverLevel.random, tiles.size());

        long now = serverLevel.getGameTime();

        for (int i = 0; i < tiles.size(); i++) {
            BlockPos tilePos = tiles.get(i);

            if (serverLevel.getBlockEntity(tilePos) instanceof MemoryPuzzleBlockEntity puzzle) {
                puzzle.configure(worldPosition, shuffledSymbols.get(i), now + (long) i * REVEAL_STAGGER);
            }
        }

        firstSelection = null;
        secondSelection = null;

        phase = Phase.REVEALING;

        timer = (tiles.size() - 1) * REVEAL_STAGGER + REVEAL_ANIMATION_TICKS;

        coreOn = false;
        animation = MemoryPuzzleBlockEntity.VisualAnimation.REVEAL;
        animationStart = now;

        sync();

        spawnActivationEffects(serverLevel);

        serverLevel.playSound(null, worldPosition, ModSounds.CAST.get(), SoundSource.BLOCKS, 0.9F, 1.08F);
    }

    public void handleTileClick(ServerPlayer player, BlockPos tilePos) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        if (phase != Phase.INPUT) {
            return;
        }

        if (!tiles.contains(tilePos)) {
            return;
        }

        if (!(serverLevel.getBlockEntity(tilePos) instanceof MemoryPuzzleBlockEntity puzzle)) {
            return;
        }

        if (puzzle.isMatched()) {
            return;
        }

        solvedPushDirection = resolveSolvedPushDirection(player);

        long now = serverLevel.getGameTime();

        if (firstSelection == null) {
            firstSelection = tilePos.immutable();

            puzzle.revealForSelection(now);

            playTileClickSound(serverLevel, tilePos, 1.0F);

            return;
        }

        if (firstSelection.equals(tilePos)) {
            return;
        }

        secondSelection = tilePos.immutable();

        puzzle.revealForSelection(now);

        playTileClickSound(serverLevel, tilePos, 1.08F);

        phase = Phase.RESOLVING;
        timer = SECOND_CARD_PAUSE;

        sync();
    }

    public boolean isRunning() {
        return phase != Phase.IDLE;
    }

    public boolean isCoreOn() {
        return coreOn;
    }

    public MemoryPuzzleBlockEntity.VisualAnimation getAnimation() {
        return animation;
    }

    public long getAnimationStart() {
        return animationStart;
    }

    private void serverTick(ServerLevel level) {
        if (phase == Phase.IDLE) {
            return;
        }

        if (!validateTiles(level)) {
            abortPuzzle();
            return;
        }

        if (timer > 0) {
            timer--;
        }

        if (timer > 0) {
            return;
        }

        switch (phase) {
            case REVEALING -> {
                phase = Phase.MEMORIZE;
                timer = MEMORIZE_TICKS;
                sync();
            }

            case MEMORIZE -> beginHide(level);

            case HIDING -> {
                phase = Phase.INPUT;
                timer = 0;
                sync();
            }

            case RESOLVING -> resolvePair(level);

            case WRONG_FEEDBACK -> finishWrongPair(level);

            case SOLVED -> dissolve(level);

            default -> {
            }
        }
    }

    private void beginHide(ServerLevel level) {
        long now = level.getGameTime();

        for (int i = 0; i < tiles.size(); i++) {
            BlockPos tilePos = tiles.get(i);

            if (level.getBlockEntity(tilePos) instanceof MemoryPuzzleBlockEntity puzzle) {
                puzzle.hide(now + (long) i * HIDE_STAGGER);
            }
        }

        phase = Phase.HIDING;

        timer = (tiles.size() - 1) * HIDE_STAGGER + HIDE_ANIMATION_TICKS;

        sync();
    }

    private void resolvePair(ServerLevel level) {
        if (firstSelection == null || secondSelection == null) {
            clearSelections();
            phase = Phase.INPUT;
            sync();
            return;
        }

        MemoryPuzzleBlockEntity first = tile(level, firstSelection);
        MemoryPuzzleBlockEntity second = tile(level, secondSelection);

        if (first == null || second == null) {
            abortPuzzle();
            return;
        }

        long now = level.getGameTime();

        if (first.getSymbol() == second.getSymbol()) {
            first.markMatched(now);
            second.markMatched(now);

            BlockPos soundPos = midpoint(firstSelection, secondSelection);

            level.playSound(null, soundPos, Blocks.AMETHYST_BLOCK.defaultBlockState().getSoundType().getPlaceSound(), SoundSource.BLOCKS, 0.85F, 1.45F);
            level.sendParticles(ParticleTypes.END_ROD, soundPos.getX() + 0.5D, soundPos.getY() + 0.5D, soundPos.getZ() + 0.5D, 8, 0.35D, 0.35D, 0.35D, 0.035D);

            clearSelections();

            if (allMatched(level)) {
                beginSolved(level);
            } else {
                phase = Phase.INPUT;
                sync();
            }

            return;
        }

        first.mismatch(now);
        second.mismatch(now);

        level.playSound(null, midpoint(firstSelection, secondSelection), Blocks.SCULK.defaultBlockState().getSoundType().getBreakSound(), SoundSource.BLOCKS, 0.75F, 0.72F);

        phase = Phase.WRONG_FEEDBACK;
        timer = WRONG_SHAKE_TICKS;

        sync();
    }

    private void finishWrongPair(ServerLevel level) {
        long now = level.getGameTime();

        MemoryPuzzleBlockEntity first = firstSelection != null ? tile(level, firstSelection) : null;

        MemoryPuzzleBlockEntity second = secondSelection != null ? tile(level, secondSelection) : null;

        if (first != null) {
            first.hide(now);
        }

        if (second != null) {
            second.hide(now);
        }

        clearSelections();

        phase = Phase.INPUT;
        sync();
    }

    private ItemStack findKarakolitKey(ServerPlayer player) {
        ItemStack mainHand = player.getMainHandItem();

        if (mainHand.is(ModItems.KARAKOLIT_KEY.get())) {
            return mainHand;
        }

        ItemStack offHand = player.getOffhandItem();

        if (offHand.is(ModItems.KARAKOLIT_KEY.get())) {
            return offHand;
        }

        return ItemStack.EMPTY;
    }

    private boolean skipWithKey(ServerLevel level, ServerPlayer player) {
        if (phase == Phase.SOLVED) {
            return false;
        }
        solvedPushDirection = resolveSolvedPushDirection(player);

        if (phase == Phase.IDLE) {

            Discovery discovery = discoverConnectedTiles(level);

            if (discovery.overflow || discovery.positions.size() < 2 || (discovery.positions.size() & 1) != 0) {

                invalidPuzzleFeedback(level);
                return false;
            }

            for (BlockPos tilePos : discovery.positions) {
                if (hasForeignActiveOwner(level, tilePos)) {
                    invalidPuzzleFeedback(level);
                    return false;
                }
            }

            resetStoredTiles();

            tiles.clear();
            tiles.addAll(discovery.positions);

            List<Integer> symbols = createPairLayout(level.random, tiles.size());

            long now = level.getGameTime();

            for (int i = 0; i < tiles.size(); i++) {
                BlockPos tilePos = tiles.get(i);

                if (level.getBlockEntity(tilePos) instanceof MemoryPuzzleBlockEntity puzzle) {
                    puzzle.configure(worldPosition, symbols.get(i), now);
                }
            }

        } else {
            if (!validateTiles(level)) {
                abortPuzzle();
                invalidPuzzleFeedback(level);
                return false;
            }
        }

        clearSelections();
        beginSolved(level);

        return true;
    }

    private void beginSolved(ServerLevel level) {
        long now = level.getGameTime();

        coreOn = true;
        phase = Phase.SOLVED;

        List<List<BlockPos>> pairs = collectSolvedPairs(level);

        int pairCount = pairs.size();
        long jumpStart = now + Math.max(0, pairCount - 1) * SOLVED_PAIR_STAGGER_TICKS + SOLVED_MEMBER_STAGGER_TICKS + SOLVED_PUSH_TICKS + SOLVED_PUSH_HOLD_TICKS;

        animation = MemoryPuzzleBlockEntity.VisualAnimation.SOLVED;

        animationStart = jumpStart;

        timer = (int) (jumpStart - now) + SOLVED_JUMP_TICKS;

        for (int pairIndex = 0; pairIndex < pairs.size(); pairIndex++) {

            List<BlockPos> pair = pairs.get(pairIndex);
            long firstStart = now + (long) pairIndex * SOLVED_PAIR_STAGGER_TICKS;

            for (int member = 0; member < pair.size(); member++) {
                BlockPos tilePos = pair.get(member);
                long pushStart = firstStart + (long) member * SOLVED_MEMBER_STAGGER_TICKS;
                if (level.getBlockEntity(tilePos) instanceof MemoryPuzzleBlockEntity puzzle) {

                    puzzle.solvedAnimation(pushStart, jumpStart, solvedPushDirection);
                }
            }
        }

        sync();

        level.sendParticles(ParticleTypes.SOUL, worldPosition.getX() + 0.5D, worldPosition.getY() + 0.65D, worldPosition.getZ() + 0.5D, 28, 0.75D, 0.65D, 0.75D, 0.035D);
        level.playSound(null, worldPosition, ModSounds.CAST.get(), SoundSource.BLOCKS, 1.0F, 1.42F);
        level.playSound(null, worldPosition, Blocks.AMETHYST_BLOCK.defaultBlockState().getSoundType().getPlaceSound(), SoundSource.BLOCKS, 1.0F, 1.75F);
    }


    private void dissolve(ServerLevel level) {
        List<BlockPos> allBlocks = new ArrayList<>(tiles.size() + 1);

        allBlocks.addAll(tiles);
        allBlocks.add(worldPosition);

        BlockParticleOption sandDust = new BlockParticleOption(ParticleTypes.BLOCK, Blocks.SAND.defaultBlockState());

        for (BlockPos pos : allBlocks) {
            level.sendParticles(sandDust, pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, 22, 0.40D, 0.40D, 0.40D, 0.075D);

            level.playSound(null, pos, Blocks.DEEPSLATE.defaultBlockState().getSoundType().getBreakSound(), SoundSource.BLOCKS, 0.52F, 0.86F + level.random.nextFloat() * 0.24F);
        }

        for (BlockPos tilePos : tiles) {
            if (level.getBlockState(tilePos).is(ModBlocks.MEMORY_PUZ.get())) {
                level.setBlock(tilePos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            }
        }

        if (level.getBlockState(worldPosition).is(ModBlocks.MEMORY_CORE.get())) {
            level.setBlock(worldPosition, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        }
    }

    private void abortPuzzle() {
        resetStoredTiles();

        tiles.clear();
        clearSelections();

        phase = Phase.IDLE;
        timer = 0;
        coreOn = false;
        animation = MemoryPuzzleBlockEntity.VisualAnimation.NONE;
        animationStart = Long.MIN_VALUE;

        sync();
    }

    private void resetStoredTiles() {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        for (BlockPos tilePos : tiles) {
            if (serverLevel.getBlockEntity(tilePos) instanceof MemoryPuzzleBlockEntity puzzle) {
                puzzle.resetPuzzle();
            }
        }
    }

    private boolean validateTiles(ServerLevel level) {
        if (tiles.isEmpty()) {
            return false;
        }

        for (BlockPos tilePos : tiles) {
            if (!level.getBlockState(tilePos).is(ModBlocks.MEMORY_PUZ.get())) {
                return false;
            }

            if (!(level.getBlockEntity(tilePos) instanceof MemoryPuzzleBlockEntity puzzle)) {
                return false;
            }

            if (puzzle.getCorePos() == null || !worldPosition.equals(puzzle.getCorePos())) {

                return false;
            }
        }

        return true;
    }

    private boolean allMatched(ServerLevel level) {
        for (BlockPos tilePos : tiles) {
            MemoryPuzzleBlockEntity puzzle = tile(level, tilePos);

            if (puzzle == null || !puzzle.isMatched()) {
                return false;
            }
        }

        return true;
    }

    @Nullable
    private static MemoryPuzzleBlockEntity tile(ServerLevel level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof MemoryPuzzleBlockEntity puzzle ? puzzle : null;
    }

    private Discovery discoverConnectedTiles(ServerLevel level) {
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        Set<BlockPos> visited = new HashSet<>();
        List<BlockPos> result = new ArrayList<>();

        visited.add(worldPosition);

        for (Direction direction : Direction.values()) {
            BlockPos next = worldPosition.relative(direction);

            if (level.getBlockState(next).is(ModBlocks.MEMORY_PUZ.get())) {
                queue.add(next.immutable());
                visited.add(next.immutable());
            }
        }

        boolean overflow = false;

        while (!queue.isEmpty()) {
            BlockPos current = queue.removeFirst();

            result.add(current);

            if (result.size() > MAX_TILES) {
                overflow = true;
                break;
            }

            for (Direction direction : Direction.values()) {
                BlockPos next = current.relative(direction);

                if (visited.contains(next)) {
                    continue;
                }

                visited.add(next.immutable());

                if (level.getBlockState(next).is(ModBlocks.MEMORY_PUZ.get())) {
                    queue.addLast(next.immutable());
                }
            }
        }

        return new Discovery(result, overflow);
    }

    private List<List<BlockPos>> collectSolvedPairs(ServerLevel level) {
        List<List<BlockPos>> result = new ArrayList<>();

        for (int symbol = 0; symbol < SYMBOL_COUNT; symbol++) {

            List<BlockPos> pair = new ArrayList<>(2);

            for (BlockPos tilePos : tiles) {
                MemoryPuzzleBlockEntity puzzle = tile(level, tilePos);

                if (puzzle != null && puzzle.getSymbol() == symbol) {

                    pair.add(tilePos);
                }
            }

            if (pair.size() == 2) {
                pair.sort(java.util.Comparator.<BlockPos>comparingInt(BlockPos::getY).thenComparingInt(BlockPos::getX).thenComparingInt(BlockPos::getZ));

                result.add(pair);
            }
        }

        return result;
    }

    private Direction resolveSolvedPushDirection(ServerPlayer player) {
        int minX = worldPosition.getX();
        int maxX = minX;

        int minY = worldPosition.getY();
        int maxY = minY;

        int minZ = worldPosition.getZ();
        int maxZ = minZ;

        for (BlockPos pos : tiles) {
            minX = Math.min(minX, pos.getX());
            maxX = Math.max(maxX, pos.getX());

            minY = Math.min(minY, pos.getY());
            maxY = Math.max(maxY, pos.getY());

            minZ = Math.min(minZ, pos.getZ());
            maxZ = Math.max(maxZ, pos.getZ());
        }

        int spanX = maxX - minX;
        int spanY = maxY - minY;
        int spanZ = maxZ - minZ;

        int smallest = Math.min(spanX, Math.min(spanY, spanZ));

        double centerX = worldPosition.getX() + 0.5D;
        double centerY = worldPosition.getY() + 0.5D;
        double centerZ = worldPosition.getZ() + 0.5D;

        Direction best = Direction.SOUTH;
        double bestDistance = -1.0D;

        if (spanX == smallest) {
            double distance = Math.abs(player.getX() - centerX);

            if (distance > bestDistance) {
                bestDistance = distance;

                best = player.getX() >= centerX ? Direction.EAST : Direction.WEST;
            }
        }

        if (spanY == smallest) {
            double distance = Math.abs(player.getY() - centerY);

            if (distance > bestDistance) {
                bestDistance = distance;

                best = player.getY() >= centerY ? Direction.UP : Direction.DOWN;
            }
        }

        if (spanZ == smallest) {
            double distance = Math.abs(player.getZ() - centerZ);

            if (distance > bestDistance) {
                best = player.getZ() >= centerZ ? Direction.SOUTH : Direction.NORTH;
            }
        }

        return best;
    }


    private boolean hasForeignActiveOwner(ServerLevel level, BlockPos tilePos) {
        if (!(level.getBlockEntity(tilePos) instanceof MemoryPuzzleBlockEntity puzzle)) {
            return false;
        }

        BlockPos owner = puzzle.getCorePos();

        if (owner == null || owner.equals(worldPosition)) {
            return false;
        }

        return level.getBlockEntity(owner) instanceof MemoryCoreBlockEntity other && other.isRunning();
    }

    private static List<Integer> createPairLayout(RandomSource random, int tileCount) {
        int pairCount = tileCount / 2;

        List<Integer> symbolPool = new ArrayList<>();

        for (int i = 0; i < SYMBOL_COUNT; i++) {
            symbolPool.add(i);
        }

        shuffle(symbolPool, random);

        List<Integer> result = new ArrayList<>(tileCount);

        for (int i = 0; i < pairCount; i++) {
            int symbol = symbolPool.get(i);

            result.add(symbol);
            result.add(symbol);
        }

        shuffle(result, random);

        return result;
    }


    private static <T> void shuffle(List<T> list, RandomSource random) {
        for (int i = list.size() - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);

            T value = list.get(i);

            list.set(i, list.get(j));
            list.set(j, value);
        }
    }

    private void spawnActivationEffects(ServerLevel level) {
        level.sendParticles(ParticleTypes.SOUL, worldPosition.getX() + 0.5D, worldPosition.getY() + 0.55D, worldPosition.getZ() + 0.5D, 22, 0.62D, 0.52D, 0.62D, 0.03D);

        for (int i = 0; i < tiles.size(); i++) {
            BlockPos tilePos = tiles.get(i);

            level.sendParticles(ParticleTypes.SOUL, tilePos.getX() + 0.5D, tilePos.getY() + 0.5D, tilePos.getZ() + 0.5D, 2, 0.18D, 0.18D, 0.18D, 0.01D);
        }
    }

    private void invalidPuzzleFeedback(ServerLevel level) {
        level.sendParticles(ParticleTypes.SMOKE, worldPosition.getX() + 0.5D, worldPosition.getY() + 0.65D, worldPosition.getZ() + 0.5D, 8, 0.25D, 0.18D, 0.25D, 0.01D);

        level.playSound(null, worldPosition, Blocks.SCULK.defaultBlockState().getSoundType().getBreakSound(), SoundSource.BLOCKS, 0.55F, 0.68F);
    }

    private static BlockPos midpoint(BlockPos first, BlockPos second) {
        return new BlockPos((first.getX() + second.getX()) >> 1, (first.getY() + second.getY()) >> 1, (first.getZ() + second.getZ()) >> 1);
    }

    private void playTileClickSound(ServerLevel level, BlockPos pos, float pitch) {
        level.playSound(null, pos, Blocks.AMETHYST_BLOCK.defaultBlockState().getSoundType().getHitSound(), SoundSource.BLOCKS, 0.45F, pitch);
    }

    private void clearSelections() {
        firstSelection = null;
        secondSelection = null;
    }

    private void sync() {
        setChanged();

        if (level != null && !level.isClientSide) {
            BlockState state = getBlockState();

            level.sendBlockUpdated(worldPosition, state, state, Block.UPDATE_CLIENTS);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);

        tag.putInt("Phase", phase.ordinal());
        tag.putInt("Timer", timer);
        tag.putBoolean("CoreOn", coreOn);
        tag.putInt("Animation", animation.ordinal());
        tag.putLong("AnimationStart", animationStart);

        long[] tileArray = new long[tiles.size()];

        for (int i = 0; i < tiles.size(); i++) {
            tileArray[i] = tiles.get(i).asLong();
        }

        tag.putLongArray("Tiles", tileArray);

        if (firstSelection != null) {
            tag.putBoolean("HasFirst", true);
            tag.putLong("First", firstSelection.asLong());
        }

        if (secondSelection != null) {
            tag.putBoolean("HasSecond", true);
            tag.putLong("Second", secondSelection.asLong());
        }
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);

        int phaseId = tag.getInt("Phase");

        phase = Phase.values()[Math.max(0, Math.min(Phase.values().length - 1, phaseId))];

        timer = tag.getInt("Timer");
        coreOn = tag.getBoolean("CoreOn");

        int animationId = tag.getInt("Animation");

        animation = MemoryPuzzleBlockEntity.VisualAnimation.values()[Math.max(0, Math.min(MemoryPuzzleBlockEntity.VisualAnimation.values().length - 1, animationId))];

        animationStart = tag.getLong("AnimationStart");

        tiles.clear();

        for (long packed : tag.getLongArray("Tiles")) {
            tiles.add(BlockPos.of(packed));
        }

        firstSelection = tag.getBoolean("HasFirst") ? BlockPos.of(tag.getLong("First")) : null;

        secondSelection = tag.getBoolean("HasSecond") ? BlockPos.of(tag.getLong("Second")) : null;
    }

    @Override
    public CompoundTag getUpdateTag() {
        return saveWithoutMetadata();
    }

    @Nullable
    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    private record Discovery(List<BlockPos> positions, boolean overflow) {
    }
}
