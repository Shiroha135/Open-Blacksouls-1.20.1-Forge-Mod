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

public class ItemSkillBookRequiem extends ItemLoreBase {

    public ItemSkillBookRequiem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!level.isClientSide) {
            if (SkillUtils.hasLearnedSkill(player, "bs2_skill_requiem")) {
                player.sendSystemMessage(Component.translatable("message.blacksouls.skill.already_learned").withStyle(ChatFormatting.RED));
                return InteractionResultHolder.fail(stack);
            } else {
                SkillUtils.learnSkill(player, "bs2_skill_requiem");

                Component skillName = Component.translatable("skill.blacksouls.requiem.name");
                player.sendSystemMessage(Component.translatable("message.blacksouls.skill.learned", skillName).withStyle(ChatFormatting.GOLD));

                if (!player.isCreative()) stack.shrink(1);
            }
        }
        return InteractionResultHolder.consume(stack);
    }
}
