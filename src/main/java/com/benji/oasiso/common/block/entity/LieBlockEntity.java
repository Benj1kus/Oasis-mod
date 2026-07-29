package com.benji.oasiso.common.block.entity;

import com.benji.oasiso.Oasiso;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class LieBlockEntity extends BlockEntity {
    private BlockState mimicState = null;
    private boolean isPhasing = false;

    public LieBlockEntity(BlockPos pos, BlockState state) {
        super(Oasiso.LIE_BLOCK_BE.get(), pos, state);
    }

    public BlockState getMimicState() { return this.mimicState; }
    public boolean isPhasing() { return this.isPhasing; }

    public void setMimicState(BlockState state) {
        this.mimicState = state;
        this.setChanged();
        if (this.level != null) {
            this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), 3);
        }
    }

    public void setPhasing(boolean phasing) {
        this.isPhasing = phasing;
        this.setChanged();
        if (this.level != null) {
            this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), 3);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        if (this.mimicState != null) {
            tag.put("MimicState", NbtUtils.writeBlockState(this.mimicState));
        }
        tag.putBoolean("IsPhasing", this.isPhasing);
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