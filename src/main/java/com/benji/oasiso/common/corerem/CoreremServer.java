package com.benji.oasiso.common.corerem;

import com.benji.oasiso.Oasiso;
import com.benji.oasiso.common.entity.AngerEntity;
import com.benji.oasiso.common.entity.HappinessEntity;
import com.benji.oasiso.common.entity.SadnessEntity;
import com.benji.oasiso.common.item.CoreremItem;
import com.benji.oasiso.registry.ModEntities;
import com.benji.oasiso.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.benji.oasiso.ModSounds;
import net.minecraft.sounds.SoundSource;


@Mod.EventBusSubscriber(modid = Oasiso.MODID)
public final class CoreremServer {
    private static final String DATA = "OasisoCorerem";
    private static final String[] READY = {"HappyReady", "AngerReady", "SadReady"};
    private static final Map<UUID, Roll> ROLLS = new HashMap<>();

    private record Roll(ServerPlayer owner, ResourceKey<Level> dimension, long started, int mask, int steps) {
    }

    private static long now(ServerPlayer player) {
        return player.serverLevel().getServer().overworld().getGameTime();
    }

    private static CompoundTag data(ServerPlayer player) {
        CompoundTag root = player.getPersistentData();
        if (!root.contains(DATA, 10)) root.put(DATA, new CompoundTag());
        return root.getCompound(DATA);
    }

    public static boolean holding(ServerPlayer player) {
        return player.getMainHandItem().getItem() instanceof CoreremItem || player.getOffhandItem().getItem() instanceof CoreremItem;
    }

    public static boolean start(ServerPlayer player) {
        if (!player.isAlive() || player.isSpectator() || !holding(player)) return false;
        long now = now(player);
        CompoundTag tag = data(player);
        if (ROLLS.containsKey(player.getUUID()) || tag.getLong("UseReady") > now) {
            sync(player);
            return false;
        }
        int mask = CoreremRoll.available(now, new long[]{tag.getLong(READY[0]), tag.getLong(READY[1]), tag.getLong(READY[2])});
        if (mask == 0) {
            player.displayClientMessage(Component.translatable("message.oasiso.corerem.recharging"), true);
            sync(player);
            return false;
        }
        int steps = CoreremRoll.steps(mask, player.getRandom().nextInt(Integer.bitCount(mask)));
        ROLLS.put(player.getUUID(), new Roll(player, player.level().dimension(), now, mask, steps));
        tag.putLong("UseReady", now + CoreremRoll.USE_COOLDOWN);
        player.getCooldowns().addCooldown(ModItems.COREREM.get(), CoreremRoll.USE_COOLDOWN);
        sync(player);
        return true;
    }

