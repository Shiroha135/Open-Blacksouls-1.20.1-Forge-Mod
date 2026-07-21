package com.BlackSouls.BlackSoulsMod.network.packets;

import com.BlackSouls.BlackSoulsMod.BlackSouls;
import com.BlackSouls.BlackSoulsMod.capability.BSPlayerStats;
import com.BlackSouls.BlackSoulsMod.util.skill.AbstractSkill;
import com.BlackSouls.BlackSoulsMod.util.skill.SkillRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

@SuppressWarnings({"resource", "unused"})
public class PacketCastSkill {
    private static final int MAX_SKILL_ID_LENGTH = 128;

    private String skillName;

    public PacketCastSkill() {}

    public PacketCastSkill(String skillName) {
        this.skillName = skillName;
    }

    public PacketCastSkill(FriendlyByteBuf buf) {
        this.skillName = buf.readUtf(MAX_SKILL_ID_LENGTH);
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUtf(this.skillName, MAX_SKILL_ID_LENGTH);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) return;

            if (BlackSouls.BUFF_STUN.isPresent() && player.hasEffect(BlackSouls.BUFF_STUN.get())) {
                player.sendSystemMessage(Component.translatable("message.blacksouls.skill.stunned").withStyle(ChatFormatting.GRAY));
                return;
            }

            if (BlackSouls.BUFF_SILENCE.isPresent() && player.hasEffect(BlackSouls.BUFF_SILENCE.get())) {
                player.sendSystemMessage(Component.translatable("message.blacksouls.skill.silenced").withStyle(ChatFormatting.GRAY));
                return;
            }

            if (BlackSouls.BUFF_BERSERK.isPresent() && player.hasEffect(BlackSouls.BUFF_BERSERK.get())) {
                player.sendSystemMessage(Component.translatable("message.blacksouls.skill.berserk_locked").withStyle(ChatFormatting.GRAY));
                return;
            }

            BSPlayerStats stats = player.getCapability(BSPlayerStats.CAPABILITY).resolve().orElse(null);
            if (stats == null) return;

            AbstractSkill skill = SkillRegistry.SKILLS.get(skillName);
            if (skill == null || !skill.isUnlockedForGUI(player)) {
                return;
            }
            if (skill.canCast(player, stats)) {
                skill.consumeAndSetCooldown(player, stats);
                skill.execute(player, stats);
            }
        });
        context.setPacketHandled(true);
        return true;
    }
}
