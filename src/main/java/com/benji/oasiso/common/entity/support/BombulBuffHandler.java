package com.benji.oasiso.common.entity.support;

import com.benji.oasiso.Oasiso;
import com.benji.oasiso.common.entity.BombulEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Mod.EventBusSubscriber(
        modid = Oasiso.MODID,
        bus = Mod.EventBusSubscriber.Bus.FORGE
)
public final class BombulBuffHandler {

    private static final double BUFF_RADIUS = 10.0D;
    private static final double BUFF_RADIUS_SQR =
            BUFF_RADIUS * BUFF_RADIUS;

    private static final int BUFF_REFRESH_TIME = 25;
    private static final int BUFF_EXPIRE_DELAY = 15;

    private static final String LAST_REFRESH_TAG =
            Oasiso.MODID + ":bombul_buff_last_refresh";

    private static final UUID HEALTH_BOOST_UUID =
            UUID.fromString(
                    "f141a680-86c9-46de-8aeb-99bc84e76cb4"
            );

    private static final AttributeModifier HEALTH_BOOST =
            new AttributeModifier(
                    HEALTH_BOOST_UUID,
                    "Bombul health boost",
                    1.0D,
                    AttributeModifier.Operation.MULTIPLY_TOTAL
            );

    private BombulBuffHandler() {
    }

    public static boolean applyBuffAround(
            BombulEntity bombul
    ) {
        if (!(bombul.level() instanceof ServerLevel level)) {
            return false;
        }

        AABB area = bombul.getBoundingBox()
                .inflate(BUFF_RADIUS);

        List<LivingEntity> targets =
                level.getEntitiesOfClass(
                        LivingEntity.class,
                        area,
                        entity ->
                                isValidTarget(entity, bombul)
                                        && entity.distanceToSqr(bombul)
                                        <= BUFF_RADIUS_SQR
                );

        for (LivingEntity target : targets) {
            applyBuff(target, bombul, level.getGameTime());
        }

        return !targets.isEmpty();
    }

    private static boolean isValidTarget(
            LivingEntity entity,
            BombulEntity bombul
    ) {
        return entity != bombul
                && entity.isAlive()
                && !entity.isSpectator()
                && !(entity instanceof BombulEntity)
                && entity.getType().getCategory()
                == MobCategory.MONSTER;
    }

    private static void applyBuff(
            LivingEntity target,
            BombulEntity source,
            long gameTime
    ) {
        AttributeInstance maxHealth =
                target.getAttribute(Attributes.MAX_HEALTH);

        if (maxHealth != null
                && maxHealth.getModifier(
                HEALTH_BOOST_UUID
        ) == null) {

            float oldHealth = target.getHealth();
            float oldMaxHealth = target.getMaxHealth();

            maxHealth.addTransientModifier(HEALTH_BOOST);

            if (oldMaxHealth > 0.0F) {
                float healthPercentage =
                        oldHealth / oldMaxHealth;

                target.setHealth(
                        Math.min(
                                target.getMaxHealth(),
                                target.getMaxHealth()
                                        * healthPercentage
                        )
                );
            }
        }

        target.getPersistentData()
                .putLong(LAST_REFRESH_TAG, gameTime);

        /*
         * Это только сетевой маркер для клиентской иконки.
         * Он не создаёт частицы и не показывается в HUD.
         */
        target.forceAddEffect(
                new MobEffectInstance(
                        Oasiso.BOMBUL_BUFF_EFFECT.get(),
                        BUFF_REFRESH_TIME,
                        0,
                        false,
                        false,
                        false
                ),
                source
        );
    }

    public static boolean isBuffed(
            LivingEntity entity
    ) {
        AttributeInstance maxHealth =
                entity.getAttribute(Attributes.MAX_HEALTH);

        return maxHealth != null
                && maxHealth.getModifier(
                HEALTH_BOOST_UUID
        ) != null;
    }

    @SubscribeEvent
    public static void onLivingTick(
            LivingEvent.LivingTickEvent event
    ) {
        LivingEntity entity = event.getEntity();

        if (entity.level().isClientSide) {
            return;
        }

        AttributeInstance maxHealth =
                entity.getAttribute(Attributes.MAX_HEALTH);

        if (maxHealth == null
                || maxHealth.getModifier(
                HEALTH_BOOST_UUID
        ) == null) {
            return;
        }

        if (entity.level() instanceof ServerLevel serverLevel) {
            spawnGoldenStars(serverLevel, entity);
        }

        CompoundTag data = entity.getPersistentData();

        if (!data.contains(
                LAST_REFRESH_TAG,
                Tag.TAG_LONG
        )) {
            removeBuff(entity);
            return;
        }

        long lastRefresh =
                data.getLong(LAST_REFRESH_TAG);

        long currentTime =
                entity.level().getGameTime();

        if (currentTime - lastRefresh
                > BUFF_EXPIRE_DELAY) {
            removeBuff(entity);
        }
    }

