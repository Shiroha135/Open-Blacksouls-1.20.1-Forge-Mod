package com.BlackSouls.BlackSoulsMod.util.skill;

import com.BlackSouls.BlackSoulsMod.BlackSouls;
import com.BlackSouls.BlackSoulsMod.capability.BSPlayerStats;
import com.BlackSouls.BlackSoulsMod.handler.StatEventHandler;
import com.BlackSouls.BlackSoulsMod.network.NetworkHandler;
import com.BlackSouls.BlackSoulsMod.network.packets.PacketPlayAnim;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Optional;

@SuppressWarnings("resource")
public class SkillUltimateTripleSlash extends WeaponSkill {

    @Override
    public String getSkillId() { return "bs2_skill_ultimate_triple_slash"; }

    @Override
    public float getManaCost() { return 30.0f; } 

    @Override
    public int getBaseCooldownTicks() { return 200; } 

    @Override
    public String getTranslationKey() { return "skill.blacksouls.ultimate_triple_slash.name"; }

    @Override
    public ChatFormatting getTextColor() { return ChatFormatting.GOLD; }

    @Override
    protected boolean isWeaponEquipped(Player player) {
        ItemStack mainHand = player.getMainHandItem();
        return !mainHand.isEmpty() && mainHand.getItem() == BlackSouls.BRAVE_SWORD_VORPAL.get();
    }

    @Override
    public void execute(ServerPlayer player, BSPlayerStats stats) {
        LivingEntity target = findTarget(player, 8.0);
        if (target == null) {
            player.sendSystemMessage(Component.translatable("message.blacksouls.skill.no_target").withStyle(ChatFormatting.GRAY));
            return;
        }

        long currentTime = player.level().getGameTime();
        int stage = 1;
        if (player.getPersistentData().contains("bs2_uts_time")) {
            long lastTime = player.getPersistentData().getLong("bs2_uts_time");
            if (currentTime - lastTime <= 600) { 
                stage = player.getPersistentData().getInt("bs2_uts_stage");
            }
        }

        player.swing(InteractionHand.MAIN_HAND, true);

        if (stage == 1) {
            player.sendSystemMessage(Component.translatable("message.blacksouls.skill.ultimate_triple_slash_1.use", player.getName().getString()).withStyle(ChatFormatting.WHITE));
            playAnim(target, 371, false); 
            executeStage1(player, target, stats);

            player.getPersistentData().putInt("bs2_uts_stage", 2);
            player.getPersistentData().putLong("bs2_uts_time", currentTime);

        } else if (stage == 2) {
            player.sendSystemMessage(Component.translatable("message.blacksouls.skill.ultimate_triple_slash_2.use", player.getName().getString()).withStyle(ChatFormatting.WHITE));
            playAnim(target, 372, true);
            executeStage2(player, target, stats);

            player.getPersistentData().putInt("bs2_uts_stage", 3);
            player.getPersistentData().putLong("bs2_uts_time", currentTime);

        } else if (stage == 3) {
            player.sendSystemMessage(Component.translatable("message.blacksouls.skill.ultimate_triple_slash_3.use", player.getName().getString()).withStyle(ChatFormatting.WHITE));
            playAnim(target, 373, false); 
            executeStage3(player, target, stats);

            player.getPersistentData().putInt("bs2_uts_stage", 1);
            player.getPersistentData().putLong("bs2_uts_time", 0);
        }
    }

    private void executeStage1(ServerPlayer player, LivingEntity target, BSPlayerStats stats) {
        net.minecraft.server.MinecraftServer server = player.serverLevel().getServer();
        server.tell(new net.minecraft.server.TickTask(0, () -> {
            playSkillSound(target, BlackSouls.SLASH4_EVENT.get(), 1.0f, 1.0f);
            applyHit(player, target, stats, false);
        }));
        server.tell(new net.minecraft.server.TickTask(Math.max(1, (int) Math.round(198 / 50.0)), () -> {
            playSkillSound(target, BlackSouls.SLASH12_EVENT.get(), 1.0f, 1.0f);
            applyHit(player, target, stats, false);
        }));
        server.tell(new net.minecraft.server.TickTask(Math.max(1, (int) Math.round((198 + 198) / 50.0)), () -> {
            playSkillSound(target, BlackSouls.ATTACK3_EVENT.get(), 1.0f, 1.0f);
        }));
        server.tell(new net.minecraft.server.TickTask(Math.max(1, (int) Math.round((198 + 198 + 132) / 50.0)), () -> {
            playSkillSound(target, BlackSouls.DAO_EVENT.get(), 1.0f, 1.0f);
            playSkillSound(target, BlackSouls.SWORD4_EVENT.get(), 1.0f, 1.0f);
            applyHit(player, target, stats, false);
        }));
    }

