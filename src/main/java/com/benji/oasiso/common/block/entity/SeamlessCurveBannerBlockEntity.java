package com.benji.oasiso.common.block.entity;

import com.benji.oasiso.common.block.SeamlessCurveBannerBlock;
import com.benji.oasiso.registry.ModBlockEntities;
import com.benji.oasiso.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public class SeamlessCurveBannerBlockEntity extends BlockEntity {

    @Nullable
    private BlockPos endSupportPos;

    @Nullable
    private Direction endFace;

    private int validationTimer;
    private int incompleteTicks;
    private long connectionGameTime = -1L;

    public SeamlessCurveBannerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SEEMLESS_CURVE_BANNER_BE.get(), pos, state);
    }

    public long getConnectionGameTime() {
        return this.connectionGameTime;
    }

    public boolean isConnected() {
        return this.endSupportPos != null && this.endFace != null;
    }

    @Nullable
    public BlockPos getEndSupportPos() {
        return this.endSupportPos;
    }

    @Nullable
    public Direction getEndFace() {
        return this.endFace;
    }

    public Vec3 getStartPoint() {
        Direction face = this.getBlockState().getValue(SeamlessCurveBannerBlock.FACING);
        Vec3 center = Vec3.atCenterOf(this.worldPosition);
        return center.add(-face.getStepX() * 0.499D, -face.getStepY() * 0.499D, -face.getStepZ() * 0.499D);
    }

    @Nullable
    public Vec3 getEndPoint() {
        if (this.endSupportPos == null || this.endFace == null) {

            return null;
        }

        Vec3 center = Vec3.atCenterOf(this.endSupportPos);
        return center.add(this.endFace.getStepX() * 0.501D, this.endFace.getStepY() * 0.501D, this.endFace.getStepZ() * 0.501D);
    }

    public void setConnection(BlockPos supportPos, Direction face) {
        this.endSupportPos = supportPos.immutable();

        this.endFace = face;

        this.connectionGameTime = this.level != null ? this.level.getGameTime() : 0L;

        this.incompleteTicks = 0;

        this.setChanged();

        if (this.level != null && !this.level.isClientSide) {
            BlockState state = this.getBlockState();
            this.level.sendBlockUpdated(this.worldPosition, state, state, Block.UPDATE_CLIENTS);
        }
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, SeamlessCurveBannerBlockEntity banner) {
        if (banner.isConnected()) {

            banner.validationTimer++;

            if (banner.validationTimer < 20) {
                return;
            }
            banner.validationTimer = 0;

            Direction startFace = state.getValue(SeamlessCurveBannerBlock.FACING);
            BlockPos startSupport = pos.relative(startFace.getOpposite());
            BlockState startState = level.getBlockState(startSupport);

            boolean startValid = startState.isFaceSturdy(level, startSupport, startFace);

            if (!startValid) {
                banner.dropAndRemove(level, pos);
                return;
            }

            if (banner.endSupportPos == null || banner.endFace == null) {
                return;
            }
            if (!level.hasChunkAt(banner.endSupportPos)) {
                return;
            }

            BlockState endState = level.getBlockState(banner.endSupportPos);
            boolean endValid = endState.isFaceSturdy(level, banner.endSupportPos, banner.endFace);

            if (!endValid) {
                banner.dropAndRemove(level, pos);
            }
            return;
        }
        banner.incompleteTicks++;
        if (banner.incompleteTicks > 20 * 30) {
            level.removeBlock(pos, false);
        }
    }

    private void dropAndRemove(Level level, BlockPos pos) {
        Block.popResource(level, pos, new ItemStack(ModItems.SEEMLESS_CURVE_BANNER_ITEM.get()));

        level.removeBlock(pos, false);
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);

        if (this.endSupportPos != null && this.endFace != null) {
            tag.putLong("ConnectionGameTime", this.connectionGameTime);
            tag.putLong("EndSupport", this.endSupportPos.asLong());
            tag.putInt("EndFace", this.endFace.get3DDataValue());
        }
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);

        this.endSupportPos = null;
        this.endFace = null;

        if (tag.contains("EndSupport") && tag.contains("EndFace")) {
            this.endSupportPos = BlockPos.of(tag.getLong("EndSupport"));
            this.endFace = Direction.from3DDataValue(tag.getInt("EndFace"));
            this.connectionGameTime = tag.contains("ConnectionGameTime") ? tag.getLong("ConnectionGameTime") : -1L;
        }
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = new CompoundTag();

        this.saveAdditional(tag);

        return tag;
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public AABB getRenderBoundingBox() {
        Vec3 start = this.getStartPoint();
        Vec3 end = this.getEndPoint();

        if (end == null) {
            return new AABB(this.worldPosition).inflate(1.0D);
        }
        return new AABB(Math.min(start.x, end.x) - 1.5D, Math.min(start.y, end.y) - 5.5D, Math.min(start.z, end.z) - 1.5D,
                Math.max(start.x, end.x) + 1.5D, Math.max(start.y, end.y) + 1.5D, Math.max(start.z, end.z) + 1.5D);
    }
}