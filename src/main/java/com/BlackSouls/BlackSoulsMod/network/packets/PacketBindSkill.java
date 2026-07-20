package com.BlackSouls.BlackSoulsMod.network.packets;

import com.BlackSouls.BlackSoulsMod.capability.BSPlayerStats;
import com.BlackSouls.BlackSoulsMod.handler.StatEventHandler;
import com.BlackSouls.BlackSoulsMod.util.skill.AbstractSkill;
import com.BlackSouls.BlackSoulsMod.util.skill.SkillRegistry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

public class PacketBindSkill {
    private static final int MAX_KEY_BIND_LENGTH = 1;
    private static final int MAX_SKILL_ID_LENGTH = 128;

    private String keyBind; 
    private String skillName;

    public PacketBindSkill() {}

    public PacketBindSkill(String keyBind, String skillName) {
        this.keyBind = keyBind;
        this.skillName = skillName;
    }

    public PacketBindSkill(FriendlyByteBuf buf) {
        this.keyBind = buf.readUtf(MAX_KEY_BIND_LENGTH);
        this.skillName = buf.readUtf(MAX_SKILL_ID_LENGTH);
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUtf(keyBind, MAX_KEY_BIND_LENGTH);
        buf.writeUtf(skillName, MAX_SKILL_ID_LENGTH);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context ctx = supplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player != null) {
                BSPlayerStats stats = player.getCapability(BSPlayerStats.CAPABILITY).orElse(null);
                if (stats != null) {
                    AbstractSkill skill = SkillRegistry.SKILLS.get(skillName);
                    if (skill == null || !skill.isUnlockedForGUI(player)) {
                        return;
                    }

                    boolean changed = false;
                    if (keyBind.equals("Z") && !skillName.equals(stats.skillZ)) {
                        stats.skillZ = skillName;
                        changed = true;
                    } else if (keyBind.equals("X") && !skillName.equals(stats.skillX)) {
                        stats.skillX = skillName;
                        changed = true;
                    } else if (keyBind.equals("C") && !skillName.equals(stats.skillC)) {
                        stats.skillC = skillName;
                        changed = true;
                    } else if (keyBind.equals("V") && !skillName.equals(stats.skillV)) {
                        stats.skillV = skillName;
                        changed = true;
                    }

                    if (changed) {
                        StatEventHandler.syncToClient(player);
                    }
                }
            }
        });
        ctx.setPacketHandled(true);
        return true;
    }
}
