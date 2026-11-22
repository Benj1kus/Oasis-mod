package com.benji.oasiso.common.block;

import com.benji.oasiso.Oasiso;
import com.benji.oasiso.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlockIgnoreProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class FeralSaplingBlock extends ToothFlowerBlock implements BonemealableBlock {
    public FeralSaplingBlock(Properties properties) {
        super(MobEffects.SATURATION, 1, properties);
    }

    @Override
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (level.getMaxLocalRawBrightness(pos.above()) >= 9 && random.nextInt(14) == 0) {
            grow(level, pos, random);
        }
    }

    @Override
    public boolean isValidBonemealTarget(LevelReader level, BlockPos pos, BlockState state, boolean client) {
        return true;
    }

    @Override
    public boolean isBonemealSuccess(Level level, RandomSource random, BlockPos pos, BlockState state) {
        return true;
    }

    @Override
    public void performBonemeal(ServerLevel level, RandomSource random, BlockPos pos, BlockState state) {
        grow(level, pos, random);
    }

    private void grow(ServerLevel level, BlockPos sapling, RandomSource random) {
        var id = ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "feral_tree" + (1 + random.nextInt(3)));
        var result = level.getStructureManager().get(id);
        if (result.isEmpty()) return;
        StructureTemplate tree = result.get();
        var size = tree.getSize();
        var pivot = new BlockPos(size.getX() / 2, 0, size.getZ() / 2);
        var origin = sapling.subtract(pivot);
        var settings = new StructurePlaceSettings().setIgnoreEntities(true).setRotation(Rotation.getRandom(random)).setRotationPivot(pivot).addProcessor(BlockIgnoreProcessor.STRUCTURE_AND_AIR);

        CompoundTag data = tree.save(new CompoundTag());
        ListTag palette = data.getList("palette", Tag.TAG_COMPOUND);
        if (palette.isEmpty()) return;
        Set<BlockPos> occupied = new HashSet<>();
        Set<BlockPos> logs = new HashSet<>();
        for (Tag entry : data.getList("blocks", Tag.TAG_COMPOUND)) {
            CompoundTag block = (CompoundTag) entry;
            String name = palette.getCompound(block.getInt("state")).getString("Name");
            if (name.equals("minecraft:air") || name.equals("minecraft:structure_block")) continue;
            ListTag xyz = block.getList("pos", Tag.TAG_INT);
            var local = new BlockPos(xyz.getInt(0), xyz.getInt(1), xyz.getInt(2));
            var target = origin.offset(StructureTemplate.calculateRelativePosition(settings, local));
            if (level.isOutsideBuildHeight(target) || !level.hasChunkAt(target) || !level.getWorldBorder().isWithinBounds(target))
                return;
            BlockState existing = level.getBlockState(target);
            if (!target.equals(sapling) && (!existing.canBeReplaced() || existing.hasBlockEntity())) return;
            occupied.add(target);
            if (name.equals("oasiso:tooth_log")) logs.add(target);
        }
        if (logs.isEmpty()) return;

        var extensions = new ArrayList<BlockPos>();
        int bottom = logs.stream().mapToInt(BlockPos::getY).min().orElseThrow();
        for (BlockPos foot : logs) {
            if (foot.getY() != bottom || !logs.contains(foot.above())) continue;
            BlockPos end = foot.below();
            while (!occupied.contains(end)) {
                if (level.isOutsideBuildHeight(end)) return;
                BlockState below = level.getBlockState(end);
                if (!below.isAir() && !(below.getBlock() instanceof LiquidBlock)) break;
                extensions.add(end);
                end = end.below();
            }
        }
        if (!tree.placeInWorld(level, origin, origin, settings, random, Block.UPDATE_CLIENTS)) return;
        if (level.getBlockState(sapling).is(this)) level.removeBlock(sapling, false);
        BlockState root = ModBlocks.TOOTH_LOG.get().defaultBlockState();
        for (BlockPos pos : extensions) level.setBlock(pos, root, Block.UPDATE_ALL);
    }
}
