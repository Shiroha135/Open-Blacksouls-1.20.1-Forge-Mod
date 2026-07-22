package com.BlackSouls.BlackSoulsMod.util.skill;

import com.BlackSouls.BlackSoulsMod.BlackSouls;
import com.BlackSouls.BlackSoulsMod.capability.BSPlayerStats;
import com.BlackSouls.BlackSoulsMod.handler.StatEventHandler;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;

import java.util.ArrayList;
import java.util.List;

public class SkillDeepSeaAnchorArt extends AbstractOriginalWeaponSkill {
    public enum Art { CRUSHING_WATER, RAGE }
    private final Art art;

    public SkillDeepSeaAnchorArt(Art art) {
        super(Family.DEEP_SEA_ANCHOR, art == Art.RAGE ? 5 : 0);
        this.art = art;
    }

    @Override public String getSkillId() { return art == Art.CRUSHING_WATER ? "bs2_skill_crushing_water" : "bs2_skill_rage"; }
    @Override public float getManaCost() { return art == Art.CRUSHING_WATER ? 25.0F : 50.0F; }
    @Override public int getBaseCooldownTicks() { return art == Art.CRUSHING_WATER ? 0 : 1000; }
    @Override public String getTranslationKey() { return "skill.blacksouls." + getSkillId() + ".name"; }
    @Override public ChatFormatting getTextColor() { return ChatFormatting.DARK_AQUA; }
    @Override public ResourceLocation getIcon() { return new ResourceLocation(BlackSouls.MODID,
            "textures/gui/skills/" + (art == Art.CRUSHING_WATER ? "crushing_water.png" : "rage.png")); }

    @Override
    public void execute(ServerPlayer player, BSPlayerStats stats) {
        if (art == Art.RAGE) {
            executeRage(player);
            return;
        }
        List<LivingEntity> targets = findTargets(player, 14.0D, Integer.MAX_VALUE);
        if (targets.isEmpty()) {
            player.sendSystemMessage(Component.translatable("message.blacksouls.skill.no_target").withStyle(ChatFormatting.GRAY));
            return;
        }
        player.sendSystemMessage(Component.translatable("message.blacksouls.skill.crushing_water.use", player.getName().getString()).withStyle(ChatFormatting.WHITE));
        player.swing(InteractionHand.MAIN_HAND, true);
        targets.forEach(target -> playAnimation(target, 356));
        playSound(player, BlackSouls.DIVE_EVENT.get(), 1.0F);
        playSound(player, BlackSouls.WATER1_EVENT.get(), 0.5F);
        playSound(player, BlackSouls.POISON_EVENT.get(), 0.5F);
        playSound(player, BlackSouls.WATER6_EVENT.get(), 1.0F);
        schedule(player, 4, () -> playSound(player, BlackSouls.DIVE_EVENT.get(), 0.85F));
        schedule(player, 8, () -> playSound(player, BlackSouls.DIVE_EVENT.get(), 0.9F));
        schedule(player, 8, () -> {
            for (LivingEntity target : targets) {
                double rawDamage = stats.magicAttack * 4.0D * (0.8D + Math.random() * 0.4D);
                if (applyRawHit(player, target, rawDamage, true, true, 0.0D)) {
                    target.addEffect(new MobEffectInstance(BlackSouls.BUFF_FEAR.get(), 1000, 0));
                    target.removeEffect(BlackSouls.BUFF_DAGGER_EVASION.get());
                    target.removeEffect(BlackSouls.BUFF_QUICK_RELOAD.get());
                }
            }
        });
    }

    private void executeRage(ServerPlayer player) {
        player.sendSystemMessage(Component.translatable("message.blacksouls.skill.rage.use", player.getName().getString()).withStyle(ChatFormatting.WHITE));
        player.swing(InteractionHand.MAIN_HAND, true);
        playAnimation(player, 358);
        playSound(player, BlackSouls.WATER6_EVENT.get(), 1.0F);
        playSound(player, BlackSouls.SAND_EVENT.get(), 0.7F);
        for (MobEffectInstance effect : new ArrayList<>(player.getActiveEffects())) {
            if (effect.getEffect().getCategory() == MobEffectCategory.HARMFUL) player.removeEffect(effect.getEffect());
        }
        player.addEffect(new MobEffectInstance(BlackSouls.BUFF_HIGH_MOBILITY.get(), 1000, 0));
        StatEventHandler.applyStats(player);
        StatEventHandler.syncToClient(player);
    }

    private void schedule(ServerPlayer player, int delay, Runnable task) {
        net.minecraft.server.MinecraftServer server = player.serverLevel().getServer();
        server.tell(new net.minecraft.server.TickTask(server.getTickCount() + delay, task));
    }
}
