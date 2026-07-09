package com.BlackSouls.BlackSoulsMod.network.packets;

import com.BlackSouls.BlackSoulsMod.util.SkillUtils;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

public class PacketSyncSkill {
    private final String skillName;

    public PacketSyncSkill(String skillName) {
        this.skillName = skillName;
    }

    public PacketSyncSkill(FriendlyByteBuf buf) {
        this.skillName = buf.readUtf();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUtf(this.skillName);
    }

    public void handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context ctx = supplier.get();
        ctx.enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientHandler.handle(this));
        });
        ctx.setPacketHandled(true);
    }

    private static class ClientHandler {
        public static void handle(PacketSyncSkill msg) {
            Player player = net.minecraft.client.Minecraft.getInstance().player;
            if (player != null) {
                SkillUtils.learnSkill(player, msg.skillName);
            }
        }
    }
}
