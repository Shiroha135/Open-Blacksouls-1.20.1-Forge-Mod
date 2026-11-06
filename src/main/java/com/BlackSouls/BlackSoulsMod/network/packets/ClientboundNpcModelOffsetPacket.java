package com.BlackSouls.BlackSoulsMod.network.packets;

import com.shiroha.mmdskin.render.entity.CustomEntityModelOffsetProvider;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

public final class ClientboundNpcModelOffsetPacket {
    private final int entityId;
    private final UUID entityUuid;
    private final Vec3 offset;
    private final boolean accepted;

    public ClientboundNpcModelOffsetPacket(int entityId, UUID entityUuid, Vec3 offset, boolean accepted) {
        this.entityId = entityId;
        this.entityUuid = entityUuid;
        this.offset = offset;
        this.accepted = accepted;
    }

    public ClientboundNpcModelOffsetPacket(FriendlyByteBuf buffer) {
        this(buffer.readVarInt(), buffer.readUUID(), new Vec3(buffer.readDouble(), buffer.readDouble(), buffer.readDouble()), buffer.readBoolean());
    }

    public void toBytes(FriendlyByteBuf buffer) {
        buffer.writeVarInt(entityId);
        buffer.writeUUID(entityUuid);
        buffer.writeDouble(offset.x);
        buffer.writeDouble(offset.y);
        buffer.writeDouble(offset.z);
        buffer.writeBoolean(accepted);
    }

    public void handle(Supplier<NetworkEvent.Context> supplier) {
        PacketHandlers.handleClient(supplier, () -> ClientHandler.receive(this));
    }

    private static final class ClientHandler {
        private static void receive(ClientboundNpcModelOffsetPacket packet) {
            var minecraft = net.minecraft.client.Minecraft.getInstance();
            var target = minecraft.level == null ? null : minecraft.level.getEntity(packet.entityId);
            if (target == null || !target.getUUID().equals(packet.entityUuid)
                    || !CustomEntityModelOffsetProvider.isValidOffset(packet.offset)) return;
            if (target instanceof CustomEntityModelOffsetProvider provider) provider.setMmdModelOffset(packet.offset);
            if (minecraft.screen instanceof com.BlackSouls.BlackSoulsMod.client.gui.GuiNpcModelOffset screen) {
                screen.receive(packet.entityUuid, packet.offset, packet.accepted);
            }
        }
    }
}
