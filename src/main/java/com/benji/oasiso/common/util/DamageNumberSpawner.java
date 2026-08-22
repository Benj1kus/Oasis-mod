package com.benji.oasiso.common.util;

import com.benji.oasiso.Oasiso;
import com.benji.oasiso.common.entity.DamageNumberEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

public final class DamageNumberSpawner {

    private DamageNumberSpawner() {
    }


    public static void spawn(
            ServerLevel level,
            LivingEntity target,
            float damage
    ) {
        if (damage < 0.0F) {
            return;
        }

        //0-9
        int displayedDamage = Math.max(0, Math.round(damage));
        DamageNumberEntity number = Oasiso.DAMAGE_NUMBER.get().create(level);
        if (number == null) {
            return;
        }

        //direction
        Vec3 look = target.getLookAngle();
        Vec3 forward = new Vec3(look.x, 0.0D, look.z);
        if (forward.lengthSqr() < 0.0001D) {
            forward = new Vec3(0.0D, 0.0D, 1.0D);
        } else {
            forward = forward.normalize();
        }

        //vectr of nmbrs
        Vec3 right = new Vec3(forward.z, 0.0D, -forward.x);
        double side = level.random.nextBoolean() ? 1.0D : -1.0D;
        double sideDistance = target.getBbWidth() * 0.5D + 0.75D + level.random.nextDouble() * 0.65D;

        //randomness
        double forwardJitter = (level.random.nextDouble() - 0.5D) * 1.4D;
        double height = target.getBbHeight() * (0.30D + level.random.nextDouble() * 0.45D);

        double x = target.getX() + right.x * sideDistance * side + forward.x * forwardJitter;
        double y = target.getY() + height;
        double z = target.getZ() + right.z * sideDistance * side + forward.z * forwardJitter;

        number.moveTo(x, y, z, 0.0F, 0.0F);

        number.setDamageValue(displayedDamage);

        level.addFreshEntity(number);
    }
}