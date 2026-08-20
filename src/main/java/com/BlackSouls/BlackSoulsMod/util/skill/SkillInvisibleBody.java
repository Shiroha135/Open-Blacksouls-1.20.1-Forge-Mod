package com.BlackSouls.BlackSoulsMod.util.skill;

import com.BlackSouls.BlackSoulsMod.BlackSouls;
import com.BlackSouls.BlackSoulsMod.capability.BSPlayerStats;
import com.BlackSouls.BlackSoulsMod.util.SkillUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;

@SuppressWarnings("removal")
public class SkillInvisibleBody extends AbstractSkill {

    private static final int DURATION_TICKS = 800;

    @Override
    public String getSkillId() {
        return "bs2_skill_invisible_body";
    }

    @Override
    public float getManaCost() {
        return 20.0f;
    }

    @Override
    public int getBaseCooldownTicks() {
        return 200;
    }

    @Override
    public String getTranslationKey() {
        return "skill.blacksouls.bs2_skill_invisible_body.name";
    }

    @Override
    public ChatFormatting getTextColor() {
        return ChatFormatting.GRAY;
    }

    @Override
    public ResourceLocation getIcon() {
        return new ResourceLocation(BlackSouls.MODID, "textures/gui/skills/original/invisible_body.png");
    }

    @Override
    public boolean isUnlockedForGUI(Player player) {
        return SkillUtils.hasLearnedSkill(player, getSkillId());
    }

    @Override
    public void execute(ServerPlayer player, BSPlayerStats stats) {
        if (BlackSouls.BUFF_INVISIBLE_BODY.isPresent()) {
            player.addEffect(new MobEffectInstance(BlackSouls.BUFF_INVISIBLE_BODY.get(), DURATION_TICKS, 0, false, false, true));
        }
        player.sendSystemMessage(Component.translatable("message.blacksouls.skill.invisible_on", player.getName().getString()).withStyle(ChatFormatting.WHITE));
    }
}
