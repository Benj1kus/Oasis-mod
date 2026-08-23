package com.benji.oasiso.dialogue.trigger;

import com.benji.oasiso.Oasiso;
import com.benji.oasiso.dialogue.DialogueRegistry;
import com.benji.oasiso.dialogue.DialogueSessionManager;
import com.benji.oasiso.dialogue.data.DialogueDefinition;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

@Mod.EventBusSubscriber(modid = Oasiso.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class DialogueTriggerEngine {

    private static final Map<String, Long> COOLDOWNS = new HashMap<>();

    private DialogueTriggerEngine() {
    }


    @SubscribeEvent
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        Entity target = event.getTarget();

        forEachTrigger("right_click_entity", (id, definition, trigger) -> {

            if (!matchesEntity(target, trigger.target)) {
                return false;
            }

            boolean started = start(player, target, id, definition, trigger);

            if (started && trigger.consume) {

                event.setCanceled(true);

                event.setCancellationResult(InteractionResult.SUCCESS);
            }

            return started;
        });
    }


    @SubscribeEvent
    public static void onBlockInteract(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !(event.getLevel() instanceof ServerLevel level)) {
            return;
        }

        BlockState state = level.getBlockState(event.getPos());

        forEachTrigger("right_click_block", (id, definition, trigger) -> {

            if (!matchesBlock(state, trigger.target)) {
                return false;
            }

            boolean started = start(player, null, id, definition, trigger);

            if (started && trigger.consume) {

                event.setCanceled(true);

                event.setCancellationResult(InteractionResult.SUCCESS);
            }

            return started;
        });
    }


    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        if (!(event.getSource().getEntity() instanceof ServerPlayer player)) {
            return;
        }

        Entity target = event.getEntity();

        forEachTrigger("hit_entity", (id, definition, trigger) -> {

            if (!matchesEntity(target, trigger.target)) {
                return false;
            }

            boolean started = start(player, target, id, definition, trigger);

            if (started && trigger.consume) {
                event.setCanceled(true);
            }

            return started;
        });
    }


    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (!(event.getSource().getEntity() instanceof ServerPlayer player)) {
            return;
        }

        Entity killed = event.getEntity();

        forEachTrigger("kill_entity", (id, definition, trigger) -> {

            if (!matchesEntity(killed, trigger.target)) {
                return false;
            }

            return start(player, null, id, definition, trigger);
        });
    }


    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player) || DialogueSessionManager.isActive(player)) {
            return;
        }

        checkTickTriggers(player);
    }


    private static void checkTickTriggers(ServerPlayer player) {
        for (Map.Entry<ResourceLocation, DialogueDefinition> entry : DialogueRegistry.entries().entrySet()) {

            ResourceLocation id = entry.getKey();

            DialogueDefinition definition = entry.getValue();

            if (definition.triggers == null) {
                continue;
            }

            for (DialogueDefinition.Trigger trigger : definition.triggers) {

                if (trigger == null || trigger.type == null) {
                    continue;
                }

                int interval = Math.max(1, trigger.check_interval);

                if (player.tickCount % interval != 0) {
                    continue;
                }

                String type = trigger.type.toLowerCase(Locale.ROOT);

                boolean started = switch (type) {

                    case "proximity_entity" -> checkNearbyEntity(player, id, definition, trigger, false);

                    case "shift_near_entity" ->
                            player.isShiftKeyDown() && checkNearbyEntity(player, id, definition, trigger, false);

                    case "look_at_entity" -> checkNearbyEntity(player, id, definition, trigger, true);

                    case "proximity_block" -> checkNearbyBlock(player, id, definition, trigger);

                    case "enter_area" -> checkArea(player, id, definition, trigger);

                    default -> false;
                };

                if (started) {
                    return;
                }
            }
        }
    }


    private static boolean checkNearbyEntity(ServerPlayer player, ResourceLocation id, DialogueDefinition definition, DialogueDefinition.Trigger trigger, boolean requireLook) {
        double radius = Math.max(0.5D, trigger.radius);

        AABB box = player.getBoundingBox().inflate(radius);

        for (Entity entity : player.serverLevel().getEntities(player, box, candidate -> candidate.isAlive() && matchesEntity(candidate, trigger.target))) {

            if (requireLook && !isLookingAt(player, entity, trigger.look_angle)) {
                continue;
            }

            if (start(player, entity, id, definition, trigger)) {
                return true;
            }
        }

        return false;
    }


    private static boolean checkNearbyBlock(ServerPlayer player, ResourceLocation id, DialogueDefinition definition, DialogueDefinition.Trigger trigger) {
        ServerLevel level = player.serverLevel();

        int radius = Mth.clamp((int) Math.ceil(trigger.radius), 1, 16);

        BlockPos center = player.blockPosition();

        double radiusSqr = trigger.radius * trigger.radius;

        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-radius, -radius, -radius), center.offset(radius, radius, radius))) {

            if (center.distSqr(pos) > radiusSqr) {
                continue;
            }

            if (!matchesBlock(level.getBlockState(pos), trigger.target)) {
                continue;
            }

            return start(player, null, id, definition, trigger);
        }

        return false;
    }


    private static boolean checkArea(ServerPlayer player, ResourceLocation id, DialogueDefinition definition, DialogueDefinition.Trigger trigger) {
        if (trigger.dimension != null && !player.level().dimension().location().toString().equals(trigger.dimension)) {
            return false;
        }

        Vec3 pos = player.position();

        boolean inside;

        if (trigger.min_x != null && trigger.min_y != null && trigger.min_z != null && trigger.max_x != null && trigger.max_y != null && trigger.max_z != null) {

            inside = pos.x >= trigger.min_x && pos.x <= trigger.max_x && pos.y >= trigger.min_y && pos.y <= trigger.max_y && pos.z >= trigger.min_z && pos.z <= trigger.max_z;

        } else if (trigger.x != null && trigger.y != null && trigger.z != null) {

            double dx = pos.x - trigger.x;

            double dy = pos.y - trigger.y;

            double dz = pos.z - trigger.z;

            inside = dx * dx + dy * dy + dz * dz <= trigger.radius * trigger.radius;

        } else {
            return false;
        }

        return inside && start(player, null, id, definition, trigger);
    }


    public static boolean fireExternal(ServerPlayer player, Entity source, String event) {
        for (Map.Entry<ResourceLocation, DialogueDefinition> entry : DialogueRegistry.entries().entrySet()) {

            DialogueDefinition definition = entry.getValue();

            if (definition.triggers == null) {
                continue;
            }

            for (DialogueDefinition.Trigger trigger : definition.triggers) {

                if (trigger == null || !"external".equalsIgnoreCase(trigger.type)) {
                    continue;
                }

                if (trigger.event == null || !trigger.event.equals(event)) {
                    continue;
                }

                if (source != null && !matchesEntity(source, trigger.target)) {
                    continue;
                }

                if (start(player, source, entry.getKey(), definition, trigger)) {
                    return true;
                }
            }
        }

        return false;
    }


    private static boolean start(ServerPlayer player, Entity source, ResourceLocation id, DialogueDefinition definition, DialogueDefinition.Trigger trigger) {
        if (DialogueSessionManager.isActive(player)) {
            return false;
        }

        String cooldownKey = player.getUUID() + "|" + id + "|" + (source != null ? source.getUUID() : "none");

        long now = player.serverLevel().getGameTime();

        long last = COOLDOWNS.getOrDefault(cooldownKey, Long.MIN_VALUE / 2);

        if (now - last < Math.max(0, trigger.cooldown_ticks)) {
            return false;
        }

        boolean started = DialogueSessionManager.start(player, source, id, trigger.once, null);

        if (started) {
            COOLDOWNS.put(cooldownKey, now);
        }

        return started;
    }


    private static boolean matchesEntity(Entity entity, String target) {
        if (target == null || target.isBlank() || target.equals("*")) {
            return true;
        }

        if (target.startsWith("#")) {

            ResourceLocation id = ResourceLocation.tryParse(target.substring(1));

            if (id == null) {
                return false;
            }

            TagKey<net.minecraft.world.entity.EntityType<?>> tag = TagKey.create(Registries.ENTITY_TYPE, id);

            return entity.getType().is(tag);
        }

        ResourceLocation id = ResourceLocation.tryParse(target);

        return id != null && id.equals(ForgeRegistries.ENTITY_TYPES.getKey(entity.getType()));
    }


    private static boolean matchesBlock(BlockState state, String target) {
        if (target == null || target.isBlank() || target.equals("*")) {
            return true;
        }

        if (target.startsWith("#")) {

            ResourceLocation id = ResourceLocation.tryParse(target.substring(1));

            if (id == null) {
                return false;
            }

            TagKey<Block> tag = TagKey.create(Registries.BLOCK, id);

            return state.is(tag);
        }

        ResourceLocation id = ResourceLocation.tryParse(target);

        return id != null && id.equals(ForgeRegistries.BLOCKS.getKey(state.getBlock()));
    }


    private static boolean isLookingAt(ServerPlayer player, Entity target, double angleDegrees) {
        if (!player.hasLineOfSight(target)) {
            return false;
        }

        Vec3 look = player.getLookAngle().normalize();

        Vec3 direction = target.getBoundingBox().getCenter().subtract(player.getEyePosition()).normalize();

        double minimumDot = Math.cos(Math.toRadians(Math.max(1.0D, angleDegrees)));

        return look.dot(direction) >= minimumDot;
    }


    private static void forEachTrigger(String type, TriggerVisitor visitor) {
        for (Map.Entry<ResourceLocation, DialogueDefinition> entry : DialogueRegistry.entries().entrySet()) {

            DialogueDefinition definition = entry.getValue();

            if (definition.triggers == null) {
                continue;
            }

            for (DialogueDefinition.Trigger trigger : definition.triggers) {

                if (trigger == null || trigger.type == null || !trigger.type.equalsIgnoreCase(type)) {
                    continue;
                }

                if (visitor.visit(entry.getKey(), definition, trigger)) {
                    return;
                }
            }
        }
    }


    @FunctionalInterface
    private interface TriggerVisitor {

        boolean visit(ResourceLocation id, DialogueDefinition definition, DialogueDefinition.Trigger trigger);
    }
}