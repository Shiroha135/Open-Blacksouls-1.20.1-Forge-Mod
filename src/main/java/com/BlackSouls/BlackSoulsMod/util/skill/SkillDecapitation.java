package com.BlackSouls.BlackSoulsMod.util.skill;

import com.BlackSouls.BlackSoulsMod.BlackSouls;
import com.BlackSouls.BlackSoulsMod.capability.BSPlayerStats;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;

public class SkillDecapitation extends AbstractDaggerSkill {

    @Override
    public String getSkillId() {
        return "bs2_skill_decapitation";
    }

    @Override
    public float getManaCost() {
        return 10.0F;
    }

    @Override
    public int getBaseCooldownTicks() {
        return 600;
    }

    @Override
    public String getTranslationKey() {
        return "skill.blacksouls.bs2_skill_decapitation.name";
    }

    @Override
    public ChatFormatting getTextColor() {
        return ChatFormatting.DARK_RED;
    }

    @Override
    public ResourceLocation getIcon() {
        return new ResourceLocation(BlackSouls.MODID, "textures/gui/skills/decapitation.png");
    }

    @Override
    public void execute(ServerPlayer player, BSPlayerStats stats) {
        LivingEntity target = findTarget(player, 8.0D);
        if (target == null) {
            player.sendSystemMessage(Component.translatable("message.blacksouls.skill.no_target").withStyle(ChatFormatting.GRAY));
            return;
        }

        player.sendSystemMessage(Component.translatable(
                "message.blacksouls.skill.decapitation.use",
                player.getName().getString()
        ).withStyle(ChatFormatting.WHITE));
        player.swing(InteractionHand.MAIN_HAND, true);
        playAnimation(target, 142);
        playSound(target, BlackSouls.BLIND_EVENT.get(), 1.0F);

        player.serverLevel().getServer().tell(new net.minecraft.server.TickTask(17, () -> {
            if (!target.isRemoved() && target.isAlive()) {
                playSound(target, BlackSouls.DARKNESS7_EVENT.get(), 1.0F);
                playSound(target, BlackSouls.SWORD4_EVENT.get(), 1.0F);
                playSound(target, BlackSouls.SLASH9_EVENT.get(), 1.0F);
                playSound(target, BlackSouls.SWORD5_EVENT.get(), 1.0F);
                applyHit(player, target, stats, 3.0D, false, false, 30.0D);
            }
        }));
    }
}
