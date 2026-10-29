package com.benji.oasiso.common.tooltip;

import com.benji.oasiso.Oasiso;
import com.google.gson.*;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.OnDatapackSyncEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.io.Reader;
import java.util.List;

@Mod.EventBusSubscriber(modid = Oasiso.MODID)
public final class TooltipDatapacks {
    public static final String DIRECTORY = "oasiso_tooltips";

    private record Snapshot(String json, List<TooltipData.Definition> definitions) {
    }

    private static volatile Snapshot current = new Snapshot("{}", List.of());

    @SubscribeEvent
    public static void addReloadListener(AddReloadListenerEvent event) {
        event.addListener(new Loader());
    }

    @SubscribeEvent
    public static void sync(OnDatapackSyncEvent event) {
        var packet = new TooltipDataNetwork.Sync(current.json());
        if (event.getPlayer() != null) TooltipDataNetwork.send(event.getPlayer(), packet);
        else for (var player : event.getPlayerList().getPlayers()) TooltipDataNetwork.send(player, packet);
    }

    @SubscribeEvent
    public static void stop(ServerStoppedEvent event) {
        current = new Snapshot("{}", List.of());
    }

    private static final class Loader extends SimplePreparableReloadListener<Snapshot> {
        @Override
        protected Snapshot prepare(ResourceManager resources, ProfilerFiller profiler) {
            JsonObject files = new JsonObject();
            var found = resources.listResources(DIRECTORY, id -> id.getPath().endsWith(".json"));
            for (ResourceLocation id : found.keySet().stream().sorted().toList()) {
                String path = id.getPath();
                String name = id.getNamespace() + ":" + path.substring(DIRECTORY.length() + 1, path.length() - 5);
                try (Reader reader = found.get(id).openAsReader()) {
                    JsonElement element = JsonParser.parseReader(reader);
                    if (!element.isJsonObject()) throw new JsonParseException("Expected JSON object");
                    files.add(name, element);
                } catch (Exception e) {
                    throw new IllegalArgumentException("Invalid tooltip datapack file " + id + ": " + e.getMessage(), e);
                }
            }
            List<TooltipData.Definition> definitions = TooltipData.parse(files);
            String json = files.toString();
            if (json.length() > TooltipData.MAX_JSON_CHARS)
                throw new IllegalArgumentException("Tooltip datapack exceeds " + TooltipData.MAX_JSON_CHARS + " JSON characters");
            return new Snapshot(json, definitions);
        }

        @Override
        protected void apply(Snapshot snapshot, ResourceManager resources, ProfilerFiller profiler) {
            current = snapshot;
            LogUtils.getLogger().info("Loaded {} Oasiso tooltip descriptions", snapshot.definitions().size());
        }
    }

    private TooltipDatapacks() {
    }
}
