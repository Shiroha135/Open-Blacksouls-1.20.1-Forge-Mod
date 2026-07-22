package com.BlackSouls.BlackSoulsMod.util.skill;

import com.BlackSouls.BlackSoulsMod.BlackSouls;
import com.BlackSouls.BlackSoulsMod.capability.BSPlayerStats;
import com.BlackSouls.BlackSoulsMod.handler.StatEventHandler;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;

public class SkillSpoonArt extends AbstractOriginalWeaponSkill {
    public SkillSpoonArt() {
        super(Family.SPOON, 5);
    }

    @Override public String getSkillId() { return "bs2_skill_delicious_turtle_soup"; }
    @Override public float getManaCost() { return 0.0F; }
    @Override public int getBaseCooldownTicks() { return 0; }
    @Override public String getTranslationKey() { return "skill.blacksouls.bs2_skill_delicious_turtle_soup.name"; }
    @Override public ChatFormatting getTextColor() { return ChatFormatting.GREEN; }
    @Override public ResourceLocation getIcon() { return new ResourceLocation(BlackSouls.MODID, "textures/gui/skills/delicious_turtle_soup.png"); }

    @Override
    public void execute(ServerPlayer player, BSPlayerStats stats) {
        player.sendSystemMessage(Component.translatable("message.blacksouls.skill.delicious_turtle_soup.use", player.getName().getString()).withStyle(ChatFormatting.WHITE));
        player.swing(InteractionHand.MAIN_HAND, true);
        playAnimation(player, 37);
        playSound(player, BlackSouls.ICE1_EVENT.get(), 0.5F);
        player.heal(player.getMaxHealth() * 0.5F);
        stats.mp = Math.min(stats.maxMp, stats.mp + stats.maxMp * 0.5D);
        StatEventHandler.syncToClient(player);
    }
}
