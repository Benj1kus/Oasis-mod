package com.benji.oasiso.client.tooltip;

import com.benji.oasiso.common.item.*;
import com.benji.oasiso.common.util.OasisoTextFx;
import com.mojang.datafixers.util.Either;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.item.ArmorItem;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderTooltipEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;

import static com.benji.oasiso.client.tooltip.OasisoAnimatedTooltip.part;


@Mod.EventBusSubscriber(modid = "oasiso", value = Dist.CLIENT)
public final class TooltipVFXClient {
    private static final String OLD_KEY = "tooltip.oasiso.scroll";

    private static final OasisoTextFx.Palette SCISSORS_PLAIN =
            new OasisoTextFx.Palette(0xBFEFFF, 0xBFEFFF, 0xBFEFFF, 0xBFEFFF, 0xFFFFFF);

    private static final OasisoTextFx.Palette SCISSORS_BUTTON = //YELLOW
            new OasisoTextFx.Palette(
                    0xFFD45A,
                    0xFF982E,
                    0xFFF0A0,
                    0xFFE47A,
                    0xFFFFFF
            );

    private static final OasisoTextFx.Palette SCISSORS_WARNING =
            new OasisoTextFx.Palette(0xB76CFF, 0xFF75BC, 0x6495FF, 0xE7BAFF, 0xFFFFFF); //PURPLE

    //GLOVE

    private static final OasisoTextFx.Palette GLOVE_PAL = //CYAN GRADIENT
            new OasisoTextFx.Palette(
                    0x00FFC3, 0x2CD3E6, 0x0D89D1, 0x8C7AFF,
                    0xFFFFFF
            );



    private static final OasisoTextFx.Palette ACCENT = //WHITE GRADIENT
            new OasisoTextFx.Palette(
                    0xFFFFFF, 0xA8EFFF, 0xA1FFEB, 0x9EFFA1,
                    0xFFFFFF
            );

    private static final OasisoAnimatedTooltip.Sparks GLOVE_SPARKS = //WHITE SPARKS
            new OasisoAnimatedTooltip.Sparks(
                    0x9EFFA1,
                    0xFFFFFF,
                    0xA8EFFF,
                    0xA1FFEB,
                    0x7DFFFF
            );

