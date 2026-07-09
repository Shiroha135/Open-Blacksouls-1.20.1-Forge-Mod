package com.BlackSouls.BlackSoulsMod.item;

import com.BlackSouls.BlackSoulsMod.handler.BSEntityRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.ClipContext;

public class ItemNodenSpawn extends Item {

    public ItemNodenSpawn(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (!(context.getLevel() instanceof ServerLevel serverLevel)) {
            return InteractionResult.sidedSuccess(context.getLevel().isClientSide());
        }

        BlockPos spawnPos = context.getClickedPos().relative(context.getClickedFace());
        return spawnNoden(serverLevel, context.getPlayer(), context.getItemInHand(), spawnPos)
                ? InteractionResult.CONSUME
                : InteractionResult.FAIL;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!(level instanceof ServerLevel serverLevel)) {
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
        }

        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        Vec3 end = eye.add(look.scale(5.0D));
        BlockHitResult hit = level.clip(new ClipContext(eye, end, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));

        BlockPos spawnPos;
        if (hit.getType() == HitResult.Type.BLOCK) {
            spawnPos = hit.getBlockPos().relative(hit.getDirection());
        } else {
            spawnPos = player.blockPosition().relative(Direction.fromYRot(player.getYRot()));
        }

        if (spawnNoden(serverLevel, player, stack, spawnPos)) {
            return InteractionResultHolder.consume(stack);
        }
        return InteractionResultHolder.fail(stack);
    }

    private boolean spawnNoden(ServerLevel level, Player player, ItemStack stack, BlockPos spawnPos) {
        Entity entity = BSEntityRegistry.NODEN.get().create(level);
        if (entity == null) {
            return false;
        }

        double x = spawnPos.getX() + 0.5D;
        double y = spawnPos.getY();
        double z = spawnPos.getZ() + 0.5D;
        entity.moveTo(x, y, z, player.getYRot(), 0.0F);

        if (!level.noCollision(entity)) {
            return false;
        }

        level.addFreshEntity(entity);
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        return true;
    }
}
