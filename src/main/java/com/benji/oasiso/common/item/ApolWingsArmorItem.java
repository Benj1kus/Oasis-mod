package com.benji.oasiso.common.item;

import com.benji.oasiso.registry.ModItems;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.function.Consumer;

public class ApolWingsArmorItem extends ArmorItem implements GeoItem {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    private static final RawAnimation FLY = RawAnimation.begin().thenLoop("fly");
    private static final RawAnimation JUMP = RawAnimation.begin().thenPlayAndHold("jump");

    @Override
    public boolean canElytraFly(ItemStack stack, LivingEntity entity) {
        return !stack.isDamageableItem() || stack.getDamageValue() < stack.getMaxDamage() - 1;
    }

    @Override
    public boolean elytraFlightTick(ItemStack stack, LivingEntity entity, int flightTicks) {
        if (!canElytraFly(stack, entity)) return false;
        if (!entity.level().isClientSide && (flightTicks + 1) % 20 == 0) {
            stack.hurtAndBreak(1, entity, e -> e.broadcastBreakEvent(EquipmentSlot.CHEST));
        }
        if ((flightTicks + 1) % 10 == 0) entity.gameEvent(net.minecraft.world.level.gameevent.GameEvent.ELYTRA_GLIDE);
        return true;
    }

    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle");

    public ApolWingsArmorItem(Type type, Properties properties) {
        super(ApolWingsArmorMaterial.INSTANCE, type, properties.fireResistant());
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            private com.benji.oasiso.client.renderer.ApolWingsRenderer renderer;

            @Override
            public HumanoidModel<?> getHumanoidArmorModel(LivingEntity livingEntity, ItemStack itemStack, EquipmentSlot equipmentSlot, HumanoidModel<?> original) {
                if (this.renderer == null) {
                    this.renderer = new com.benji.oasiso.client.renderer.ApolWingsRenderer();
                }
                this.renderer.prepForRender(livingEntity, itemStack, equipmentSlot, original);
                return this.renderer;
            }
        });
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "idle_controller", 0, state -> {
            var entity = state.getData(software.bernie.geckolib.constant.DataTickets.ENTITY);
            if (entity instanceof LivingEntity living) {
                long end = living.getPersistentData().getLong(com.benji.oasiso.common.wings.ApolWingsMechanics.JUMP_UNTIL);
                if (end > living.level().getGameTime()) return state.setAndContinue(JUMP);
                if (living.isFallFlying()) return state.setAndContinue(FLY);
            }
            return state.setAndContinue(IDLE);
        }));
    }

    @Override
    public boolean isValidRepairItem(ItemStack toRepair, ItemStack repair) {
        return repair.is(ModItems.AZUMALIT_SHARD.get()) || super.isValidRepairItem(toRepair, repair);
    }

    @Override
    public boolean isEnchantable(ItemStack stack) {
        return true;
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }
}