package com.BlackSouls.BlackSoulsMod.util.skill;

import com.BlackSouls.BlackSoulsMod.BlackSouls;
import com.BlackSouls.BlackSoulsMod.capability.BSPlayerStats;
import com.BlackSouls.BlackSoulsMod.handler.StatEventHandler;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;

@SuppressWarnings("removal")
public class SkillHideAndSeek extends AbstractDaggerSkill {

    @Override
    public String getSkillId() {
        return "bs2_skill_hide_and_seek";
    }

    @Override
    public float getManaCost() {
        return 4.0F;
    }

    @Override
    public int getBaseCooldownTicks() {
        return 600;
    }

    @Override
    public String getTranslationKey() {
        return "skill.blacksouls.bs2_skill_hide_and_seek.name";
    }

    @Override
    public ChatFormatting getTextColor() {
        return ChatFormatting.AQUA;
    }

    @Override
    public ResourceLocation getIcon() {
        return new ResourceLocation(BlackSouls.MODID, "textures/gui/skills/hide_and_seek.png");
    }

    @Override
    public void execute(ServerPlayer player, BSPlayerStats stats) {
        LivingEntity target = findTarget(player, 8.0D);
        if (target == null) {
            player.sendSystemMessage(Component.translatable("message.blacksouls.skill.no_target").withStyle(ChatFormatting.GRAY));
            return;
        }

        player.sendSystemMessage(Component.translatable(
                "message.blacksouls.skill.hide_and_seek.use",
                player.getName().getString()
        ).withStyle(ChatFormatting.WHITE));
        player.swing(InteractionHand.MAIN_HAND, true);
        playAnimation(target, 395);
        playSound(target, BlackSouls.SWORD5_EVENT.get(), 1.0F);
        playSound(target, BlackSouls.SWORD4_EVENT.get(), 1.0F);

        player.serverLevel().getServer().tell(new net.minecraft.server.TickTask(1, () -> {
            if (!target.isRemoved() && target.isAlive()) {
                playSound(target, BlackSouls.WIND1_EVENT.get(), 1.0F);
                applyHit(player, target, stats, 6.0D, true, true, 0.0D);
            }
            if (BlackSouls.BUFF_DAGGER_EVASION.isPresent()) {
                player.addEffect(new MobEffectInstance(BlackSouls.BUFF_DAGGER_EVASION.get(), 400, 0));
                StatEventHandler.applyStats(player);
                StatEventHandler.syncToClient(player);
            }
        }));
    }
}
