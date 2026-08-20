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
public class SkillStruggle extends AbstractClubSkill {

    @Override
    public String getSkillId() { return "bs2_skill_struggle"; }

    @Override
    public float getManaCost() { return 4.0F; }

    @Override
    public int getBaseCooldownTicks() { return 600; }

    @Override
    public String getTranslationKey() { return "skill.blacksouls.bs2_skill_struggle.name"; }

    @Override
    public ChatFormatting getTextColor() { return ChatFormatting.YELLOW; }

    @Override
    public ResourceLocation getIcon() {
        return new ResourceLocation(BlackSouls.MODID, "textures/gui/skills/struggle.png");
    }

    @Override
    public void execute(ServerPlayer player, BSPlayerStats stats) {
        player.sendSystemMessage(Component.translatable("message.blacksouls.skill.struggle.use", player.getName().getString()).withStyle(ChatFormatting.WHITE));
        playAnimation(player, 95);
        playSound(player, BlackSouls.PUSH_EVENT.get(), 1.0F);
        player.serverLevel().getServer().tell(new net.minecraft.server.TickTask(8, () ->
                playSound(player, BlackSouls.RAISE1_EVENT.get(), 1.0F)));
        if (BlackSouls.BUFF_STRUGGLE.isPresent()) {
            player.addEffect(new MobEffectInstance(BlackSouls.BUFF_STRUGGLE.get(), 600, 0));
            StatEventHandler.applyStats(player);
            StatEventHandler.syncToClient(player);
        }
    }
}
