package com.BlackSouls.BlackSoulsMod.item.skillbook;

import com.BlackSouls.BlackSoulsMod.item.ItemLoreBase;
import com.BlackSouls.BlackSoulsMod.util.SkillUtils;
import com.BlackSouls.BlackSoulsMod.util.skill.SkillOriginalMagic;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class ItemOriginalSkillBook extends ItemLoreBase {
    private final SkillOriginalMagic.Profile profile;

    public ItemOriginalSkillBook(Properties properties, SkillOriginalMagic.Profile profile) {
        super(properties.stacksTo(64));
        this.profile = profile;
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @Nullable Level level, List<Component> tooltip, @NotNull TooltipFlag flag) {
        for (int line = 1; line <= profile.getLoreLines(); line++) {
            tooltip.add(Component.translatable("item.blacksouls." + profile.getBookId() + ".lore." + line).withStyle(ChatFormatting.WHITE));
        }
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(Level level, Player player, @NotNull InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide) {
            if (SkillUtils.hasLearnedSkill(player, profile.getSkillId())) {
                player.sendSystemMessage(Component.translatable("message.blacksouls.skill.already_learned").withStyle(ChatFormatting.RED));
                return InteractionResultHolder.fail(stack);
            }
            SkillUtils.learnSkill(player, profile.getSkillId());
            player.sendSystemMessage(Component.translatable(
                    "message.blacksouls.skill.learned",
                    Component.translatable(profile.getTranslationKey())
            ).withStyle(ChatFormatting.GOLD));
            if (!player.isCreative()) {
                stack.shrink(1);
            }
        }
        return InteractionResultHolder.consume(stack);
    }
}
