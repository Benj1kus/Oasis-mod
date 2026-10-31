package com.benji.oasiso.common.voice;

import com.benji.oasiso.common.portal.SoulStorageVoiceArea;
import com.google.gson.*;
import com.mojang.logging.LogUtils;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.resources.*;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.*;
import java.util.function.Predicate;

@Mod.EventBusSubscriber(modid = "oasiso")
public final class InnerVoiceRules extends SimpleJsonResourceReloadListener {
    private static final String DATA = "OasisoInnerVoice";
    private static volatile List<Rule> rules = List.of();

    private record Rule(ResourceLocation id, List<String> phrases, long cooldown,
                        List<Predicate<ServerPlayer>> checks) {
    }

    private InnerVoiceRules() {
        super(new Gson(), "inner_voice");
    }

    @SubscribeEvent
    public static void reload(AddReloadListenerEvent e) {
        e.addListener(new InnerVoiceRules());
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> files, ResourceManager rm, ProfilerFiller profiler) {
        List<Rule> out = new ArrayList<>();
        files.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(entry -> {
            try {
                JsonObject o = entry.getValue().getAsJsonObject();
                if (o.has("enabled") && !o.get("enabled").getAsBoolean()) return;
                JsonArray list = o.getAsJsonArray("phrases");
                if (list == null || list.size() < 1 || list.size() > 10)
                    throw new IllegalArgumentException("Expected 1..10 phrases");
                List<String> phrases = new ArrayList<>();
                for (JsonElement x : list) {
                    String key = x.getAsString();
                    if (key.isBlank() || key.length() > 256) throw new IllegalArgumentException("Invalid lang key");
                    phrases.add(key);
                }
                double seconds = o.has("cooldown_seconds") ? o.get("cooldown_seconds").getAsDouble() : 300;
                if (!Double.isFinite(seconds) || seconds < 1 || seconds > 31536000)
                    throw new IllegalArgumentException("cooldown_seconds must be 1..31536000");
                JsonArray conditions = o.getAsJsonArray("conditions");
                if (conditions == null || conditions.size() < 1 || conditions.size() > 16)
                    throw new IllegalArgumentException("Expected 1..16 conditions");
                List<Predicate<ServerPlayer>> checks = new ArrayList<>();
                for (JsonElement c : conditions) checks.add(condition(c.getAsJsonObject()));
                out.add(new Rule(entry.getKey(), List.copyOf(phrases), (long) Math.ceil(seconds * 20), List.copyOf(checks)));
            } catch (Exception ex) {
                LogUtils.getLogger().warn("Skipping inner voice rule {}: {}", entry.getKey(), ex.toString());
            }
        });
        rules = List.copyOf(out);
    }

    private static double number(JsonObject o, String key, double fallback) {
        double n = o.has(key) ? o.get(key).getAsDouble() : fallback;
        if (!Double.isFinite(n)) throw new IllegalArgumentException(key);
        return n;
    }

    private static ResourceLocation id(JsonObject o) {
        return Objects.requireNonNull(ResourceLocation.tryParse(o.get("id").getAsString()), "Invalid resource id");
    }

