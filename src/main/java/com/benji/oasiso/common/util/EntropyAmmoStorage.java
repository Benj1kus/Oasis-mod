package com.benji.oasiso.common.util;

import com.benji.oasiso.registry.ModItems;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.item.ArrowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class EntropyAmmoStorage {

    public static final int SLOT_COUNT = 12;

    private static final String TAG_AMMO_INVENTORY = "EntropyAmmoInventory";
    private static final String TAG_BLAZE_REMAINDER = "EntropyBlazeShotRemainder";

    private EntropyAmmoStorage() {
    }

    public enum AmmoType {
        ARROW, BLAZE, CACTUS_SPIKE, CHAOS_BOMB
    }

    public static final class ShotAmmo {
        private final AmmoType type;
        private final ItemStack stack;

        private ShotAmmo(AmmoType type, ItemStack stack) {
            this.type = type;
            this.stack = stack;
        }

        public AmmoType type() {
            return this.type;
        }
        public ItemStack stack() {
            return this.stack;
        }
    }

    public static NonNullList<ItemStack> getItems(ItemStack chestplate) {
        NonNullList<ItemStack> items = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);

        CompoundTag root = chestplate.getTag();
        if (root == null || !root.contains(TAG_AMMO_INVENTORY, Tag.TAG_COMPOUND)) {
            return items;
        }

        ContainerHelper.loadAllItems(root.getCompound(TAG_AMMO_INVENTORY), items);
        return items;
    }

    public static void setItems(ItemStack chestplate, NonNullList<ItemStack> items) {
        CompoundTag inventoryTag = new CompoundTag();
        ContainerHelper.saveAllItems(inventoryTag, items, true);
        chestplate.getOrCreateTag().put(TAG_AMMO_INVENTORY, inventoryTag);
    }

    public static ItemStack getItem(ItemStack chestplate, int slot) {
        if (slot < 0 || slot >= SLOT_COUNT) {
            return ItemStack.EMPTY;
        }
        return getItems(chestplate).get(slot);
    }

    public static boolean isAllowedAmmo(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }

        if (stack.getItem() instanceof ArrowItem || stack.is(ItemTags.ARROWS)) {
            return true;
        }

        return stack.is(Items.BLAZE_POWDER) || stack.is(ModItems.CACTUS_SPIKE.get()) || stack.is(ModItems.CHAOS_BOMB_ITEM.get());
    }

    public static AmmoType getAmmoType(ItemStack stack) {
        if (stack.isEmpty()) {
            return null;
        }

        if (stack.getItem() instanceof ArrowItem || stack.is(ItemTags.ARROWS)) {
            return AmmoType.ARROW;
        }
        if (stack.is(Items.BLAZE_POWDER)) {
            return AmmoType.BLAZE;
        }
        if (stack.is(ModItems.CACTUS_SPIKE.get())) {
            return AmmoType.CACTUS_SPIKE;
        }
        if (stack.is(ModItems.CHAOS_BOMB_ITEM.get())) {
            return AmmoType.CHAOS_BOMB;
        }

        return null;
    }
    public static int insert(ItemStack chestplate, ItemStack source) {
        if (!isAllowedAmmo(source)) {
            return 0;
        }

        int before = source.getCount();
        NonNullList<ItemStack> items = getItems(chestplate);
        for (int i = 0; i < SLOT_COUNT && !source.isEmpty(); i++) {
            ItemStack existing = items.get(i);
            if (existing.isEmpty() || !ItemStack.isSameItemSameTags(existing, source)) {
                continue;
            }

            int max = Math.min(existing.getMaxStackSize(), source.getMaxStackSize());
            int room = max - existing.getCount();
            if (room <= 0) {
                continue;
            }

            int moved = Math.min(room, source.getCount());
            existing.grow(moved);
            source.shrink(moved);
        }
        for (int i = 0; i < SLOT_COUNT && !source.isEmpty(); i++) {
            if (!items.get(i).isEmpty()) {
                continue;
            }

            int moved = Math.min(source.getCount(), source.getMaxStackSize());
            ItemStack inserted = source.copy();
            inserted.setCount(moved);
            items.set(i, inserted);
            source.shrink(moved);
        }

        int inserted = before - source.getCount();
        if (inserted > 0) {
            setItems(chestplate, items);
        }
        return inserted;
    }

    public static ItemStack extract(ItemStack chestplate, int slot) {
        if (slot < 0 || slot >= SLOT_COUNT) {
            return ItemStack.EMPTY;
        }

        NonNullList<ItemStack> items = getItems(chestplate);
        ItemStack result = items.get(slot);
        if (result.isEmpty()) {
            return ItemStack.EMPTY;
        }

        items.set(slot, ItemStack.EMPTY);
        setItems(chestplate, items);
        return result;
    }
    public static ShotAmmo takeNextShot(ItemStack chestplate) {
        NonNullList<ItemStack> items = getItems(chestplate);

        for (int i = 0; i < SLOT_COUNT; i++) {
            ItemStack ammo = items.get(i);
            AmmoType type = getAmmoType(ammo);
            if (type == null) {
                continue;
            }

            ItemStack shotStack = ammo.copy();
            shotStack.setCount(1);

            if (type == AmmoType.BLAZE) {
                int remainder = chestplate.getOrCreateTag().getInt(TAG_BLAZE_REMAINDER);

                if (remainder == 0) {
                    chestplate.getOrCreateTag().putInt(TAG_BLAZE_REMAINDER, 1);
                } else {
                    ammo.shrink(1);
                    chestplate.getOrCreateTag().putInt(TAG_BLAZE_REMAINDER, 0);
                    if (ammo.isEmpty()) {
                        items.set(i, ItemStack.EMPTY);
                    }
                }
            } else {
                ammo.shrink(1);
                if (ammo.isEmpty()) {
                    items.set(i, ItemStack.EMPTY);
                }
            }

            setItems(chestplate, items);
            return new ShotAmmo(type, shotStack);
        }

        return null;
    }
    public static int countPossibleShots(ItemStack chestplate, int maxShots) {
        if (maxShots <= 0) {
            return 0;
        }

        ItemStack copy = chestplate.copy();
        int shots = 0;

        while (shots < maxShots && takeNextShot(copy) != null) {
            shots++;
        }

        return shots;
    }
}
