package com.BlackSouls.BlackSoulsMod.util.skill;

import com.BlackSouls.BlackSoulsMod.BlackSouls;
import com.BlackSouls.BlackSoulsMod.capability.BSPlayerStats;
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
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.List;

public class SkillSoulLight extends AbstractSkill {

    @Override
    public String getSkillId() { return "bs2_skill_soul_light"; }

    @Override
    public float getManaCost() { return 12.0f; }

    @Override
    public int getBaseCooldownTicks() { return 200; } 

    @Override
    public String getTranslationKey() { return "skill.blacksouls.bs2_skill_soul_light.name"; }

    @Override
    public ChatFormatting getTextColor() { return ChatFormatting.AQUA; } 

    @Override
    public ResourceLocation getIcon() { return new ResourceLocation(BlackSouls.MODID, "textures/gui/skills/original/soul_light.png"); }

    @Override
    public boolean isUnlockedForGUI(Player player) {
        return SkillUtils.hasLearnedSkill(player, getSkillId());
    }

    @Override
    public void execute(ServerPlayer player, BSPlayerStats stats) {
        player.swing(InteractionHand.MAIN_HAND, true);

        player.sendSystemMessage(Component.translatable("message.blacksouls.skill.soul_light.use", player.getName().getString()).withStyle(ChatFormatting.WHITE));

        List<LivingEntity> targets = getAlliesInRange(player, 10.0);

        for (LivingEntity target : targets) {
            playAnim(target, 37);
        }

        net.minecraft.server.MinecraftServer server = player.serverLevel().getServer();
        server.tell(new net.minecraft.server.TickTask(0, () -> playSkillSound(player, BlackSouls.ICE1_EVENT.get(), 1.0f, 1.0f)));
        server.tell(new net.minecraft.server.TickTask(Math.max(1, (int) Math.round(330 / 50.0)), () -> {
            for (LivingEntity target : targets) {
                if (!target.isRemoved() && target.isAlive()) {
                    applyHealing(player, target, stats);
                }
            }
        }));
    }

    @Override
    public void executeInTurnBattle(ServerPlayer player, BSPlayerStats stats, LivingEntity target) {
        LivingEntity ally = target == null ? player : target;
        player.swing(InteractionHand.MAIN_HAND, true);
        playAnim(ally, 37);
        playSkillSound(player, BlackSouls.ICE1_EVENT.get(), 1.0f, 1.0f);
        applyHealing(player, ally, stats);
    }

    private void playSkillSound(LivingEntity source, SoundEvent sound, float volume, float pitch) {
        source.level().playSound(null, source.getX(), source.getY() + source.getBbHeight() / 2.0, source.getZ(), sound, SoundSource.PLAYERS, volume, pitch);
    }

    private void playAnim(LivingEntity target, int animId) {
        PacketPlayAnim animPacket = new PacketPlayAnim(animId, target.getX(), target.getY() + target.getBbHeight() / 2.0F, target.getZ());
        NetworkHandler.sendToAllAround(animPacket, target);
    }

    private void applyHealing(ServerPlayer caster, LivingEntity target, BSPlayerStats stats) {
        double rawHeal = stats.magicAttack * 6.0;

        double variance = 0.8 + (Math.random() * 0.4);
        rawHeal *= variance;

        target.heal((float) rawHeal);
    }

    private List<LivingEntity> getAlliesInRange(Player player, double range) {
        List<LivingEntity> allies = new ArrayList<>();
        allies.add(player); 

        AABB boundingBox = player.getBoundingBox().inflate(range);
        List<LivingEntity> entities = player.level().getEntitiesOfClass(LivingEntity.class, boundingBox);

        for (LivingEntity entity : entities) {
            if (entity == player) continue; 

            if (entity instanceof Player || (entity instanceof TamableAnimal tamable && tamable.isTame())) {
                allies.add(entity);
            }
        }
        return allies;
    }
}
