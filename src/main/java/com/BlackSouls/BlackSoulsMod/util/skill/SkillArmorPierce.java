package com.BlackSouls.BlackSoulsMod.util.skill;

import com.BlackSouls.BlackSoulsMod.BlackSouls;
import com.BlackSouls.BlackSoulsMod.capability.BSPlayerStats;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;

@SuppressWarnings("removal")
public class SkillArmorPierce extends AbstractSpearSkill {

    @Override
    public String getSkillId() { return "bs2_skill_armor_pierce"; }

    @Override
    public float getManaCost() { return 12.0F; }

    @Override
    public int getBaseCooldownTicks() { return 600; }

    @Override
    public String getTranslationKey() { return "skill.blacksouls.bs2_skill_armor_pierce.name"; }

    @Override
    public ChatFormatting getTextColor() { return ChatFormatting.AQUA; }

    @Override
    public ResourceLocation getIcon() {
        return new ResourceLocation(BlackSouls.MODID, "textures/gui/skills/armor_pierce.png");
    }

    @Override
    public void execute(ServerPlayer player, BSPlayerStats stats) {
        LivingEntity target = findTarget(player, 8.0D);
        if (target == null) {
            player.sendSystemMessage(Component.translatable("message.blacksouls.skill.no_target").withStyle(ChatFormatting.GRAY));
            return;
        }

        player.sendSystemMessage(Component.translatable("message.blacksouls.skill.armor_pierce.use", player.getName().getString()).withStyle(ChatFormatting.WHITE));
        player.swing(InteractionHand.MAIN_HAND, true);
        playAnimation(target, 137);
        playSound(target, BlackSouls.ATTACK3_EVENT.get(), 1.0F);

        player.serverLevel().getServer().tell(new net.minecraft.server.TickTask(4, () -> {
            if (!target.isRemoved() && target.isAlive()) {
                playSound(target, BlackSouls.ICE11_EVENT.get(), 1.0F);
                applyFormulaHit(player, target, stats, 5.0D, 0.0D, 0.2D, true, false, 0.0D);
            }
        }));
    }
}
