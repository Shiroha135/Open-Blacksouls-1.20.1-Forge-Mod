package com.BlackSouls.BlackSoulsMod.network.packets;

import com.BlackSouls.BlackSoulsMod.util.EatMeSizeManager;
import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.network.NetworkEvent;

public class ClientboundPlayerSizePacket {
    private final int entityId;
    private final boolean small;

    public ClientboundPlayerSizePacket(int entityId, boolean small) {
        this.entityId = entityId;
        this.small = small;
    }

    public ClientboundPlayerSizePacket(FriendlyByteBuf buffer) {
        this.entityId = buffer.readVarInt();
        this.small = buffer.readBoolean();
    }

    public void toBytes(FriendlyByteBuf buffer) {
        buffer.writeVarInt(this.entityId);
        buffer.writeBoolean(this.small);
    }

    public void handle(Supplier<NetworkEvent.Context> supplier) {
        PacketHandlers.handleClient(supplier, () -> {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.level == null) {
                return;
            }
            Entity entity = minecraft.level.getEntity(this.entityId);
            if (entity instanceof Player player) {
                EatMeSizeManager.setSmall(player, this.small);
            }
        });
    }
}
