package com.benji.oasiso.common.item;

import com.benji.oasiso.Oasiso;
import net.minecraft.ChatFormatting;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Consumer;

public class SuperGoldArmorItem extends ArmorItem implements GeoItem {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation WALK = RawAnimation.begin().thenLoop("walk");
    private static final RawAnimation RUN = RawAnimation.begin().thenLoop("run");
    private static final RawAnimation JUMP = RawAnimation.begin().thenLoop("jump");
    private static final RawAnimation ROTATE_RIGHT = RawAnimation.begin().thenLoop("rotate_right");
    private static final RawAnimation ROTATE_LEFT = RawAnimation.begin().thenLoop("rotate_left");

    public SuperGoldArmorItem(Type type, Properties properties) {
        super(SuperGoldArmorMaterial.INSTANCE, type, properties.fireResistant());
    }

    @Override
    public boolean isValidRepairItem(ItemStack toRepair, ItemStack repair) {
        return repair.is(Oasiso.KARAKOLIT_INGOT.get()) || super.isValidRepairItem(toRepair, repair);
    }

    @Override
    public boolean isEnchantable(ItemStack stack) {
        return true;
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            private com.benji.oasiso.client.renderer.SuperGoldArmorRenderer renderer;

            @Override
            public HumanoidModel<?> getHumanoidArmorModel(LivingEntity livingEntity, ItemStack itemStack, EquipmentSlot equipmentSlot, HumanoidModel<?> original) {
                if (this.renderer == null) {
                    this.renderer = new com.benji.oasiso.client.renderer.SuperGoldArmorRenderer();
                }
                this.renderer.prepForRender(livingEntity, itemStack, equipmentSlot, original);
                return this.renderer;
            }
        });
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltipComponents, TooltipFlag isAdvanced) {

        ArmorItem.Type type = this.getType();

        switch (type) {
            case CHESTPLATE -> {
                tooltipComponents.add(Component.translatable("tooltip.oasiso.karak_chest").withStyle(ChatFormatting.GOLD));
            }
        }

        tooltipComponents.add(Component.empty());
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "armor_controller", 5, this::predicate));
    }

    private PlayState predicate(AnimationState<SuperGoldArmorItem> event) {
        Entity rawEntity = event.getData(DataTickets.ENTITY);
        if (!(rawEntity instanceof LivingEntity entity)) {
            return PlayState.CONTINUE;
        }

// JUMP
        if (!entity.onGround()) {
            return event.setAndContinue(JUMP);
        }

// RUN / WALK
        double speed = entity.getDeltaMovement().horizontalDistance();

        if (speed > 0.01) {
            if (entity.isSprinting()) {
                return event.setAndContinue(RUN);
            } else {
                return event.setAndContinue(WALK);
            }
        }
        //BODY ROTATION
        float yawDelta = Mth.wrapDegrees(entity.yBodyRot - entity.yBodyRotO);

        if (yawDelta > 1.5F) {
            return event.setAndContinue(ROTATE_RIGHT);
        } else if (yawDelta < -1.5F) {
            return event.setAndContinue(ROTATE_LEFT);
        }
        return event.setAndContinue(IDLE); //IDLE
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }
}