    private TooltipVFXClient() {}

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void tooltip(ItemTooltipEvent event) {
        if (!(event.getItemStack().getItem() instanceof ConfigScrollItem)) return;
        var lines = event.getToolTip();
        for (int i = lines.size() - 1; i >= 1; i--) {
            if (isOldDescription(lines.get(i))) lines.remove(i);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void gather(RenderTooltipEvent.GatherComponents event) {
        if (!(event.getItemStack().getItem() instanceof ConfigScrollItem)) return;
        var elements = event.getTooltipElements();
        for (int i = elements.size() - 1; i >= 1; i--) {
            if (elements.get(i).left().map(TooltipVFXClient::isOldDescription).orElse(false))
                elements.remove(i);
        }
        if (elements.stream().anyMatch(e -> e.right().map(OasisoAnimatedTooltip.class::isInstance).orElse(false)))
            return;
        elements.add(Math.min(1, elements.size()), Either.right(description()));
    }

    private static OasisoAnimatedTooltip description() {
        return OasisoAnimatedTooltip.line(
                part(Component.translatable("tooltip.oasiso.scroll.mods").withStyle(ChatFormatting.ITALIC),
                        OasisoTextFx.CYAN_PURPLE),
                part(Component.literal(" "), OasisoTextFx.CYAN_PURPLE),
                part(Component.translatable("tooltip.oasiso.scroll.config").withStyle(ChatFormatting.ITALIC),
                        OasisoTextFx.GOLD_ORANGE).glow().sparks(OasisoAnimatedTooltip.Sparks.GOLD)
        );
    }

    private static boolean isOldDescription(FormattedText text) {
        if (text instanceof Component component && hasOldKey(component)) return true;
        String plain = plain(text.getString());
        return plain.equals(plain(Component.translatable(OLD_KEY).getString()))
                || plain.equals(plain(Component.translatable("tooltip.oasiso.scroll.mods").getString()
                + " " + Component.translatable("tooltip.oasiso.scroll.config").getString()));
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onScissorsTooltip(
            RenderTooltipEvent.GatherComponents event
    ) {
        if (!(event.getItemStack().getItem() instanceof ChaosScissorsItem)) {
            return;
        }

        var elements = event.getTooltipElements();
        if (elements.stream().anyMatch(e -> e.right().map(OasisoAnimatedTooltip.class::isInstance).orElse(false))) {
            return;
        }
        int index = Math.min(1, elements.size());

        elements.add(index++, Either.right(scissorsFirstLine()));
        elements.add(index, Either.right(scissorsSecondLine()));
    }

    private static OasisoAnimatedTooltip scissorsFirstLine() {
        String fullText = Component.translatable("tooltip.oasiso.scis").getString();

        String selectedText = Component.translatable("tooltip.oasiso.rmb").getString();

        int start = selectedText.isEmpty() ? -1 : fullText.indexOf(selectedText);
        if (start < 0) {
            selectedText = "[RMB]";
            start = fullText.indexOf(selectedText);
        }
        if (start < 0) {
            return OasisoAnimatedTooltip.line(
                    part(Component.literal(fullText), GLOVE_PAL).withoutShine()
            );
        }

        String before = fullText.substring(0, start);
        String after = fullText.substring(start + selectedText.length());
        var parts = new ArrayList<OasisoAnimatedTooltip.Part>();

        parts.add(part(Component.literal(before), GLOVE_PAL).withoutShine());

        parts.add(part(
                Component.literal(selectedText).withStyle(ChatFormatting.ITALIC),
                ACCENT
        ).glow().sparks(OasisoAnimatedTooltip.Sparks.BLUE));

        parts.add(part(Component.literal(after), GLOVE_PAL).withoutShine());

        return new OasisoAnimatedTooltip(parts);
    }

    private static OasisoAnimatedTooltip scissorsSecondLine() {
        return OasisoAnimatedTooltip.line(
                part(
                        Component.translatable("tooltip.oasiso.scis2")
                                .withStyle(style -> style.withItalic(false).withBold(false)),
                        GLOVE_PAL
                ).glow()
        );
    }

    //GLOVE

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onGloveTooltip(
            RenderTooltipEvent.GatherComponents event
    ) {
        if (!(event.getItemStack().getItem() instanceof EntropyChestplateGloveItem)) {
            return;
        }

        var elements = event.getTooltipElements();
        if (elements.stream().anyMatch(e -> e.right().map(OasisoAnimatedTooltip.class::isInstance).orElse(false))) {
            return;
        }

        int index = Math.min(1, elements.size());

        elements.add(index++, Either.right(gloveFirstLine()));
        elements.add(index, Either.right(gloveSecondLine()));
        elements.add(index, Either.right(gloveThirdLine()));
        elements.add(index, Either.right(gloveFourthLine()));
    }

    private static OasisoAnimatedTooltip gloveFirstLine() {
        //full
        String fullText = Component.translatable("tooltip.oasiso.glove").getString();
        //part text
        String selectedText = Component.translatable("tooltip.oasiso.n").getString();

        int start = selectedText.isEmpty() ? -1 : fullText.indexOf(selectedText);
        if (start < 0) {
            selectedText = "[N]";
            start = fullText.indexOf(selectedText);
        }
        if (start < 0) {
            return OasisoAnimatedTooltip.line(
                    part(Component.literal(fullText), ACCENT).withoutShine()
            );
        }

        String before = fullText.substring(0, start);
        String after = fullText.substring(start + selectedText.length());
        var parts = new ArrayList<OasisoAnimatedTooltip.Part>();

        // before part
        parts.add(part(Component.literal(before), GLOVE_PAL));

        parts.add(part(
                Component.literal(selectedText).withStyle(ChatFormatting.ITALIC),
                ACCENT
        ).glow());

        // after part
        parts.add(part(Component.literal(after), GLOVE_PAL));

        return new OasisoAnimatedTooltip(parts);
    }

    private static OasisoAnimatedTooltip gloveSecondLine() {
        return OasisoAnimatedTooltip.line(
                part(
                        Component.translatable("tooltip.oasiso.glove2")
                                .withStyle(style -> style.withItalic(false).withBold(false)),
                        GLOVE_PAL
                )
        );
    }

    private static OasisoAnimatedTooltip gloveThirdLine() {
        //full
        String fullText = Component.translatable("tooltip.oasiso.glove4").getString();
        //part text
        String selectedText = Component.translatable("tooltip.oasiso.k").getString();

        int start = selectedText.isEmpty() ? -1 : fullText.indexOf(selectedText);
        if (start < 0) {
            selectedText = "[K]";
            start = fullText.indexOf(selectedText);
        }
        if (start < 0) {
            return OasisoAnimatedTooltip.line(
                    part(Component.literal(fullText), ACCENT).withoutShine()
            );
        }

        String before = fullText.substring(0, start);
        String after = fullText.substring(start + selectedText.length());
        var parts = new ArrayList<OasisoAnimatedTooltip.Part>();

        // before part
        parts.add(part(Component.literal(before), GLOVE_PAL));

        parts.add(part(
                Component.literal(selectedText).withStyle(ChatFormatting.ITALIC),
                ACCENT
        ).glow());

        // after part
        parts.add(part(Component.literal(after), GLOVE_PAL));

        return new OasisoAnimatedTooltip(parts);
    }

    private static OasisoAnimatedTooltip gloveFourthLine() {
        //full
        String fullText = Component.translatable("tooltip.oasiso.glove3").getString();
        //part text
        String selectedText = Component.translatable("tooltip.oasiso.sh").getString();

        int start = selectedText.isEmpty() ? -1 : fullText.indexOf(selectedText);
        if (start < 0) {
            selectedText = "[SHIFT] + [RMB]";
            start = fullText.indexOf(selectedText);
        }
        if (start < 0) {
            return OasisoAnimatedTooltip.line(
                    part(Component.literal(fullText), ACCENT).withoutShine()
            );
        }

        String before = fullText.substring(0, start);
        String after = fullText.substring(start + selectedText.length());
        var parts = new ArrayList<OasisoAnimatedTooltip.Part>();

        // before part
        parts.add(part(Component.literal(before), GLOVE_PAL));

        parts.add(part(
                Component.literal(selectedText).withStyle(ChatFormatting.ITALIC),
                ACCENT
        ).glow());

        // after part
        parts.add(part(Component.literal(after), GLOVE_PAL));

        return new OasisoAnimatedTooltip(parts);
    }

    //KARAKOLIT CHESTPLATE

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onKarakolitChestplateTooltip(
            RenderTooltipEvent.GatherComponents event
    ) {

        if (!(event.getItemStack().getItem() instanceof SuperGoldArmorItem armor)) {
            return;
        }

        if (armor.getType() != ArmorItem.Type.CHESTPLATE) {
            return;
        }

        var elements = event.getTooltipElements();
        if (elements.stream().anyMatch(e -> e.right().map(OasisoAnimatedTooltip.class::isInstance).orElse(false))) {
            return;
        }

        int index = Math.min(1, elements.size());

        elements.add(index++, Either.right(kchestplateFirstLine()));
    }

    private static OasisoAnimatedTooltip kchestplateFirstLine() {
        //full
        String fullText = Component.translatable("tooltip.oasiso.karak_chest").getString();
        //part text
        String selectedText = Component.translatable("tooltip.oasiso.karak").getString();

        int start = selectedText.isEmpty() ? -1 : fullText.indexOf(selectedText);
        if (start < 0) {
            selectedText = "[SHIFT] + [RMB]";
            start = fullText.indexOf(selectedText);
        }
        if (start < 0) {
            return OasisoAnimatedTooltip.line(
                    part(Component.literal(fullText), SCISSORS_BUTTON).withoutShine()
            );
        }

        String before = fullText.substring(0, start);
        String after = fullText.substring(start + selectedText.length());
        var parts = new ArrayList<OasisoAnimatedTooltip.Part>();

        // before part
        parts.add(part(Component.literal(before), GLOVE_PAL));

        parts.add(part(
                Component.literal(selectedText).withStyle(ChatFormatting.ITALIC),
                SCISSORS_BUTTON
        ).glow());

        // after part
        parts.add(part(Component.literal(after), GLOVE_PAL));

        return new OasisoAnimatedTooltip(parts);
    }

    //AZUMALIT CHESTPLATE

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onAzumalitChestplateTooltip(
            RenderTooltipEvent.GatherComponents event
    ) {

        if (!(event.getItemStack().getItem() instanceof AzumalitArmorItem armor)) {
            return;
        }

        if (armor.getType() != ArmorItem.Type.CHESTPLATE) {
            return;
        }

        var elements = event.getTooltipElements();
        if (elements.stream().anyMatch(e -> e.right().map(OasisoAnimatedTooltip.class::isInstance).orElse(false))) {
            return;
        }

        int index = Math.min(1, elements.size());

        elements.add(index++, Either.right(azumalitchFirstLine()));
        elements.add(index++, Either.right(azumalitchSecondLine()));
    }

    private static OasisoAnimatedTooltip azumalitchFirstLine() {
        //full
        String fullText = Component.translatable("tooltip.oasiso.azumalit_chestplate.line1").getString();
        //part text
        String selectedText = Component.translatable("tooltip.oasiso.az.line1").getString();

        int start = selectedText.isEmpty() ? -1 : fullText.indexOf(selectedText);
        if (start < 0) {
            selectedText = "[SHIFT] + [RMB]";
            start = fullText.indexOf(selectedText);
        }
        if (start < 0) {
            return OasisoAnimatedTooltip.line(
                    part(Component.literal(fullText), ACCENT).withoutShine()
            );
        }

        String before = fullText.substring(0, start);
        String after = fullText.substring(start + selectedText.length());
        var parts = new ArrayList<OasisoAnimatedTooltip.Part>();

        // before part
        parts.add(part(Component.literal(before), SCISSORS_WARNING));

        parts.add(part(
                Component.literal(selectedText).withStyle(ChatFormatting.ITALIC),
                ACCENT
        ).glow());

        // after part
        parts.add(part(Component.literal(after), SCISSORS_WARNING));

        return new OasisoAnimatedTooltip(parts);
    }

    private static OasisoAnimatedTooltip azumalitchSecondLine() {
        //full
        String fullText = Component.translatable("tooltip.oasiso.azumalit_chestplate.line2").getString();
        //part text
        String selectedText = Component.translatable("tooltip.oasiso.azu.line2").getString();

        int start = selectedText.isEmpty() ? -1 : fullText.indexOf(selectedText);
        if (start < 0) {
            selectedText = "[H]";
            start = fullText.indexOf(selectedText);
        }
        if (start < 0) {
            return OasisoAnimatedTooltip.line(
                    part(Component.literal(fullText),  SCISSORS_BUTTON).withoutShine()
            );
        }

        String before = fullText.substring(0, start);
        String after = fullText.substring(start + selectedText.length());
        var parts = new ArrayList<OasisoAnimatedTooltip.Part>();

        // before part
        parts.add(part(Component.literal(before), GLOVE_PAL));

        parts.add(part(
                Component.literal(selectedText).withStyle(ChatFormatting.ITALIC),
                 SCISSORS_BUTTON
        ).glow());

        // after part
        parts.add(part(Component.literal(after), GLOVE_PAL));

        return new OasisoAnimatedTooltip(parts);
    }

    //SCARAB

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onScarabTooltip(
            RenderTooltipEvent.GatherComponents event
    ) {
        if (!(event.getItemStack().getItem() instanceof ScarabCoreItem)) {
            return;
        }

        var elements = event.getTooltipElements();
        if (elements.stream().anyMatch(e -> e.right().map(OasisoAnimatedTooltip.class::isInstance).orElse(false))) {
            return;
        }

        int index = Math.min(1, elements.size());

        elements.add(index++, Either.right(scarabFirstLine()));
        elements.add(index, Either.right(scarabSecondLine()));
    }

    private static OasisoAnimatedTooltip scarabFirstLine() {
        //full
        String fullText = Component.translatable("tooltip.oasiso.scarab2").getString();
        //part text
        String selectedText = Component.translatable("tooltip.oasiso.p").getString();

        int start = selectedText.isEmpty() ? -1 : fullText.indexOf(selectedText);
        if (start < 0) {
            selectedText = "[H]";
            start = fullText.indexOf(selectedText);
        }
        if (start < 0) {
            return OasisoAnimatedTooltip.line(
                    part(Component.literal(fullText),  SCISSORS_WARNING).withoutShine()
            );
        }

        String before = fullText.substring(0, start);
        String after = fullText.substring(start + selectedText.length());
        var parts = new ArrayList<OasisoAnimatedTooltip.Part>();

        // before part
        parts.add(part(Component.literal(before), GLOVE_PAL));

        parts.add(part(
                Component.literal(selectedText).withStyle(ChatFormatting.ITALIC),
                SCISSORS_WARNING
        ).glow());

        // after part
        parts.add(part(Component.literal(after), GLOVE_PAL));

        return new OasisoAnimatedTooltip(parts);
    }

    private static OasisoAnimatedTooltip scarabSecondLine() {
        return OasisoAnimatedTooltip.line(
                part(
                        Component.translatable("tooltip.oasiso.scarab1")
                                .withStyle(style -> style.withItalic(false).withBold(false)),
                        GLOVE_PAL
                ).glow()
        );
    }

    private static boolean hasOldKey(Component component) {
        if (component.getContents() instanceof TranslatableContents translated && OLD_KEY.equals(translated.getKey()))
            return true;
        return component.getSiblings().stream().anyMatch(TooltipVFXClient::hasOldKey);
    }

    private static String plain(String value) {
        String stripped = ChatFormatting.stripFormatting(value);
        return stripped == null ? "" : stripped.trim();
    }
}
