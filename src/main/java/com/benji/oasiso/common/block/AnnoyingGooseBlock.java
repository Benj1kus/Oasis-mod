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

    private static final String[] FACT_KEYS = {
            "message.oasiso.annoying_goose.fact_1",
            "message.oasiso.annoying_goose.fact_2",
            "message.oasiso.annoying_goose.fact_3",
            "message.oasiso.annoying_goose.fact_4",
            "message.oasiso.annoying_goose.fact_5",
            "message.oasiso.annoying_goose.fact_6",
            "message.oasiso.annoying_goose.fact_7",
            "message.oasiso.annoying_goose.fact_8",
            "message.oasiso.annoying_goose.fact_9",
            "message.oasiso.annoying_goose.fact_10"
    };

    public AnnoyingGooseBlock(Properties properties) {
        super(properties);
    }


    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide) {

            level.playSound(null, pos, ModSounds.HONK.get(), SoundSource.BLOCKS, 1.0F, 0.95F + level.random.nextFloat() * 0.10F);

            String factKey = FACT_KEYS[level.random.nextInt(FACT_KEYS.length)];

            Component message = Component.literal("<")
                    .append(Component.translatable("message.oasiso.annoying_goose.name").withStyle(ChatFormatting.YELLOW))
                    .append(Component.literal("> ").withStyle(ChatFormatting.WHITE))
                    .append(Component.translatable(factKey).withStyle(ChatFormatting.WHITE));

            player.sendSystemMessage(message);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}