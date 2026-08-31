package com.benji.oasiso.common.item;

import com.benji.oasiso.client.renderer.AzumalitArmorRenderer;
import com.benji.oasiso.registry.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import org.jetbrains.annotations.NotNull;
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

public class AzumalitArmorItem extends ArmorItem implements GeoItem {
    public static final String TRIGGER_DEFEND = "defend";
    public static final String TRIGGER_DEFEND_PROJECTILE = "defend_proj";

    public static final String TRIGGER_ATTACK_LEFT = "attack_left";
    public static final String TRIGGER_ATTACK_RIGHT = "attack_right";
    public static final String TRIGGER_ATTACK_BOTH = "attack_both";
    public static final String TRIGGER_WAYPOINT = "waypont";
    public static final String TRIGGER_CHAIN = "chain";

    public static final int ATTACK_MODE_NONE = 0;
    public static final int ATTACK_MODE_LEFT = 1;
    public static final int ATTACK_MODE_RIGHT = 2;
    public static final int ATTACK_MODE_BOTH = 3;

    private static final String TAG_GUARD_ANIMATION = "OasisoAzumalitGuardAnimation";
    private static final String TAG_GUARD_ANIMATION_UNTIL = "OasisoAzumalitGuardAnimationUntil";

    private static final int ARMOR_ANIMATION_NONE = 0;
    private static final int ARMOR_ANIMATION_DEFEND = 1;
    private static final int ARMOR_ANIMATION_DEFEND_PROJECTILE = 2;
    private static final int ARMOR_ANIMATION_ATTACK_LEFT = 3;
    private static final int ARMOR_ANIMATION_ATTACK_RIGHT = 4;
    private static final int ARMOR_ANIMATION_ATTACK_BOTH = 5;
    private static final int ARMOR_ANIMATION_WAYPOINT = 6;
    private static final int ARMOR_ANIMATION_CHAIN = 7;

    public static final int ATTACK_ANIMATION_TICKS = 25;
    public static final int WAYPOINT_ANIMATION_TICKS = 70;
    public static final int CHAIN_ANIMATION_TICKS = 50;
    public static final int ATTACK_DAMAGE_KEY_TICK = 10;

