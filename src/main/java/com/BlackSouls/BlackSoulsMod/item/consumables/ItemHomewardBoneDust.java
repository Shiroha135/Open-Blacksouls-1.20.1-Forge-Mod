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
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class ItemHomewardBoneDust extends Item {

    public ItemHomewardBoneDust(Properties properties) {
        super(properties.stacksTo(64));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
            BlockPos respawnPos = serverPlayer.getRespawnPosition();
            ServerLevel targetLevel = serverPlayer.server.getLevel(serverPlayer.getRespawnDimension());

            if (respawnPos != null && targetLevel != null) {
                level.playSound(null, player.blockPosition(), BlackSouls.FIRE6_EVENT.get(), SoundSource.PLAYERS, 1.0F, 1.0F);

                BlockPos safePos = respawnPos;
                for (Direction dir : Direction.Plane.HORIZONTAL) {
                    BlockPos candidate = respawnPos.relative(dir);
                    if (targetLevel.getBlockState(candidate).getCollisionShape(targetLevel, candidate).isEmpty() &&
                            targetLevel.getBlockState(candidate.above()).getCollisionShape(targetLevel, candidate.above()).isEmpty()) {
                        safePos = candidate;
                        break;
                    }
                }

                double dx = respawnPos.getX() - safePos.getX();
                double dz = respawnPos.getZ() - safePos.getZ();
                float targetYaw = (float) (Math.atan2(dz, dx) * (180D / Math.PI)) - 90.0F;

                serverPlayer.teleportTo(targetLevel, safePos.getX() + 0.5, safePos.getY(), safePos.getZ() + 0.5, targetYaw, 15.0F);
                targetLevel.playSound(null, safePos, BlackSouls.FIRE6_EVENT.get(), SoundSource.PLAYERS, 1.0F, 1.0F);

                if (!player.getAbilities().instabuild) {
                    stack.shrink(1);
                }
            } else {
                player.sendSystemMessage(Component.translatable("message.blacksouls.no_bonfire").withStyle(ChatFormatting.RED));
                return InteractionResultHolder.fail(stack);
            }
        }
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flagIn) {
        tooltip.add(Component.translatable("item.blacksouls.homeward_bone_dust.lore.1").withStyle(ChatFormatting.WHITE));
        tooltip.add(Component.translatable("item.blacksouls.homeward_bone_dust.lore.2").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
    }
}
