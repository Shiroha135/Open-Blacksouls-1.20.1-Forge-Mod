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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class ItemPigeonEgg extends Item {

    public ItemPigeonEgg(Properties properties) {
        super(properties.stacksTo(64));
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(Level level, Player player, @NotNull InteractionHand hand) {
        ItemStack itemstack = player.getItemInHand(hand);

        if (!level.isClientSide()) {
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    BlackSouls.ICE1_EVENT.get(), SoundSource.PLAYERS, 1.0F, 1.0F);

            BSPlayerStats stats = player.getCapability(BSPlayerStats.CAPABILITY).orElse(null);
            if (stats != null) {
                double maxMana = stats.maxMp;
                stats.mp = Math.min(maxMana, stats.mp + maxMana * 0.30 * StatEventHandler.getConsumableRecoveryMultiplier(player));
                StatEventHandler.syncToClient(player);
            }

            if (!player.getAbilities().instabuild) {
                itemstack.shrink(1);
            }
        }

        return InteractionResultHolder.consume(itemstack);
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @Nullable Level level, List<Component> tooltip, @NotNull TooltipFlag flagIn) {
        tooltip.add(Component.translatable("item.blacksouls.pigeon_egg.lore.1")
                .withStyle(ChatFormatting.WHITE));
        tooltip.add(Component.translatable("item.blacksouls.pigeon_egg.lore.2")
                .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
    }
}
