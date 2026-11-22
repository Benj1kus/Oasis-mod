package com.benji.oasiso.common.fluwirussort;

import com.benji.oasiso.common.item.FluwirusHeadItem;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.*;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.SlotItemHandler;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.*;

@Mod.EventBusSubscriber(modid = "oasiso")
public final class FluwirusSort {
    private record Opening(BlockEntity block, AbstractContainerMenu oldMenu, Item item, int tick) {
    }

    private static final Map<ServerPlayer, Opening> openings = new HashMap<>();
    private static final Map<ServerPlayer, Job> jobs = new HashMap<>();
    private static int nextId;

    public static boolean storage(Level level, BlockPos pos) {
        var state = level.getBlockState(pos);
        Block block = state.getBlock();
        if (block instanceof ChestBlock || block instanceof BarrelBlock || block instanceof ShulkerBoxBlock || block instanceof EnderChestBlock)
            return true;
        var key = ForgeRegistries.BLOCKS.getKey(block);
        if (key == null || key.getNamespace().equals("minecraft")) return false;
        var be = level.getBlockEntity(pos);
        return be instanceof Container || be != null && be.getCapability(ForgeCapabilities.ITEM_HANDLER).isPresent();
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void interact(PlayerInteractEvent.RightClickBlock event) {
        if (event.getUseBlock() == Event.Result.DENY || event.getUseItem() == Event.Result.DENY) return;
        var p = event.getEntity();
        if (p.isShiftKeyDown() && event.getItemStack().getItem() instanceof FluwirusHeadItem item && !p.getCooldowns().isOnCooldown(item) && storage(event.getLevel(), event.getPos())) {
            event.setUseBlock(Event.Result.ALLOW);
        }
    }

    public static void awaitStorage(ServerPlayer p, BlockPos pos, Item item) {
        if (!ready(p, item)) return;
        var block = p.level().getBlockEntity(pos);
        if (block != null) openings.put(p, new Opening(block, p.containerMenu, item, p.tickCount));
    }

    public static void personal(ServerPlayer p, Item item) {
        if (ready(p, item) && p.containerMenu == p.inventoryMenu) begin(p, item, null);
    }

    private static boolean ready(ServerPlayer p, Item item) {
        return p.isAlive() && !p.isSpectator() && !p.getCooldowns().isOnCooldown(item) && !jobs.containsKey(p);
    }

    private static boolean regularStorageSlot(Slot s, BlockEntity block, AbstractContainerMenu menu) {
        if (menu instanceof ChestMenu || menu instanceof ShulkerBoxMenu)
            return s.getClass() == Slot.class || menu.getClass() == ShulkerBoxMenu.class;
        if (s.getClass() == Slot.class) return s.container == block;
        if (s.getClass() != SlotItemHandler.class) return false;
        var handler = ((SlotItemHandler) s).getItemHandler();
        return handler instanceof ItemStackHandler && block.getCapability(ForgeCapabilities.ITEM_HANDLER).map(backing -> backing == handler).orElse(false);
    }

    private static void begin(ServerPlayer p, Item item, BlockEntity block) {
        AbstractContainerMenu menu = p.containerMenu;
        List<Slot> slots = new ArrayList<>();
        for (Slot s : menu.slots) {
            if (block == null) {
                if (s.container == p.getInventory() && s.getSlotIndex() >= 0 && s.getSlotIndex() < 36) slots.add(s);
            } else if (s.container != p.getInventory() && s.isActive() && regularStorageSlot(s, block, menu))
                slots.add(s);
        }
        if (slots.size() < 2 || slots.size() > SortWire.MAX || !menu.stillValid(p)) return;
        Job job = new Job(p, slots, menu, block);
        jobs.put(p, job);
        p.getCooldowns().addCooldown(item, 200);
        menu.sendAllDataToRemote();
        job.cue(SortWire.Kind.OPEN, 0, slots.stream().mapToInt(s -> s.index).toArray());
    }

    static void order(ServerPlayer p, int id, int[] order) {
        Job job = jobs.get(p);
        if (job == null || job.id != id || job.order != null || order.length != job.slots.size()) return;
        boolean[] seen = new boolean[order.length];
        for (int index : order) {
            if (index < 0 || index >= order.length || seen[index]) return;
            seen[index] = true;
        }
        job.order = order;
    }

    @SubscribeEvent
    public static void tick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer p)) return;
        Opening open = openings.remove(p);
        if (open != null && ready(p, open.item) && !open.block.isRemoved() && p.containerMenu != open.oldMenu && p.containerMenu != p.inventoryMenu && p.tickCount - open.tick <= 2)
            begin(p, open.item, open.block);
        Job job = jobs.get(p);
        if (job != null && !job.advance()) jobs.remove(p);
    }

    @SubscribeEvent
    public static void logout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) {
            openings.remove(p);
            jobs.remove(p);
        }
    }

    @SubscribeEvent
    public static void stop(ServerStoppedEvent event) {
        openings.clear();
        jobs.clear();
    }

    private static final class Job {
        final int id = ++nextId;
        final ServerPlayer player;
        final Level level;
        final AbstractContainerMenu menu;
        final BlockEntity block;
        final List<Slot> slots;
        final ItemStack[] expected;
        final int[] at, where;
        final int started;
        int[] order;
        int target, source, arrival, moves;

        Job(ServerPlayer p, List<Slot> slots, AbstractContainerMenu menu, BlockEntity block) {
            this.player = p;
            this.slots = slots;
            this.menu = menu;
            this.block = block;
            level = p.level();
            started = p.tickCount;
            expected = new ItemStack[slots.size()];
            at = new int[slots.size()];
            where = new int[slots.size()];
            for (int i = 0; i < slots.size(); i++) {
                expected[i] = slots.get(i).getItem().copy();
                at[i] = where[i] = i;
            }
        }

        void cue(SortWire.Kind kind, int duration, int... ids) {
            SortWire.cue(player, kind, id, menu.containerId, duration, ids);
        }

        boolean valid() {
            if (player.level() != level || !player.isAlive() || player.isSpectator()) return false;
            if (block != null) {
                if (block.isRemoved() || !level.hasChunkAt(block.getBlockPos()) || !block.getBlockPos().closerToCenterThan(player.position(), 8) || !menu.stillValid(player))
                    return false;
                boolean vanillaStorage = menu.getClass() == ChestMenu.class || menu.getClass() == ShulkerBoxMenu.class;
                if (player.containerMenu != menu && !(vanillaStorage && player.containerMenu == player.inventoryMenu))
                    return false;
            }
            return true;
        }

        boolean unchanged() {
            for (int i = 0; i < expected.length; i++)
                if (!ItemStack.matches(expected[i], slots.get(i).getItem())) return false;
            return true;
        }

        boolean cancel() {
            cue(SortWire.Kind.CANCEL, 0);
            return false;
        }

        boolean advance() {
            if (!valid()) return cancel();
            if (order == null) {
                if (player.tickCount - started >= 100) return cancel();
                return true;
            }
            if (arrival != 0 && player.tickCount < arrival) return true;
            if (!unchanged()) return cancel();
            if (arrival != 0) {
                if (!canSwap()) return cancel();
                Slot a = slots.get(source), b = slots.get(target);
                ItemStack moving = a.getItem(), displaced = b.getItem();
                a.set(displaced);
                b.set(moving);
                if (block != null) block.setChanged();
                expected[source] = displaced.copy();
                expected[target] = moving.copy();
                if (player.containerMenu == menu) menu.broadcastChanges();
                cue(SortWire.Kind.LAND, 0);
                swapIds();
                moves++;
                target++;
                arrival = 0;
            }
            while (target < order.length) {
                source = where[order[target]];
                if (source != target && !ItemStack.matches(expected[source], expected[target])) {
                    if (!canSwap()) return cancel();
                    int duration = Math.max(3, (int) Math.round(15 * Math.pow(.89, moves)));
                    arrival = player.tickCount + duration;
                    cue(SortWire.Kind.MOVE, duration, slots.get(source).index, slots.get(target).index);
                    return true;
                }
                swapIds();
                target++;
            }
            cue(SortWire.Kind.DONE, 0);
            return false;
        }

        void swapIds() {
            int first = at[source], second = at[target];
            at[source] = second;
            at[target] = first;
            where[first] = target;
            where[second] = source;
        }

        boolean canSwap() {
            Slot a = slots.get(source), b = slots.get(target);
            return allows(a, b.getItem()) && allows(b, a.getItem());
        }

        boolean allows(Slot slot, ItemStack incoming) {
            ItemStack current = slot.getItem();
            if (!current.isEmpty() && !slot.mayPickup(player)) return false;
            if (slot instanceof SlotItemHandler handler && !current.isEmpty() && handler.getItemHandler().extractItem(slot.getSlotIndex(), current.getCount(), true).getCount() != current.getCount())
                return false;
            return incoming.isEmpty() || slot.mayPlace(incoming) && incoming.getCount() <= Math.min(incoming.getMaxStackSize(), slot.getMaxStackSize(incoming));
        }
    }
}
