package com.BlackSouls.BlackSoulsMod.item.dev;

import com.BlackSouls.BlackSoulsMod.network.NetworkHandler;
import com.BlackSouls.BlackSoulsMod.network.packets.PacketSyncUnlockedAvatars;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class ItemKingExitAvatarPack extends Item {
    private final List<String> avatars;

    public ItemKingExitAvatarPack(Properties properties, List<String> avatars) {
        super(properties.stacksTo(1));
        this.avatars = avatars;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
            CompoundTag data = player.getPersistentData();

            ListTag list;
            if (data.contains("bs2_unlocked_dlc_avatars", 9)) {
                list = data.getList("bs2_unlocked_dlc_avatars", 8);
            } else {
                list = new ListTag();
            }

            int added = 0;

            for (String avatarId : avatars) {
                boolean exists = false;

                for (int i = 0; i < list.size(); i++) {
                    if (list.getString(i).equals(avatarId)) {
                        exists = true;
                        break;
                    }
                }

                if (!exists) {
                    list.add(StringTag.valueOf(avatarId));
                    added++;
                }
            }

            data.put("bs2_unlocked_dlc_avatars", list);

            if (added > 0) {
                stack.shrink(1);
                serverPlayer.sendSystemMessage(
                        Component.translatable("message.blacksouls.avatar_pack.used", added)
                                .withStyle(ChatFormatting.AQUA)
                );
            } else {
                serverPlayer.sendSystemMessage(
                        Component.translatable("message.blacksouls.avatar_pack.already_unlocked")
                                .withStyle(ChatFormatting.GRAY)
                );
            }

            NetworkHandler.INSTANCE.send(
                    PacketDistributor.PLAYER.with(() -> serverPlayer),
                    new PacketSyncUnlockedAvatars(list)
            );
        }

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flagIn) {
        super.appendHoverText(stack, level, tooltip, flagIn);
        tooltip.add(Component.translatable("item.blacksouls.king_exit_avatar_pack.desc")
                .withStyle(ChatFormatting.GRAY));

        tooltip.add(Component.translatable("item.blacksouls.king_exit_avatar_pack.lore1")
                .withStyle(ChatFormatting.RED, ChatFormatting.ITALIC));

        tooltip.add(Component.translatable("item.blacksouls.king_exit_avatar_pack.lore2")
                .withStyle(ChatFormatting.RED, ChatFormatting.ITALIC));
    }
}