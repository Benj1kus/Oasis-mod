package com.benji.oasiso.common.item;

import com.benji.oasiso.client.renderer.EntropyChestplateGloveRenderer;
import com.benji.oasiso.common.entity.EntropyPhysicsBlockEntity;
import com.benji.oasiso.common.world.MeltedNephritisSavedData;
import com.benji.oasiso.registry.ModEntities;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.TorchBlock;
import net.minecraft.world.level.block.WallTorchBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

public class EntropyChestplateGloveItem extends Item implements GeoItem {

    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle");
    public static final float MAX_PICKUP_HARDNESS = 100.0F;

    private static final String TAG_HELD_BLOCK = "EntropyGravityHeldBlock";
    private static final String TAG_FILL_MODE = "EntropyGravityFillMode";

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public EntropyChestplateGloveItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            private EntropyChestplateGloveRenderer renderer;

            @Override
            public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (this.renderer == null) {
                    this.renderer = new EntropyChestplateGloveRenderer();
                }
                return this.renderer;
            }
        });
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();

        if (player == null) {
            return InteractionResult.PASS;
        }

        ItemStack glove = context.getItemInHand();

        if (isFillMode(glove)) {
            return level.isClientSide
                    ? InteractionResult.SUCCESS
                    : InteractionResult.CONSUME;
        }

        if (level.isClientSide) {
            BlockState clientState = level.getBlockState(context.getClickedPos());
            return hasHeldBlock(glove) || canLiftBlock(level, context.getClickedPos(), clientState) ? InteractionResult.SUCCESS : InteractionResult.PASS;
        }

        if (!(level instanceof ServerLevel serverLevel) || !(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.PASS;
        }

        EntropyPhysicsBlockEntity held = resolveHeldBlock(serverLevel, glove);
        if (held != null) {
            if (held.isHeldBy(serverPlayer)) {
                held.releaseHolder(serverPlayer);
            }

            clearHeldBlock(glove);
            syncInventory(serverPlayer);
            return InteractionResult.CONSUME;
        }

        if (hasHeldBlock(glove)) {
            clearHeldBlock(glove);
        }

        BlockPos pos = context.getClickedPos();
        BlockState state = serverLevel.getBlockState(pos);

        if (!canLiftBlock(serverLevel, pos, state)) {
            return InteractionResult.PASS;
        }

        float hardness = Math.max(0.0F, state.getDestroySpeed(serverLevel, pos));
        BlockEntity sourceBlockEntity = serverLevel.getBlockEntity(pos);
        CompoundTag blockEntityData = sourceBlockEntity == null ? null : sourceBlockEntity.saveWithFullMetadata();

        MeltedNephritisSavedData nephritisData = MeltedNephritisSavedData.get(serverLevel);
        boolean nephritisCoated = nephritisData.isCoated(pos);

        EntropyPhysicsBlockEntity physicsBlock = new EntropyPhysicsBlockEntity(ModEntities.ENTROPY_PHYSICS_BLOCK.get(), serverLevel);

        physicsBlock.initializeFromBlock(state, hardness, blockEntityData, serverPlayer, context.getHand(), pos, nephritisCoated);
        if (sourceBlockEntity != null) {
            serverLevel.removeBlockEntity(pos);
        }

        boolean removed = serverLevel.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL | Block.UPDATE_SUPPRESS_DROPS);

        if (!removed) {
            restoreBlock(serverLevel, pos, state, blockEntityData);
            return InteractionResult.FAIL;
        }

        if (!serverLevel.addFreshEntity(physicsBlock)) {
            restoreBlock(serverLevel, pos, state, blockEntityData);
            return InteractionResult.FAIL;
        }

        if (nephritisCoated) {
            nephritisData.remove(pos);
        }

        bindHeldBlock(glove, physicsBlock.getUUID());
        syncInventory(serverPlayer);

        serverLevel.playSound(null, pos, SoundEvents.PISTON_EXTEND, SoundSource.PLAYERS, 0.65F, 1.35F);

        return InteractionResult.CONSUME;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack glove = player.getItemInHand(hand);

        if (isFillMode(glove)) {
            return InteractionResultHolder.sidedSuccess(
                    glove,
                    level.isClientSide
            );
        }

        if (level.isClientSide) {
            return hasHeldBlock(glove) ? InteractionResultHolder.success(glove) : InteractionResultHolder.pass(glove);
        }

        if (level instanceof ServerLevel serverLevel && player instanceof ServerPlayer serverPlayer) {
            EntropyPhysicsBlockEntity held = resolveHeldBlock(serverLevel, glove);

            if (held != null) {
                if (held.isHeldBy(serverPlayer)) {
                    held.releaseHolder(serverPlayer);
                }

                clearHeldBlock(glove);
                syncInventory(serverPlayer);
                return InteractionResultHolder.consume(glove);
            }

            if (hasHeldBlock(glove)) {
                clearHeldBlock(glove);
                syncInventory(serverPlayer);
            }
        }

        return InteractionResultHolder.pass(glove);
    }

    public static boolean canLiftBlock(Level level, BlockPos pos, BlockState state) {
        if (!passesBaseLiftRules(level, pos, state)) {
            return false;
        }

        BlockEntity blockEntity = level.getBlockEntity(pos);

        if (blockEntity != null) {
            if (blockEntity instanceof GeoBlockEntity) {
                return false;
            }

            return blockEntity instanceof Container;
        }

        return state.getRenderShape() == RenderShape.MODEL;
    }

    public static boolean isFillMode(ItemStack stack) {
        CompoundTag tag = stack.getTag();

        return tag != null && tag.getBoolean(TAG_FILL_MODE);
    }


    public static void setFillMode(ItemStack stack, boolean enabled) {
        if (enabled) {
            stack.getOrCreateTag().putBoolean(TAG_FILL_MODE, true);
            return;
        }

        CompoundTag tag = stack.getTag();

        if (tag != null) {
            tag.remove(TAG_FILL_MODE);
        }
    }


    public static ItemStack findGloveInHands(Player player) {
        ItemStack main = player.getMainHandItem();

        if (main.getItem() instanceof EntropyChestplateGloveItem) {
            return main;
        }

        ItemStack off = player.getOffhandItem();
        if (off.getItem() instanceof EntropyChestplateGloveItem) {
            return off;
        }

        return ItemStack.EMPTY;
    }


    public static InteractionHand findGloveHand(Player player) {
        if (player.getMainHandItem().getItem() instanceof EntropyChestplateGloveItem) {
            return InteractionHand.MAIN_HAND;
        }
        if (player.getOffhandItem().getItem() instanceof EntropyChestplateGloveItem) {
            return InteractionHand.OFF_HAND;
        }

        return null;
    }

    public static boolean canAttachBlockState(Level level, BlockPos referencePos, BlockState state) {
        return passesBaseLiftRules(level, referencePos, state) && state.getRenderShape() == RenderShape.MODEL;
    }

    private static boolean passesBaseLiftRules(Level level, BlockPos pos, BlockState state) {
        if (state.isAir() || !state.getFluidState().isEmpty()) {
            return false;
        }

        if (state.is(Blocks.BEDROCK) || state.is(Blocks.BARRIER) || state.is(Blocks.STRUCTURE_BLOCK) || state.is(Blocks.JIGSAW) || state.is(Blocks.END_PORTAL) || state.is(Blocks.END_GATEWAY) || state.is(Blocks.END_PORTAL_FRAME)) {
            return false;
        }

        Block block = state.getBlock();
        if (block instanceof TorchBlock || block instanceof WallTorchBlock || block instanceof DoorBlock || block instanceof BedBlock || block instanceof DoublePlantBlock || state.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF)) {
            return false;
        }

        float hardness = state.getDestroySpeed(level, pos);
        return hardness >= 0.0F && hardness <= MAX_PICKUP_HARDNESS;
    }

    private static void restoreBlock(ServerLevel level, BlockPos pos, BlockState state, CompoundTag blockEntityData) {
        level.setBlock(pos, state, Block.UPDATE_ALL);

        if (blockEntityData == null) {
            return;
        }

        BlockEntity restored = level.getBlockEntity(pos);
        if (restored == null) {
            return;
        }

        CompoundTag data = blockEntityData.copy();
        data.putInt("x", pos.getX());
        data.putInt("y", pos.getY());
        data.putInt("z", pos.getZ());

        restored.load(data);
        restored.setChanged();
        level.sendBlockUpdated(pos, state, state, Block.UPDATE_ALL);
    }

    public static boolean hasHeldBlock(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag != null && tag.hasUUID(TAG_HELD_BLOCK);
    }

    public static UUID getHeldBlockId(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag != null && tag.hasUUID(TAG_HELD_BLOCK) ? tag.getUUID(TAG_HELD_BLOCK) : null;
    }

    public static boolean isBoundTo(ItemStack stack, UUID entityId) {
        UUID current = getHeldBlockId(stack);
        return current != null && current.equals(entityId);
    }

    public static void bindHeldBlock(ItemStack stack, UUID entityId) {
        stack.getOrCreateTag().putUUID(TAG_HELD_BLOCK, entityId);
    }

    public static void clearHeldBlock(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag != null) {
            tag.remove(TAG_HELD_BLOCK);
        }
    }

    public static EntropyPhysicsBlockEntity resolveHeldBlock(ServerLevel level, ItemStack stack) {
        UUID uuid = getHeldBlockId(stack);
        if (uuid == null) {
            return null;
        }

        Entity entity = level.getEntity(uuid);
        return entity instanceof EntropyPhysicsBlockEntity physicsBlock ? physicsBlock : null;
    }

    public static ItemStack findActiveGlove(Player player) {
        ItemStack main = player.getMainHandItem();

        if (main.getItem() instanceof EntropyChestplateGloveItem
                && !isFillMode(main)
                && hasHeldBlock(main)) {
            return main;
        }

        ItemStack off = player.getOffhandItem();
        if (off.getItem() instanceof EntropyChestplateGloveItem
                && !isFillMode(off)
                && hasHeldBlock(off)) {
            return off;
        }

        return ItemStack.EMPTY;
    }

    public static void clearReferenceFromInventory(ServerPlayer player, UUID entityId) {
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.getItem() instanceof EntropyChestplateGloveItem && isBoundTo(stack, entityId)) {
                clearHeldBlock(stack);
            }
        }

        syncInventory(player);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltipComponents, TooltipFlag isAdvanced) {
        Component gloveb = Component.translatable("tooltip.oasiso.glove")
                .withStyle(ChatFormatting.AQUA);
        tooltipComponents.add(gloveb);
    }

    private static void syncInventory(ServerPlayer player) {
        player.getInventory().setChanged();
        player.containerMenu.broadcastChanges();
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 0, state -> state.setAndContinue(IDLE)));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }
}
