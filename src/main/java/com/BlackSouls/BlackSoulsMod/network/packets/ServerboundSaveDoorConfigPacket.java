package com.BlackSouls.BlackSoulsMod.network.packets;

import com.BlackSouls.BlackSoulsMod.handler.DoorEditorService;
import com.BlackSouls.BlackSoulsMod.util.DoorConfigMode;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record ServerboundSaveDoorConfigPacket(
        BlockPos pos,
        DoorConfigMode mode,
        String requiredItem,
        boolean consume,
        String eventId,
        String conditionId,
        String targetDimension,
        double targetX,
        double targetY,
        double targetZ,
        boolean eventTriggered
) {
    private static final int MAX_ITEM_ID_LENGTH = 128;
    private static final int MAX_EVENT_ID_LENGTH = 64;
    private static final int MAX_DIMENSION_ID_LENGTH = 128;

    public static void encode(ServerboundSaveDoorConfigPacket packet, FriendlyByteBuf buffer) {
        buffer.writeBlockPos(packet.pos);
        PacketHandlers.writeEnum(buffer, packet.mode);
        buffer.writeUtf(packet.requiredItem, MAX_ITEM_ID_LENGTH);
        buffer.writeBoolean(packet.consume);
        buffer.writeUtf(packet.eventId, MAX_EVENT_ID_LENGTH);
        buffer.writeUtf(packet.conditionId, MAX_EVENT_ID_LENGTH);
        buffer.writeUtf(packet.targetDimension, MAX_DIMENSION_ID_LENGTH);
        buffer.writeDouble(packet.targetX);
        buffer.writeDouble(packet.targetY);
        buffer.writeDouble(packet.targetZ);
        buffer.writeBoolean(packet.eventTriggered);
    }

    public static ServerboundSaveDoorConfigPacket decode(FriendlyByteBuf buffer) {
        BlockPos pos = buffer.readBlockPos();
        DoorConfigMode mode = PacketHandlers.readEnum(buffer, DoorConfigMode.values());
        return new ServerboundSaveDoorConfigPacket(
                pos,
                mode == null ? DoorConfigMode.NONE : mode,
                buffer.readUtf(MAX_ITEM_ID_LENGTH),
                buffer.readBoolean(),
                buffer.readUtf(MAX_EVENT_ID_LENGTH),
                buffer.readUtf(MAX_EVENT_ID_LENGTH),
                buffer.readUtf(MAX_DIMENSION_ID_LENGTH),
                buffer.readDouble(),
                buffer.readDouble(),
                buffer.readDouble(),
                buffer.readBoolean()
        );
    }

    public static void handle(ServerboundSaveDoorConfigPacket packet, Supplier<NetworkEvent.Context> supplier) {
        PacketHandlers.handleServer(supplier, context -> {
            if (context.getSender() != null) {
                DoorEditorService.apply(context.getSender(), packet);
            }
        });
    }
}
