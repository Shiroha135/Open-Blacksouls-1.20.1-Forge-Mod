package yuki135.blacksouls.item;

import yuki135.blacksouls.entity.EntityRabbitHoleNpc;
import yuki135.blacksouls.handler.BSEntityRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;

public class ItemRabbitHoleNpcSpawn extends Item {
    private final EntityRabbitHoleNpc.Role role;

    public ItemRabbitHoleNpcSpawn(Properties properties, EntityRabbitHoleNpc.Role role) {
        super(properties);
        this.role = role;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (!(context.getLevel() instanceof ServerLevel level)) {
            return InteractionResult.sidedSuccess(context.getLevel().isClientSide());
        }
        Player player = context.getPlayer();
        ItemStack stack = context.getItemInHand();
        BlockPos pos = context.getClickedPos().relative(context.getClickedFace());
        EntityRabbitHoleNpc rabbit = BSEntityRegistry.RABBIT_HOLE_NPC.get().create(level);
        if (rabbit == null) {
            return InteractionResult.FAIL;
        }
        rabbit.setRole(this.role);
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
