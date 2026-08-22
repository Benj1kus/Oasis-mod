package com.benji.oasiso.common.dimension;

import com.benji.oasiso.Oasiso;
import com.benji.oasiso.common.entity.GasterEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public final class ScarletTreeEncounter {
//structure
    private static final ResourceLocation SCARLET_TREE = ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "scarlet_tree");

    private static final int BASE_X = 10_000_000;
    private static final int STRUCTURE_Y = 80;
    private static final int CELL_SPACING = 256;

    private ScarletTreeEncounter() {
    }

    public static boolean hasFlowery(ServerPlayer player) {
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (stack.is(Oasiso.FLOWERY_ITEM.get())) {
                return true;
            }
        }

        return false;
    }

    @Nullable
    public static Vec3 prepareEncounter(ServerLevel level, ServerPlayer player) {
        StructureTemplate template = level.getStructureManager().get(SCARLET_TREE).orElse(null);

        if (template == null) {
            return null;
        }

        BlockPos origin = getPersonalOrigin(player);

        Vec3i size = template.getSize();

        removeOldGasters(level, origin, size);

        StructurePlaceSettings settings = new StructurePlaceSettings().setMirror(Mirror.NONE).setRotation(Rotation.NONE).setIgnoreEntities(false);

        boolean placed = template.placeInWorld(level, origin, origin, settings, level.random, 3);

        if (!placed) {
            return null;
        }

        BlockPos spawnMarker = findSpawnMarker(level, origin, size);

        if (spawnMarker == null) {
            return null;
        }
        return new Vec3(spawnMarker.getX() + 0.5D, spawnMarker.getY() + 1.05D, spawnMarker.getZ() + 0.5D);
    }

    private static BlockPos getPersonalOrigin(ServerPlayer player) {
        UUID uuid = player.getUUID();

        int cellX = (short) (uuid.getMostSignificantBits() & 0xFFFFL);
        int cellZ = (short) (uuid.getLeastSignificantBits() & 0xFFFFL);

        return new BlockPos(BASE_X + cellX * CELL_SPACING, STRUCTURE_Y, cellZ * CELL_SPACING);
    }

    @Nullable
    private static BlockPos findSpawnMarker(ServerLevel level, BlockPos origin, Vec3i size) {
        BlockPos max = origin.offset(size.getX() - 1, size.getY() - 1, size.getZ() - 1);

        for (BlockPos pos : BlockPos.betweenClosed(origin, max)) {
            if (level.getBlockState(pos).is(Oasiso.NEPHRITIS_SPIRAL.get())) {
                return pos.immutable();
            }
        }
        return null;
    }

    private static void removeOldGasters(ServerLevel level, BlockPos origin, Vec3i size) {
        AABB area = new AABB(origin.getX(), origin.getY(), origin.getZ(), origin.getX() + size.getX(), origin.getY() + size.getY(), origin.getZ() + size.getZ()).inflate(3.0D);
        for (GasterEntity gaster : level.getEntitiesOfClass(GasterEntity.class, area, GasterEntity::isAlive)) {

            gaster.discard();
        }
    }
}