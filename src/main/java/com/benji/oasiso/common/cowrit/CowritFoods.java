package com.benji.oasiso.common.cowrit;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.food.Foods;

public final class CowritFoods {
    public static final FoodProperties RAW = new FoodProperties.Builder().nutrition(Foods.BEEF.getNutrition()).saturationMod(Foods.BEEF.getSaturationModifier()).meat().effect(() -> new MobEffectInstance(MobEffects.HUNGER, 600), 0.30F).build();
    public static final FoodProperties SMOKED = Foods.COOKED_BEEF;

    private CowritFoods() {
    }
}
