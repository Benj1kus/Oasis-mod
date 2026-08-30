package com.benji.oasiso.common.item;

import com.benji.oasiso.Oasiso;
import com.benji.oasiso.registry.ModItems;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.crafting.Ingredient;

public class AzumalitArmorMaterial implements ArmorMaterial {
    public static final AzumalitArmorMaterial INSTANCE = new AzumalitArmorMaterial();

    private static final int[] HEALTH_PER_SLOT = new int[]{13, 15, 16, 11};
    private static final int DURABILITY_MULTIPLIER = 65;

    @Override
    public int getDurabilityForType(ArmorItem.Type type) {
        return HEALTH_PER_SLOT[type.ordinal()] * DURABILITY_MULTIPLIER;
    }

    @Override
    public int getDefenseForType(ArmorItem.Type type) {
        return switch (type) {
            case HELMET -> 5;
            case CHESTPLATE -> 12;
            case LEGGINGS -> 9;
            case BOOTS -> 5;
        };
    }

    @Override
    public int getEnchantmentValue() {
        return 15;
    }

    @Override
    public SoundEvent getEquipSound() {
        return SoundEvents.ARMOR_EQUIP_GOLD;
    }

    @Override
    public Ingredient getRepairIngredient() {
        return Ingredient.of(ModItems.AZUMALIT_SHARD.get());
    }

    @Override
    public String getName() {
        return Oasiso.MODID + ":azumalit_armor";
    }

    @Override
    public float getToughness() {
        return 3.75F;
    }

    @Override
    public float getKnockbackResistance() {
        return 0.075F;
    }
}
