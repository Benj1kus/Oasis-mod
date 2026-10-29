package com.benji.oasiso.client.tooltip;

import com.benji.oasiso.Oasiso;
import com.benji.oasiso.common.tooltip.TooltipData;
import com.benji.oasiso.common.tooltip.TooltipTextLayout;
import com.benji.oasiso.common.util.OasisoTextFx;
import com.google.gson.JsonParser;
import com.mojang.datafixers.util.Either;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RenderTooltipEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import java.util.*;

@Mod.EventBusSubscriber(modid = Oasiso.MODID, value = Dist.CLIENT)
public final class TooltipVFXClient {
    private record Entry(TooltipData.Definition data, List<TagKey<Item>> tags) {}
    private static List<Entry> entries = List.of();

    /** Called on the client thread, including login and successful /reload. */
    public static void receive(String json) {
        entries = TooltipData.parse(JsonParser.parseString(json).getAsJsonObject()).stream()
                .map(d -> new Entry(d, d.tags().stream()
                        .map(id -> TagKey.create(Registries.ITEM, Objects.requireNonNull(ResourceLocation.tryParse(id))))
                        .toList()))
                .toList();
    }

    @SubscribeEvent
    public static void logout(ClientPlayerNetworkEvent.LoggingOut event) { entries = List.of(); }

    private static TooltipData.Definition find(ItemStack stack) {
        if (stack.isEmpty()) return null;
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(stack.getItem());
        if (id == null) return null;
        for (Entry entry : entries) {
            if (entry.data().items().contains(id.toString()) || entry.tags().stream().anyMatch(stack::is))
                return entry.data();
        }
        return null;
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void tooltip(ItemTooltipEvent event) {
        TooltipData.Definition definition = find(event.getItemStack());
        if (definition == null) return;
        Set<String> removed = removedText(definition);
        var lines = event.getToolTip();
        for (int i = lines.size() - 1; i >= 1; i--)
            if (remove(lines.get(i), definition, removed)) lines.remove(i);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void gather(RenderTooltipEvent.GatherComponents event) {
        TooltipData.Definition definition = find(event.getItemStack());
        if (definition == null) return;
        var elements = event.getTooltipElements();
        Set<String> removed = removedText(definition);
        for (int i = elements.size() - 1; i >= 1; i--)
            if (elements.get(i).left().map(t -> remove(t, definition, removed)).orElse(false)) elements.remove(i);
        if (elements.stream().anyMatch(e -> e.right().map(OasisoAnimatedTooltip.class::isInstance).orElse(false))) return;
        int index = Math.min(1, elements.size());
        for (TooltipData.Line line : definition.lines()) {
            List<OasisoAnimatedTooltip.Part> parts = new ArrayList<>();
            for (var span : TooltipTextLayout.build(definition, line, TooltipVFXClient::resolve)) {
                var fx = span.style();
                var p = fx.palette();
                var s = fx.sparks();
                Component text = Component.literal(span.text()).withStyle(style -> style.withItalic(fx.italic()).withBold(fx.bold()));
                parts.add(new OasisoAnimatedTooltip.Part(text,
                        new OasisoTextFx.Palette(p.first(), p.second(), p.third(), p.breath(), p.shine()),
                        fx.shine(), fx.glow(), s == null ? null : new OasisoAnimatedTooltip.Sparks(
                        s.auraDark(), s.auraBright(), s.coreDark(), s.coreBright(), s.flash())));
            }
            elements.add(index++, Either.right(new OasisoAnimatedTooltip(parts)));
        }
    }

    private static String resolve(TooltipData.Text text) {
        return text.translate() == null ? text.literal() : Component.translatable(text.translate()).getString();
    }

    private static Set<String> removedText(TooltipData.Definition definition) {
        Set<String> result = new HashSet<>();
        for (String key : definition.removeTranslations()) result.add(plain(Component.translatable(key).getString()));
        if (definition.removeMatchingLines()) {
            for (var line : definition.lines()) {
                StringBuilder text = new StringBuilder();
                for (var span : TooltipTextLayout.build(definition, line, TooltipVFXClient::resolve)) text.append(span.text());
                result.add(plain(text.toString()));
            }
        }
        result.remove("");
        return result;
    }

    private static boolean remove(FormattedText text, TooltipData.Definition definition, Set<String> removed) {
        return (text instanceof Component component && hasKey(component, definition.removeTranslations()))
                || removed.contains(plain(text.getString()));
    }
    private static boolean hasKey(Component component, List<String> keys) {
        if (component.getContents() instanceof TranslatableContents t && keys.contains(t.getKey())) return true;
        return component.getSiblings().stream().anyMatch(c -> hasKey(c, keys));
    }
    private static String plain(String value) {
        String stripped = ChatFormatting.stripFormatting(value);
        return stripped == null ? "" : stripped.trim();
    }
    private TooltipVFXClient() {}
}
