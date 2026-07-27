package com.BlackSouls.BlackSoulsMod.item;

import com.BlackSouls.BlackSoulsMod.entity.EntityCorpseEatingRabbit;
import com.BlackSouls.BlackSoulsMod.handler.BSEntityRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;

public class ItemCorpseEatingRabbitSpawn extends Item {
    public ItemCorpseEatingRabbitSpawn(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (!(context.getLevel() instanceof ServerLevel level)) {
            return InteractionResult.sidedSuccess(context.getLevel().isClientSide());
        }
        Player player = context.getPlayer();
        ItemStack stack = context.getItemInHand();
        BlockPos pos = context.getClickedPos().relative(context.getClickedFace());
        EntityCorpseEatingRabbit rabbit = BSEntityRegistry.CORPSE_EATING_RABBIT.get().create(level);
        if (rabbit == null) {
            return InteractionResult.FAIL;
        }
        rabbit.moveTo(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D,
                player == null ? 0.0F : player.getYRot(), 0.0F);
        if (!level.noCollision(rabbit)) {
            return InteractionResult.FAIL;
        }
        level.addFreshEntity(rabbit);
        if (player == null || !player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        return InteractionResult.CONSUME;
    }
}
