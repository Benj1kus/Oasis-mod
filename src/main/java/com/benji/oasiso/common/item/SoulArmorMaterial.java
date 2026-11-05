package com.benji.oasiso.common.item;

import com.benji.oasiso.Oasiso;
import com.benji.oasiso.registry.ModItems;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.crafting.Ingredient;

public class SoulArmorMaterial implements ArmorMaterial {
    public static final SoulArmorMaterial INSTANCE = new SoulArmorMaterial();

    private static final int[] HEALTH_PER_SLOT = new int[]{13, 15, 16, 11};
    private static final int DURABILITY_MULTIPLIER = 22;

    @Override
    public int getDurabilityForType(ArmorItem.Type type) {
        return HEALTH_PER_SLOT[type.ordinal()] * DURABILITY_MULTIPLIER;
    }

    @Override
    public int getDefenseForType(ArmorItem.Type type) {
        return switch (type) {
            case HELMET -> 3;
            case CHESTPLATE -> 7;
            case LEGGINGS -> 6;
            case BOOTS -> 2;
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
        return 1.0F;
    }

    @Override
    public float getKnockbackResistance() {
        return 0.0F;
    }
}