    private void executeStage2(ServerPlayer player, LivingEntity target, BSPlayerStats stats) {
        net.minecraft.server.MinecraftServer server = player.serverLevel().getServer();
        server.tell(new net.minecraft.server.TickTask(0, () -> {
            playSkillSound(target, BlackSouls.SLASH12_EVENT.get(), 1.0f, 1.0f);
        }));
        server.tell(new net.minecraft.server.TickTask(Math.max(1, (int) Math.round(132 / 50.0)), () -> {
            playSkillSound(target, BlackSouls.SKILL3_EVENT.get(), 1.0f, 1.0f);
            applyHit(player, target, stats, true);
        }));
    }

    private void executeStage3(ServerPlayer player, LivingEntity target, BSPlayerStats stats) {
        net.minecraft.server.MinecraftServer server = player.serverLevel().getServer();
        server.tell(new net.minecraft.server.TickTask(0, () -> {
            playSkillSound(target, BlackSouls.WIND10_EVENT.get(), 1.0f, 1.0f);
        }));
        server.tell(new net.minecraft.server.TickTask(Math.max(1, (int) Math.round(528 / 50.0)), () -> {
            playSkillSound(target, BlackSouls.SLASH9_EVENT.get(), 1.0f, 1.0f);
            applyHit(player, target, stats, false);
        }));
        server.tell(new net.minecraft.server.TickTask(Math.max(1, (int) Math.round((528 + 330) / 50.0)), () -> {
            playSkillSound(target, BlackSouls.SLASH9_EVENT.get(), 1.0f, 1.0f);
            applyHit(player, target, stats, false);
        }));
        server.tell(new net.minecraft.server.TickTask(Math.max(1, (int) Math.round((528 + 330 + 330) / 50.0)), () -> {
            playSkillSound(target, BlackSouls.SLASH9_EVENT.get(), 1.0f, 1.0f);
            applyHit(player, target, stats, false);
        }));
        server.tell(new net.minecraft.server.TickTask(Math.max(1, (int) Math.round((528 + 330 + 330 + 330) / 50.0)), () -> {
            playSkillSound(target, BlackSouls.EARTH6_EVENT.get(), 1.0f, 1.0f);
            playSkillSound(target, BlackSouls.DAO_EVENT.get(), 1.0f, 1.0f);
            playSkillSound(target, BlackSouls.SWORD4_EVENT.get(), 1.0f, 1.0f);
            applyHit(player, target, stats, false);
        }));
    }

    private void playSkillSound(LivingEntity target, SoundEvent sound, float volume, float pitch) {
        if (!target.isRemoved()) {
            target.level().playSound(null, target.getX(), target.getY(), target.getZ(), sound, SoundSource.PLAYERS, volume, pitch);
        }
    }

    private void playAnim(LivingEntity target, int animId, boolean isHead) {
        double yOffset = isHead ? target.getBbHeight() : target.getBbHeight() / 2.0F; 
        PacketPlayAnim animPacket = new PacketPlayAnim(animId, target.getX(), target.getY() + yOffset, target.getZ());
        NetworkHandler.sendToAllAround(animPacket, target);
    }

    private void applyHit(ServerPlayer player, LivingEntity target, BSPlayerStats stats, boolean ignoreDef) {
        if (target == null || target.isRemoved() || target.getHealth() <= 0) return;

        double bDef = ignoreDef ? 0.0D : StatEventHandler.getRpgPhysicalDefense(target);

        double rawDamage = ignoreDef ? (stats.attack * 5.0) : (stats.attack * 4.0 - bDef * 2.0);
        if (rawDamage < 1.0) rawDamage = 1.0;

        double variance = 0.8 + (Math.random() * 0.4);
        rawDamage *= variance;

        float finalDamage = StatEventHandler.rollSkillCrit(player, (float) rawDamage);
        target.invulnerableTime = 0;

        DamageSource source = player.damageSources().playerAttack(player);
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
