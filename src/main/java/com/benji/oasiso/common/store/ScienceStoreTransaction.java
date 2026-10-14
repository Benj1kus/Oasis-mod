package com.benji.oasiso.common.store;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public final class ScienceStoreTransaction {
    public enum Result {OK, NOT_ENOUGH, NO_SPACE, UNAVAILABLE}

    public static int balance(Inventory inventory, Item currency) {
        int amount = 0;
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack s = inventory.getItem(i);
            if (!s.isEmpty() && s.is(currency)) amount += s.getCount();
        }
        return amount;
    }

    public static Result purchase(Inventory inventory, Item currency, int price, ItemStack reward) {
        if (reward.isEmpty() || price <= 0) return Result.UNAVAILABLE;
        if (balance(inventory, currency) < price) return Result.NOT_ENOUGH;
        List<ItemStack> plan = new ArrayList<>();
        for (int i = 0; i < inventory.getContainerSize(); i++) plan.add(inventory.getItem(i).copy());
        int left = price;
        for (ItemStack s : plan) {
            if (!s.isEmpty() && s.is(currency) && left > 0) {
                int take = Math.min(left, s.getCount());
                s.shrink(take);
                left -= take;
            }
        }
        ItemStack remaining = reward.copy();
        for (int i = 0; i < 36 && !remaining.isEmpty(); i++) {
            ItemStack s = plan.get(i);
            if (!s.isEmpty() && ItemStack.isSameItemSameTags(s, remaining)) {
                int add = Math.min(remaining.getCount(), Math.max(0, Math.min(s.getMaxStackSize(), inventory.getMaxStackSize()) - s.getCount()));
                s.grow(add);
                remaining.shrink(add);
            }
        }
        for (int i = 0; i < 36 && !remaining.isEmpty(); i++) {
            if (plan.get(i).isEmpty()) {
                int add = Math.min(remaining.getCount(), Math.min(remaining.getMaxStackSize(), inventory.getMaxStackSize()));
                ItemStack inserted = remaining.copy();
                inserted.setCount(add);
                plan.set(i, inserted);
                remaining.shrink(add);
            }
        }
        if (!remaining.isEmpty()) return Result.NO_SPACE;
        for (int i = 0; i < plan.size(); i++) {
            if (!ItemStack.matches(inventory.getItem(i), plan.get(i))) inventory.setItem(i, plan.get(i));
        }
        inventory.setChanged();
        return Result.OK;
    }

    private ScienceStoreTransaction() {
    }
}
