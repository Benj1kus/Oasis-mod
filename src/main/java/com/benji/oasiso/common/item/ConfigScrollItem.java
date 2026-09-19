package com.benji.oasiso.common.item;

import net.minecraft.ChatFormatting; // (убедись, что правильный импорт net.minecraft...)
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class ConfigScrollItem extends Item {

    public ConfigScrollItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        tooltipComponents.add(Component.translatable("tooltip.oasiso.scroll").withStyle(ChatFormatting.AQUA));

        super.appendHoverText(stack, level, tooltipComponents, tooltipFlag);
    }
}