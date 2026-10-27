package com.benji.oasiso.common.effect;

import com.benji.oasiso.Oasiso;
import com.benji.oasiso.common.entity.AngerEntity;
import com.benji.oasiso.common.entity.HappinessEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.TickEvent;
import java.util.*;
import net.minecraftforge.event.entity.living.*;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.RegisterEvent;

public final class SadnessStun extends MobEffect {
    public static final SadnessStun INSTANCE = new SadnessStun();
    private static final Set<Mob> LOCKED = Collections.newSetFromMap(new WeakHashMap<>());
    private static final String KEY = "OasisoSadnessLock";
    private SadnessStun() { super(MobEffectCategory.HARMFUL, 0x70C8F0); }
    public static boolean stunned(Entity entity) {
        return entity instanceof Mob mob && mob.isAlive() && mob.getMaxHealth() < 200 && mob.hasEffect(INSTANCE);
    }
    public static void apply(Mob mob) {
        if (mob.getMaxHealth() >= 200 || !mob.isAlive()) return;
        if (mob instanceof AngerEntity || mob instanceof HappinessEntity) return;

        mob.addEffect(new MobEffectInstance(INSTANCE, 100, 0, false, false, false));
        if (stunned(mob)) lock(mob);
    }
    private static void lock(Mob mob) {
        LOCKED.add(mob);
        CompoundTag data = mob.getPersistentData();
        if (!data.contains(KEY)) {
            CompoundTag saved = new CompoundTag();
            saved.putBoolean("NoAI", mob.isNoAi()); saved.putBoolean("NoGravity", mob.isNoGravity());
            saved.putDouble("X", mob.getX()); saved.putDouble("Y", mob.getY()); saved.putDouble("Z", mob.getZ());
            data.put(KEY, saved);
        }
        CompoundTag saved = data.getCompound(KEY);
        if (mob.isPassenger()) mob.stopRiding();
        mob.setNoAi(true); mob.setNoGravity(true); mob.getNavigation().stop(); mob.setTarget(null);
        mob.setJumping(false); mob.setDeltaMovement(Vec3.ZERO); mob.fallDistance = 0;
        mob.setPos(saved.getDouble("X"), saved.getDouble("Y"), saved.getDouble("Z"));
        mob.hasImpulse = true;
    }
    private static void unlock(Mob mob) {
        CompoundTag data = mob.getPersistentData();
        if (!data.contains(KEY)) return;
        CompoundTag saved = data.getCompound(KEY);
        mob.setNoAi(saved.getBoolean("NoAI")); mob.setNoGravity(saved.getBoolean("NoGravity"));
        mob.setDeltaMovement(Vec3.ZERO); data.remove(KEY);
    }
    @Mod.EventBusSubscriber(modid=Oasiso.MODID, bus=Mod.EventBusSubscriber.Bus.MOD)
    public static final class Registration {
        @SubscribeEvent public static void register(RegisterEvent event) {
            event.register(Registries.MOB_EFFECT, helper -> helper.register(
                    ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "sadness_stun"), INSTANCE));
        }
    }
    @Mod.EventBusSubscriber(modid=Oasiso.MODID)
    public static final class Events {
        @SubscribeEvent public static void tick(LivingEvent.LivingTickEvent event) {
            if (!(event.getEntity() instanceof Mob mob) || mob.level().isClientSide) return;
            if (stunned(mob)) lock(mob); else unlock(mob);
        }
        @SubscribeEvent public static void endTick(TickEvent.LevelTickEvent event) {
            if (event.phase != TickEvent.Phase.END || event.level.isClientSide) return;
            for (Mob mob : new ArrayList<>(LOCKED)) {
                if (mob.level() != event.level) continue;
                if (!mob.isRemoved() && stunned(mob)) lock(mob);
                else { unlock(mob); LOCKED.remove(mob); }
            }
        }
        @SubscribeEvent public static void target(LivingChangeTargetEvent event) {
            if (stunned(event.getEntity()) && event.getNewTarget() != null) event.setCanceled(true);
        }
        @SubscribeEvent public static void attack(LivingAttackEvent event) {
            if (stunned(event.getSource().getEntity()) || stunned(event.getSource().getDirectEntity())) event.setCanceled(true);
        }
        @SubscribeEvent public static void projectile(EntityJoinLevelEvent event) {
            if (!event.getLevel().isClientSide && event.getEntity() instanceof Projectile projectile
                    && stunned(projectile.getOwner())) event.setCanceled(true);
        }
    }
}
