package yuki135.blacksouls.item.consumables;

import yuki135.blacksouls.BlackSouls;
import yuki135.blacksouls.capability.BSPlayerStats;
import yuki135.blacksouls.util.HokoniwaDestination;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
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

public class ItemBlackAsh extends Item {

    private static final ResourceKey<Level> HOKONIWA_KEY = HokoniwaDestination.DIMENSION;

    public ItemBlackAsh(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(Level level, Player player, @NotNull InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
            boolean success = teleportToHokoniwa(serverPlayer);
            if (!success) {
                return InteractionResultHolder.fail(stack);
            }
        }

        return InteractionResultHolder.consume(stack);
    }

    private boolean teleportToHokoniwa(ServerPlayer serverPlayer) {
        BSPlayerStats stats = serverPlayer.getCapability(BSPlayerStats.CAPABILITY).resolve().orElse(null);

        ServerLevel targetLevel = serverPlayer.server.getLevel(HOKONIWA_KEY);

        if (targetLevel == null || stats == null) {
            serverPlayer.sendSystemMessage(Component.translatable("message.blacksouls.no_bonfire").withStyle(ChatFormatting.RED));
            return false;
        }
        if (!HokoniwaDestination.isLandingSafe(targetLevel)) {
            serverPlayer.sendSystemMessage(Component.translatable("message.blacksouls.library_unavailable").withStyle(ChatFormatting.RED));
            return false;
        }

        
        stats.souls = 0;

        serverPlayer.level().playSound(
                null,
                serverPlayer.blockPosition(),
                BlackSouls.FIRE6_EVENT.get(),
                SoundSource.PLAYERS,
                1.0F,
                1.0F
        );

        
        serverPlayer.teleportTo(
                targetLevel,
                HokoniwaDestination.X,
                HokoniwaDestination.Y,
                HokoniwaDestination.Z,
                HokoniwaDestination.YAW,
                0.0F
        );

        yuki135.blacksouls.story.OriginalOpeningBonfireFlow.visitLibrary(serverPlayer);

        targetLevel.playSound(
                null,
                serverPlayer.blockPosition(),
                BlackSouls.FIRE6_EVENT.get(),
                SoundSource.PLAYERS,
                1.0F,
                1.0F
        );

        serverPlayer.sendSystemMessage(Component.translatable("message.blacksouls.black_ash_use").withStyle(ChatFormatting.DARK_PURPLE));
        return true;
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @Nullable Level level, List<Component> tooltip, @NotNull TooltipFlag flagIn) {
        tooltip.add(Component.translatable("item.blacksouls.black_ash.lore.1").withStyle(ChatFormatting.WHITE));
        tooltip.add(Component.translatable("item.blacksouls.black_ash.lore.2").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
    }
}
