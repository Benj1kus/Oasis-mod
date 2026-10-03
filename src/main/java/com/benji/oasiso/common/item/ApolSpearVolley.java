package com.benji.oasiso.common.item;

import com.benji.oasiso.ModSounds;
import com.benji.oasiso.Oasiso;
import com.benji.oasiso.common.entity.SpearAttackEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.*;

import java.util.*;

public final class ApolSpearVolley {
    public static final int COUNT = 6, COOLDOWN = 200;
    public static final double RANGE = 15;
    public static final float DAMAGE = 15;

    private record Target(Mob mob, Vec3 ground) {
    }

    public static boolean cast(ServerLevel level, Player player, ItemStack stack) {
        if (!player.isAlive() || player.isSpectator() || player.getCooldowns().isOnCooldown(stack.getItem()))
            return false;
        List<Target> targets = new ArrayList<>();
        var mobs = level.getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(RANGE), m -> m.isAlive() && !m.isInvulnerable() && m.distanceToSqr(player) <= RANGE * RANGE && !m.isAlliedTo(player) && !player.isAlliedTo(m));
        mobs.sort(Comparator.comparingDouble(m -> m.distanceToSqr(player)));
        for (Mob mob : mobs) {
            Vec3 ground = ground(level, mob.position(), player);
            if (ground != null) targets.add(new Target(mob, ground));
            if (targets.size() == COUNT) break;
        }
        if (targets.isEmpty()) return false;
        Vec3 look = player.getLookAngle();
        Vec3 forward = new Vec3(look.x, 0, look.z);
        if (forward.lengthSqr() < 1e-8) {
            double yaw = Math.toRadians(player.getYRot());
            forward = new Vec3(-Math.sin(yaw), 0, Math.cos(yaw));
        }
        forward = forward.normalize();
        Vec3 right = new Vec3(-forward.z, 0, forward.x);
        var type = BuiltInRegistries.ENTITY_TYPE.get(ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "spear_attack"));
        List<SpearAttackEntity> volley = new ArrayList<>();
        for (int i = 0; i < COUNT; i++) {
            int row = i % 3;
            double side = i < 3 ? -1 : 1;
            Vec3 pos = player.position().add(0, player.getEyeHeight() + 1.5 + row * .20, 0).add(forward.scale(-1.05 - row * .14)).add(right.scale(side * (.8 + row * .65)));
            if (pos.y > level.getMaxBuildHeight() - 1 || !level.hasChunkAt(BlockPos.containing(pos)) || !level.getWorldBorder().isWithinBounds(BlockPos.containing(pos)) || !level.noCollision(new AABB(pos.x - .2, pos.y - .2, pos.z - .2, pos.x + .2, pos.y + .2, pos.z + .2))) {
                volley.forEach(SpearAttackEntity::discard);
                return false;
            }
            var created = type.create(level);
            if (!(created instanceof SpearAttackEntity spear)) {
                if (created != null) created.discard();
                volley.forEach(SpearAttackEntity::discard);
                return false;
            }
            Target target = targets.get(i < targets.size() ? i : level.random.nextInt(targets.size()));
            spear.moveTo(pos.x, pos.y, pos.z, 0, 90);
            spear.prepareArtillery(player, target.mob().getUUID(), target.ground(), i);
            volley.add(spear);
        }
        for (SpearAttackEntity spear : volley) {
            if (!level.addFreshEntity(spear)) {
                volley.forEach(SpearAttackEntity::discard);
                return false;
            }
        }
        for (int i = 0; i < volley.size(); i++) {
            SpearAttackEntity spear = volley.get(i);
            float pitch = 0.85F + (i * 0.07F);
            level.playSound(null, spear.getX(), spear.getY(), spear.getZ(), ModSounds.BOMB_SPAWN.get(), SoundSource.PLAYERS, 0.8F, pitch);
        }
        player.getCooldowns().addCooldown(stack.getItem(), COOLDOWN);
        for (SpearAttackEntity spear : volley)
            level.sendParticles(Oasiso.ENTROPY_LIGHTNING.get(), spear.getX(), spear.getY() - .7, spear.getZ(), 3, .3, .8, .3, 0);
        return true;
    }

    public static Vec3 ground(ServerLevel level, Vec3 feet, net.minecraft.world.entity.Entity context) {
        BlockPos pos = BlockPos.containing(feet);
        if (!level.hasChunkAt(pos) || !level.getWorldBorder().isWithinBounds(pos)) return null;
        Vec3 start = feet.add(0, .6, 0), end = feet.add(0, -12, 0);
        var hit = level.clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, context));
        return hit.getType() == HitResult.Type.BLOCK ? hit.getLocation() : null;
    }

    private ApolSpearVolley() {
    }
}
