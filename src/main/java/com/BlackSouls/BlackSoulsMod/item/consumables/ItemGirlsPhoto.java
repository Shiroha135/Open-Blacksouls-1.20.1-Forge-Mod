package com.BlackSouls.BlackSoulsMod.item.consumables;

import com.BlackSouls.BlackSoulsMod.BlackSouls;
import com.BlackSouls.BlackSoulsMod.capability.BSPlayerStats;
import com.BlackSouls.BlackSoulsMod.handler.StatEventHandler;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
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

public class ItemGirlsPhoto extends Item {

    public ItemGirlsPhoto(Properties properties) {
        super(properties.stacksTo(64));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
            BSPlayerStats stats = player.getCapability(BSPlayerStats.CAPABILITY).resolve().orElse(null);

            if (stats != null) {
                
                if (stats.sen <= 0) {
                    player.sendSystemMessage(Component.translatable("message.blacksouls.sen_empty").withStyle(ChatFormatting.RED));
                    return InteractionResultHolder.fail(stack);
                }

                
                stats.sen = Math.max(0, stats.sen - 10);

                if (BlackSouls.RAISE3_EVENT.isPresent()) {
                    level.playSound(null, player.getX(), player.getY(), player.getZ(),
                            BlackSouls.RAISE3_EVENT.get(), SoundSource.PLAYERS, 0.8F, 1.0F);
                }
                if (BlackSouls.ICE8_EVENT.isPresent()) {
                    level.playSound(null, player.getX(), player.getY(), player.getZ(),
                            BlackSouls.ICE8_EVENT.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
                }

                
                StatEventHandler.syncToClient(player);

                if (!player.getAbilities().instabuild) {
                    stack.shrink(1);
                }
            }
        }

        return InteractionResultHolder.consume(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flagIn) {
        tooltip.add(Component.translatable("item.blacksouls.girls_photo.lore.1").withStyle(ChatFormatting.WHITE));
        tooltip.add(Component.translatable("item.blacksouls.girls_photo.lore.2").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
    }
}