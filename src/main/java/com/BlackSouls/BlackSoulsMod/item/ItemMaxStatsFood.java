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
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class ItemMaxStatsFood extends Item {
    private final double addHp;
    private final double addMp;

    public ItemMaxStatsFood(Properties properties, double addHp, double addMp) {
        super(properties.stacksTo(64));
        this.addHp = addHp;
        this.addMp = addMp;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide()) {
            BSPlayerStats stats = player.getCapability(BSPlayerStats.CAPABILITY).orElse(null);
            if (stats != null) {
                if (addHp > 0) stats.addPermanentStat("HP", addHp);
                if (addMp > 0) stats.addPermanentStat("MP", addMp);

                stats.recalculateStats();
                StatEventHandler.applyStats(player);

                if (addHp > 0) player.heal((float) addHp);

                StatEventHandler.syncToClient(player);

                level.playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.GENERIC_EAT, SoundSource.PLAYERS, 1.0F, 1.0F);
            }
            if (!player.getAbilities().instabuild) stack.shrink(1);
        }
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flagIn) {
        tooltip.add(Component.translatable(this.getDescriptionId() + ".effect").withStyle(ChatFormatting.WHITE));
        tooltip.add(Component.translatable(this.getDescriptionId() + ".lore").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
    }
}
