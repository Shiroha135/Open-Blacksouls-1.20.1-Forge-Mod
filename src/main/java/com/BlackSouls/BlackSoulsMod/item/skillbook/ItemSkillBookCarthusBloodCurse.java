package com.BlackSouls.BlackSoulsMod.item.skillbook;

import com.BlackSouls.BlackSoulsMod.capability.BSPlayerStats;
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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class ItemSkillBookCarthusBloodCurse extends Item {

    public ItemSkillBookCarthusBloodCurse(Properties properties) {
        super(properties.stacksTo(64));
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @Nullable Level level, List<Component> tooltip, @NotNull TooltipFlag flag) {
        tooltip.add(Component.translatable("item.blacksouls.skill_book_carthus_blood_curse.lore1").withStyle(ChatFormatting.WHITE));
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(Level level, Player player, @NotNull InteractionHand hand) {
        ItemStack itemstack = player.getItemInHand(hand);

        if (!level.isClientSide()) {
            player.getCapability(BSPlayerStats.CAPABILITY).ifPresent(stats -> {
                String skillId = "bs2_skill_carthus_blood_curse";

                if (!stats.unlockedSkills.contains(skillId)) {
                    stats.unlockedSkills.add(skillId);

                    level.playSound(null, player.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1.0F, 1.0F);
                    player.sendSystemMessage(Component.translatable("message.blacksouls.skill.learned", Component.translatable("skill.blacksouls.bs2_skill_carthus_blood_curse.name")).withStyle(ChatFormatting.GREEN));

                    if (!player.isCreative()) {
                        itemstack.shrink(1);
                    }
                    com.BlackSouls.BlackSoulsMod.handler.StatEventHandler.syncToClient(player);
                } else {
                    player.sendSystemMessage(Component.translatable("message.blacksouls.skill.already_learned").withStyle(ChatFormatting.RED));
                }
            });
        }
        return InteractionResultHolder.consume(itemstack);
    }
}
