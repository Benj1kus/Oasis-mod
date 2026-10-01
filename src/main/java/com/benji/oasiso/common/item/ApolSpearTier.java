package com.benji.oasiso.common.item;

import com.benji.oasiso.registry.ModItems;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.crafting.Ingredient;

public enum ApolSpearTier implements Tier {
    INSTANCE;

    @Override
    public int getUses() {
        return 600;
    }

    @Override
    public float getSpeed() {
        return 9;
    }

    @Override
    public float getAttackDamageBonus() {
        return 0;
    }

    @Override
    public int getLevel() {
        return 4;
    }

    @Override
    public int getEnchantmentValue() {
        return 22;
    }

    @Override
    public Ingredient getRepairIngredient() {
        return Ingredient.of(ModItems.AZUMALIT_SHARD.get());
    }
}
