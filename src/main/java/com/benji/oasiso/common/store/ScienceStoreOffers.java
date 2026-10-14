package com.benji.oasiso.common.store;

import com.benji.oasiso.Oasiso;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.registries.ForgeRegistries;

public final class ScienceStoreOffers {
    public static final int HOLD_TICKS = 20;
    public static final int OPEN_COOLDOWN = 20 * 30;
    public static final int COUNT = 3;
    private static final String[] IDS = {"entropy_chestplate_glove", "entropy_chestplate", "titana_hammer"};
    private static final int[] PRICES = {45, 30, 20};

    public static Item item(String path) {
        Item item = ForgeRegistries.ITEMS.getValue(ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, path));
        return item == null ? Items.AIR : item;
    }

    public static boolean valid(int offer) {
        return offer >= 0 && offer < COUNT;
    }

    public static int price(int offer) {
        return PRICES[offer];
    }

    public static ItemStack stack(int offer) {
        return new ItemStack(item(IDS[offer]));
    }

    public static Item currency() {
        return item("azumalit_shard");
    }

    private ScienceStoreOffers() {
    }
}
