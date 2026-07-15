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
        this.setChanged(); // Сообщаем игре, что данные изменились
        if (this.level != null && !this.level.isClientSide) {
            // Синхронизируем изменения с клиентом
            this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), 3);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        if (!this.storedItem.isEmpty()) {
            // Если предмет есть, сохраняем его
            tag.put("StoredItem", this.storedItem.save(new CompoundTag()));
            tag.putBoolean("IsEmpty", false);
        } else {
            // Если предмета нет, ЯВНО говорим об этом клиенту
            tag.putBoolean("IsEmpty", true);
        }
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        // Читаем предмет
        if (tag.contains("StoredItem")) {
            this.storedItem = ItemStack.of(tag.getCompound("StoredItem"));
        }
        // Если пришел флаг пустоты — стираем предмет на клиенте
        else if (tag.getBoolean("IsEmpty")) {
            this.storedItem = ItemStack.EMPTY;
        }
    }

    // Эти два метода нужны для правильной отправки данных на клиент при загрузке чанка
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