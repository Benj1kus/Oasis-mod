package com.benji.oasiso.common.item;

import com.benji.oasiso.ModSounds;
import com.benji.oasiso.common.entity.ScarabEntity;
import com.benji.oasiso.registry.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.core.dispenser.DispenseItemBehavior;
import net.minecraftforge.common.ForgeSpawnEggItem;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

public class ScarabCoreItem extends ForgeSpawnEggItem {

    private static final String TAG_CORE_ID = "ScarabCoreId";
    private static final String TAG_ACTIVE_SCARAB = "ActiveScarab";

    private static final int SUMMON_COST = 2;
    private static final int DEATH_COST = 2;

    public ScarabCoreItem(Supplier<? extends EntityType<? extends Mob>> type, int backgroundColor, int highlightColor, Item.Properties properties) {
        super(type, backgroundColor, highlightColor, properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        ItemStack core = context.getItemInHand();

        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }

        if (!(level instanceof ServerLevel serverLevel) || !(player instanceof ServerPlayer serverPlayer)) {

            return InteractionResult.PASS;
        }
        UUID coreId = getOrCreateCoreId(core);

        if (hasActiveScarab(core)) {
            playFailedSummonSound(serverPlayer);
            return InteractionResult.CONSUME;
        }

        if (!canSpendCharge(core, SUMMON_COST)) {
            playFailedSummonSound(serverPlayer);
            return InteractionResult.CONSUME;
        }

        BlockPos clickedPos = context.getClickedPos();
        Direction face = context.getClickedFace();
        BlockPos spawnPos;

        if (serverLevel.getBlockState(clickedPos).getCollisionShape(serverLevel, clickedPos).isEmpty()) {

            spawnPos = clickedPos;

        } else {

            spawnPos = clickedPos.relative(face);
        }

        ScarabEntity scarab = (ScarabEntity) this.getType(core.getTag()).spawn(serverLevel, spawnPos, MobSpawnType.SPAWN_EGG);

        if (scarab == null) {
            playFailedSummonSound(serverPlayer);
            return InteractionResult.CONSUME;
        }

        scarab.setPersistenceRequired();
        scarab.bindToScarabCore(coreId, serverPlayer.getUUID());
        scarab.startCoreSummonEffect();
        core.getOrCreateTag().putUUID(TAG_ACTIVE_SCARAB, scarab.getUUID());
        spendCharge(core, SUMMON_COST);
        serverPlayer.getInventory().setChanged();
        return InteractionResult.CONSUME;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        return InteractionResultHolder.pass(player.getItemInHand(hand));
    }

    @Nullable
    @Override
    protected DispenseItemBehavior createDispenseBehavior() {
        return null;
    }

    @Override
    public boolean overrideOtherStackedOnMe(ItemStack core, ItemStack carriedStack, Slot slot, ClickAction action, Player player, SlotAccess carriedSlotAccess) {
        if (action != ClickAction.SECONDARY) {
            return false;
        }

        if (!carriedStack.is(ModItems.AZUMALIT_PIECE.get())) {
            return false;
        }
        if (core.getDamageValue() <= 0) {
            return true;
        }

        if (!player.level().isClientSide) {
            core.setDamageValue(Math.max(0, core.getDamageValue() - 1));

            if (!player.getAbilities().instabuild) {
                carriedStack.shrink(1);
            }

            if (player instanceof ServerPlayer serverPlayer) {
                serverPlayer.playNotifySound(SoundEvents.SOUL_ESCAPE, SoundSource.PLAYERS, 10.5F, 1.05F);
            }
            player.getInventory().setChanged();
        }
        return true;
    }

    private static UUID getOrCreateCoreId(ItemStack core) {
        if (core.getOrCreateTag().hasUUID(TAG_CORE_ID)) {
            return core.getTag().getUUID(TAG_CORE_ID);
        }
        UUID id = UUID.randomUUID();
        core.getOrCreateTag().putUUID(TAG_CORE_ID, id);
        return id;
    }

    private static boolean hasActiveScarab(ItemStack core) {
        return core.hasTag() && core.getTag().hasUUID(TAG_ACTIVE_SCARAB);
    }

    public static boolean belongsToCore(ItemStack core, UUID coreId) {
        return core.hasTag() && core.getTag().hasUUID(TAG_CORE_ID) && core.getTag().getUUID(TAG_CORE_ID).equals(coreId);
    }

    private static int getRemainingCharge(ItemStack core) {
        return core.getMaxDamage() - core.getDamageValue();
    }

    private static boolean canSpendCharge(ItemStack core, int amount) {
        return getRemainingCharge(core) > amount;
    }

    private static void spendCharge(ItemStack core, int amount) {
        int maximumDamage = core.getMaxDamage() - 1;
        int newDamage = Math.min(maximumDamage, core.getDamageValue() + amount);
        core.setDamageValue(newDamage);
    }

    private static void playFailedSummonSound(ServerPlayer player) {
        player.playNotifySound(ModSounds.SCARAB_DEATH.get(), SoundSource.PLAYERS, 1.0F, 0.85F);
    }

    public static void onBoundScarabDeath(MinecraftServer server, UUID ownerId, UUID coreId, UUID scarabId) {
        ServerPlayer owner = server.getPlayerList().getPlayer(ownerId);

        if (owner != null && damageMatchingCore(owner, coreId, scarabId)) {
            return;
        }

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (player == owner) {
                continue;
            }
            if (damageMatchingCore(player, coreId, scarabId)) {
                return;
            }
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltipComponents, TooltipFlag isAdvanced) {
        Component glove = Component.translatable("tooltip.oasiso.scarab1").withStyle(ChatFormatting.DARK_AQUA);

        tooltipComponents.add(Component.translatable("tooltip.oasiso.scarab2", glove).withStyle(ChatFormatting.AQUA));

        tooltipComponents.add(glove);
    }

    private static boolean damageMatchingCore(ServerPlayer player, UUID coreId, UUID scarabId) {
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {

            ItemStack stack = player.getInventory().getItem(slot);

            if (!(stack.getItem() instanceof ScarabCoreItem)) {

                continue;
            }
            if (!belongsToCore(stack, coreId)) {
                continue;
            }
            if (stack.hasTag() && stack.getTag().hasUUID(TAG_ACTIVE_SCARAB) && stack.getTag().getUUID(TAG_ACTIVE_SCARAB).equals(scarabId)) {
                stack.getTag().remove(TAG_ACTIVE_SCARAB);
            }

            spendCharge(stack, DEATH_COST);
            player.getInventory().setChanged();
            player.inventoryMenu.broadcastChanges();

            return true;
        }

        return false;
    }
}