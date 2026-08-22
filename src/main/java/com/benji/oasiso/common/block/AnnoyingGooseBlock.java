package com.benji.oasiso.common.block;

import com.benji.oasiso.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;


public class AnnoyingGooseBlock extends GenericDecorateBlock {

    private static final String[] FACTS = {

            "Breaking a block makes the block disappear.",
            "Creepers can explode.",
            "Torches produce light.",
            "Chests can store items.",
            "Sand falls when there is nothing below it.",
            "Water can be found in oceans.",
            "Zombies are hostile.",
            "You can jump by pressing the jump button.",
            "Doors can be opened.",
            "Beds are useful when you want to sleep."};


    public AnnoyingGooseBlock(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide) {

            level.playSound(null, pos, ModSounds.HONK.get(), SoundSource.BLOCKS, 1.0F, 0.95F + level.random.nextFloat() * 0.10F);

            String fact = FACTS[level.random.nextInt(FACTS.length)];

            Component message = Component.literal("<").append(Component.literal("Annoying Goose").withStyle(ChatFormatting.YELLOW)).append(Component.literal("> ").withStyle(ChatFormatting.WHITE)).append(Component.literal(fact).withStyle(ChatFormatting.WHITE));
            player.sendSystemMessage(message);
        }


        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}