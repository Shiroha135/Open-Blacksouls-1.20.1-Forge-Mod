package com.BlackSouls.BlackSoulsMod.util.skill;

import com.BlackSouls.BlackSoulsMod.BlackSouls;
import com.BlackSouls.BlackSoulsMod.capability.BSPlayerStats;
import com.BlackSouls.BlackSoulsMod.item.weapon.ItemClub;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;

@SuppressWarnings("removal")
public class SkillHeavyStrike extends AbstractBandersnatchSkill {

    @Override
    public String getSkillId() { return "bs2_skill_heavy_strike"; }

    @Override
    public float getManaCost() { return 0.0F; }

    @Override
    public int getBaseCooldownTicks() { return 600; }

    @Override
    public String getTranslationKey() { return "skill.blacksouls.bs2_skill_heavy_strike.name"; }

    @Override
    public ChatFormatting getTextColor() { return ChatFormatting.DARK_RED; }

    @Override
    public ResourceLocation getIcon() {
        return new ResourceLocation(BlackSouls.MODID, "textures/gui/skills/heavy_strike.png");
    }

    @Override
    public void execute(ServerPlayer player, BSPlayerStats stats) {
        LivingEntity target = findTarget(player, 8.0D);
        if (target == null) {
            player.sendSystemMessage(Component.translatable("message.blacksouls.skill.no_target").withStyle(ChatFormatting.GRAY));
            return;
        }

        player.sendSystemMessage(Component.translatable("message.blacksouls.skill.heavy_strike.use", player.getName().getString()).withStyle(ChatFormatting.WHITE));
        player.swing(InteractionHand.MAIN_HAND, true);
        playAnimation(target, 315);
        playSound(target, BlackSouls.WIND7_EVENT.get(), 0.5F);
        player.serverLevel().getServer().tell(new net.minecraft.server.TickTask(5, () -> {
            if (!target.isRemoved() && target.isAlive()) {
                playSound(target, BlackSouls.BLOW6_EVENT.get(), 0.5F);
                playSound(target, BlackSouls.DAMAGE4_EVENT.get(), 0.75F);
                if (applyFormulaHit(player, target, stats, 5.0D, 2.0D, 0.2D, true, false, 0.0D)) {
                    ItemClub.applyCrush(target, false);
                }
            }
        }));
    }
}
