package com.benji.oasiso.common.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.item.ItemStack;
import java.util.ArrayList;
import java.util.List;

public final class FaultEffect extends MobEffect {
    public FaultEffect() { super(MobEffectCategory.HARMFUL, 0x5FFFCC); }
    @Override public List<ItemStack> getCurativeItems() { return new ArrayList<>(); }
}
