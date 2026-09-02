package com.benji.oasiso.common.item;

import com.benji.oasiso.ModSounds;
import com.benji.oasiso.common.entity.ChaosPortalEntity;
import com.benji.oasiso.registry.ModEntities;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class ChaosScissorsItem extends Item {

    private static final double PORTAL_DISTANCE = 3.20D;
    private static final double HEIGHT_ABOVE_GROUND = 1.0D;
    private static final int COOLDOWN_TICKS = 20 * 30;

    public ChaosScissorsItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (level.isClientSide) {
            return InteractionResultHolder.success(stack);
        }

        if (!(level instanceof ServerLevel serverLevel)) {
            return InteractionResultHolder.pass(stack);
        }

        if (player.getCooldowns().isOnCooldown(this)) {
            return InteractionResultHolder.fail(stack);
        }

        if (ChaosPortalEntity.hasOwnedPortal(serverLevel.getServer(), player.getUUID())) {
            return InteractionResultHolder.fail(stack);
        }

        Vec3 look = player.getLookAngle();
        Vec3 forward = new Vec3(look.x, 0.0D, look.z);

        if (forward.lengthSqr() < 0.0001D) {
            float yaw = player.getYRot() * Mth.DEG_TO_RAD;
            forward = new Vec3(-Mth.sin(yaw), 0.0D, Mth.cos(yaw));
        }

        forward = forward.normalize();

        Vec3 horizontalTarget = new Vec3(player.getX(), player.getY(), player.getZ()).add(forward.scale(PORTAL_DISTANCE));

        double groundY = findGroundY(serverLevel, player, horizontalTarget);
        double portalY = groundY + HEIGHT_ABOVE_GROUND;

        double toPlayerX = player.getX() - horizontalTarget.x;
        double toPlayerZ = player.getZ() - horizontalTarget.z;

        float portalYaw = (float) Math.toDegrees(Math.atan2(toPlayerX, toPlayerZ));

        ChaosPortalEntity portal = ModEntities.CHAOS_PORTAL_ENTITY.get().create(serverLevel);

        if (portal == null) {
            return InteractionResultHolder.fail(stack);
        }
        portal.moveTo(horizontalTarget.x, portalY, horizontalTarget.z, portalYaw, 0.0F);

        float shaderSeed = RandomSource.create().nextFloat() * 1000.0F;
        portal.initializePortal(player, portalYaw, shaderSeed);
        serverLevel.addFreshEntity(portal);

        serverLevel.playSound(null,
                portal.getX(), portal.getY() + 1.0D, portal.getZ(),
                ModSounds.PORTAL_OPEN.get(),
                SoundSource.PLAYERS,
                1.25F, 1.0F);

        player.getCooldowns().addCooldown(this, COOLDOWN_TICKS);
        return InteractionResultHolder.consume(stack);
    }


    private double findGroundY(ServerLevel level, Player player, Vec3 target) {
        Vec3 start = new Vec3(target.x, player.getY() + 4.0D, target.z);
        Vec3 end = new Vec3(target.x, Math.max(level.getMinBuildHeight(), player.getY() - 16.0D), target.z);
        BlockHitResult hit = level.clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        if (hit.getType() == HitResult.Type.BLOCK) {
            return hit.getLocation().y;
        }
        return player.getY() - 1.0D;
    }
}