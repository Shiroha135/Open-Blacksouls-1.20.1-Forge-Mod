package com.BlackSouls.BlackSoulsMod.util.skill;

import com.BlackSouls.BlackSoulsMod.BlackSouls;
import com.BlackSouls.BlackSoulsMod.capability.BSPlayerStats;
import com.BlackSouls.BlackSoulsMod.handler.StatEventHandler;
import com.BlackSouls.BlackSoulsMod.network.NetworkHandler;
import com.BlackSouls.BlackSoulsMod.network.packets.PacketPlayAnim;
import com.BlackSouls.BlackSoulsMod.util.SkillUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.List;

public class SkillSoulRadiation extends AbstractSkill {

    @Override
    public String getSkillId() { return "bs2_skill_soul_radiation"; }

    @Override
    public float getManaCost() { return 12.0f; }

    @Override
    public int getBaseCooldownTicks() { return 200; } 

    @Override
    public String getTranslationKey() { return "skill.blacksouls.bs2_skill_soul_radiation.name"; }

    @Override
    public ChatFormatting getTextColor() { return ChatFormatting.GOLD; }

    @Override
    public boolean isUnlockedForGUI(Player player) {
        return SkillUtils.hasLearnedSkill(player, getSkillId());
    }

    @Override
    public void execute(ServerPlayer player, BSPlayerStats stats) {
        // 范围索敌：15格超大范围内的所有敌对生物
        List<LivingEntity> targets = getEnemiesInRange(player, 15.0);
        if (targets.isEmpty()) {
            player.sendSystemMessage(Component.translatable("message.blacksouls.skill.no_target").withStyle(ChatFormatting.GRAY));
            return;
        }

        player.swing(InteractionHand.MAIN_HAND, true);

        // 白字播报台词
        player.sendSystemMessage(Component.translatable("message.blacksouls.skill.soul_radiation.use", player.getName().getString()).withStyle(ChatFormatting.WHITE));

        // 给所有命中的目标发送 76 号动画包 (076:神圣_全体007)
        for (LivingEntity target : targets) {
            playAnim(target, 76);
        }

        // 开启硬核 RMVA 踩点引擎！
        net.minecraft.server.MinecraftServer server = player.serverLevel().getServer();
        // 第 1 帧 (0 ms): Flash3, Skill1
        server.tell(new net.minecraft.server.TickTask(0, () -> {
            for (LivingEntity target : targets) {
                playSkillSound(target, BlackSouls.FLASH3_EVENT.get(), 1.0f, 1.0f);
                playSkillSound(target, BlackSouls.SKILL1_EVENT.get(), 1.0f, 1.0f);
            }
        }));
        // 第 12 帧 (距离第1帧过了 11 帧, 11 * 66 = 726 ms): Explosion3, Saint7
        int srDelay = 726;
        server.tell(new net.minecraft.server.TickTask(Math.max(1, (int) Math.round(srDelay / 50.0)), () -> {
            for (LivingEntity target : targets) {
                if (!target.isRemoved()) {
                    playSkillSound(target, BlackSouls.EXPLOSION3_EVENT.get(), 1.0f, 1.0f);
                    playSkillSound(target, BlackSouls.SAINT7_EVENT.get(), 1.0f, 1.0f);
                }
            }
        }));
        // 第 14 帧 (距离第12帧过了 2 帧, 2 * 66 = 132 ms): 开始循环 Saint9
        srDelay += 132;
        // 第 14 ~ 19 帧 (连续 6 帧): 狂轰滥炸 Saint9
        for (int i = 0; i < 6; i++) {
            final int d = srDelay;
            server.tell(new net.minecraft.server.TickTask(Math.max(1, (int) Math.round(d / 50.0)), () -> {
                for (LivingEntity target : targets) {
                    if (!target.isRemoved()) playSkillSound(target, BlackSouls.SAINT9_EVENT.get(), 1.0f, 1.0f);
                }
            }));
            srDelay += 66;
        }
        // 第 20 帧 (播放完最后一个 Saint9 后无缝衔接): Saint6 并结算伤害！
        final int srFinalDelay = srDelay;
        server.tell(new net.minecraft.server.TickTask(Math.max(1, (int) Math.round(srFinalDelay / 50.0)), () -> {
            for (LivingEntity target : targets) {
                if (!target.isRemoved() && target.isAlive()) {
                    playSkillSound(target, BlackSouls.SAINT6_EVENT.get(), 1.0f, 1.0f);
                    applyMagicHit(player, target, stats);
                }
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
        double bMdf = StatEventHandler.getRpgMagicDefense(target);

        // 公式：a.mat * 6 - b.mdf * 2
        double rawDamage = (stats.magicAttack * 6.0) - (bMdf * 2.0);
        if (rawDamage < 1.0) rawDamage = 1.0;

        // 光属性克制计算
        java.util.List<String> attackAttrs = new java.util.ArrayList<>();
        attackAttrs.add(com.BlackSouls.BlackSoulsMod.util.BSAttributeManager.LIGHT);
        float elementalMultiplier = com.BlackSouls.BlackSoulsMod.util.BSAttributeManager.getBestMultiplier(target, attackAttrs);
        rawDamage *= elementalMultiplier;

        // 20% 离散度
        double variance = 0.8 + (Math.random() * 0.4);
        rawDamage *= variance;

        float finalDamage = StatEventHandler.rollSkillCrit(player, (float) rawDamage);

        target.invulnerableTime = 0;
        DamageSource source = player.damageSources().indirectMagic(player, player);
        target.hurt(source, finalDamage);
    }

    // 获取范围内的所有敌人（过滤掉自己和宠物）
    private List<LivingEntity> getEnemiesInRange(Player player, double range) {
        List<LivingEntity> enemies = new ArrayList<>();
        AABB boundingBox = player.getBoundingBox().inflate(range);
        List<LivingEntity> entities = player.level().getEntitiesOfClass(LivingEntity.class, boundingBox);

        for (LivingEntity entity : entities) {
            if (entity == player) continue;

            // 过滤掉其他玩家和被驯服的宠物，只攻击怪物/中立生物
            if (entity instanceof Player) continue;
            if (entity instanceof TamableAnimal tamable && tamable.isTame()) continue;

            if (entity instanceof Mob) {
                enemies.add(entity);
            }
        }
        return enemies;
    }
}
