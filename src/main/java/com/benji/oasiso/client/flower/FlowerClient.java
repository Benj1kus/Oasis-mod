package com.benji.oasiso.client.flower;

import com.benji.oasiso.Oasiso;
import com.benji.oasiso.common.flower.*;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.*;

@Mod.EventBusSubscriber(modid = Oasiso.MODID, value = Dist.CLIENT)
public final class FlowerClient {
    static final Map<BlockPos, Flame> FLAMES = new HashMap<>();
    static final Map<BlockPos, Speech> SPEECHES = new HashMap<>();
    private static Map<ResourceLocation, FlowerDefinition> definitions = Map.of(), byBlock = Map.of();
    private static ClientLevel world;
    private static int scanTick;

    public static void configure(String json) {
        Map<ResourceLocation, FlowerDefinition> next = new HashMap<>(), blocks = new HashMap<>();
        try {
            for (var entry : JsonParser.parseString(json).getAsJsonObject().entrySet()) {
                FlowerDefinition def = FlowerDefinition.parse(ResourceLocation.tryParse(entry.getKey()), entry.getValue().getAsJsonObject());
                next.put(def.id(), def);
                blocks.put(def.block(), def);
            }
        } catch (RuntimeException ex) {
            LogUtils.getLogger().error("Invalid talking flower sync", ex);
            next.clear();
            blocks.clear();
        }
        definitions = Map.copyOf(next);
        byBlock = Map.copyOf(blocks);
        FlowerInteractions.clientBlocks = Set.copyOf(blocks.keySet());
        clearEffects();
    }

    public static void speak(FlowerNetwork.Speech packet) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || !mc.level.dimension().location().equals(packet.dimension())) return;
        if (world != mc.level) {
            clearEffects();
            world = mc.level;
        }
        FlowerDefinition def = definitions.get(packet.flower());
        if (def == null) return;
        FlowerDefinition.Line line = def.lines().get(packet.line());
        if (line == null) return;
        String text = Component.translatable(line.key()).getString();
        int[] chars = text.codePoints().limit(512).toArray();
        Speech speech = new Speech(def, line, chars, packet.start());
        speech.voiced = speech.visible(mc.level.getGameTime() - packet.start());
        SPEECHES.put(packet.pos().immutable(), speech);
    }

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        if (world != mc.level) {
            clearEffects();
            world = mc.level;
        }
        if (mc.level == null || mc.player == null || mc.isPaused()) return;
        long now = mc.level.getGameTime();
        Iterator<Map.Entry<BlockPos, Speech>> speechIterator = SPEECHES.entrySet().iterator();
        while (speechIterator.hasNext()) {
            var entry = speechIterator.next();
            BlockPos pos = entry.getKey();
            Speech speech = entry.getValue();
            long elapsed = now - speech.start;
            if (elapsed >= speech.line.duration() || !matches(pos, speech.def)) {
                speechIterator.remove();
                continue;
            }
            int visible = speech.visible(elapsed);
            var sound = ForgeRegistries.SOUND_EVENTS.getValue(speech.def.voice());
            if (sound != null && mc.player.distanceToSqr(Vec3.atCenterOf(pos)) <= speech.def.viewRadius() * speech.def.viewRadius()) {
                for (int i = Math.max(speech.voiced, visible - 4); i < visible; i++) {
                    if (!Character.isLetterOrDigit(speech.chars[i])) continue;
                    mc.level.playLocalSound(pos.getX() + .5, pos.getY() + speech.def.textHeight(), pos.getZ() + .5, sound, SoundSource.BLOCKS, speech.def.volume(), speech.def.pitch() * (.94F + mc.level.random.nextFloat() * .12F), false);
                }
            }
            speech.voiced = Math.max(speech.voiced, visible);
        }
        if (scanTick++ % 5 == 0) scan(mc);
        Iterator<Map.Entry<BlockPos, Flame>> flames = FLAMES.entrySet().iterator();
        while (flames.hasNext()) {
            var entry = flames.next();
            BlockPos pos = entry.getKey();
            Flame flame = entry.getValue();
            boolean near = matches(pos, flame.def) && mc.player.isAlive() && mc.player.distanceToSqr(Vec3.atCenterOf(pos)) <= flame.def.activationRadius() * flame.def.activationRadius();
            flame.previous = flame.alpha;
            boolean speaking = SPEECHES.containsKey(pos) && now >= SPEECHES.get(pos).start;
            flame.alpha = Math.max(0, Math.min(1, flame.alpha + (near && !speaking ? .125F : -.125F)));
            if (!near && flame.alpha == 0) flames.remove();
        }
    }

    private static void scan(Minecraft mc) {
        if (byBlock.isEmpty()) return;
        int r = (int) Math.ceil(byBlock.values().stream().mapToDouble(FlowerDefinition::activationRadius).max().orElse(5));
        BlockPos origin = mc.player.blockPosition();
        for (BlockPos pos : BlockPos.betweenClosed(origin.offset(-r, -r, -r), origin.offset(r, r, r))) {
            if (!mc.level.hasChunkAt(pos)) continue;
            FlowerDefinition def = byBlock.get(ForgeRegistries.BLOCKS.getKey(mc.level.getBlockState(pos).getBlock()));
            if (def == null || mc.player.distanceToSqr(Vec3.atCenterOf(pos)) > def.activationRadius() * def.activationRadius())
                continue;
            FLAMES.computeIfAbsent(pos.immutable(), p -> new Flame(def));
        }
    }

    static boolean matches(BlockPos pos, FlowerDefinition def) {
        Minecraft mc = Minecraft.getInstance();
        return mc.level != null && mc.level.hasChunkAt(pos) && def.block().equals(ForgeRegistries.BLOCKS.getKey(mc.level.getBlockState(pos).getBlock()));
    }

    @SubscribeEvent
    public static void logout(ClientPlayerNetworkEvent.LoggingOut event) {
        definitions = Map.of();
        byBlock = Map.of();
        FlowerInteractions.clientBlocks = Set.of();
        world = null;
        clearEffects();
    }

    private static void clearEffects() {
        FLAMES.clear();
        SPEECHES.clear();
        scanTick = 0;
        FlowerRenderer.clear();
    }

    static final class Flame {
        final FlowerDefinition def;
        float alpha, previous;

        Flame(FlowerDefinition def) {
            this.def = def;
        }
    }

    static final class Speech {
        final FlowerDefinition def;
        final FlowerDefinition.Line line;
        final int[] chars;
        final long start;
        int voiced;

        Speech(FlowerDefinition def, FlowerDefinition.Line line, int[] chars, long start) {
            this.def = def;
            this.line = line;
            this.chars = chars;
            this.start = start;
        }

        int visible(double elapsed) {
            return Math.max(0, Math.min(chars.length, (int) Math.floor(elapsed * chars.length / line.typingTicks())));
        }
    }

    private FlowerClient() {
    }
}