    @SubscribeEvent
    public static void tick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)) return;
        CompoundTag cooldownData = data(player);
        long time = now(player);
        boolean soulReady = false;

        for (String key : READY) {
            long readyAt = cooldownData.getLong(key);

            if (readyAt > 0 && time >= readyAt) {
                cooldownData.remove(key);

                player.playNotifySound(ModSounds.YES.get(), SoundSource.MASTER, 1F, 1F);

                soulReady = true;
            }
        }

        if (soulReady) sync(player);
        Roll roll = ROLLS.get(player.getUUID());
        if (roll != null) {
            if (roll.owner != player || !player.isAlive() || player.isSpectator() || !roll.dimension.equals(player.level().dimension())) {
                ROLLS.remove(player.getUUID());
                sync(player);
                return;
            }
            if (now(player) - roll.started >= CoreremRoll.duration(roll.steps)) {
                ROLLS.remove(player.getUUID());
                int emotion = CoreremRoll.emotionAt(roll.mask, roll.steps);
                if (summon(player, emotion)) {
                    data(player).putLong(READY[emotion], now(player) + CoreremRoll.SOUL_COOLDOWN);
                    sync(player);
                    CoreremNetwork.result(player, emotion);
                } else {
                    data(player).putLong("UseReady", now(player));
                    player.getCooldowns().removeCooldown(ModItems.COREREM.get());
                    sync(player);
                    player.displayClientMessage(Component.translatable("message.oasiso.corerem.no_space"), true);
                }
                return;
            }
        }
        if (player.tickCount % 10 == 0 && (roll != null || holding(player))) sync(player);
    }

    private static boolean summon(ServerPlayer player, int emotion) {
        ServerLevel level = player.serverLevel();
        Mob mob = switch (emotion) {
            case CoreremRoll.HAPPY -> ModEntities.HAPPINESS.get().create(level);
            case CoreremRoll.ANGER -> ModEntities.ANGER.get().create(level);
            case CoreremRoll.SAD -> ModEntities.SADNESS.get().create(level);
            default -> null;
        };
        if (mob == null) return false;
        Vec3 position = findPosition(level, player, mob);
        if (position == null) {
            mob.discard();
            return false;
        }
        mob.moveTo(position.x, position.y, position.z, player.getYRot(), 0);
        mob.yBodyRot = player.getYRot();
        mob.yHeadRot = player.getYRot();
        mob.finalizeSpawn(level, level.getCurrentDifficultyAt(mob.blockPosition()), MobSpawnType.MOB_SUMMONED, null, null);
        if (mob instanceof HappinessEntity happy) happy.startSummoning();
        else if (mob instanceof AngerEntity anger) anger.startSummoning();
        else if (mob instanceof SadnessEntity sad) sad.startSummoning();
        if (!level.addFreshEntity(mob)) {
            mob.discard();
            return false;
        }
        SimpleParticleType particle = switch (emotion) {
            case CoreremRoll.HAPPY -> Oasiso.HAPPY_FLARE.get();
            case CoreremRoll.ANGER -> Oasiso.ANGER_PART.get();
            default -> Oasiso.SAD_BOP.get();
        };
        level.sendParticles(particle, mob.getX(), mob.getY() + mob.getBbHeight() * .5, mob.getZ(), 38, mob.getBbWidth() * .4, mob.getBbHeight() * .3, mob.getBbWidth() * .4, .045);
        return true;
    }

    private static Vec3 findPosition(ServerLevel level, ServerPlayer player, Mob mob) {
        double initial = Math.toRadians(player.getYRot()) + player.getRandom().nextDouble() * Math.PI * 2;
        for (double radius : new double[]{2.3, 3.2, 4.2}) {
            for (int i = 0; i < 16; i++) {
                double angle = initial + i * Math.PI / 8;
                double x = player.getX() + Math.cos(angle) * radius;
                double z = player.getZ() + Math.sin(angle) * radius;
                if (!level.hasChunkAt(BlockPos.containing(x, player.getY(), z))) continue;
                BlockHitResult floor = level.clip(new ClipContext(new Vec3(x, player.getY() + 2, z), new Vec3(x, player.getY() - 3, z), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
                if (floor.getType() == HitResult.Type.MISS) continue;
                Vec3 pos = floor.getLocation().add(0, .01, 0);
                mob.setPos(pos);
                AABB box = mob.getBoundingBox();
                if (!level.getWorldBorder().isWithinBounds(box) || !level.noCollision(mob, box) || level.containsAnyLiquid(box) || box.intersects(player.getBoundingBox()) || !level.getEntities(mob, box, e -> e instanceof LivingEntity && e.isAlive()).isEmpty())
                    continue;
                if (level.clip(new ClipContext(player.getEyePosition(), pos.add(0, Math.min(1, mob.getBbHeight() * .5), 0), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player)).getType() != HitResult.Type.MISS)
                    continue;
                return pos;
            }
        }
        return null;
    }

    private static void sync(ServerPlayer player) {
        long now = now(player);
        CompoundTag tag = data(player);
        Roll roll = ROLLS.get(player.getUUID());
        CoreremNetwork.state(player, new CoreremNetwork.State(CoreremRoll.remaining(now, tag.getLong("UseReady")), CoreremRoll.remaining(now, tag.getLong(READY[0])), CoreremRoll.remaining(now, tag.getLong(READY[1])), CoreremRoll.remaining(now, tag.getLong(READY[2])), roll == null ? 0 : roll.mask, roll == null ? 0 : roll.steps, roll == null ? 0 : (int) Math.max(0, now - roll.started)));
    }

    private static void restore(ServerPlayer player) {
        ROLLS.remove(player.getUUID());
        int remaining = CoreremRoll.remaining(now(player), data(player).getLong("UseReady"));
        if (remaining > 0) player.getCooldowns().addCooldown(ModItems.COREREM.get(), remaining);
        sync(player);
    }

    @SubscribeEvent
    public static void login(PlayerEvent.PlayerLoggedInEvent e) {
        if (e.getEntity() instanceof ServerPlayer p) restore(p);
    }

    @SubscribeEvent
    public static void respawn(PlayerEvent.PlayerRespawnEvent e) {
        if (e.getEntity() instanceof ServerPlayer p) restore(p);
    }

    @SubscribeEvent
    public static void dimension(PlayerEvent.PlayerChangedDimensionEvent e) {
        if (e.getEntity() instanceof ServerPlayer p) restore(p);
    }

    @SubscribeEvent
    public static void clone(PlayerEvent.Clone e) {
        CompoundTag original = e.getOriginal().getPersistentData();
        if (original.contains(DATA, 10)) e.getEntity().getPersistentData().put(DATA, original.getCompound(DATA).copy());
    }

    @SubscribeEvent
    public static void logout(PlayerEvent.PlayerLoggedOutEvent e) {
        ROLLS.remove(e.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void stop(ServerStoppedEvent e) {
        ROLLS.clear();
    }

    private CoreremServer() {
    }
}
