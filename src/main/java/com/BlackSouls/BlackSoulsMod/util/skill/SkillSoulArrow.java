package com.BlackSouls.BlackSoulsMod.util.skill;

import com.BlackSouls.BlackSoulsMod.BlackSouls;
import com.BlackSouls.BlackSoulsMod.capability.BSPlayerStats;
import com.BlackSouls.BlackSoulsMod.handler.StatEventHandler;
import com.BlackSouls.BlackSoulsMod.network.NetworkHandler;
import com.BlackSouls.BlackSoulsMod.network.packets.PacketPlayAnim;
import com.BlackSouls.BlackSoulsMod.util.SkillUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Optional;

@SuppressWarnings({"resource", "removal"})
public class SkillSoulArrow extends AbstractSkill {

    @Override
    public String getSkillId() { return "bs2_skill_soul_arrow"; }

    @Override
    public float getManaCost() { return 6.0f; }

    @Override
    public int getBaseCooldownTicks() { return 200; } 

    @Override
    public String getTranslationKey() { return "skill.blacksouls.bs2_skill_soul_arrow.name"; }

    @Override
    public ChatFormatting getTextColor() { return ChatFormatting.YELLOW; }

    @Override
    public ResourceLocation getIcon() { return new ResourceLocation(BlackSouls.MODID, "textures/gui/skills/original/soul_arrow.png"); }

    @Override
    public boolean isUnlockedForGUI(Player player) {
        return SkillUtils.hasLearnedSkill(player, getSkillId());
    }

    @Override
    public void execute(ServerPlayer player, BSPlayerStats stats) {
        LivingEntity target = findTarget(player, 12.0); 
        if (target == null) {
            player.sendSystemMessage(Component.translatable("message.blacksouls.skill.no_target").withStyle(ChatFormatting.GRAY));
            return;
        }

        player.swing(InteractionHand.MAIN_HAND, true);

        player.sendSystemMessage(Component.translatable("message.blacksouls.skill.soul_arrow.use", player.getName().getString()).withStyle(ChatFormatting.WHITE));

        playAnim(target, 75);

        net.minecraft.server.MinecraftServer server = player.serverLevel().getServer();
        // 第 0 帧 (0 ms): Ice7 + Ice2
        server.tell(new net.minecraft.server.TickTask(0, () -> {
            if (!target.isRemoved()) {
                playSkillSound(target, BlackSouls.ICE7_EVENT.get(), 1.0f, 1.0f);
                playSkillSound(target, BlackSouls.ICE2_EVENT.get(), 1.0f, 1.0f);
            }
        }));
        // 第 2 帧 (132 ms): Ice2
        server.tell(new net.minecraft.server.TickTask(Math.max(1, (int) Math.round(132 / 50.0)), () -> {
            if (!target.isRemoved()) playSkillSound(target, BlackSouls.ICE2_EVENT.get(), 1.0f, 1.0f);
        }));
        // 第 4 帧 (264 ms): Ice2
        server.tell(new net.minecraft.server.TickTask(Math.max(1, (int) Math.round((132 + 132) / 50.0)), () -> {
            if (!target.isRemoved()) playSkillSound(target, BlackSouls.ICE2_EVENT.get(), 1.0f, 1.0f);
        }));
        // 第 6 帧 (396 ms): Ice2 并 结算伤害！
        server.tell(new net.minecraft.server.TickTask(Math.max(1, (int) Math.round((132 + 132 + 132) / 50.0)), () -> {
            if (!target.isRemoved()) {
                playSkillSound(target, BlackSouls.ICE2_EVENT.get(), 1.0f, 1.0f);
                applyMagicHit(player, target, stats);
            }
        }));
    }

    private void playSkillSound(LivingEntity target, SoundEvent sound, float volume, float pitch) {
        target.level().playSound(null, target.getX(), target.getY() + target.getBbHeight() / 2.0, target.getZ(), sound, SoundSource.PLAYERS, volume, pitch);
    }

    private void playAnim(LivingEntity target, int animId) {
        PacketPlayAnim animPacket = new PacketPlayAnim(animId, target.getX(), target.getY() + target.getBbHeight() / 2.0F, target.getZ());
        NetworkHandler.sendToAllAround(animPacket, target);
    }

    private void applyMagicHit(ServerPlayer player, LivingEntity target, BSPlayerStats stats) {
        if (target == null || target.isRemoved() || target.getHealth() <= 0) return;

        // 获取敌人的【魔法防御力】
        double bMdf = StatEventHandler.getRpgMagicDefense(target);

        // 🌟 RMVA 伤害公式：a.mat * 5 - b.mdf * 2
        double rawDamage = (stats.magicAttack * 5.0) - (bMdf * 2.0);
        if (rawDamage < 1.0) rawDamage = 1.0;

        // 🌟 魔法属性克制 (光属性)
        java.util.List<String> attackAttrs = new java.util.ArrayList<>();
        attackAttrs.add(com.BlackSouls.BlackSoulsMod.util.BSAttributeManager.LIGHT); // 添加光属性标签
        float elementalMultiplier = com.BlackSouls.BlackSoulsMod.util.BSAttributeManager.getBestMultiplier(target, attackAttrs);
        rawDamage *= elementalMultiplier;

        // 🌟 20% 离散度 (0.8 ~ 1.2)
        double variance = 0.8 + (Math.random() * 0.4);
        rawDamage *= variance;

        // 判定暴击
        float finalDamage = StatEventHandler.rollSkillCrit(player, (float) rawDamage);

        target.invulnerableTime = 0;
        DamageSource source = player.damageSources().indirectMagic(player, player);
        target.hurt(source, finalDamage);
    }

    private LivingEntity findTarget(Player player, double range) {
        LivingEntity target = null;
        double closest = Double.MAX_VALUE;
        Vec3 eyePos = player.getEyePosition(1.0F);
        Vec3 lookVec = player.getLookAngle();
        Vec3 targetVec = eyePos.add(lookVec.x * range, lookVec.y * range, lookVec.z * range);

        List<Entity> list = player.level().getEntities(player, player.getBoundingBox().inflate(range));
        for (Entity e : list) {
            if (e instanceof LivingEntity le && le.isAlive() && !le.isSpectator()) {
                AABB aabb = e.getBoundingBox().inflate(0.5);
                if (aabb.contains(eyePos)) return le;
                Optional<Vec3> result = aabb.clip(eyePos, targetVec);
                if (result.isPresent()) {
                    double dist = eyePos.distanceToSqr(result.get());
                    if (dist < closest) { closest = dist; target = le; }
                }
            }
        }
        return target;
    }
}
