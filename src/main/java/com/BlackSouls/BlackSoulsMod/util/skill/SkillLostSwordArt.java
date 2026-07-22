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

import java.util.List;

public class SkillLostSwordArt extends AbstractOriginalWeaponSkill {
    public enum Art { ECLIPSE, ZENITH_BLADE, SOLAR_FLARE }
    private final Art art;

    public SkillLostSwordArt(Art art) {
        super(Family.LOST_SWORD, art == Art.SOLAR_FLARE ? 5 : 0);
        this.art = art;
    }

    @Override public String getSkillId() { return switch (art) {
        case ECLIPSE -> "bs2_skill_eclipse";
        case ZENITH_BLADE -> "bs2_skill_zenith_blade";
        case SOLAR_FLARE -> "bs2_skill_solar_flare";
    }; }
    @Override public float getManaCost() { return switch (art) {
        case ECLIPSE -> 4.0F;
        case ZENITH_BLADE -> 28.0F;
        case SOLAR_FLARE -> 30.0F;
    }; }
    @Override public int getBaseCooldownTicks() { return switch (art) {
        case ECLIPSE -> 0;
        case ZENITH_BLADE -> 800;
        case SOLAR_FLARE -> 1000;
    }; }
    @Override public String getTranslationKey() { return "skill.blacksouls." + getSkillId() + ".name"; }
    @Override public ChatFormatting getTextColor() { return ChatFormatting.YELLOW; }
    @Override public ResourceLocation getIcon() { return new ResourceLocation(BlackSouls.MODID, "textures/gui/skills/" + switch (art) {
        case ECLIPSE -> "eclipse.png";
        case ZENITH_BLADE -> "zenith_blade.png";
        case SOLAR_FLARE -> "solar_flare.png";
    }); }

    @Override
    public void execute(ServerPlayer player, BSPlayerStats stats) {
        if (art == Art.ECLIPSE) {
            player.sendSystemMessage(Component.translatable("message.blacksouls.skill.eclipse.use", player.getName().getString()).withStyle(ChatFormatting.WHITE));
            player.swing(InteractionHand.MAIN_HAND, true);
            playAnimation(player, 376);
            playSound(player, BlackSouls.SAINT7_EVENT.get(), 1.0F);
            player.addEffect(new MobEffectInstance(BlackSouls.BUFF_ECLIPSE.get(), 2000, 0));
            StatEventHandler.applyStats(player);
            StatEventHandler.syncToClient(player);
            return;
        }
        List<LivingEntity> targets = art == Art.ZENITH_BLADE
                ? findTargets(player, 10.0D, 1)
                : findTargets(player, 16.0D, Integer.MAX_VALUE);
        if (targets.isEmpty()) {
            player.sendSystemMessage(Component.translatable("message.blacksouls.skill.no_target").withStyle(ChatFormatting.GRAY));
            return;
        }
        String key = art == Art.ZENITH_BLADE ? "zenith_blade" : "solar_flare";
        player.sendSystemMessage(Component.translatable("message.blacksouls.skill." + key + ".use", player.getName().getString()).withStyle(ChatFormatting.WHITE));
        player.swing(InteractionHand.MAIN_HAND, true);
        if (art == Art.ZENITH_BLADE) executeZenith(player, targets.get(0), stats);
        else executeSolarFlare(player, targets, stats);
    }

    private void executeZenith(ServerPlayer player, LivingEntity target, BSPlayerStats stats) {
        playAnimation(target, 377);
        playSound(target, BlackSouls.SAINT7_EVENT.get(), 0.5F);
        playSound(target, BlackSouls.HEAL4_EVENT.get(), 0.5F);
        playSound(target, BlackSouls.MAGIC1_EVENT.get(), 0.5F);
        int[] delays = {10, 19, 28, 37};
        for (int delay : delays) {
            schedule(player, delay, () -> {
                playSound(target, BlackSouls.SAINT3_EVENT.get(), 0.5F);
                double rawDamage = stats.magicAttack * 5.0D - StatEventHandler.getRpgMagicDefense(target) * 2.0D;
                rawDamage *= 0.8D + Math.random() * 0.4D;
                applyRawHit(player, target, rawDamage, true, false, 0.0D);
            });
        }
    }

    private void executeSolarFlare(ServerPlayer player, List<LivingEntity> targets, BSPlayerStats stats) {
        targets.forEach(target -> playAnimation(target, 378));
        playSound(player, BlackSouls.SAINT7_EVENT.get(), 0.8F);
        playSound(player, BlackSouls.FLASH3_EVENT.get(), 1.0F);
        for (int delay = 0; delay <= 12; delay += 2) schedule(player, delay, () -> playSound(player, BlackSouls.SAINT9_EVENT.get(), 1.5F));
        schedule(player, 12, () -> {
            for (LivingEntity target : targets) {
                double rawDamage = stats.magicAttack * 10.0D - StatEventHandler.getRpgMagicDefense(target) * 2.0D;
                rawDamage *= 0.8D + Math.random() * 0.4D;
                applyRawHit(player, target, rawDamage, true, true, 0.0D);
            }
        });
    }

    private void schedule(ServerPlayer player, int delay, Runnable task) {
        net.minecraft.server.MinecraftServer server = player.serverLevel().getServer();
        server.tell(new net.minecraft.server.TickTask(server.getTickCount() + delay, task));
    }
}
