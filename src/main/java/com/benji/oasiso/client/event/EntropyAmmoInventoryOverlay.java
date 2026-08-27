package com.benji.oasiso.client.event;

import com.benji.oasiso.Oasiso;
import com.benji.oasiso.common.item.EntropyChestplateItem;
import com.benji.oasiso.common.util.EntropyAmmoStorage;
import com.benji.oasiso.network.EntropyAmmoInsertPacket;
import com.benji.oasiso.network.EntropyAmmoSlotClickPacket;
import com.benji.oasiso.network.ModMessages;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(modid = Oasiso.MODID, value = Dist.CLIENT)
public final class EntropyAmmoInventoryOverlay {

    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "textures/gui/entropy_inventory.png");

    private static final int WIDTH = 86;
    private static final int HEIGHT = 68;

    private static final int SLOT_X = 7;
    private static final int SLOT_Y = 7;
    private static final int SLOT_STEP = 18;
    private static final int SLOT_SIZE = 16;

    private static final long PANEL_LIFETIME_MS = 2_000L;
    private static int selectedChestInventorySlot = -1;

    private static int panelX;
    private static int panelY;
    private static boolean panelVisible;

    private static long panelHideAtMs;
    private static boolean wasHoveringChestplate;
    private static Screen trackedScreen;

    private EntropyAmmoInventoryOverlay() {
    }

    @SubscribeEvent
    public static void onRender(ScreenEvent.Render.Post event) {
        if (trackedScreen != event.getScreen()) {
            resetSelection();
            trackedScreen = event.getScreen();
        }

        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;

        if (player == null || !(event.getScreen() instanceof AbstractContainerScreen<?> screen)) {
            resetSelection();
            return;
        }

        if (panelVisible && System.currentTimeMillis() >= panelHideAtMs) {
            panelVisible = false;
        }

        Slot hovered = screen.getSlotUnderMouse();
        boolean hoveringChestplate = isPlayerInventoryEntropySlot(player, hovered);
        if (hoveringChestplate) {
            if (!wasHoveringChestplate) {
                selectedChestInventorySlot = hovered.getContainerSlot();

                positionPanel(event.getMouseX(), event.getMouseY(), event.getScreen().width, event.getScreen().height);

                showPanel();
            }

            wasHoveringChestplate = true;
        } else {
            wasHoveringChestplate = false;
        }

        ItemStack chest = getSelectedChest(player);
        if (chest.isEmpty()) {
            resetSelection();
            return;
        }

        if (!panelVisible) {
            return;
        }

        boolean carriedEmpty = screen.getMenu().getCarried().isEmpty();

        renderPanel(event.getGuiGraphics(), chest, event.getMouseX(), event.getMouseY(), carriedEmpty);

        if (isInsidePanel(event.getMouseX(), event.getMouseY())) {
            renderCarriedStackOnTop(event.getGuiGraphics(), screen.getMenu().getCarried(), event.getMouseX(), event.getMouseY());
        } else if (carriedEmpty) {
            renderHoveredVanillaSlotTooltipOnTop(event.getGuiGraphics(), screen, event.getMouseX(), event.getMouseY());
        }
    }

    @SubscribeEvent
    public static void onMousePressed(ScreenEvent.MouseButtonPressed.Pre event) {
        int button = event.getButton();

        if (button != GLFW.GLFW_MOUSE_BUTTON_LEFT && button != GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;

        if (player == null || !(event.getScreen() instanceof AbstractContainerScreen<?> screen)) {
            return;
        }

        ItemStack chest = getSelectedChest(player);
        if (!chest.isEmpty() && panelVisible && isInsidePanel(event.getMouseX(), event.getMouseY())) {

            int internalSlot = getInternalSlotAt(event.getMouseX(), event.getMouseY());

            if (internalSlot >= 0) {
                ModMessages.sendToServer(new EntropyAmmoSlotClickPacket(selectedChestInventorySlot, internalSlot, button, Screen.hasShiftDown()));

                resetPanelTimer();
            }

            event.setCanceled(true);
            return;
        }
        if (button != GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
            return;
        }

        Slot hovered = screen.getSlotUnderMouse();

        if (isPlayerInventoryEntropySlot(player, hovered)) {
            selectedChestInventorySlot = hovered.getContainerSlot();

            positionPanel(event.getMouseX(), event.getMouseY(), event.getScreen().width, event.getScreen().height);
            showPanel();

            ItemStack carried = screen.getMenu().getCarried();
            if (EntropyAmmoStorage.isAllowedAmmo(carried)) {
                ModMessages.sendToServer(new EntropyAmmoInsertPacket(selectedChestInventorySlot, -1, carried.copyWithCount(1)));
                resetPanelTimer();
                event.setCanceled(true);
            }
            return;
        }

        if (!panelVisible || hovered == null || hovered.container != player.getInventory() || chest.isEmpty() || !EntropyAmmoStorage.isAllowedAmmo(hovered.getItem())) {
            return;
        }
        ModMessages.sendToServer(new EntropyAmmoInsertPacket(selectedChestInventorySlot, hovered.getContainerSlot(), hovered.getItem().copyWithCount(1)));
        resetPanelTimer();
        event.setCanceled(true);
    }

    private static void renderPanel(GuiGraphics graphics, ItemStack chest, int mouseX, int mouseY, boolean allowTooltip) {
        graphics.pose().pushPose();
        graphics.pose().translate(0.0F, 0.0F, 450.0F);

        RenderSystem.enableBlend();
        graphics.blit(TEXTURE, panelX, panelY, 0, 0, WIDTH, HEIGHT, WIDTH, HEIGHT);

        for (int slot = 0; slot < EntropyAmmoStorage.SLOT_COUNT; slot++) {
            int x = slotX(slot);
            int y = slotY(slot);
            ItemStack ammo = EntropyAmmoStorage.getItem(chest, slot);

            if (!ammo.isEmpty()) {
                graphics.renderItem(ammo, x, y);
                graphics.renderItemDecorations(Minecraft.getInstance().font, ammo, x, y);
            }

            if (mouseX >= x && mouseX < x + SLOT_SIZE && mouseY >= y && mouseY < y + SLOT_SIZE) {
                graphics.fill(x, y, x + SLOT_SIZE, y + SLOT_SIZE, 0x55FFFFFF);
            }
        }

        RenderSystem.disableBlend();
        graphics.pose().popPose();

        if (!allowTooltip) {
            return;
        }

        int hoveredInternalSlot = getInternalSlotAt(mouseX, mouseY);
        if (hoveredInternalSlot >= 0) {
            ItemStack hoveredAmmo = EntropyAmmoStorage.getItem(chest, hoveredInternalSlot);

            if (!hoveredAmmo.isEmpty()) {
                renderTooltipOnTop(graphics, hoveredAmmo, mouseX, mouseY);
            }
        }
    }

    private static void renderHoveredVanillaSlotTooltipOnTop(GuiGraphics graphics, AbstractContainerScreen<?> screen, int mouseX, int mouseY) {
        Slot hoveredSlot = screen.getSlotUnderMouse();

        if (hoveredSlot == null || !hoveredSlot.hasItem()) {
            return;
        }

        renderTooltipOnTop(graphics, hoveredSlot.getItem(), mouseX, mouseY);
    }

    private static void renderTooltipOnTop(GuiGraphics graphics, ItemStack stack, int mouseX, int mouseY) {
        graphics.pose().pushPose();
        graphics.pose().translate(0.0F, 0.0F, 800.0F);
        graphics.renderTooltip(Minecraft.getInstance().font, stack, mouseX, mouseY);
        graphics.pose().popPose();
    }

    private static void renderCarriedStackOnTop(GuiGraphics graphics, ItemStack carried, int mouseX, int mouseY) {
        if (carried.isEmpty()) {
            return;
        }

        int x = mouseX - 8;
        int y = mouseY - 8;

        graphics.pose().pushPose();
        graphics.pose().translate(0.0F, 0.0F, 700.0F);
        graphics.renderItem(carried, x, y);
        graphics.renderItemDecorations(Minecraft.getInstance().font, carried, x, y);

        graphics.pose().popPose();
    }

    private static boolean isPlayerInventoryEntropySlot(Player player, Slot slot) {
        return slot != null && slot.container == player.getInventory() && slot.getItem().getItem() instanceof EntropyChestplateItem;
    }

    private static ItemStack getSelectedChest(Player player) {
        Inventory inventory = player.getInventory();

        if (selectedChestInventorySlot < 0 || selectedChestInventorySlot >= inventory.getContainerSize()) {
            return ItemStack.EMPTY;
        }

        ItemStack stack = inventory.getItem(selectedChestInventorySlot);
        return stack.getItem() instanceof EntropyChestplateItem ? stack : ItemStack.EMPTY;
    }

    private static void positionPanel(double mouseX, double mouseY, int screenWidth, int screenHeight) {
        int mouseXi = (int) mouseX;
        int mouseYi = (int) mouseY;

        int preferredX = mouseXi + 14;

        if (preferredX + WIDTH > screenWidth - 4) {
            preferredX = mouseXi - WIDTH - 14;
        }

        panelX = Math.max(4, Math.min(preferredX, screenWidth - WIDTH - 4));
        panelY = Math.max(4, Math.min(mouseYi - 8, screenHeight - HEIGHT - 4));
    }

    private static boolean isInsidePanel(double mouseX, double mouseY) {
        return mouseX >= panelX && mouseX < panelX + WIDTH && mouseY >= panelY && mouseY < panelY + HEIGHT;
    }

    private static int getInternalSlotAt(double mouseX, double mouseY) {
        for (int slot = 0; slot < EntropyAmmoStorage.SLOT_COUNT; slot++) {
            int x = slotX(slot);
            int y = slotY(slot);

            if (mouseX >= x && mouseX < x + SLOT_SIZE && mouseY >= y && mouseY < y + SLOT_SIZE) {
                return slot;
            }
        }

        return -1;
    }

    private static int slotX(int slot) {
        return panelX + SLOT_X + (slot % 4) * SLOT_STEP;
    }

    private static int slotY(int slot) {
        return panelY + SLOT_Y + (slot / 4) * SLOT_STEP;
    }

    private static void showPanel() {
        panelVisible = true;
        resetPanelTimer();
    }

    private static void resetPanelTimer() {
        panelHideAtMs = System.currentTimeMillis() + PANEL_LIFETIME_MS;
    }

    private static void resetSelection() {
        selectedChestInventorySlot = -1;
        panelVisible = false;
        panelHideAtMs = 0L;
        wasHoveringChestplate = false;
    }
}
