package com.BlackSouls.BlackSoulsMod.item;

import com.BlackSouls.BlackSoulsMod.capability.BSPlayerStats;
import com.BlackSouls.BlackSoulsMod.handler.StatEventHandler;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class ItemCovenant extends ItemLoreBase {

    private final String covenantId;

    public ItemCovenant(String covenantId, Properties properties) {
        super(properties.stacksTo(1));
        this.covenantId = covenantId;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!level.isClientSide()) {
            BSPlayerStats stats = player.getCapability(BSPlayerStats.CAPABILITY).orElse(null);

            if (stats != null) {
                if (!stats.unlockedCovenants.contains(this.covenantId)) {
                    stats.unlockedCovenants.add(this.covenantId);
                    
                    Component covName = Component.translatable("covenant.blacksouls." + this.covenantId + ".name");
                    player.displayClientMessage(Component.translatable("message.blacksouls.covenant.unlocked", covName).withStyle(ChatFormatting.GOLD), false);
                    
                    level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1.0F, 1.0F);
                    StatEventHandler.syncToClient(player);

                    if (!player.getAbilities().instabuild) stack.shrink(1);
                    return InteractionResultHolder.success(stack);
                } else {
                    Component covName = Component.translatable("covenant.blacksouls." + this.covenantId + ".name");
                    player.displayClientMessage(Component.translatable("message.blacksouls.covenant.already_unlocked", covName).withStyle(ChatFormatting.RED), false);
                    return InteractionResultHolder.fail(stack);
                }
            }
        }
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flagIn) {
        super.appendHoverText(stack, level, tooltip, flagIn);

        tooltip.add(Component.empty());
        tooltip.add(Component.translatable("tooltip.blacksouls.covenant.hint1").withStyle(ChatFormatting.YELLOW));
        tooltip.add(Component.translatable("tooltip.blacksouls.covenant.hint2").withStyle(ChatFormatting.GRAY));
    }
}