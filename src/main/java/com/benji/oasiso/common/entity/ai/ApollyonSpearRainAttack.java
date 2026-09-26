package com.benji.oasiso.common.entity.ai;

import com.benji.oasiso.Oasiso;
import com.benji.oasiso.common.entity.ApollyonEntity;
import com.benji.oasiso.common.entity.SpearAttackEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

public final class ApollyonSpearRainAttack {

    public static final ResourceLocation SPEAR_ID = ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "spear_attack");
    public static final int WARNING_TICKS = 60, WAVE_INTERVAL = 40, WAVES = 3;
    public static final double SPAWN_HEIGHT = 8, SPACING = 1.25;
    private final ApollyonEntity mob;
    private final List<SpearAttackEntity> spears = new ArrayList<>();
    private Player victim;
    private boolean active, marked;
    private int age, waves, lastWave;
    private static boolean reportedMissing;

    public ApollyonSpearRainAttack(ApollyonEntity mob) {
        this.mob = mob;
    }

    public boolean isActive() {
        return active;
    }

    public boolean start(Player target) {
        if (active || !ApollyonCombatController.valid(mob, target)) return false;
        if (!BuiltInRegistries.ENTITY_TYPE.containsKey(SPEAR_ID)) {
            reportMissing();
            return false;
        }
        active = true;
        marked = false;
        age = waves = lastWave = 0;
        victim = target;
        spears.clear();
        mob.setSpearTarget(-1);
        mob.setAttackControlsMovement(true);
        mob.setCombatMode(6);
        mob.getNavigation().stop();
        mob.setDeltaMovement(Vec3.ZERO);
        return true;
    }

    public void tick() {
        if (!active) return;
        if (!ApollyonCombatController.valid(mob, victim) || victim.distanceToSqr(mob) > 48 * 48) {
            cancel();
            return;
        }
        mob.setTarget(victim);
        mob.updateHover(.30, .12, .18);
        if (age >= ApollyonAttackTimeline.SUMMON_LENGTH) mob.setCombatMode(7);
        if (!marked && age >= ApollyonAttackTimeline.UP_SPEAR) {
            marked = true;
            mob.setSpearTarget(victim.getId());
        }
        int first = ApollyonAttackTimeline.UP_SPEAR + WARNING_TICKS;
        if (waves < WAVES && age >= first + waves * WAVE_INTERVAL) {
            if (!spawnWave()) {
                cancel();
                return;
            }
            waves++;
            lastWave = age;
        }
        spears.removeIf(SpearAttackEntity::isRemoved);
        if (waves >= WAVES && ((age > lastWave && spears.isEmpty()) || age - lastWave > 160)) {
            cancel();
            return;
        }
        age++;
    }

    private boolean spawnWave() {
        if (!(mob.level() instanceof ServerLevel level)) return false;
        Vec3 target = victim.position();
        float damage = (float) mob.getAttributeValue(Attributes.ATTACK_DAMAGE);
        var type = BuiltInRegistries.ENTITY_TYPE.get(SPEAR_ID);
        for (int row = -1; row <= 1; row++)
            for (int col = -1; col <= 1; col++) {
                double x = target.x + col * SPACING, z = target.z + row * SPACING;
                double y = Math.min(level.getMaxBuildHeight() - 3, target.y + SPAWN_HEIGHT);
                if (!level.hasChunkAt(BlockPos.containing(x, y, z))) continue;
                var entity = type.create(level);
                if (!(entity instanceof SpearAttackEntity spear)) {
                    if (entity != null) entity.discard();
                    reportMissing();
                    return false;
                }
                spear.moveTo(x, y, z, 0, 0);
                spear.prepare(mob, damage);
                if (!level.addFreshEntity(spear)) {
                    spear.discard();
                    continue;
                }
                spears.add(spear);
                level.sendParticles(Oasiso.ENTROPY_LIGHTNING.get(), x, y + .8, z, 2, .3, .65, .3, 0);
            }
        return true;
    }

    private static void reportMissing() {
        if (reportedMissing) return;
        reportedMissing = true;
        com.mojang.logging.LogUtils.getLogger().error("[Apollyon] {} must be registered as SpearAttackEntity; check SPEAR_ID", SPEAR_ID);
    }

    public void cancel() {
        if (!active) return;
        for (SpearAttackEntity spear : spears) if (!spear.isRemoved()) spear.discard();
        spears.clear();
        active = false;
        victim = null;
        mob.setSpearTarget(-1);
        mob.setCombatMode(0);
        mob.setAttackControlsMovement(false);
        mob.setDeltaMovement(Vec3.ZERO);
    }
}
