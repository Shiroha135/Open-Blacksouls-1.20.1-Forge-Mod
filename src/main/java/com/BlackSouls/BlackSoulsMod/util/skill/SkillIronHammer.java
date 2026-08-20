package com.BlackSouls.BlackSoulsMod.util.skill;

import com.BlackSouls.BlackSoulsMod.BlackSouls;
import com.BlackSouls.BlackSoulsMod.capability.BSPlayerStats;
import com.BlackSouls.BlackSoulsMod.handler.StatEventHandler;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;

@SuppressWarnings("removal")
public class SkillIronHammer extends AbstractGreatSwordSkill {

    @Override
    protected boolean isGiantSwordOnly() { return true; }

    @Override
    public String getSkillId() { return "bs2_skill_iron_hammer"; }

    @Override
    public float getManaCost() { return 30.0F; }

    @Override
    public int getBaseCooldownTicks() { return 1600; }

    @Override
    public String getTranslationKey() { return "skill.blacksouls.bs2_skill_iron_hammer.name"; }

    @Override
    public ChatFormatting getTextColor() { return ChatFormatting.DARK_RED; }

    @Override
    public ResourceLocation getIcon() {
        return new ResourceLocation(BlackSouls.MODID, "textures/gui/skills/iron_hammer.png");
    }

    @Override
    public void execute(ServerPlayer player, BSPlayerStats stats) {
        LivingEntity target = findTarget(player, 8.0D);
        if (target == null) {
            player.sendSystemMessage(Component.translatable("message.blacksouls.skill.no_target").withStyle(ChatFormatting.GRAY));
            return;
        }

        player.sendSystemMessage(Component.translatable("message.blacksouls.skill.iron_hammer.use", player.getName().getString()).withStyle(ChatFormatting.WHITE));
        player.swing(InteractionHand.MAIN_HAND, true);
        playAnimation(target, 143);
        playSound(target, BlackSouls.SLASH2_EVENT.get(), 1.0F);

        player.serverLevel().getServer().tell(new net.minecraft.server.TickTask(3, () -> {
            if (!target.isRemoved() && target.isAlive()) {
                playSound(target, BlackSouls.SLASH9_EVENT.get(), 1.0F);
                playSound(target, BlackSouls.BREAK_EVENT.get(), 0.5F);
                double rawDamage = target.getHealth() * 0.25D + stats.attack * 6.0D
                        - StatEventHandler.getRpgPhysicalDefense(target) * 2.0D;
                applyRawHit(player, target, rawDamage, false, false, 0.0D);
            }
        }));
    }
}
