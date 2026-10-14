package com.benji.oasiso.common.store;

import com.benji.oasiso.Oasiso;
import com.benji.oasiso.common.item.ScienceHeartItem;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = Oasiso.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ScienceStoreServer {
    private static final Map<UUID, Session> SESSIONS = new HashMap<>();

    private static final class Session {
        final UUID token = UUID.randomUUID();
        final InteractionHand hand;
        final ItemStack heart;
        final ResourceKey<Level> dimension;
        final long opened;
        int sequence = 0, offer = -1;
        long started;

        Session(ServerPlayer p, InteractionHand hand, long now) {
            this.hand = hand;
            heart = p.getItemInHand(hand);
            dimension = p.level().dimension();
            opened = now;
        }
    }

    private static long now(ServerPlayer p) {
        return p.server.overworld().getGameTime();
    }

    public static boolean open(ServerPlayer p, InteractionHand hand) {
        ItemStack heart = p.getItemInHand(hand);
        if (!p.isAlive() || p.isSpectator() || !(heart.getItem() instanceof ScienceHeartItem) || p.getCooldowns().isOnCooldown(heart.getItem()) || p.containerMenu != p.inventoryMenu)
            return false;
        Session session = new Session(p, hand, now(p));
        SESSIONS.put(p.getUUID(), session);
        p.getCooldowns().addCooldown(heart.getItem(), ScienceStoreOffers.OPEN_COOLDOWN);
        ScienceStoreNetwork.open(p, session.token, hand, balance(p));
        return true;
    }

    private static boolean valid(ServerPlayer p, Session s) {
        return p.isAlive() && !p.isSpectator() && p.level().dimension().equals(s.dimension) && p.containerMenu == p.inventoryMenu && p.getItemInHand(s.hand) == s.heart && s.heart.getItem() instanceof ScienceHeartItem && now(p) - s.opened < 20 * 60 * 10;
    }

    private static int balance(ServerPlayer p) {
        return ScienceStoreTransaction.balance(p.getInventory(), ScienceStoreOffers.currency());
    }

    public static void action(ServerPlayer p, ScienceStoreNetwork.Action packet) {
        Session s = SESSIONS.get(p.getUUID());
        if (s == null || !s.token.equals(packet.session())) return;
        if (!valid(p, s)) {
            invalidate(p, s);
            return;
        }
        if (packet.action() == ScienceStoreNetwork.CLOSE) {
            SESSIONS.remove(p.getUUID());
            return;
        }
        if (packet.action() == ScienceStoreNetwork.CANCEL) {
            if (packet.sequence() == s.sequence) s.offer = -1;
            return;
        }
        if (!ScienceStoreOffers.valid(packet.offer())) return;
        if (packet.action() == ScienceStoreNetwork.BEGIN) {
            if (packet.sequence() <= s.sequence) return;
            s.sequence = packet.sequence();
            s.offer = packet.offer();
            s.started = now(p);
            return;
        }
        if (packet.action() != ScienceStoreNetwork.BUY || packet.sequence() != s.sequence || packet.offer() != s.offer)
            return;
        if (now(p) - s.started < ScienceStoreOffers.HOLD_TICKS) {
            ScienceStoreNetwork.reply(p, s.token, s.sequence, ScienceStoreNetwork.WAIT, balance(p));
            return;
        }
        int offer = s.offer;
        s.offer = -1; // Consume the attempt BEFORE mutating inventory. Duplicate BUY packets cannot purchase twice.
        ScienceStoreTransaction.Result result = ScienceStoreTransaction.purchase(p.getInventory(), ScienceStoreOffers.currency(), ScienceStoreOffers.price(offer), ScienceStoreOffers.stack(offer));
        p.inventoryMenu.broadcastChanges();
        ScienceStoreNetwork.reply(p, s.token, s.sequence, result.ordinal(), balance(p));
    }

    private static void invalidate(ServerPlayer p, Session s) {
        SESSIONS.remove(p.getUUID());
        ScienceStoreNetwork.reply(p, s.token, s.sequence, ScienceStoreNetwork.INVALID, balance(p));
    }

    @SubscribeEvent
    public static void tick(TickEvent.PlayerTickEvent e) {
        if (e.phase == TickEvent.Phase.END && e.player instanceof ServerPlayer p) {
            Session s = SESSIONS.get(p.getUUID());
            if (s != null && !valid(p, s)) invalidate(p, s);
        }
    }

    @SubscribeEvent
    public static void logout(PlayerEvent.PlayerLoggedOutEvent e) {
        SESSIONS.remove(e.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void stop(ServerStoppedEvent e) {
        SESSIONS.clear();
    }

    private ScienceStoreServer() {
    }
}
