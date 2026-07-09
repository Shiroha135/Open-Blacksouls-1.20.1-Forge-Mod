package com.BlackSouls.BlackSoulsMod.util.skill;

import com.BlackSouls.BlackSoulsMod.BlackSouls;
import com.BlackSouls.BlackSoulsMod.capability.BSPlayerStats;
import com.BlackSouls.BlackSoulsMod.util.SkillUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;

public class SkillRequiem extends AbstractSkill {

    private static final int DURATION_TICKS = 600;

    @Override
    public String getSkillId() {
        return "bs2_skill_requiem";
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
        return "skill.blacksouls.bs2_skill_requiem.name";
    }

    @Override
    public ChatFormatting getTextColor() {
        return ChatFormatting.GOLD;
    }

    @Override
    public boolean isUnlockedForGUI(Player player) {
        return SkillUtils.hasLearnedSkill(player, getSkillId());
    }

    @Override
    public void execute(ServerPlayer player, BSPlayerStats stats) {
        if (BlackSouls.BUFF_REQUIEM.isPresent()) {
            player.addEffect(new MobEffectInstance(BlackSouls.BUFF_REQUIEM.get(), DURATION_TICKS, 0, false, false, true));
        }

        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                BlackSouls.SAND_EVENT.get(), SoundSource.PLAYERS, 0.8F, 1.0F);
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                BlackSouls.SAINT7_EVENT.get(), SoundSource.PLAYERS, 1.0F, 1.0F);

        player.sendSystemMessage(Component.translatable("message.blacksouls.skill.requiem", player.getName().getString()).withStyle(ChatFormatting.WHITE));
    }
}
