package com.BlackSouls.BlackSoulsMod.util.skill;

import com.BlackSouls.BlackSoulsMod.BlackSouls;
import com.BlackSouls.BlackSoulsMod.capability.BSPlayerStats;
import com.BlackSouls.BlackSoulsMod.handler.StatEventHandler;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;

public class SkillShadowless extends AbstractDaggerSkill {

    @Override
    protected boolean isGreatDaggerOnly() {
        return true;
    }

    @Override
    public String getSkillId() {
        return "bs2_skill_shadowless";
    }

    @Override
    public float getManaCost() {
        return 5.0F;
    }

    @Override
    public int getBaseCooldownTicks() {
        return 0;
    }

    @Override
    public String getTranslationKey() {
        return "skill.blacksouls.bs2_skill_shadowless.name";
    }

    @Override
    public ChatFormatting getTextColor() {
        return ChatFormatting.LIGHT_PURPLE;
    }

    @Override
    public ResourceLocation getIcon() {
        return new ResourceLocation(BlackSouls.MODID, "textures/gui/skills/shadowless.png");
    }

    @Override
    public void execute(ServerPlayer player, BSPlayerStats stats) {
        player.sendSystemMessage(Component.translatable(
                "message.blacksouls.skill.shadowless.use",
                player.getName().getString()
        ).withStyle(ChatFormatting.WHITE));
        playAnimation(player, 31);
        playSound(player, BlackSouls.FOG1_EVENT.get(), 1.0F);
        playSound(player, BlackSouls.SAND_EVENT.get(), 1.1F);

        if (BlackSouls.BUFF_DAGGER_EVASION.isPresent()) {
            player.addEffect(new MobEffectInstance(BlackSouls.BUFF_DAGGER_EVASION.get(), 400, 0));
        }
        if (BlackSouls.BUFF_DAGGER_GUARD.isPresent()) {
            player.addEffect(new MobEffectInstance(BlackSouls.BUFF_DAGGER_GUARD.get(), 400, 0));
        }
        StatEventHandler.applySpeedUp(player, 400);
        StatEventHandler.applySpeedUp(player, 400);
        StatEventHandler.applyStats(player);
        StatEventHandler.syncToClient(player);
    }
}
