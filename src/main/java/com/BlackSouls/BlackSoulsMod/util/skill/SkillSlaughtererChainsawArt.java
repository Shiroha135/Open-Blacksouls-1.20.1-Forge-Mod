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

public class SkillSlaughtererChainsawArt extends AbstractOriginalWeaponSkill {
    public enum Art { BLOOD_TRIAL, BLESSING_OF_PAIN, SLAUGHTER_BEGINS }
    private final Art art;

    public SkillSlaughtererChainsawArt(Art art) {
        super(Family.CHAINSAW, art == Art.SLAUGHTER_BEGINS ? 5 : 0);
        this.art = art;
    }

    @Override public String getSkillId() { return switch (art) {
        case BLOOD_TRIAL -> "bs2_skill_blood_trial";
        case BLESSING_OF_PAIN -> "bs2_skill_blessing_of_pain";
        case SLAUGHTER_BEGINS -> "bs2_skill_slaughter_begins";
    }; }
    @Override public float getManaCost() { return switch (art) {
        case BLOOD_TRIAL -> 8.0F;
        case BLESSING_OF_PAIN, SLAUGHTER_BEGINS -> 10.0F;
    }; }
    @Override public int getBaseCooldownTicks() { return art == Art.SLAUGHTER_BEGINS ? 4000 : 600; }
    @Override public String getTranslationKey() { return "skill.blacksouls." + getSkillId() + ".name"; }
    @Override public ChatFormatting getTextColor() { return ChatFormatting.RED; }
    @Override public ResourceLocation getIcon() { return new ResourceLocation(BlackSouls.MODID,
            "textures/gui/skills/" + (art == Art.SLAUGHTER_BEGINS ? "slaughter_begins.png" : "chainsaw_art.png")); }

    @Override
    public void execute(ServerPlayer player, BSPlayerStats stats) {
        if (art == Art.SLAUGHTER_BEGINS) {
            player.sendSystemMessage(Component.translatable("message.blacksouls.skill.slaughter_begins.use", player.getName().getString()).withStyle(ChatFormatting.WHITE));
            player.swing(InteractionHand.MAIN_HAND, true);
            playAnimation(player, 34);
            playSound(player, BlackSouls.MONSTER1_EVENT.get(), 1.0F);
            player.addEffect(new MobEffectInstance(BlackSouls.BUFF_SLAUGHTER_MODE.get(), 2000, 0));
            return;
        }
        LivingEntity target = findTarget(player, 10.0D);
        if (target == null) {
            player.sendSystemMessage(Component.translatable("message.blacksouls.skill.no_target").withStyle(ChatFormatting.GRAY));
            return;
        }
        String key = art == Art.BLOOD_TRIAL ? "blood_trial" : "blessing_of_pain";
        player.sendSystemMessage(Component.translatable("message.blacksouls.skill." + key + ".use", player.getName().getString()).withStyle(ChatFormatting.WHITE));
        player.swing(InteractionHand.MAIN_HAND, true);
        int hits = player.hasEffect(BlackSouls.BUFF_SLAUGHTER_MODE.get()) ? 4 : 1;
        for (int hit = 0; hit < hits; hit++) {
            int delay = hit * 3;
            schedule(player, delay, () -> executeHit(player, target, stats));
        }
    }

    private void executeHit(ServerPlayer player, LivingEntity target, BSPlayerStats stats) {
        if (target.isRemoved() || !target.isAlive()) return;
        playAnimation(target, 384);
        playSound(target, BlackSouls.CHAINSAW_REV_EVENT.get(), 1.2F);
        playSound(target, BlackSouls.GUCHA004A_EVENT.get(), 1.5F);
        double rawDamage = stats.attack * 4.0D - StatEventHandler.getRpgPhysicalDefense(target) * 2.0D;
        if (art == Art.BLESSING_OF_PAIN) {
            double hpRatioMultiplier = Math.min(3.0D, target.getMaxHealth() / Math.max(1.0D, target.getHealth()));
            rawDamage *= hpRatioMultiplier;
        }
        rawDamage *= 0.8D + Math.random() * 0.4D;
        if (applyRawHit(player, target, rawDamage, true, art == Art.BLOOD_TRIAL, 0.0D)
                && art == Art.BLOOD_TRIAL && Math.random() < 0.30D) {
            target.addEffect(new MobEffectInstance(BlackSouls.BUFF_BLEEDING.get(), 600, 0));
        }
    }

    private void schedule(ServerPlayer player, int delay, Runnable task) {
        net.minecraft.server.MinecraftServer server = player.serverLevel().getServer();
        server.tell(new net.minecraft.server.TickTask(server.getTickCount() + delay, task));
    }
}
