package com.benji.oasiso.client.fluwirussort;

import com.benji.oasiso.ModSounds;
import com.benji.oasiso.common.fluwirussort.SortWire;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.ContainerScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.text.Collator;
import java.util.Arrays;
import java.util.Locale;
import java.util.stream.IntStream;

@Mod.EventBusSubscriber(modid = "oasiso", value = Dist.CLIENT)
public final class SortDisplay {
    private static int session;
    private static AbstractContainerMenu menu;
    private static Flight flight;
    private static CleanSound loop;

    private record Flight(Slot from, Slot to, ItemStack moving, ItemStack displaced, double start, int duration) {
    }

    public static void receive(SortWire.Cue cue) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        if (cue.kind() == SortWire.Kind.OPEN) {
            SortStars.clear();
            finish();
            menu = mc.player.containerMenu;
            if (menu.containerId != cue.menu()) return;
            session = cue.session();
            loop = new CleanSound();
            mc.getSoundManager().play(loop);
            if (menu == mc.player.inventoryMenu) {
                if (mc.player.isCreative()) mc.setScreen(new PersonalScreen(mc.player));
                else mc.setScreen(new InventoryScreen(mc.player));
            }
            int[] ids = cue.slots();
            String[] names = new String[ids.length];
            for (int i = 0; i < ids.length; i++) {
                ItemStack stack = menu.getSlot(ids[i]).getItem();
                names[i] = stack.isEmpty() ? null : stack.getHoverName().getString();
            }
            Locale locale = Locale.forLanguageTag(mc.getLanguageManager().getSelected().replace('_', '-'));
            Collator collator = Collator.getInstance(locale);
            collator.setStrength(Collator.SECONDARY);
            Integer[] indices = IntStream.range(0, ids.length).boxed().toArray(Integer[]::new);
            Arrays.sort(indices, (a, b) -> {
                if (names[a] == null) return names[b] == null ? 0 : 1;
                if (names[b] == null) return -1;
                return collator.compare(names[a], names[b]);
            });
            SortWire.reply(session, Arrays.stream(indices).mapToInt(Integer::intValue).toArray());
            return;
        }
        if (cue.session() != session || menu == null) return;
        switch (cue.kind()) {
            case MOVE -> {
                Slot a = menu.getSlot(cue.slots()[0]), b = menu.getSlot(cue.slots()[1]);
                SortOutline.invalidate();
                SortStars.begin();
                flight = new Flight(a, b, a.getItem().copy(), b.getItem().copy(), clock(), cue.duration());
                play(ModSounds.FLUWIRUS_BOP.get(), 1.09F);
            }
            case LAND -> {
                flight = null;
                play(ModSounds.FLUWIRUS_BOP.get(), .9F);
            }
            case DONE -> {
                finish();
                play(SoundEvents.EXPERIENCE_ORB_PICKUP, 1.42F);
            }
            case CANCEL -> finish();
            default -> {
            }
        }
    }

    private static double clock() {
        Minecraft mc = Minecraft.getInstance();
        return mc.level.getGameTime() + (double) mc.getFrameTime();
    }

    private static void play(SoundEvent sound, float pitch) {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(sound, pitch, .65F));
    }

    private static void finish() {
        if (loop != null) loop.fading = true;
        loop = null;
        flight = null;
        menu = null;
    }

    @SubscribeEvent
    public static void logout(ClientPlayerNetworkEvent.LoggingOut event) {
        finish();
        SortStars.clear();
        SortOutline.release();
    }

    @SubscribeEvent
    public static void draw(ContainerScreenEvent.Render.Foreground event) {
        if (Minecraft.getInstance().level == null) return;
        Flight f = flight;
        var screenMenu = event.getContainerScreen().getMenu();
        if (f == null || screenMenu != menu) {
            SortStars.draw(event.getGuiGraphics(), screenMenu, clock());
            return;
        }
        GuiGraphics g = event.getGuiGraphics();
        float t = Mth.clamp((float) ((clock() - f.start) / f.duration), 0, 1);
        float u = Mth.clamp((t - .14F) / .86F, 0, 1);
        u = u * u * (3 - 2 * u);
        float arc = Mth.sin(u * (float) Math.PI);
        float x = Mth.lerp(u, f.from.x, f.to.x), y = Mth.lerp(u, f.from.y, f.to.y) - arc * 8;
        var pose = g.pose();
        pose.pushPose();
        pose.translate(0, 0, 310);
        for (Slot s : new Slot[]{f.from, f.to}) {
            g.fill(s.x, s.y, s.x + 16, s.y + 16, 0xFF817A66);
            g.fill(s.x + 1, s.y + 1, s.x + 15, s.y + 15, 0xFF8B8574);
        }
        if (!f.displaced.isEmpty())
            icon(g, f.displaced, Mth.lerp(u, f.to.x, f.from.x), Mth.lerp(u, f.to.y, f.from.y) + arc * 6, 1 - .13F * arc);
        float lift = Math.min(1, t / .14F) * (1 - u);
        float scale = 1 + .3F * lift + .13F * arc;
        SortOutline.draw(g, f.moving, x, y, scale);
        icon(g, f.moving, x, y, scale);
        SortStars.emit(menu, clock(), x + 8, y + 8);
        pose.popPose();
        SortStars.draw(g, menu, clock());
    }

    private static void icon(GuiGraphics g, ItemStack stack, float x, float y, float scale) {
        var pose = g.pose();
        pose.pushPose();
        pose.translate(x + 8, y + 8, 0);
        pose.scale(scale, scale, 1);
        g.renderItem(stack, -8, -8);
        g.renderItemDecorations(Minecraft.getInstance().font, stack, -8, -8);
        pose.popPose();
    }

    private static final class CleanSound extends AbstractTickableSoundInstance {
        boolean fading;

        CleanSound() {
            super(ModSounds.FLUWIRUS_CLEAN.get(), SoundSource.PLAYERS, RandomSource.create());
            looping = true;
            relative = true;
            delay = 0;
            volume = .65F;
            attenuation = Attenuation.NONE;
        }

        @Override
        public void tick() {
            if (fading) {
                volume = Math.max(0, volume - .065F);
                if (volume <= .001F) stop();
            }
        }
    }

    private static final class PersonalScreen extends AbstractContainerScreen<InventoryMenu> {
        private static final ResourceLocation BG = ResourceLocation.fromNamespaceAndPath("minecraft", "textures/gui/container/inventory.png");

        PersonalScreen(Player p) {
            super(p.inventoryMenu, p.getInventory(), Component.translatable("container.inventory"));
        }

        @Override
        protected void renderBg(GuiGraphics g, float partial, int mx, int my) {
            g.blit(BG, leftPos, topPos, 0, 0, imageWidth, imageHeight);
        }

        @Override
        protected void renderLabels(GuiGraphics g, int x, int y) {
            g.drawString(font, Component.translatable("container.crafting"), 97, 8, 0x404040, false);
        }

        @Override
        public void render(GuiGraphics g, int x, int y, float partial) {
            renderBackground(g);
            super.render(g, x, y, partial);
            renderTooltip(g, x, y);
        }
    }
}
