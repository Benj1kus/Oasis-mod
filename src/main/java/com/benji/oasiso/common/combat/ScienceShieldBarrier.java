package com.benji.oasiso.common.combat;

import com.benji.oasiso.Oasiso;
import com.benji.oasiso.common.entity.CrusaderScienceEntity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Oasiso.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ScienceShieldBarrier {
    @SubscribeEvent
    public static void tick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player) || !player.isAlive() || player.isSpectator() || player.isCreative() || player.isPassenger())
            return;
        for (CrusaderScienceEntity scientist : player.level().getEntitiesOfClass(CrusaderScienceEntity.class, player.getBoundingBox().inflate(8), CrusaderScienceEntity::isShieldActive)) {
            Vec3 c = scientist.shieldCenter(), r = scientist.shieldRadii();
            var box = player.getBoundingBox();
            double closestY = Mth.clamp(c.y, box.minY, box.maxY);
            double y = (closestY - c.y) / r.y;
            if (Math.abs(y) >= 1) continue;
            double slice = Math.sqrt(1 - y * y);
            double rx = r.x * slice + player.getBbWidth() * .5 + .10;
            double rz = r.z * slice + player.getBbWidth() * .5 + .10;
            double dx = player.getX() - c.x, dz = player.getZ() - c.z;
            double distance = Math.sqrt(dx * dx / (rx * rx) + dz * dz / (rz * rz));
            if (distance >= 1) continue;
            Vec3 normal = new Vec3(dx / (rx * rx), 0, dz / (rz * rz));
            if (normal.lengthSqr() < 1.0E-8) {
                normal = new Vec3(player.xo - c.x, 0, player.zo - c.z);
                if (normal.lengthSqr() < 1.0E-8) normal = new Vec3(1, 0, 0);
            }
            normal = normal.normalize();
            Vec3 old = player.getDeltaMovement();
            double speed = .32 + .30 * (1 - distance);
            double add = Math.max(0, speed - old.dot(normal));
            if (add > 0) {
                player.setDeltaMovement(old.add(normal.scale(add)));
                player.hasImpulse = true;
                player.hurtMarked = true;
            }
            if (player.tickCount % 8 == 0) {
                Vec3 direction = new Vec3(normal.x, y, normal.z).normalize();
                scientist.shieldHit(ScienceShieldGeometry.surface(direction, c, r));
            }
        }
    }

    private ScienceShieldBarrier() {
    }
}
