package com.livingecology.item;

import com.livingecology.data.MobMindData;
import com.livingecology.debug.DebugFormatter;
import com.livingecology.territory.TerritoryManager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

public final class DebugAnalyzerItem extends Item {
    public DebugAnalyzerItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        if (!player.level().isClientSide && player instanceof ServerPlayer serverPlayer
                && player.level() instanceof ServerLevel level && target instanceof Mob mob) {
            if (MobMindData.supports(mob)) {
                MobMindData.initialize(mob, level);
                TerritoryManager.ensureTerritory(mob, level);
            }
            DebugFormatter.sendMob(serverPlayer, mob, level);
        }
        return InteractionResult.sidedSuccess(player.level().isClientSide);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player instanceof ServerPlayer serverPlayer && context.getLevel() instanceof ServerLevel level) {
            DebugFormatter.sendLocation(serverPlayer, level, context.getClickedPos());
        }
        return InteractionResult.sidedSuccess(context.getLevel().isClientSide);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide && level instanceof ServerLevel serverLevel && player instanceof ServerPlayer serverPlayer) {
            DebugFormatter.sendLocation(serverPlayer, serverLevel, player.blockPosition());
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }
}
