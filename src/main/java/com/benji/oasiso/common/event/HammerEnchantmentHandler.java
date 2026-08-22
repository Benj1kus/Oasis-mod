package com.benji.oasiso.common.event;

import com.benji.oasiso.Oasiso;
import com.benji.oasiso.common.item.TitanaHammerItem;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraftforge.event.AnvilUpdateEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Oasiso.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class HammerEnchantmentHandler {

    private static final int ENCHANTMENT_COST = 20;

    private HammerEnchantmentHandler() {
    }

    @SubscribeEvent
    public static void onAnvilUpdate(AnvilUpdateEvent event) {
        ItemStack hammerStack = event.getLeft();
        ItemStack bookStack = event.getRight();

        if (!(hammerStack.getItem() instanceof TitanaHammerItem)) {
            return;
        }

        if (!bookStack.is(Oasiso.ENCHANTED_BOOK_HAMMER.get())) {
            return;
        }


        if (EnchantmentHelper.getItemEnchantmentLevel(Oasiso.HAMMER_POWER.get(), hammerStack) > 0) {
            return;
        }


        ItemStack result = hammerStack.copy();
        result.setCount(1);

        result.enchant(Oasiso.HAMMER_POWER.get(), 1);


        result.getOrCreateTag().putInt("TitanaHits", 0);


        String newName = event.getName();

        if (newName != null) {
            if (newName.isBlank()) {
                result.resetHoverName();
            } else {
                result.setHoverName(Component.literal(newName));
            }
        }

        event.setOutput(result);


        event.setCost(ENCHANTMENT_COST);


        event.setMaterialCost(1);
    }
}