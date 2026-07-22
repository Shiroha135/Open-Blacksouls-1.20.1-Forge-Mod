package com.BlackSouls.BlackSoulsMod.util.skill;

import com.BlackSouls.BlackSoulsMod.BlackSouls;
import com.BlackSouls.BlackSoulsMod.capability.BSPlayerStats;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;

import java.util.List;

public class SkillGlachidArt extends AbstractOriginalWeaponSkill {
    public enum Art { LAKE_GOD_APOCALYPSE, GREEN_COLLAPSE }
    private final Art art;

    public SkillGlachidArt(Art art) {
        super(Family.GLACHID, art == Art.GREEN_COLLAPSE ? 5 : 0);
        this.art = art;
    }

    @Override public String getSkillId() { return art == Art.LAKE_GOD_APOCALYPSE ? "bs2_skill_lake_god_apocalypse" : "bs2_skill_green_collapse"; }
    @Override public float getManaCost() { return art == Art.LAKE_GOD_APOCALYPSE ? 15.0F : 18.0F; }
    @Override public int getBaseCooldownTicks() { return art == Art.LAKE_GOD_APOCALYPSE ? 800 : 600; }
    @Override public String getTranslationKey() { return "skill.blacksouls." + getSkillId() + ".name"; }
    @Override public ChatFormatting getTextColor() { return ChatFormatting.GREEN; }
    @Override public ResourceLocation getIcon() { return new ResourceLocation(BlackSouls.MODID, "textures/gui/skills/glachid.png"); }

    @Override
    public void execute(ServerPlayer player, BSPlayerStats stats) {
        List<LivingEntity> targets = findTargets(player, 14.0D, Integer.MAX_VALUE);
        if (targets.isEmpty()) {
            player.sendSystemMessage(Component.translatable("message.blacksouls.skill.no_target").withStyle(ChatFormatting.GRAY));
            return;
        }
        String key = art == Art.LAKE_GOD_APOCALYPSE ? "lake_god_apocalypse" : "green_collapse";
        player.sendSystemMessage(Component.translatable("message.blacksouls.skill." + key + ".use", player.getName().getString()).withStyle(ChatFormatting.WHITE));
        player.swing(InteractionHand.MAIN_HAND, true);
        if (art == Art.LAKE_GOD_APOCALYPSE) executeApocalypse(player, targets, stats.attack);
        else executeGreenCollapse(player, targets, stats.attack);
    }

    private void executeApocalypse(ServerPlayer player, List<LivingEntity> targets, double attack) {
        LivingEntity primary = targets.get(0);
        playAnimation(primary, 380);
        playSound(primary, BlackSouls.ATTACK3_EVENT.get(), 1.0F);
        schedule(player, 3, () -> {
            playSound(primary, BlackSouls.SKILL3_EVENT.get(), 1.5F);
            playSound(primary, BlackSouls.SLASH8_EVENT.get(), 1.0F);
            hit(player, primary, attack * 4.0D, true);
        });
        for (int strike = 0; strike < 2; strike++) {
            int delay = 7 + strike * 4;
            schedule(player, delay, () -> {
                LivingEntity target = targets.get(player.getRandom().nextInt(targets.size()));
                playAnimation(target, 379);
                playSound(target, BlackSouls.ATTACK3_EVENT.get(), 1.0F);
                playSound(target, BlackSouls.SLASH8_EVENT.get(), 1.0F);
                playSound(target, BlackSouls.SLASH3_EVENT.get(), 1.0F);
                hit(player, target, attack * 4.0D, true);
            });
        }
    }

    private void executeGreenCollapse(ServerPlayer player, List<LivingEntity> targets, double attack) {
        targets.forEach(target -> playAnimation(target, 381));
        playSound(player, BlackSouls.BATTLE3_EVENT.get(), 1.0F);
        playSound(player, BlackSouls.WIND8_EVENT.get(), 1.0F);
        schedule(player, 3, () -> playSound(player, BlackSouls.SLASH11_EVENT.get(), 1.0F));
        schedule(player, 10, () -> {
            for (LivingEntity target : targets) hit(player, target, attack * 6.0D, false);
        });
        schedule(player, 200, () -> {
            for (LivingEntity target : targets) {
                if (target.isRemoved() || !target.isAlive()) continue;
                playAnimation(target, 382);
                playSound(target, BlackSouls.FOG2_EVENT.get(), 1.0F);
                playSound(target, BlackSouls.POLLEN_EVENT.get(), 1.3F);
                hit(player, target, attack * 6.0D, true);
            }
        });
    }

    private void hit(ServerPlayer player, LivingEntity target, double rawDamage, boolean sureHit) {
        rawDamage *= 0.8D + Math.random() * 0.4D;
        applyRawHit(player, target, rawDamage, true, sureHit, 0.0D);
    }

    private void schedule(ServerPlayer player, int delay, Runnable task) {
        net.minecraft.server.MinecraftServer server = player.serverLevel().getServer();
        server.tell(new net.minecraft.server.TickTask(server.getTickCount() + delay, task));
    }
}