    private static Predicate<ServerPlayer> condition(JsonObject o) {
        String type = o.get("type").getAsString();
        if (type.equals("health")) {
            double min = number(o, "min", 0), max = number(o, "max", 20);
            if (min < 0 || max < min) throw new IllegalArgumentException("Invalid health range");
            return p -> p.getHealth() >= min && p.getHealth() <= max;
        }
        ResourceLocation id = id(o);
        return switch (type) {
            case "effect" -> {
                if (!ForgeRegistries.MOB_EFFECTS.containsKey(id))
                    throw new IllegalArgumentException("Unknown effect " + id);
                var effect = ForgeRegistries.MOB_EFFECTS.getValue(id);
                int amplifier = (int) number(o, "min_amplifier", 0);
                yield p -> p.hasEffect(effect) && p.getEffect(effect).getAmplifier() >= amplifier;
            }
            case "item" -> {
                if (!ForgeRegistries.ITEMS.containsKey(id)) throw new IllegalArgumentException("Unknown item " + id);
                var item = ForgeRegistries.ITEMS.getValue(id);
                int count = (int) number(o, "count", 1);
                if (count < 1) throw new IllegalArgumentException("count");
                yield p -> {
                    int n = 0;
                    for (int i = 0; i < p.getInventory().getContainerSize(); i++) {
                        var stack = p.getInventory().getItem(i);
                        if (stack.is(item)) n += stack.getCount();
                    }
                    return n >= count;
                };
            }
            case "biome" ->
                    p -> p.serverLevel().getBiome(p.blockPosition()).unwrapKey().map(k -> k.location().equals(id)).orElse(false);
            case "structure" -> p -> {
                if (id.equals(ResourceLocation.fromNamespaceAndPath("oasiso", "soul_storage")))
                    return SoulStorageVoiceArea.contains(p);
                var structure = p.serverLevel().registryAccess().registryOrThrow(Registries.STRUCTURE).getOptional(id);
                return structure.isPresent() && p.serverLevel().structureManager().getStructureWithPieceAt(p.blockPosition(), structure.get()).isValid();
            };
            case "mob" -> {
                if (!ForgeRegistries.ENTITY_TYPES.containsKey(id))
                    throw new IllegalArgumentException("Unknown entity " + id);
                var entity = ForgeRegistries.ENTITY_TYPES.getValue(id);
                double radius = number(o, "radius", 5);
                if (radius <= 0 || radius > 64) throw new IllegalArgumentException("radius must be 0..64");
                yield p -> !p.serverLevel().getEntitiesOfClass(LivingEntity.class, p.getBoundingBox().inflate(radius), e -> e != p && e.isAlive() && e.getType() == entity && e.distanceToSqr(p) <= radius * radius).isEmpty();
            }
            default -> throw new IllegalArgumentException("Unknown condition " + type);
        };
    }

    private static CompoundTag data(ServerPlayer p) {
        CompoundTag root = p.getPersistentData();
        if (!root.contains(DATA, 10)) root.put(DATA, new CompoundTag());
        return root.getCompound(DATA);
    }

    @SubscribeEvent
    public static void tick(TickEvent.PlayerTickEvent e) {
        if (e.phase != TickEvent.Phase.END || !(e.player instanceof ServerPlayer p) || p.tickCount % 20 != 0 || !p.isAlive() || p.isSpectator())
            return;
        CompoundTag d = data(p);
        long now = p.serverLevel().getServer().overworld().getGameTime();
        if (now < d.getLong("BusyUntil")) return;
        CompoundTag times = d.getCompound("Cooldowns");
        List<Rule> ordered = new ArrayList<>(rules);
        Collections.shuffle(ordered);
        for (Rule rule : ordered) {
            if (now < times.getLong(rule.id.toString()) || !rule.checks.stream().allMatch(c -> c.test(p))) continue;
            String key = rule.phrases.get(p.getRandom().nextInt(rule.phrases.size()));
            long token = p.getRandom().nextLong();
            times.putLong(rule.id.toString(), now + rule.cooldown);
            d.put("Cooldowns", times);
            d.putLong("Token", token);
            d.putLong("BusyUntil", now + 800);
            InnerVoiceNetwork.say(p, key, token);
            break;
        }
    }

    public static void done(ServerPlayer p, long token) {
        CompoundTag d = data(p);
        if (d.getLong("Token") == token) {
            d.putLong("BusyUntil", p.serverLevel().getServer().overworld().getGameTime() + 20);
        }
    }

    @SubscribeEvent
    public static void clone(PlayerEvent.Clone e) {
        if (e.getOriginal().getPersistentData().contains(DATA)) {
            CompoundTag d = e.getOriginal().getPersistentData().getCompound(DATA).copy();
            d.remove("BusyUntil");
            e.getEntity().getPersistentData().put(DATA, d);
        }
    }

    @SubscribeEvent
    public static void login(PlayerEvent.PlayerLoggedInEvent e) {
        if (e.getEntity() instanceof ServerPlayer p) data(p).remove("BusyUntil");
    }
}
