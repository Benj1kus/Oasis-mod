package com.benji.oasiso.common.block;

import com.benji.oasiso.Oasiso;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.grower.AbstractTreeGrower;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;

import java.util.Optional;

public class DoumPalmSaplingBlock extends SaplingBlock {

    private static final ResourceLocation[] PALM_STRUCTURES = new ResourceLocation[]{
            ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "palm_little"),
            ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "palm_little_mid"),
            ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "palm_mid"),
            ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "palm_big"),
            ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "palm_very_big")
    };

    public DoumPalmSaplingBlock(AbstractTreeGrower treeGrower, Properties properties) {
        super(treeGrower, properties);
    }

    @Override
    protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
        if (state.is(BlockTags.SAND)) {
            return true;
        }
        return super.mayPlaceOn(state, level, pos);
    }

    @Override
    public void advanceTree(ServerLevel level, BlockPos pos, BlockState state, RandomSource random) {
        if (state.getValue(STAGE) == 0) {
            level.setBlock(pos, state.cycle(STAGE), 4);
        } else {
            ResourceLocation nbtLocation = PALM_STRUCTURES[random.nextInt(PALM_STRUCTURES.length)];
            StructureTemplateManager structureManager = level.getStructureManager();
            Optional<StructureTemplate> templateOptional = structureManager.get(nbtLocation);

            if (templateOptional.isPresent()) {
                StructureTemplate template = templateOptional.get();

                level.setBlock(pos, Blocks.AIR.defaultBlockState(), 4);

                StructurePlaceSettings placeSettings = new StructurePlaceSettings().setIgnoreEntities(true);

                BlockPos placePos = pos.offset(
                        -template.getSize().getX() / 2,
                        0,
                        -template.getSize().getZ() / 2
                );

                boolean placed = template.placeInWorld(level, placePos, placePos, placeSettings, random, 2);

                if (!placed) {
                    level.setBlock(pos, state, 4);
                }
            } else {
                super.advanceTree(level, pos, state, random);
            }
        }
    }
}