    private static final int DEFEND_ANIMATION_TICKS = 40;
    private static final int DEFEND_PROJECTILE_ANIMATION_TICKS = 10;

    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation DEFEND = RawAnimation.begin().thenPlay("defend");
    private static final RawAnimation DEFEND_PROJECTILE = RawAnimation.begin().thenPlay("defend_proj");
    private static final RawAnimation ATTACK_LEFT = RawAnimation.begin().thenPlay("attack_left");
    private static final RawAnimation ATTACK_RIGHT = RawAnimation.begin().thenPlay("attack_right");
    private static final RawAnimation ATTACK_BOTH = RawAnimation.begin().thenPlay("attack_both");
    private static final RawAnimation WAYPOINT = RawAnimation.begin().thenPlay("waypont");
    private static final RawAnimation CHAIN = RawAnimation.begin().thenPlay("chain");

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public AzumalitArmorItem(Type type, Properties properties) {
        super(AzumalitArmorMaterial.INSTANCE, type, properties.fireResistant());
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            private AzumalitArmorRenderer renderer;

            @Override
            public @NotNull HumanoidModel<?> getHumanoidArmorModel(LivingEntity livingEntity, ItemStack itemStack, EquipmentSlot equipmentSlot, HumanoidModel<?> original) {
                if (this.renderer == null) {
                    this.renderer = new AzumalitArmorRenderer();
                }
                this.renderer.prepForRender(livingEntity, itemStack, equipmentSlot, original);
                return this.renderer;
            }
        });
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "armor_controller", 0, this::armorPredicate));
    }

    private PlayState armorPredicate(AnimationState<AzumalitArmorItem> state) {
        Entity rawEntity = state.getData(DataTickets.ENTITY);

        if (!(rawEntity instanceof LivingEntity entity)) {
            return state.setAndContinue(IDLE);
        }

        int animation = getActiveArmorAnimation(entity);

        return switch (animation) {
            case ARMOR_ANIMATION_DEFEND -> state.setAndContinue(DEFEND);
            case ARMOR_ANIMATION_DEFEND_PROJECTILE -> state.setAndContinue(DEFEND_PROJECTILE);

            case ARMOR_ANIMATION_ATTACK_LEFT -> state.setAndContinue(ATTACK_LEFT);
            case ARMOR_ANIMATION_ATTACK_RIGHT -> state.setAndContinue(ATTACK_RIGHT);
            case ARMOR_ANIMATION_ATTACK_BOTH -> state.setAndContinue(ATTACK_BOTH);
            case ARMOR_ANIMATION_WAYPOINT -> state.setAndContinue(WAYPOINT);
            case ARMOR_ANIMATION_CHAIN -> state.setAndContinue(CHAIN);

            default -> state.setAndContinue(IDLE);
        };
    }

    public static void triggerChestAnimation(LivingEntity wearer, String animationName) {
        if (wearer == null || wearer.level().isClientSide) {
            return;
        }

        ItemStack chest = wearer.getItemBySlot(EquipmentSlot.CHEST);

        if (!chest.is(ModItems.AZUMALIT_CHESTPLATE.get())) {
            return;
        }

        int animation;
        int durationTicks;

        if (TRIGGER_DEFEND.equals(animationName)) {
            animation = ARMOR_ANIMATION_DEFEND;
            durationTicks = DEFEND_ANIMATION_TICKS;
        } else if (TRIGGER_DEFEND_PROJECTILE.equals(animationName)) {
            animation = ARMOR_ANIMATION_DEFEND_PROJECTILE;
            durationTicks = DEFEND_PROJECTILE_ANIMATION_TICKS;
        } else if (TRIGGER_ATTACK_LEFT.equals(animationName)) {
            animation = ARMOR_ANIMATION_ATTACK_LEFT;
            durationTicks = ATTACK_ANIMATION_TICKS;
        } else if (TRIGGER_ATTACK_RIGHT.equals(animationName)) {
            animation = ARMOR_ANIMATION_ATTACK_RIGHT;
            durationTicks = ATTACK_ANIMATION_TICKS;
        } else if (TRIGGER_ATTACK_BOTH.equals(animationName)) {
            animation = ARMOR_ANIMATION_ATTACK_BOTH;
            durationTicks = ATTACK_ANIMATION_TICKS;
        } else if (TRIGGER_WAYPOINT.equals(animationName)) {
            animation = ARMOR_ANIMATION_WAYPOINT;
            durationTicks = WAYPOINT_ANIMATION_TICKS;
        } else if (TRIGGER_CHAIN.equals(animationName)) {
            animation = ARMOR_ANIMATION_CHAIN;
            durationTicks = CHAIN_ANIMATION_TICKS;
        } else {
            animation = ARMOR_ANIMATION_NONE;
            durationTicks = 0;
        }

        CompoundTag tag = chest.getOrCreateTag();
        tag.putInt(TAG_GUARD_ANIMATION, animation);
        tag.putLong(TAG_GUARD_ANIMATION_UNTIL, wearer.level().getGameTime() + durationTicks);

        syncEquippedChestplate(wearer);
    }

    public static boolean isGuardAnimationActive(LivingEntity wearer) {
        int animation = getActiveArmorAnimation(wearer);

        return animation == ARMOR_ANIMATION_DEFEND || animation == ARMOR_ANIMATION_DEFEND_PROJECTILE;
    }

    public static boolean isWaypointAnimationActive(LivingEntity wearer) {
        return getActiveArmorAnimation(wearer) == ARMOR_ANIMATION_WAYPOINT;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltipComponents, TooltipFlag isAdvanced) {

        ArmorItem.Type type = this.getType();

        switch (type) {
            case CHESTPLATE -> {
                tooltipComponents.add(Component.translatable("tooltip.oasiso.azumalit_chestplate.line1").withStyle(ChatFormatting.BLUE));
                tooltipComponents.add(Component.translatable("tooltip.oasiso.azumalit_chestplate.line2").withStyle(ChatFormatting.AQUA));
            }
        }

        tooltipComponents.add(Component.empty());
    }

    public static boolean isChainAnimationActive(LivingEntity wearer) {
        return getActiveArmorAnimation(wearer) == ARMOR_ANIMATION_CHAIN;
    }

    public static int getAttackMode(LivingEntity wearer) {
        return switch (getActiveArmorAnimation(wearer)) {
            case ARMOR_ANIMATION_ATTACK_LEFT -> ATTACK_MODE_LEFT;
            case ARMOR_ANIMATION_ATTACK_RIGHT -> ATTACK_MODE_RIGHT;
            case ARMOR_ANIMATION_ATTACK_BOTH -> ATTACK_MODE_BOTH;
            default -> ATTACK_MODE_NONE;
        };
    }

    public static long getAttackAnimationStartTick(LivingEntity wearer) {
        if (getAttackMode(wearer) == ATTACK_MODE_NONE) {
            return Long.MIN_VALUE;
        }

        ItemStack chest = wearer.getItemBySlot(EquipmentSlot.CHEST);
        CompoundTag tag = chest.getTag();

        if (tag == null) {
            return Long.MIN_VALUE;
        }

        return tag.getLong(TAG_GUARD_ANIMATION_UNTIL) - ATTACK_ANIMATION_TICKS;
    }

    public static boolean isAttackTrailActive(LivingEntity wearer) {
        long startTick = getAttackAnimationStartTick(wearer);

        if (startTick == Long.MIN_VALUE) {
            return false;
        }

        long elapsed = wearer.level().getGameTime() - startTick;

        return elapsed >= 0L && elapsed < ATTACK_DAMAGE_KEY_TICK;
    }

    private static int getActiveArmorAnimation(LivingEntity wearer) {
        if (wearer == null) {
            return ARMOR_ANIMATION_NONE;
        }

        ItemStack chest = wearer.getItemBySlot(EquipmentSlot.CHEST);

        if (!chest.is(ModItems.AZUMALIT_CHESTPLATE.get())) {
            return ARMOR_ANIMATION_NONE;
        }

        CompoundTag tag = chest.getTag();

        if (tag == null) {
            return ARMOR_ANIMATION_NONE;
        }

        if (wearer.level().getGameTime() >= tag.getLong(TAG_GUARD_ANIMATION_UNTIL)) {
            return ARMOR_ANIMATION_NONE;
        }

        return tag.getInt(TAG_GUARD_ANIMATION);
    }

    private static void syncEquippedChestplate(LivingEntity wearer) {
        if (wearer instanceof net.minecraft.server.level.ServerPlayer player) {
            player.getInventory().setChanged();
            player.inventoryMenu.broadcastChanges();
            player.containerMenu.broadcastChanges();
        }
    }

    public static boolean isWearingFullSet(LivingEntity entity) {
        if (entity == null) {
            return false;
        }

        return entity.getItemBySlot(EquipmentSlot.HEAD).is(ModItems.AZUMALIT_HELMET.get()) && entity.getItemBySlot(EquipmentSlot.CHEST).is(ModItems.AZUMALIT_CHESTPLATE.get()) && entity.getItemBySlot(EquipmentSlot.LEGS).is(ModItems.AZUMALIT_LEGGINGS.get()) && entity.getItemBySlot(EquipmentSlot.FEET).is(ModItems.AZUMALIT_BOOTS.get());
    }

    public static boolean hasAzumalitChestplate(LivingEntity entity) {
        return entity != null && entity.getItemBySlot(EquipmentSlot.CHEST).is(ModItems.AZUMALIT_CHESTPLATE.get());
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
