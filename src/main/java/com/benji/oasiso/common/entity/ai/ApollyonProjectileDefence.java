package com.benji.oasiso.common.entity.ai;

import com.benji.oasiso.Oasiso;
import com.benji.oasiso.common.entity.ApollyonEntity;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.network.protocol.game.ClientboundTeleportEntityPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.projectile.AbstractHurtingProjectile;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.ProjectileImpactEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Oasiso.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ApollyonProjectileDefence {
    private static final String CHECK_BOSS = "OasisoDefenceBoss";
    private static final String CHECK_TIME = "OasisoDefenceCheck";
    private static final String REFLECTED = "OasisoDefenceReflected";

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onImpact(ProjectileImpactEvent event) {
        if (event.getImpactResult() != ProjectileImpactEvent.ImpactResult.DEFAULT) return;
        if (event.getRayTraceResult() instanceof EntityHitResult hit
                && hit.getEntity() instanceof ApollyonEntity boss
                && tryDeflect(boss, event.getProjectile())) {

            event.setImpactResult(ProjectileImpactEvent.ImpactResult.SKIP_ENTITY);
        }
    }

    public static boolean tryDeflect(ApollyonEntity boss, Projectile projectile) {
        if (!(boss.level() instanceof ServerLevel level) || !boss.isAlive() || boss.isRemoved()
                || projectile.isRemoved()) return false;
        long now = level.getGameTime();
        var data = projectile.getPersistentData();
        boolean sameBoss = data.hasUUID(CHECK_BOSS) && data.getUUID(CHECK_BOSS).equals(boss.getUUID());

        if (sameBoss && data.getLong(CHECK_TIME) == now) return data.getBoolean(REFLECTED);
        data.putUUID(CHECK_BOSS, boss.getUUID());
        data.putLong(CHECK_TIME, now);
        data.putBoolean(REFLECTED, false);
        if (!boss.tryBeginDefence()) return false;
        data.putBoolean(REFLECTED, true);
        if (projectile.isRemoved()) return true;

        Vec3 incoming = projectile.getDeltaMovement();
        Vec3 center = boss.getBoundingBox().getCenter();
        Vec3 outward = projectile.position().subtract(center);
        Vec3 direction = incoming.lengthSqr() > 1E-8 ? incoming.normalize().scale(-1) : outward.normalize();
        if (direction.lengthSqr() < 1E-8) direction = boss.getLookAngle();
        double speed = Mth.clamp(incoming.length(), .65, 4.0);
        double radius = Math.sqrt(2 * boss.getBbWidth() * boss.getBbWidth()
                + boss.getBbHeight() * boss.getBbHeight()) * .5 + projectile.getBbWidth() + .3;
        Vec3 start = center.add(direction.scale(radius));
        projectile.setPos(start.x, start.y, start.z);
        projectile.setOwner(boss);
        projectile.setDeltaMovement(direction.scale(speed));

        if (projectile instanceof AbstractHurtingProjectile fireball) {
            double power = Math.sqrt(fireball.xPower * fireball.xPower
                    + fireball.yPower * fireball.yPower + fireball.zPower * fireball.zPower);
            fireball.xPower = direction.x * power;
            fireball.yPower = direction.y * power;
            fireball.zPower = direction.z * power;
        }
        projectile.setYRot((float)(Mth.atan2(direction.x, direction.z) * 180 / Math.PI));
        projectile.setXRot((float)(Mth.atan2(direction.y, direction.horizontalDistance()) * 180 / Math.PI));
        projectile.yRotO = projectile.getYRot();
        projectile.xRotO = projectile.getXRot();
        projectile.hasImpulse = true;
        level.getChunkSource().broadcastAndSend(projectile, new ClientboundTeleportEntityPacket(projectile));
        level.getChunkSource().broadcastAndSend(projectile, new ClientboundSetEntityMotionPacket(projectile));
        boss.playSound(SoundEvents.ITEM_BREAK, .9F, .9F + boss.getRandom().nextFloat() * .2F);
        return true;
    }

    private ApollyonProjectileDefence() {}
}
