package com.benji.oasiso.common.flower;

import com.benji.oasiso.Oasiso;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.OnDatapackSyncEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.*;

@Mod.EventBusSubscriber(modid = Oasiso.MODID)
public final class FlowerInteractions {
    public static Set<ResourceLocation> clientBlocks = Set.of();

    private record Place(ResourceLocation dimension, BlockPos pos) {
    }

    private record Pending(Place source, ResourceLocation flower, String line, long due) {
    }

    private static final Map<Place, Long> BUSY = new HashMap<>();
    private static final List<Pending> REPLIES = new ArrayList<>();

    @SubscribeEvent
    public static void reload(AddReloadListenerEvent event) {
        event.addListener(new FlowerData());
    }

    @SubscribeEvent
    public static void sync(OnDatapackSyncEvent event) {
        if (event.getPlayer() == null) {
            BUSY.clear();
            REPLIES.clear();
            event.getPlayerList().getPlayers().forEach(FlowerNetwork::sync);
        } else FlowerNetwork.sync(event.getPlayer());
    }

    @SubscribeEvent
    public static void interact(PlayerInteractEvent.RightClickBlock event) {
        ResourceLocation block = ForgeRegistries.BLOCKS.getKey(event.getLevel().getBlockState(event.getPos()).getBlock());
        FlowerDefinition def = event.getLevel().isClientSide ? null : FlowerData.forBlock(block);
        if (event.getLevel().isClientSide ? !clientBlocks.contains(block) : def == null) return;
        if (event.getEntity().isSpectator() || event.getEntity().isShiftKeyDown()) return;
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
        if (event.getHand() != InteractionHand.MAIN_HAND || !(event.getLevel() instanceof ServerLevel level)) return;
        if (event.getEntity().distanceToSqr(Vec3.atCenterOf(event.getPos())) > 36) return;
        Place place = new Place(level.dimension().location(), event.getPos().immutable());
        long now = level.getGameTime();
        if (BUSY.getOrDefault(place, 0L) > now) return;
        List<String> pool = def.lines().entrySet().stream().filter(e -> e.getValue().random()).map(Map.Entry::getKey).toList();
        String lineId = pool.get(level.random.nextInt(pool.size()));
        play(level, place.pos(), def, lineId, now);
        if (!def.lines().get(lineId).replies().isEmpty())
            REPLIES.add(new Pending(place, def.id(), lineId, now + def.lines().get(lineId).duration()));
    }

    private static void play(ServerLevel level, BlockPos pos, FlowerDefinition def, String line, long start) {
        BUSY.put(new Place(level.dimension().location(), pos), start + def.lines().get(line).duration() + 10);
        FlowerNetwork.speak(level, pos, def, line, start);
    }

    @SubscribeEvent
    public static void tick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.level instanceof ServerLevel level)) return;
        ResourceLocation dimension = level.dimension().location();
        long now = level.getGameTime();
        BUSY.entrySet().removeIf(e -> e.getKey().dimension().equals(dimension) && e.getValue() <= now);
        Iterator<Pending> it = REPLIES.iterator();
        while (it.hasNext()) {
            Pending pending = it.next();
            if (!pending.source().dimension().equals(dimension) || pending.due() > now) continue;
            it.remove();
            FlowerDefinition source = FlowerData.definitions().get(pending.flower());
            if (source == null || !matches(level, pending.source().pos(), source)) continue;
            FlowerDefinition.Line line = source.lines().get(pending.line());
            if (line == null) continue;
            List<FlowerDefinition.Reply> choices = new ArrayList<>(line.replies());
            while (!choices.isEmpty()) {
                var reply = choices.remove(level.random.nextInt(choices.size()));
                if (level.random.nextDouble() >= reply.chance()) continue;
                FlowerDefinition target = FlowerData.definitions().get(reply.flower());
                if (target == null || !target.lines().containsKey(reply.line())) continue;
                BlockPos pos = findNeighbour(level, pending.source().pos(), target, source.replyRadius(), now);
                if (pos == null) continue;
                play(level, pos, target, reply.line(), now + reply.delayTicks());
                break;
            }
        }
    }

    private static BlockPos findNeighbour(ServerLevel level, BlockPos origin, FlowerDefinition def, double radius, long now) {
        int r = (int) Math.ceil(radius), count = 0;
        BlockPos chosen = null;
        for (BlockPos pos : BlockPos.betweenClosed(origin.offset(-r, -r, -r), origin.offset(r, r, r))) {
            if (pos.equals(origin) || pos.distSqr(origin) > radius * radius || !matches(level, pos, def)) continue;
            if (BUSY.getOrDefault(new Place(level.dimension().location(), pos), 0L) > now) continue;
            if (level.random.nextInt(++count) == 0) chosen = pos.immutable();
        }
        return chosen;
    }

    private static boolean matches(ServerLevel level, BlockPos pos, FlowerDefinition def) {
        return level.hasChunkAt(pos) && def.block().equals(ForgeRegistries.BLOCKS.getKey(level.getBlockState(pos).getBlock()));
    }

    @SubscribeEvent
    public static void unload(LevelEvent.Unload event) {
        if (event.getLevel() instanceof ServerLevel level) {
            ResourceLocation dimension = level.dimension().location();
            BUSY.keySet().removeIf(p -> p.dimension().equals(dimension));
            REPLIES.removeIf(p -> p.source().dimension().equals(dimension));
        }
    }

    @SubscribeEvent
    public static void stop(ServerStoppedEvent event) {
        BUSY.clear();
        REPLIES.clear();
    }

    private FlowerInteractions() {
    }
}
