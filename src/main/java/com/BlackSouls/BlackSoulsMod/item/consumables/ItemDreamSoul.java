package com.BlackSouls.BlackSoulsMod.item.consumables;

import com.BlackSouls.BlackSoulsMod.BlackSouls;
import com.BlackSouls.BlackSoulsMod.capability.BSPlayerStats;
import com.BlackSouls.BlackSoulsMod.handler.StatEventHandler;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
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

public class ItemDreamSoul extends Item {

    public ItemDreamSoul(Properties properties) {
        super(properties.stacksTo(64));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!level.isClientSide()) {
            BSPlayerStats stats = player.getCapability(BSPlayerStats.CAPABILITY).orElse(null);

            if (stats != null) {
                
                if (stats.level >= 999) {
                    player.sendSystemMessage(Component.translatable("message.blacksouls.max_level").withStyle(ChatFormatting.RED));
                    return InteractionResultHolder.fail(stack);
                }

                
                int targetLevel = Math.min(999, stats.level + 5);
                long expNeeded = stats.getExpToReachLevel(targetLevel) - stats.getExpToReachLevel(stats.level);

                
                stats.addExp(expNeeded);

                StatEventHandler.applyStats(player);
                StatEventHandler.syncToClient(player);

                level.playSound(null, player.getX(), player.getY(), player.getZ(),
                        BlackSouls.ICE7_EVENT.get(), SoundSource.PLAYERS, 1.0F, 1.0F);

                if (!player.getAbilities().instabuild) {
                    stack.shrink(1);
                }
            }
        }
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flagIn) {
        tooltip.add(Component.translatable("item.blacksouls.dream_soul.lore.1").withStyle(ChatFormatting.WHITE));
        tooltip.add(Component.translatable("item.blacksouls.dream_soul.lore.2").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
    }
}