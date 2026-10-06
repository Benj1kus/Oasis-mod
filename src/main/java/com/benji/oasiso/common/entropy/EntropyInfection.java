package com.benji.oasiso.common.entropy;

import com.benji.oasiso.Oasiso;
import com.benji.oasiso.common.entity.EntZombieEntity;
import com.benji.oasiso.common.entity.EntCreeperEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

@Mod.EventBusSubscriber(modid = Oasiso.MODID)
public final class EntropyInfection {
    public static final int WATER_TICKS = 100;
    private static final String TIMER = "OasisoEntropyInfectionTicks";
    private static final ResourceLocation WATER = ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "entropy_water");
    private static final ResourceLocation ZOMBIE = ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "ent_zombie");
    private static final ResourceLocation CREEPER = ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "ent_creeper");
    private static boolean registryErrorLogged;

    public static boolean eligible(Entity e) {
        return e instanceof Mob mob && mob.isAlive() && !mob.isRemoved() && (e.getType() == EntityType.ZOMBIE || e.getType() == EntityType.CREEPER);
    }

    @SubscribeEvent
    public static void tick(LivingEvent.LivingTickEvent event) {
        if (!(event.getEntity().level() instanceof ServerLevel level) || !eligible(event.getEntity())) return;
        Mob mob = (Mob) event.getEntity();
        var data = mob.getPersistentData();
        if (!data.contains(TIMER)) {
            if (touchesWater(mob)) {
                data.putInt(TIMER, WATER_TICKS);
                EntropyInfectionNetwork.tracking(mob, WATER_TICKS);
            }
            return;
        }
        int remaining = Math.max(0, Math.min(WATER_TICKS, data.getInt(TIMER)) - 1);
        data.putInt(TIMER, remaining);
        if (remaining == 0) {
            if (convert(level, mob, false) == null) {
                data.putInt(TIMER, 20);
                EntropyInfectionNetwork.tracking(mob, 20);
            }
        }
    }

    private static boolean touchesWater(Mob mob) {
        AABB box = mob.getBoundingBox().deflate(.001);
        var level = mob.level();
        for (BlockPos pos : BlockPos.betweenClosed(BlockPos.containing(box.minX, box.minY, box.minZ), BlockPos.containing(box.maxX, box.maxY, box.maxZ))) {
            if (!level.hasChunkAt(pos)) continue;
            if (!WATER.equals(ForgeRegistries.BLOCKS.getKey(level.getBlockState(pos).getBlock()))) continue;
            var fluid = level.getFluidState(pos);
            if (!fluid.isEmpty() && pos.getY() + fluid.getHeight(level, pos) > box.minY && pos.getY() < box.maxY)
                return true;
        }
        return false;
    }

    @SubscribeEvent
    public static void tracking(PlayerEvent.StartTracking event) {
        if (event.getEntity() instanceof ServerPlayer player && eligible(event.getTarget())) {
            var data = event.getTarget().getPersistentData();
            if (data.contains(TIMER))
                EntropyInfectionNetwork.player(player, event.getTarget().getUUID(), Math.max(1, data.getInt(TIMER)));
        }
    }

    public static Mob convert(ServerLevel level, Mob original, boolean lightning) {
        if (!eligible(original)) return null;
        var key = original instanceof Zombie ? ZOMBIE : CREEPER;
        var type = ForgeRegistries.ENTITY_TYPES.getValue(key);
        if (type == null) {
            if (!registryErrorLogged) {
                com.mojang.logging.LogUtils.getLogger().error("[EntropyInfection] Missing entity type {}", key);
                registryErrorLogged = true;
            }
            return null;
        }
        Entity made = type.create(level);
        if (original instanceof Zombie ? !(made instanceof EntZombieEntity) : !(made instanceof EntCreeperEntity)) {
            if (made != null) made.discard();
            return null;
        }

        Mob replacement = (Mob) made;
        replacement.moveTo(original.getX(), original.getY(), original.getZ(), original.getYRot(), original.getXRot());
        replacement.setYBodyRot(original.yBodyRot);
        replacement.setYHeadRot(original.getYHeadRot());
        replacement.setNoAi(original.isNoAi());
        replacement.setSilent(original.isSilent());
        replacement.setInvulnerable(original.isInvulnerable());
        replacement.setNoGravity(original.isNoGravity());
        replacement.setCustomName(original.getCustomName());
        replacement.setCustomNameVisible(original.isCustomNameVisible());
        if (original.isPersistenceRequired()) replacement.setPersistenceRequired();
        replacement.setCanPickUpLoot(original.canPickUpLoot());
        replacement.setHealth(Math.max(.01F, replacement.getMaxHealth() * original.getHealth() / original.getMaxHealth()));
        replacement.setTarget(original.getTarget());
        for (var effect : original.getActiveEffects())
            replacement.addEffect(new net.minecraft.world.effect.MobEffectInstance(effect));
        for (String tag : original.getTags()) replacement.addTag(tag);
        var saved = original.saveWithoutId(new net.minecraft.nbt.CompoundTag());
        var armorDrops = saved.getList("ArmorDropChances", net.minecraft.nbt.Tag.TAG_FLOAT);
        var handDrops = saved.getList("HandDropChances", net.minecraft.nbt.Tag.TAG_FLOAT);
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            replacement.setItemSlot(slot, original.getItemBySlot(slot).copy());
            var drops = slot.getType() == EquipmentSlot.Type.ARMOR ? armorDrops : handDrops;
            if (slot.getIndex() < drops.size()) replacement.setDropChance(slot, drops.getFloat(slot.getIndex()));
        }
        if (lightning) {
            if (replacement instanceof EntZombieEntity z) z.beginEntropyStrike();
            if (replacement instanceof EntCreeperEntity c) c.beginEntropyStrike();
        }
        if (!level.addFreshEntity(replacement)) return null;
        Entity vehicle = original.getVehicle();
        var passengers = java.util.List.copyOf(original.getPassengers());
        if (vehicle != null) {
            original.stopRiding();
            replacement.startRiding(vehicle, true);
        }
        for (Entity passenger : passengers) {
            passenger.stopRiding();
            passenger.startRiding(replacement, true);
        }
        if (original.isLeashed()) original.dropLeash(true, true);
        for (EquipmentSlot slot : EquipmentSlot.values())
            original.setItemSlot(slot, net.minecraft.world.item.ItemStack.EMPTY);
        EntropyInfectionNetwork.tracking(original, 0);
        original.discard();
        level.sendParticles(Oasiso.ENTROPY_LIGHTNING.get(), replacement.getX(), replacement.getY() + replacement.getBbHeight() * .5, replacement.getZ(), 18, replacement.getBbWidth() * .6, replacement.getBbHeight() * .45, replacement.getBbWidth() * .6, 0);
        if (lightning) {
            level.playSound(null, replacement.blockPosition(), com.benji.oasiso.ModSounds.ENTROPY_LIGHTNING.get(), SoundSource.HOSTILE, 1F, 1F);
        } else
            level.playSound(null, replacement.blockPosition(), SoundEvents.ZOMBIE_CONVERTED_TO_DROWNED, SoundSource.HOSTILE, 1F, 1F);
        return replacement;
    }

    private EntropyInfection() {
    }
}
