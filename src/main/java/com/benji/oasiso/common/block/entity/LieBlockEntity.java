package com.benji.oasiso.common.block.entity;

import com.benji.oasiso.Oasiso;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import com.benji.oasiso.common.block.NephritisBlock;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public class LieBlockEntity extends BlockEntity {
    private BlockState mimicState = null;
    private boolean isPhasing = false;

    public LieBlockEntity(BlockPos pos, BlockState state) {
        super(Oasiso.LIE_BLOCK_BE.get(), pos, state);
    }

    public BlockState getMimicState() {
        return this.mimicState;
    }

    public boolean isPhasing() {
        return this.isPhasing;
    }

    @Nullable
    private BlockPos nephritisSourcePos;

    public void setMimicState(BlockState state) {
        this.mimicState = state;
        this.setChanged();
        if (this.level != null) {
            this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), 3);
        }
    }

    public void setPhasing(boolean phasing) {
        this.isPhasing = phasing;

        if (phasing && this.level instanceof ServerLevel) {
            refreshNephritisSource(null);
        }

        this.setChanged();

        if (this.level != null) {
            this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), 3);
        }
    }

    public boolean hasNephritisSource() {
        return this.nephritisSourcePos != null;
    }

    public BlockPos getFadeOriginPos() {
        return this.nephritisSourcePos != null ? this.nephritisSourcePos : this.worldPosition;
    }

    public void considerNephritisSource(BlockPos sourcePos) {
        if (distanceSqr(sourcePos, this.worldPosition) > NephritisBlock.MIMIC_EFFECT_RADIUS * NephritisBlock.MIMIC_EFFECT_RADIUS) {
            return;
        }

        if (this.nephritisSourcePos == null || distanceSqr(sourcePos, this.worldPosition) < distanceSqr(this.nephritisSourcePos, this.worldPosition)) {

            setNephritisSource(sourcePos);
        }
    }

    public void onNephritisRemoved(BlockPos removedPos) {
        if (Objects.equals(this.nephritisSourcePos, removedPos)) {
            refreshNephritisSource(removedPos);
        }
    }

    private void setNephritisSource(@Nullable BlockPos sourcePos) {
        BlockPos immutableSource = sourcePos == null ? null : sourcePos.immutable();

        if (Objects.equals(this.nephritisSourcePos, immutableSource)) {
            return;
        }

        this.nephritisSourcePos = immutableSource;
        this.setChanged();

        if (this.level != null) {
            this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), 3);
        }
    }

    private void refreshNephritisSource(@Nullable BlockPos ignoredPos) {
        if (!(this.level instanceof ServerLevel serverLevel)) {
            return;
        }

        int radius = NephritisBlock.MIMIC_EFFECT_RADIUS;
        int radiusSqr = radius * radius;

        BlockPos nearestSource = null;
        double nearestDistanceSqr = Double.MAX_VALUE;

        BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();

        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    int currentDistanceSqr = x * x + y * y + z * z;

                    if (currentDistanceSqr > radiusSqr) {
                        continue;
                    }

                    mutablePos.set(this.worldPosition.getX() + x, this.worldPosition.getY() + y, this.worldPosition.getZ() + z);

                    if (ignoredPos != null && mutablePos.equals(ignoredPos)) {
                        continue;
                    }

                    if (!serverLevel.getBlockState(mutablePos).is(Oasiso.NEPHRITIS_BLOCK.get())) {
                        continue;
                    }

                    double distanceSqr = distanceSqr(mutablePos, this.worldPosition);

                    if (distanceSqr < nearestDistanceSqr) {
                        nearestDistanceSqr = distanceSqr;
                        nearestSource = mutablePos.immutable();
                    }
                }
            }
        }

        setNephritisSource(nearestSource);
    }

    private static double distanceSqr(BlockPos first, BlockPos second) {
        long x = first.getX() - second.getX();
        long y = first.getY() - second.getY();
        long z = first.getZ() - second.getZ();

        return (double) (x * x + y * y + z * z);
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        if (this.mimicState != null) {
            tag.put("MimicState", NbtUtils.writeBlockState(this.mimicState));
        }
        tag.putBoolean("IsPhasing", this.isPhasing);

        if (this.nephritisSourcePos != null) {
            tag.putLong("NephritisSource", this.nephritisSourcePos.asLong());
        }
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.contains("MimicState")) {
            this.mimicState = NbtUtils.readBlockState(BuiltInRegistries.BLOCK.asLookup(), tag.getCompound("MimicState"));
        } else {
            this.mimicState = null;
        }
        this.isPhasing = tag.getBoolean("IsPhasing");

        if (tag.contains("NephritisSource", Tag.TAG_LONG)) {
            this.nephritisSourcePos = BlockPos.of(tag.getLong("NephritisSource"));
        } else {
            this.nephritisSourcePos = null;
        }
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag() {
        return this.saveWithoutMetadata();
    }
}