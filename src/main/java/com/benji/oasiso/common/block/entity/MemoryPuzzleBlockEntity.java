package com.benji.oasiso.common.block.entity;

import com.benji.oasiso.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import net.minecraft.core.Direction;

public final class MemoryPuzzleBlockEntity extends BlockEntity {

    public enum VisualAnimation {
        NONE, REVEAL, HIDE, SELECT, MATCH, MISMATCH, SOLVED
    }

    private static final String[] SYMBOL_TEXTURES = {"memory_spiral", "memory_tree", "memory_humanity", "memory_nudity", "memory_smiler", "memory_century"};

    @Nullable
    private BlockPos corePos;

    private int symbol = -1;

    private boolean visible;
    private boolean matched;

    private VisualAnimation animation = VisualAnimation.NONE;
    private long animationStart = Long.MIN_VALUE;
    private long solvedJumpStart = Long.MIN_VALUE;
    private Direction solvedPushDirection = Direction.SOUTH;

    public MemoryPuzzleBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MEMORY_PUZ_BE.get(), pos, state);
    }

    public void configure(BlockPos corePos, int symbol, long revealStart) {
        this.corePos = corePos.immutable();
        this.symbol = Math.max(0, Math.min(SYMBOL_TEXTURES.length - 1, symbol));
        this.visible = true;
        this.matched = false;
        this.animation = VisualAnimation.REVEAL;
        this.animationStart = revealStart;
        sync();
        solvedJumpStart = Long.MIN_VALUE;
        solvedPushDirection = Direction.SOUTH;
    }

    public void resetPuzzle() {
        corePos = null;
        symbol = -1;
        visible = false;
        matched = false;
        animation = VisualAnimation.NONE;
        animationStart = Long.MIN_VALUE;
        sync();
        solvedJumpStart = Long.MIN_VALUE;
        solvedPushDirection = Direction.SOUTH;
    }

    public void revealForSelection(long start) {
        if (matched) {
            return;
        }

        visible = true;
        animation = VisualAnimation.SELECT;
        animationStart = start;
        sync();
    }

    public void hide(long start) {
        if (matched) {
            return;
        }

        visible = false;
        animation = VisualAnimation.HIDE;
        animationStart = start;
        sync();
    }

    public void markMatched(long start) {
        matched = true;
        visible = true;
        animation = VisualAnimation.MATCH;
        animationStart = start;
        sync();
    }

    public void mismatch(long start) {
        if (matched) {
            return;
        }

        visible = true;
        animation = VisualAnimation.MISMATCH;
        animationStart = start;
        sync();
    }

    public void solvedAnimation(long pushStart, long jumpStart, Direction pushDirection) {
        visible = true;
        matched = true;

        animation = VisualAnimation.SOLVED;
        animationStart = pushStart;

        solvedJumpStart = jumpStart;
        solvedPushDirection = pushDirection != null ? pushDirection : Direction.SOUTH;

        sync();
    }

    public long getSolvedJumpStart() {
        return solvedJumpStart;
    }

    public Direction getSolvedPushDirection() {
        return solvedPushDirection;
    }

    public void onPuzzleClick(ServerPlayer player) {
        if (level == null || level.isClientSide || corePos == null) {
            return;
        }

        if (level.getBlockEntity(corePos) instanceof MemoryCoreBlockEntity core) {
            core.handleTileClick(player, worldPosition);
        }
    }

    @Nullable
    public BlockPos getCorePos() {
        return corePos;
    }

    public int getSymbol() {
        return symbol;
    }

    public boolean isVisible() {
        return visible;
    }

    public boolean isMatched() {
        return matched;
    }

    public VisualAnimation getAnimation() {
        return animation;
    }

    public long getAnimationStart() {
        return animationStart;
    }

    public String getSymbolTextureName() {
        if (symbol < 0 || symbol >= SYMBOL_TEXTURES.length) {
            return "memory_unknown";
        }

        return SYMBOL_TEXTURES[symbol];
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

        if (corePos != null) {
            tag.putLong("CorePos", corePos.asLong());
            tag.putBoolean("HasCore", true);
        }

        tag.putLong("SolvedJumpStart", solvedJumpStart);
        tag.putInt("SolvedPushDirection", solvedPushDirection.get3DDataValue());

        tag.putInt("Symbol", symbol);
        tag.putBoolean("Visible", visible);
        tag.putBoolean("Matched", matched);
        tag.putInt("Animation", animation.ordinal());
        tag.putLong("AnimationStart", animationStart);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);

        corePos = tag.getBoolean("HasCore") ? BlockPos.of(tag.getLong("CorePos")) : null;

        solvedJumpStart = tag.contains("SolvedJumpStart") ? tag.getLong("SolvedJumpStart") : Long.MIN_VALUE;

        solvedPushDirection = tag.contains("SolvedPushDirection") ? Direction.from3DDataValue(tag.getInt("SolvedPushDirection")) : Direction.SOUTH;

        symbol = tag.getInt("Symbol");
        visible = tag.getBoolean("Visible");
        matched = tag.getBoolean("Matched");

        int animationId = tag.getInt("Animation");

        animation = VisualAnimation.values()[Math.max(0, Math.min(VisualAnimation.values().length - 1, animationId))];

        animationStart = tag.getLong("AnimationStart");
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
}
