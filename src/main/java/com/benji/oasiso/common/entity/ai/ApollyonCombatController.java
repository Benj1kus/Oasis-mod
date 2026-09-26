package com.benji.oasiso.common.entity.ai;

import com.benji.oasiso.common.entity.ApollyonEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public final class ApollyonCombatController {
    public static final int COOLDOWN_MIN = 60;
    public static final int COOLDOWN_RANDOM = 40;
    private final ApollyonEntity mob;
    private final ApollyonSpearAttack spear;
    private final ApollyonCommonAttack common;
    private final ApollyonShockwaveAttack shock;
    private final ApollyonDrillAttack drill;
    private final ApollyonSpearRainAttack rain;
    private int active;
    private int cooldown = 40;

    public ApollyonCombatController(ApollyonEntity mob) {
        this.mob = mob;
        spear = new ApollyonSpearAttack(mob);
        common = new ApollyonCommonAttack(mob);
        shock = new ApollyonShockwaveAttack(mob);
        drill = new ApollyonDrillAttack(mob);
        rain = new ApollyonSpearRainAttack(mob);
    }

    public boolean tick() {
        if (!(mob.level() instanceof ServerLevel)) return false;
        if (!mob.isAlive() || mob.isRemoved()) {
            cancel();
            return false;
        }
        if (active != 0) {
            boolean running;
            switch (active) {
                case 1 -> {
                    spear.tick(false);
                    running = spear.isActive();
                }
                case 2 -> {
                    common.tick();
                    running = common.isActive();
                }
                case 3 -> {
                    shock.tick();
                    running = shock.isActive();
                }
                case 4 -> {
                    drill.tick();
                    running = drill.isActive();
                }
                default -> {
                    rain.tick();
                    running = rain.isActive();
                }
            }
            if (!running) {
                active = 0;
                cooldown = COOLDOWN_MIN + mob.getRandom().nextInt(COOLDOWN_RANDOM + 1);
            }
            return true;
        }
        if (cooldown > 0) {
            cooldown--;
            return false;
        }
        if (mob.isTeleporting() || mob.isAttackControllingMovement() || mob.isPassenger() || mob.isVehicle())
            return false;
        if (!(mob.getTarget() instanceof Player player) || !valid(mob, player) || !mob.hasLineOfSight(player) || horizontal(mob.position(), player.position()) > 100 || Math.abs(mob.getY() - player.getY()) > 12)
            return false;

        int chosen = mob.getRandom().nextInt(5);
        if (chosen == 0) {
            if (spear.tick(true) && spear.isActive()) active = 1;
        } else if (chosen == 1) {
            if (common.start(player)) {
                active = 2;
                common.tick();
            }
        } else if (chosen == 2) {
            if (shock.start(player)) {
                active = 3;
                shock.tick();
            }
        } else if (chosen == 3) {
            if (drill.start(player)) {
                active = 4;
                drill.tick();
            }
        } else if (rain.start(player)) {
            active = 5;
            rain.tick();
        }
        if (active == 0) cooldown = 20;
        return active != 0;
    }

    public void cancel() {
        spear.cancel();
        common.cancel();
        shock.cancel();
        drill.cancel();
        rain.cancel();
        ApollyonRestraint.release(mob);
        active = 0;
        mob.setCombatMode(0);
        mob.setPushedPlayerId(-1);
        mob.setAttackControlsMovement(false);
        mob.setDeltaMovement(Vec3.ZERO);
    }

    static boolean valid(ApollyonEntity mob, Player p) {
        return p != null && p.isAlive() && !p.isCreative() && !p.isSpectator() && !p.isPassenger() && p.level() == mob.level() && (!(p instanceof ServerPlayer server) || !server.hasDisconnected());
    }

    static Vec3 limit(Vec3 v, double max) {
        return v.lengthSqr() > max * max ? v.normalize().scale(max) : v;
    }

    static Vec3 flatDirection(Vec3 from, Vec3 to) {
        Vec3 v = new Vec3(to.x - from.x, 0, to.z - from.z);
        return v.lengthSqr() < 1E-8 ? new Vec3(0, 0, 1) : v.normalize();
    }

    private static double horizontal(Vec3 a, Vec3 b) {
        double x = a.x - b.x, z = a.z - b.z;
        return x * x + z * z;
    }
}
