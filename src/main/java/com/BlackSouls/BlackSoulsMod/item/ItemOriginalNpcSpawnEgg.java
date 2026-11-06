package com.BlackSouls.BlackSoulsMod.item;

import com.BlackSouls.BlackSoulsMod.handler.BSEntityRegistry;
import com.BlackSouls.BlackSoulsMod.util.BSOriginalNpcData;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import com.BlackSouls.BlackSoulsMod.entity.EntityOriginalNpc;
import net.minecraftforge.common.ForgeSpawnEggItem;

public final class ItemOriginalNpcSpawnEgg extends ForgeSpawnEggItem {
    private final String npcId;

    public ItemOriginalNpcSpawnEgg(String npcId, Properties properties) {
        super(BSEntityRegistry.ORIGINAL_NPC, 0x393B3D, 0xD4C7AE, properties);
        this.npcId = npcId;
    }

    @Override
    public ItemStack getDefaultInstance() {
        ItemStack stack = super.getDefaultInstance();
        stack.getOrCreateTagElement("EntityTag").putString("OriginalNpcId", npcId);
        return stack;
    }

    @Override
    public Component getName(ItemStack stack) {
        BSOriginalNpcData.Entry definition = BSOriginalNpcData.get(npcId);
        return Component.literal(definition == null ? npcId : definition.name());
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (!(context.getLevel() instanceof ServerLevel level)) {
            return InteractionResult.sidedSuccess(context.getLevel().isClientSide());
        }
        BlockPos position = context.getClickedPos().relative(context.getClickedFace());
        EntityOriginalNpc npc = BSEntityRegistry.ORIGINAL_NPC.get().create(level);
        if (npc == null) {
            return InteractionResult.FAIL;
        }
        npc.moveTo(position.getX() + 0.5D, position.getY(), position.getZ() + 0.5D,
                context.getPlayer() == null ? 0.0F : context.getPlayer().getYRot(), 0.0F);
        npc.setNpcId(npcId);
        if (!level.noCollision(npc) || !level.addFreshEntity(npc)) {
            return InteractionResult.FAIL;
        }
        if (context.getPlayer() instanceof net.minecraft.server.level.ServerPlayer player) {
            com.BlackSouls.BlackSoulsMod.story.OriginalNpcService.reconnectDeveloperReplacement(player, npc);
        }
        if (context.getPlayer() == null || !context.getPlayer().getAbilities().instabuild) {
            context.getItemInHand().shrink(1);
        }
        return InteractionResult.CONSUME;
    }
}
