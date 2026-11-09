package yuki135.blacksouls.item.consumables;

import yuki135.blacksouls.BlackSouls;
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

public class ItemBlackAsh extends Item {

    public ItemBlackAsh(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(Level level, Player player, @NotNull InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
            boolean success = yuki135.blacksouls.story.OriginalBlackAshService.begin(serverPlayer);
            if (!success) {
                return InteractionResultHolder.fail(stack);
            }
        }

        return InteractionResultHolder.consume(stack);
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @Nullable Level level, List<Component> tooltip, @NotNull TooltipFlag flagIn) {
        tooltip.add(Component.translatable("item.blacksouls.black_ash.lore.1").withStyle(ChatFormatting.WHITE));
        tooltip.add(Component.translatable("item.blacksouls.black_ash.lore.2").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
    }
}
