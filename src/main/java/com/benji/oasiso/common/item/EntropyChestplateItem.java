package com.benji.oasiso.common.item;

import com.benji.oasiso.Oasiso;
import com.benji.oasiso.common.entity.GlowmaskEntity;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.function.Consumer;

public class EntropyChestplateItem extends ArmorItem implements GeoItem, GlowmaskEntity {

    public static final String TAG_TURRETS_ON = "EntropyTurretsOn";
    private static final String TAG_TRANSITION = "EntropyTurretTransition";
    private static final String TAG_TRANSITION_END = "EntropyTurretTransitionEnd";

    public static final String CLIENT_FIRE_MASK = "EntropyTurretClientFireMask";
    public static final String CLIENT_FIRE_UNTIL = "EntropyTurretClientFireUntil";

    public static final int TRANSITION_NONE = 0;
    public static final int TRANSITION_ON = 1;
    public static final int TRANSITION_OFF = 2;

    public static final int TRANSITION_TICKS = 60; // 3 seconds
    public static final int FIRE_ANIMATION_TICKS = 5; // 0.25 seconds

    public static final int FIRE_LEFT_MASK = 1;
    public static final int FIRE_RIGHT_MASK = 2;

    private static final RawAnimation IDLE_OFF = RawAnimation.begin().thenLoop("idle_off");
    private static final RawAnimation IDLE_ON = RawAnimation.begin().thenLoop("idle_on");
    private static final RawAnimation TURRET_ON = RawAnimation.begin().thenPlay("turret_on");
    private static final RawAnimation TURRET_OFF = RawAnimation.begin().thenPlay("turret_off");
    private static final RawAnimation FIRE_LEFT = RawAnimation.begin().thenLoop("fire_left");
    private static final RawAnimation FIRE_RIGHT = RawAnimation.begin().thenLoop("fire_right");
    private static final RawAnimation FIRE_BOTH = RawAnimation.begin().thenLoop("fire_both");
    private static final RawAnimation PHYSIC = RawAnimation.begin().thenLoop("physic");

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public EntropyChestplateItem(Properties properties) {
        super(EntropyArmorMaterial.INSTANCE, Type.CHESTPLATE, properties.stacksTo(1));
    }

    public static boolean isTurretsOn(ItemStack stack) {
        return stack.getOrCreateTag().getBoolean(TAG_TURRETS_ON);
    }

    public static int getTransition(ItemStack stack) {
        return stack.getOrCreateTag().getInt(TAG_TRANSITION);
    }

    public static long getTransitionEnd(ItemStack stack) {
        return stack.getOrCreateTag().getLong(TAG_TRANSITION_END);
    }

    public static boolean isTransitionActive(ItemStack stack, long gameTime) {
        return getTransition(stack) != TRANSITION_NONE && gameTime < getTransitionEnd(stack);
    }

    public static boolean areTurretsOperational(ItemStack stack, long gameTime) {
        return isTurretsOn(stack) && !isTransitionActive(stack, gameTime);
    }

    public static void toggleTurrets(ItemStack stack, long gameTime) {
        boolean enable = !isTurretsOn(stack);
        setTurrets(stack, enable, gameTime);
    }

    public static void setTurrets(ItemStack stack, boolean enabled, long gameTime) {
        stack.getOrCreateTag().putBoolean(TAG_TURRETS_ON, enabled);
        stack.getOrCreateTag().putInt(TAG_TRANSITION, enabled ? TRANSITION_ON : TRANSITION_OFF);
        stack.getOrCreateTag().putLong(TAG_TRANSITION_END, gameTime + TRANSITION_TICKS);
    }

    @Override
    public boolean isEnchantable(ItemStack stack) {
        return false;
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            private com.benji.oasiso.client.renderer.EntropyChestplateRenderer renderer;

            @Override
            public HumanoidModel<?> getHumanoidArmorModel(
                    LivingEntity livingEntity,
                    ItemStack itemStack,
                    EquipmentSlot equipmentSlot,
                    HumanoidModel<?> original
            ) {
                if (this.renderer == null) {
                    this.renderer = new com.benji.oasiso.client.renderer.EntropyChestplateRenderer();
                }

                this.renderer.prepForRender(livingEntity, itemStack, equipmentSlot, original);
                return this.renderer;
            }
        });
    }

    @Override
    public void registerControllers(software.bernie.geckolib.core.animation.AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "mode_controller", 0, this::modePredicate));
        controllers.add(new AnimationController<>(this, "physics_controller", 5,
                state -> state.setAndContinue(PHYSIC)));
    }

    private PlayState modePredicate(AnimationState<EntropyChestplateItem> event) {
        Entity rawEntity = event.getData(DataTickets.ENTITY);

        if (!(rawEntity instanceof LivingEntity entity)) {
            return event.setAndContinue(IDLE_OFF);
        }

        ItemStack chest = entity.getItemBySlot(EquipmentSlot.CHEST);
        if (!(chest.getItem() instanceof EntropyChestplateItem)) {
            return event.setAndContinue(IDLE_OFF);
        }

        long gameTime = entity.level().getGameTime();
        int transition = getTransition(chest);

        if (gameTime < getTransitionEnd(chest)) {
            if (transition == TRANSITION_ON) {
                return event.setAndContinue(TURRET_ON);
            }

            if (transition == TRANSITION_OFF) {
                return event.setAndContinue(TURRET_OFF);
            }
        }

        if (!isTurretsOn(chest)) {
            return event.setAndContinue(IDLE_OFF);
        }

        if (entity.level().isClientSide
                && entity.getPersistentData().getLong(CLIENT_FIRE_UNTIL) > gameTime) {

            int fireMask = entity.getPersistentData().getInt(CLIENT_FIRE_MASK);

            if (fireMask == (FIRE_LEFT_MASK | FIRE_RIGHT_MASK)) {
                return event.setAndContinue(FIRE_BOTH);
            }

            if ((fireMask & FIRE_LEFT_MASK) != 0) {
                return event.setAndContinue(FIRE_LEFT);
            }

            if ((fireMask & FIRE_RIGHT_MASK) != 0) {
                return event.setAndContinue(FIRE_RIGHT);
            }
        }

        return event.setAndContinue(IDLE_ON);
    }

    @Override
    public ResourceLocation getGlowmaskTexture() {
        return ResourceLocation.fromNamespaceAndPath(
                Oasiso.MODID,
                "textures/models/armor/entropy_chestplate_emissive.png"
        );
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }
}
