package com.benji.oasiso.common.item;

import com.benji.oasiso.Oasiso;
import com.benji.oasiso.registry.ModItems;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.crafting.Ingredient;

public class AzumalitTier implements Tier {
    public static final AzumalitTier INSTANCE = new AzumalitTier();

    @Override
    public int getUses() { return 1500; }

    @Override
    public float getSpeed() { return 9.0F; }

    @Override
    public float getAttackDamageBonus() { return 5.0F; }

    @Override
    public int getLevel() { return 4; }

    @Override
    public int getEnchantmentValue() { return 22; }

    @Override
    public Ingredient getRepairIngredient() {
        return Ingredient.of(ModItems.AZUMALIT_SHARD.get());
    }
}