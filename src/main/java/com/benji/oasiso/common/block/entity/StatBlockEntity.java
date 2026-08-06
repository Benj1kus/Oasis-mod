package com.benji.oasiso.common.block.entity;

import com.benji.oasiso.Oasiso;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class StatBlockEntity extends BlockEntity {
    private ItemStack storedItem = ItemStack.EMPTY;

    public StatBlockEntity(BlockPos pos, BlockState state) {
        super(Oasiso.STAT_BE.get(), pos, state);
    }

    public ItemStack getStoredItem() {
        return storedItem;
    }

    public void setStoredItem(ItemStack storedItem) {
        this.storedItem = storedItem;
        this.setChanged();
        if (this.level != null && !this.level.isClientSide) {
            this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), 3);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        if (!this.storedItem.isEmpty()) {
            tag.put("StoredItem", this.storedItem.save(new CompoundTag()));
            tag.putBoolean("IsEmpty", false);
        } else {
            tag.putBoolean("IsEmpty", true);
        }
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.contains("StoredItem")) {
            this.storedItem = ItemStack.of(tag.getCompound("StoredItem"));
        }
        else if (tag.getBoolean("IsEmpty")) {
            this.storedItem = ItemStack.EMPTY;
        }
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = super.getUpdateTag();
        saveAdditional(tag);
        return tag;
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}