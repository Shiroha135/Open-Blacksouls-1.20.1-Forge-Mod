package yuki135.blacksouls.item.dev;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
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

public class ItemDemonRootsAvatarPack extends Item {

    public ItemDemonRootsAvatarPack(Properties properties, List<String> avatars) {
        super(properties.stacksTo(1));
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(Level level, Player player, @NotNull InteractionHand hand) {
        ItemStack stack=player.getItemInHand(hand);
        if(!level.isClientSide()&&player instanceof ServerPlayer serverPlayer)
            serverPlayer.sendSystemMessage(Component.translatable("message.blacksouls.avatar_pack.unavailable").withStyle(ChatFormatting.GRAY));
        return InteractionResultHolder.fail(stack);
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @Nullable Level level, @NotNull List<Component> tooltip, @NotNull TooltipFlag flagIn) {
        super.appendHoverText(stack, level, tooltip, flagIn);

        tooltip.add(Component.translatable("tooltip.blacksouls.demon_roots_avatar_pack.desc")
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.blacksouls.demon_roots_avatar_pack.lore")
                .withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.ITALIC));
        tooltip.add(Component.translatable("tooltip.blacksouls.demon_roots_avatar_pack.lore2")
                .withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.ITALIC));
    }
}