    private static void spawnGoldenStars(
            ServerLevel level,
            LivingEntity entity
    ) {
        RandomSource random = entity.getRandom();

        // 1 star per 8 ticks
        if (random.nextInt(8) != 0) {
            return;
        }

        double width =
                Math.max(0.4D, entity.getBbWidth());

        double x = entity.getX()
                + (random.nextDouble() - 0.5D)
                * width;

        double y = entity.getY()
                + 0.2D
                + random.nextDouble()
                * Math.max(
                0.4D,
                entity.getBbHeight() - 0.2D
        );

        double z = entity.getZ()
                + (random.nextDouble() - 0.5D)
                * width;

        double velocityX =
                (random.nextDouble() - 0.5D) * 0.01D;

        double velocityY =
                0.008D + random.nextDouble() * 0.012D;

        double velocityZ =
                (random.nextDouble() - 0.5D) * 0.01D;

        // speed
        level.sendParticles(
                Oasiso.GOLDEN_STARS.get(),
                x,
                y,
                z,
                0,
                velocityX,
                velocityY,
                velocityZ,
                1.0D
        );
    }

    private static void removeBuff(
            LivingEntity entity
    ) {
        AttributeInstance maxHealth =
                entity.getAttribute(Attributes.MAX_HEALTH);

        if (maxHealth != null
                && maxHealth.getModifier(
                HEALTH_BOOST_UUID
        ) != null) {

            float oldHealth = entity.getHealth();
            float oldMaxHealth = entity.getMaxHealth();

            maxHealth.removeModifier(
                    HEALTH_BOOST_UUID
            );

            if (oldMaxHealth > 0.0F) {
                float healthPercentage =
                        oldHealth / oldMaxHealth;

                entity.setHealth(
                        Math.min(
                                entity.getMaxHealth(),
                                entity.getMaxHealth()
                                        * healthPercentage
                        )
                );
            }
        }

        entity.removeEffect(
                Oasiso.BOMBUL_BUFF_EFFECT.get()
        );

        entity.getPersistentData()
                .remove(LAST_REFRESH_TAG);
    }

    @SubscribeEvent
    public static void onLivingHurt(
            LivingHurtEvent event
    ) {
        LivingEntity damagedEntity = event.getEntity();

        if (damagedEntity.level().isClientSide) {
            return;
        }


        if (event.getAmount() > 0.0F
                && isBuffed(damagedEntity)
                && damagedEntity.level()
                instanceof ServerLevel serverLevel) {

            spawnGoldenHearts(
                    serverLevel,
                    damagedEntity
            );
        }


        Entity attacker =
                event.getSource().getEntity();

        if (!(attacker
                instanceof LivingEntity livingAttacker)) {
            return;
        }

        if (!isBuffed(livingAttacker)) {
            return;
        }

        event.setAmount(
                event.getAmount() * 2.0F
        );
    }

    private static void spawnGoldenHearts(
            ServerLevel level,
            LivingEntity entity
    ) {
        RandomSource random = entity.getRandom();

        int heartCount = 3 + random.nextInt(2);

        for (int i = 0; i < heartCount; i++) {
            double x = entity.getX()
                    + (random.nextDouble() - 0.5D)
                    * entity.getBbWidth() * 0.8D;

            double y = entity.getY()
                    + entity.getBbHeight() * 0.55D
                    + random.nextDouble()
                    * entity.getBbHeight() * 0.35D;

            double z = entity.getZ()
                    + (random.nextDouble() - 0.5D)
                    * entity.getBbWidth() * 0.8D;

            double velocityX =
                    (random.nextDouble() - 0.5D) * 0.18D;

            double velocityY =
                    0.10D + random.nextDouble() * 0.12D;

            double velocityZ =
                    (random.nextDouble() - 0.5D) * 0.18D;


            level.sendParticles(
                    Oasiso.GOLDEN_HEART.get(),
                    x,
                    y,
                    z,
                    0,
                    velocityX,
                    velocityY,
                    velocityZ,
                    1.0D
            );
        }
    }

    @SubscribeEvent
    public static void onLivingDrops(
            LivingDropsEvent event
    ) {
        LivingEntity deadEntity = event.getEntity();

        if (deadEntity.level().isClientSide
                || !isBuffed(deadEntity)) {
            return;
        }

        List<ItemEntity> duplicatedDrops =
                new ArrayList<>();

        for (ItemEntity originalDrop
                : event.getDrops()) {

            ItemStack copiedStack =
                    originalDrop.getItem().copy();

            if (copiedStack.isEmpty()) {
                continue;
            }

            ItemEntity duplicatedDrop =
                    new ItemEntity(
                            deadEntity.level(),
                            originalDrop.getX(),
                            originalDrop.getY(),
                            originalDrop.getZ(),
                            copiedStack
                    );

            duplicatedDrop.setDeltaMovement(
                    originalDrop.getDeltaMovement()
            );

            duplicatedDrop.setDefaultPickUpDelay();

            duplicatedDrops.add(duplicatedDrop);
        }

        event.getDrops().addAll(duplicatedDrops);
    }
}