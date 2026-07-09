package com.BlackSouls.BlackSoulsMod.item.skillbook;

import com.BlackSouls.BlackSoulsMod.item.ItemLoreBase;
import com.BlackSouls.BlackSoulsMod.util.SkillUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

public class ItemSkillBookGrit extends ItemLoreBase {

    public ItemSkillBookGrit(Properties properties) {
        super(properties);
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(Level level, Player player, @NotNull InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!level.isClientSide) {
            if (SkillUtils.hasLearnedSkill(player, "bs2_skill_grit")) {
                player.sendSystemMessage(Component.translatable("message.blacksouls.skill.already_learned").withStyle(ChatFormatting.RED));
                return InteractionResultHolder.fail(stack);
            } else {
                SkillUtils.learnSkill(player, "bs2_skill_grit");

                Component skillName = Component.translatable("skill.blacksouls.grit.name");
                player.sendSystemMessage(Component.translatable("message.blacksouls.skill.learned", skillName).withStyle(ChatFormatting.GOLD));

                if (!player.isCreative()) stack.shrink(1);
            }
        }
        return InteractionResultHolder.consume(stack);
    }
}
