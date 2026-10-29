package com.benji.oasiso.common.fault;

import com.benji.oasiso.Oasiso;
import com.benji.oasiso.registry.ModEffects;
import com.benji.oasiso.registry.ModEntities;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.MobEffectEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Oasiso.MODID)
public final class FaultSystem {
    public static final int BOTTLES = 8, MIN_DELAY = 600, MAX_DELAY = 1200;
    public static final String DATA = "OasisoFault";

    private static CompoundTag state(Player player) {
        var data = player.getPersistentData();
        if (!data.contains(DATA, 10)) data.put(DATA, new CompoundTag());
        return data.getCompound(DATA);
    }

    public static int remaining(Player player) {
        return Math.max(0, Math.min(BOTTLES, state(player).getInt("Remaining")));
    }

    private static int delay(ServerPlayer player) {
        return MIN_DELAY + player.getRandom().nextInt(MAX_DELAY - MIN_DELAY + 1);
    }

    public static void punish(ServerPlayer player) {
        CompoundTag state = state(player);
        state.putInt("Remaining", BOTTLES);
        state.putInt("NextSpawn", delay(player));
        ensureEffect(player);
    }

    private static void ensureEffect(ServerPlayer player) {
        var effect = player.getEffect(ModEffects.FAULT.get());
        if (effect == null || effect.getDuration() != -1)
            player.addEffect(new MobEffectInstance(ModEffects.FAULT.get(), -1, effect == null ? 0 : effect.getAmplifier(), false, false, true));
    }

    public static int drink(ServerPlayer player) {
        int left = remaining(player);
        if (left == 0 && player.hasEffect(ModEffects.FAULT.get())) {
            punish(player);
            left = BOTTLES;
        }
        if (left == 0) return -1;
        left--;
        state(player).putInt("Remaining", left);
        if (left == 0) clear(player);
        return left;
    }

    public static void clear(ServerPlayer player) {
        player.getPersistentData().remove(DATA);
        player.removeEffect(ModEffects.FAULT.get());
    }

    @SubscribeEvent
    public static void removed(MobEffectEvent.Remove event) {
        if (event.getEffect() == ModEffects.FAULT.get() && event.getEntity() instanceof ServerPlayer player && remaining(player) > 0)
            event.setCanceled(true);
    }

    @SubscribeEvent
    public static void clone(PlayerEvent.Clone event) {
        var old = event.getOriginal().getPersistentData();
        if (old.contains(DATA, 10)) event.getEntity().getPersistentData().put(DATA, old.getCompound(DATA).copy());
    }

    @SubscribeEvent
    public static void login(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer p && remaining(p) > 0) ensureEffect(p);
    }

    @SubscribeEvent
    public static void respawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer p && remaining(p) > 0) ensureEffect(p);
    }

    @SubscribeEvent
    public static void tick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player) || !player.isAlive())
            return;
        if (remaining(player) == 0) {
            if (player.hasEffect(ModEffects.FAULT.get())) punish(player);
            else return;
        }
        ensureEffect(player);
        if (player.isSpectator()) return;
        CompoundTag state = state(player);
        int ticks = state.getInt("NextSpawn");
        if (ticks <= 0 || ticks > MAX_DELAY) ticks = delay(player);
        if (--ticks > 0) {
            state.putInt("NextSpawn", ticks);
            return;
        }
        spawn(player);
        state.putInt("NextSpawn", delay(player));
    }

    private static void spawn(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        for (int attempt = 0; attempt < 32; attempt++) {
            double angle = player.getRandom().nextDouble() * Math.PI * 2;
            double radius = 4 + player.getRandom().nextDouble() * 4;
            int x = (int) Math.floor(player.getX() + Math.cos(angle) * radius);
            int z = (int) Math.floor(player.getZ() + Math.sin(angle) * radius);
            for (int dy : new int[]{0, 1, -1, 2, -2, 3, -3, 4, -4}) {
                BlockPos pos = new BlockPos(x, player.getBlockY() + dy, z);
                if (pos.getY() <= level.getMinBuildHeight() || pos.getY() + 3 >= level.getMaxBuildHeight() || !level.hasChunkAt(pos) || !level.getWorldBorder().isWithinBounds(pos))
                    continue;
                if (!level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP) || !level.getFluidState(pos).isEmpty() || !level.getFluidState(pos.above()).isEmpty())
                    continue;
                var soul = ModEntities.GRAVE_SOUL.get().create(level);
                if (soul == null) return;
                soul.moveTo(x + .5, pos.getY(), z + .5, player.getYRot() + 180, 0);
                if (!level.noCollision(soul)) continue;
                if (level.addFreshEntity(soul)) {
                    level.sendParticles(Oasiso.ENT_SOUL.get(), soul.getX(), soul.getY() + .8, soul.getZ(), 24, .3, .5, .3, .035);
                    return;
                }
            }
        }
    }

    @SubscribeEvent
    public static void commands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("fault").requires(source -> source.hasPermission(2)).then(Commands.literal("clear").executes(context -> {
            ServerPlayer player = context.getSource().getPlayerOrException();
            clear(player);
            context.getSource().sendSuccess(() -> Component.literal("Fault cleared."), true);
            return 1;
        }).then(Commands.argument("players", EntityArgument.players()).executes(context -> {
            var players = EntityArgument.getPlayers(context, "players");
            for (ServerPlayer player : players) clear(player);
            context.getSource().sendSuccess(() -> Component.literal("Fault cleared for " + players.size() + " player(s)."), true);
            return players.size();
        }))));
    }

    private FaultSystem() {
    }
}
