package com.BlackSouls.BlackSoulsMod.item.consumables;

import com.BlackSouls.BlackSoulsMod.BlackSouls;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class ItemReturnToBonfire extends Item {
    public ItemReturnToBonfire(Properties properties) {
        super(properties);
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(Level level, Player player, @NotNull InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide() || !(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResultHolder.consume(stack);
        }

        BlockPos respawnPos = serverPlayer.getRespawnPosition();
        ServerLevel targetLevel = serverPlayer.server.getLevel(serverPlayer.getRespawnDimension());
        if (respawnPos == null || targetLevel == null) {
            player.sendSystemMessage(Component.translatable("message.blacksouls.no_bonfire").withStyle(ChatFormatting.RED));
            return InteractionResultHolder.fail(stack);
        }

        BlockPos safePos = respawnPos;
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            BlockPos candidate = respawnPos.relative(direction);
            if (targetLevel.getBlockState(candidate).getCollisionShape(targetLevel, candidate).isEmpty()
                    && targetLevel.getBlockState(candidate.above()).getCollisionShape(targetLevel, candidate.above()).isEmpty()) {
                safePos = candidate;
                break;
            }
        }

        level.playSound(null, player.blockPosition(), BlackSouls.FIRE6_EVENT.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
        serverPlayer.teleportTo(targetLevel, safePos.getX() + 0.5D, safePos.getY(), safePos.getZ() + 0.5D, player.getYRot(), player.getXRot());
        targetLevel.playSound(null, safePos, BlackSouls.FIRE6_EVENT.get(), SoundSource.PLAYERS, 1.0F, 1.0F);

        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @Nullable Level level, List<Component> tooltip, @NotNull TooltipFlag flag) {
        tooltip.add(Component.translatable(this.getDescriptionId() + ".lore.1").withStyle(ChatFormatting.WHITE));
        tooltip.add(Component.translatable(this.getDescriptionId() + ".lore.2").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
    }
}
