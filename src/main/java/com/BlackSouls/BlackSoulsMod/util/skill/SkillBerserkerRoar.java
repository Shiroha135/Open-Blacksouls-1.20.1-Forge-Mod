package com.BlackSouls.BlackSoulsMod.util.skill;

import com.BlackSouls.BlackSoulsMod.BlackSouls;
import com.BlackSouls.BlackSoulsMod.capability.BSPlayerStats;
import com.BlackSouls.BlackSoulsMod.handler.StatEventHandler;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;

@SuppressWarnings("removal")
public class SkillBerserkerRoar extends AbstractGreatSwordSkill {

    @Override
    public String getSkillId() { return "bs2_skill_berserker_roar"; }

    @Override
    public float getManaCost() { return 10.0F; }

    @Override
    public int getBaseCooldownTicks() { return 4000; }

    @Override
    public String getTranslationKey() { return "skill.blacksouls.bs2_skill_berserker_roar.name"; }

    @Override
    public ChatFormatting getTextColor() { return ChatFormatting.RED; }

    @Override
    public ResourceLocation getIcon() {
        return new ResourceLocation(BlackSouls.MODID, "textures/gui/skills/berserker_roar.png");
    }

    @Override
    public void execute(ServerPlayer player, BSPlayerStats stats) {
        player.sendSystemMessage(Component.translatable("message.blacksouls.skill.berserker_roar.use", player.getName().getString()).withStyle(ChatFormatting.WHITE));
        playAnimation(player, 34);
        playSound(player, BlackSouls.MONSTER1_EVENT.get(), 1.0F);
        if (BlackSouls.BUFF_BERSERK.isPresent()) {
            player.addEffect(new MobEffectInstance(BlackSouls.BUFF_BERSERK.get(), 2000, 0));
            StatEventHandler.applyStats(player);
            StatEventHandler.syncToClient(player);
        }
    }
}
