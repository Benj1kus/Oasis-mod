package com.benji.oasiso.common.item;

import com.benji.oasiso.registry.ModEffects;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MilkBucketItem;
import net.minecraft.world.level.Level;

public final class QuiMilkBucketItem extends MilkBucketItem {
    public QuiMilkBucketItem(Properties properties) {
        super(properties);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity drinker) {
        if (drinker instanceof ServerPlayer player) {
            CriteriaTriggers.CONSUME_ITEM.trigger(player, stack);
            player.awardStat(Stats.ITEM_USED.get(this));
        }
        if (!level.isClientSide) {
            int seconds = 5 + drinker.getRandom().nextInt(11);
            drinker.addEffect(new MobEffectInstance(ModEffects.AIR_RUSH.get(), seconds * 20));
        }
        if (drinker instanceof Player player && player.getAbilities().instabuild) return stack;
        return new ItemStack(Items.BUCKET);
    }
}
