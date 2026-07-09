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
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Optional;

@SuppressWarnings("resource")
public class SkillShotgunBreak extends WeaponSkill {

    @Override
    public String getSkillId() { return "bs2_skill_shotgun_blast"; }

    @Override
    public float getManaCost() { return 0.0f; }

    @Override
    public double getActionCost() { return 0.0f; }

    @Override
    public int getBaseCooldownTicks() { return 200; }

    @Override
    public String getTranslationKey() { return "skill.blacksouls.shotgun_break.name"; }

    @Override
    public ChatFormatting getTextColor() { return ChatFormatting.DARK_GRAY; } 

    @Override
    protected boolean isWeaponEquipped(Player player) {
        ItemStack offHand = player.getOffhandItem();
        return !offHand.isEmpty() && offHand.getItem() == BlackSouls.MURDERERS_SHOTGUN.get();
    }

    @Override
    public void execute(ServerPlayer player, BSPlayerStats stats) {
        LivingEntity target = findTarget(player, 24.0);
        if (target == null) {
            player.sendSystemMessage(Component.translatable("message.blacksouls.skill.no_target").withStyle(ChatFormatting.GRAY));
            return;
        }

        player.sendSystemMessage(Component.translatable("message.blacksouls.skill.shotgun_break.use", player.getName().getString()).withStyle(ChatFormatting.WHITE));


        playAnim(target, 247);

        net.minecraft.server.MinecraftServer server = player.serverLevel().getServer();
        server.tell(new net.minecraft.server.TickTask(0, () -> {
            if (!target.isRemoved()) {
                playSkillSound(player, BlackSouls.KEY_EVENT.get(), 1.0f, 1.0f);
            }
        }));
        server.tell(new net.minecraft.server.TickTask(Math.max(1, (int) Math.round(330 / 50.0)), () -> {
            if (!target.isRemoved()) {
                applyHit(player, target, stats, 5.0, true);

                if (BlackSouls.BUFF_DEFENSELESS.isPresent()) {
                    target.addEffect(new MobEffectInstance(BlackSouls.BUFF_DEFENSELESS.get(), 400, 0));
                }

                playSkillSound(player, BlackSouls.SHOTGUN_FIRE_EVENT.get(), 1.0f, 1.0f);
                playSkillSound(target, BlackSouls.BREAK_EVENT.get(), 1.0f, 1.0f);
            }
        }));
        server.tell(new net.minecraft.server.TickTask(Math.max(1, (int) Math.round((330 + 66) / 50.0)), () -> {
            if (!target.isRemoved()) {
                playSkillSound(target, BlackSouls.GUN1_EVENT.get(), 1.0f, 1.0f);
            }
        }));
        server.tell(new net.minecraft.server.TickTask(Math.max(1, (int) Math.round((330 + 66 + 66) / 50.0)), () -> {
            if (!target.isRemoved()) {
                playSkillSound(target, BlackSouls.GUN1_EVENT.get(), 1.0f, 1.0f);
            }
        }));
    }

    private void playSkillSound(Entity source, SoundEvent sound, float volume, float pitch) {
        source.level().playSound(null, source.getX(), source.getY(), source.getZ(), sound, SoundSource.PLAYERS, volume, pitch);
    }

    private void playAnim(LivingEntity target, int animId) {
        PacketPlayAnim animPacket = new PacketPlayAnim(animId, target.getX(), target.getY() + target.getBbHeight() / 2.0F, target.getZ());
        NetworkHandler.sendToAllAround(animPacket, target);
    }

    private void applyHit(ServerPlayer player, LivingEntity target, BSPlayerStats stats, double atkMultiplier, boolean isSureHit) {
        if (target == null || target.isRemoved() || target.getHealth() <= 0) return;

        double bDef = StatEventHandler.getRpgPhysicalDefense(target);

        double rawDamage = (stats.attack * atkMultiplier) - (bDef * 2.0);
        if (rawDamage < 1.0) rawDamage = 1.0;

        double variance = 0.5 + (Math.random() * 1.0);
        rawDamage *= variance;

        float finalDamage = StatEventHandler.rollSkillCrit(player, (float) rawDamage);
        target.invulnerableTime = 0;

        DamageSource source = isSureHit ? player.damageSources().indirectMagic(player, player) : player.damageSources().playerAttack(player);
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
