package com.BlackSouls.BlackSoulsMod.network.packets;

import com.BlackSouls.BlackSoulsMod.capability.BSPlayerStats;
import com.BlackSouls.BlackSoulsMod.handler.StatEventHandler;
import com.BlackSouls.BlackSoulsMod.util.SkillUtils;
import com.BlackSouls.BlackSoulsMod.util.skill.SkillRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.Set;
import java.util.function.Supplier;

public class PacketDevAction {
    public enum Action {
        SET_NO_COOLDOWN,
        SET_LIMIT_BREAK,
        UNLOCK_ALL_BOOK_SKILLS,
        FORGET_ALL_BOOK_SKILLS
    }

    private final Action action;
    private final boolean enabled;

    public PacketDevAction(Action action, boolean enabled) {
        this.action = action;
        this.enabled = enabled;
    }

    public PacketDevAction(FriendlyByteBuf buffer) {
        this.action = buffer.readEnum(Action.class);
        this.enabled = buffer.readBoolean();
    }

    public void toBytes(FriendlyByteBuf buffer) {
        buffer.writeEnum(this.action);
        buffer.writeBoolean(this.enabled);
    }

    public void handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) {
                return;
            }
            if (!player.isCreative() || !player.hasPermissions(4)) {
                player.sendSystemMessage(Component.translatable("message.blacksouls.dev.no_permission").withStyle(ChatFormatting.RED));
                return;
            }
            BSPlayerStats stats = player.getCapability(BSPlayerStats.CAPABILITY).resolve().orElse(null);
            if (stats == null) {
                return;
            }
            switch (this.action) {
                case SET_NO_COOLDOWN -> {
                    stats.developerNoCooldown = this.enabled;
                    if (this.enabled) {
                        SkillUtils.clearAllCooldownsExceptChrono(player);
                    }
                    player.displayClientMessage(Component.translatable(
                            this.enabled ? "message.blacksouls.dev.no_cooldown.enabled" : "message.blacksouls.dev.no_cooldown.disabled"
                    ).withStyle(this.enabled ? ChatFormatting.AQUA : ChatFormatting.GRAY), true);
                }
                case SET_LIMIT_BREAK -> {
                    stats.developerLimitBreak = this.enabled;
                    StatEventHandler.applyStats(player);
                    if (player.getHealth() > player.getMaxHealth()) {
                        player.setHealth(player.getMaxHealth());
                    }
                    player.displayClientMessage(Component.translatable(
                            this.enabled ? "message.blacksouls.dev.limit_break.enabled" : "message.blacksouls.dev.limit_break.disabled"
                    ).withStyle(this.enabled ? ChatFormatting.LIGHT_PURPLE : ChatFormatting.GRAY), true);
                }
                case UNLOCK_ALL_BOOK_SKILLS -> {
                    Set<String> skillIds = SkillRegistry.getSkillBookSkillIds();
                    int before = stats.unlockedSkills.size();
                    for (String skillId : skillIds) {
                        if (!stats.unlockedSkills.contains(skillId)) {
                            stats.unlockedSkills.add(skillId);
                        }
                    }
                    int learned = stats.unlockedSkills.size() - before;
                    player.displayClientMessage(Component.translatable(
                            "message.blacksouls.dev.skills.unlocked", learned, skillIds.size()
                    ).withStyle(ChatFormatting.GREEN), true);
                }
                case FORGET_ALL_BOOK_SKILLS -> {
                    Set<String> skillIds = SkillRegistry.getSkillBookSkillIds();
                    int before = stats.unlockedSkills.size();
                    stats.unlockedSkills.removeIf(skillIds::contains);
                    stats.skillZ = clearBinding(stats.skillZ, skillIds);
                    stats.skillX = clearBinding(stats.skillX, skillIds);
                    stats.skillC = clearBinding(stats.skillC, skillIds);
                    stats.skillV = clearBinding(stats.skillV, skillIds);
                    for (String skillId : skillIds) {
                        SkillUtils.getPersistedData(player).remove(SkillUtils.getCooldownTag(skillId));
                    }
                    int forgotten = before - stats.unlockedSkills.size();
                    player.displayClientMessage(Component.translatable(
                            "message.blacksouls.dev.skills.forgotten", forgotten
                    ).withStyle(ChatFormatting.YELLOW), true);
                }
            }
            StatEventHandler.syncToClient(player);
        });
        context.setPacketHandled(true);
    }

    private static String clearBinding(String binding, Set<String> skillIds) {
        return skillIds.contains(binding) ? "" : binding;
    }
}
