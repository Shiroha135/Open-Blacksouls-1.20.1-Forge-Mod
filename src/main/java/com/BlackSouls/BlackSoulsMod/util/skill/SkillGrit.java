package com.BlackSouls.BlackSoulsMod.util.skill;

import com.BlackSouls.BlackSoulsMod.BlackSouls;
import com.BlackSouls.BlackSoulsMod.capability.BSPlayerStats;
import com.BlackSouls.BlackSoulsMod.util.SkillUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;

public class SkillGrit extends AbstractSkill {

    private static final int DURATION_TICKS = 600;

    @Override
    public String getSkillId() {
        return "bs2_skill_grit";
    }

    @Override
    public float getManaCost() {
        return 30.0f;
    }

    @Override
    public int getBaseCooldownTicks() {
        return 600;
    }

    @Override
    public String getTranslationKey() {
        return "skill.blacksouls.bs2_skill_grit.name";
    }

    @Override
    public ChatFormatting getTextColor() {
        return ChatFormatting.RED;
    }

    @Override
    public boolean isUnlockedForGUI(Player player) {
        return SkillUtils.hasLearnedSkill(player, getSkillId());
    }

    @Override
    public void execute(ServerPlayer player, BSPlayerStats stats) {
        if (BlackSouls.BUFF_GRIT.isPresent()) {
            player.addEffect(new MobEffectInstance(BlackSouls.BUFF_GRIT.get(), DURATION_TICKS, 0, false, false, true));
        }
        player.sendSystemMessage(Component.translatable("message.blacksouls.skill.grit", player.getName().getString()).withStyle(ChatFormatting.WHITE));
    }
}
