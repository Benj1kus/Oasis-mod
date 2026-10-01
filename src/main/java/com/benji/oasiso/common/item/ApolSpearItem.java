package com.benji.oasiso.common.item;

import com.benji.oasiso.client.renderer.ApolSpearRenderer;
import com.benji.oasiso.registry.ModItems;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.level.Level;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import net.minecraftforge.common.ToolAction;
import net.minecraftforge.common.ToolActions;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.SingletonGeoAnimatable;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.function.Consumer;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraftforge.common.ForgeMod;

import java.util.UUID;

public class ApolSpearItem extends SwordItem implements GeoItem {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation ATTACK = RawAnimation.begin().thenPlay("attack");

    private static final UUID SPEAR_REACH_UUID = UUID.fromString("50388c39-63a4-4b57-93e3-dd820096a801");

    public ApolSpearItem(Properties properties) {
        super(ApolSpearTier.INSTANCE, 14, -3F, properties.durability(600));
        SingletonGeoAnimatable.registerSyncedAnimatable(this);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        super.inventoryTick(stack, level, entity, slot, selected);
        if (level instanceof ServerLevel server) GeoItem.getOrAssignId(stack, server);
    }

    @Override
    public boolean onLeftClickEntity(ItemStack stack, Player player, Entity target) {
        if (player.level().isClientSide) return false;
        return !ApolSpearCombat.begin(player, stack, this, true);
    }

    @Override
    public Multimap<Attribute, AttributeModifier> getDefaultAttributeModifiers(EquipmentSlot slot) {
        Multimap<Attribute, AttributeModifier> original = super.getDefaultAttributeModifiers(slot);

        if (slot != EquipmentSlot.MAINHAND) return original;

        return ImmutableMultimap.<Attribute, AttributeModifier>builder().putAll(original).put(ForgeMod.ENTITY_REACH.get(), new AttributeModifier(SPEAR_REACH_UUID, "Apollyon spear reach", 2.0D, AttributeModifier.Operation.ADDITION)).build();
    }

    @Override
    public boolean onEntitySwing(ItemStack stack, LivingEntity entity) {
        if (entity.getMainHandItem() != stack) return false;
        if (!entity.level().isClientSide && entity instanceof Player player)
            ApolSpearCombat.begin(player, stack, this, false);
        return true;
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (!attacker.level().isClientSide) {
            if (attacker instanceof Player player) ApolSpearCombat.hit(player, target, stack);
            stack.hurtAndBreak(5, attacker, e -> e.broadcastBreakEvent(InteractionHand.MAIN_HAND));
        }
        return true;
    }

    @Override
    public boolean canPerformAction(ItemStack stack, ToolAction action) {
        return action != ToolActions.SWORD_SWEEP && super.canPerformAction(stack, action);
    }

    @Override
    public boolean isValidRepairItem(ItemStack stack, ItemStack ingredient) {
        return ingredient.is(ModItems.AZUMALIT_SHARD.get());
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            private ApolSpearRenderer renderer;
            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (renderer == null) renderer = new ApolSpearRenderer();
                return renderer;
            }
        });
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 0, state -> state.setAndContinue(IDLE)).triggerableAnim("attack", ATTACK));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
