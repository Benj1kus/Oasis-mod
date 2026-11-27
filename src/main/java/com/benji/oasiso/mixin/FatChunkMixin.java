package com.benji.oasiso.mixin;

import com.benji.oasiso.common.cowrit.FatCoatings;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LevelChunk.class)
abstract class FatChunkMixin {
    @Inject(method = "setBlockState", at = @At("RETURN"))
    private void oasiso$clearReplaced(BlockPos pos, BlockState next, boolean moved, CallbackInfoReturnable<BlockState> cir) {
        var old = cir.getReturnValue();
        if (old == null || old.is(next.getBlock())) return;
        if (((LevelChunk) (Object) this).getLevel() instanceof ServerLevel level && FatCoatings.has(level, pos)) {
            FatCoatings.change(level, pos, false);
        }
    }
}
