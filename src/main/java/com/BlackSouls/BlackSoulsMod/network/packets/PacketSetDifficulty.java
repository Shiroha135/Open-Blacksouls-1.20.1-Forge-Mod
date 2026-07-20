package com.BlackSouls.BlackSoulsMod.network.packets;

import com.BlackSouls.BlackSoulsMod.capability.BSWorldData;
import com.BlackSouls.BlackSoulsMod.util.DifficultyManager;
import com.BlackSouls.BlackSoulsMod.network.NetworkHandler;
import net.minecraft.ChatFormatting;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;
import java.util.function.Supplier;

public class PacketSetDifficulty {
    private static final int MIN_DIFFICULTY = 1;
    private static final int MAX_DIFFICULTY = 9;

    public int difficulty;

    public PacketSetDifficulty() {}

    public PacketSetDifficulty(int difficulty) {
        this.difficulty = difficulty;
    }

    public PacketSetDifficulty(FriendlyByteBuf buf) {
        this.difficulty = buf.readInt();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeInt(this.difficulty);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context ctx = supplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer serverPlayer = ctx.getSender();
            if (serverPlayer != null) {
                if (!serverPlayer.hasPermissions(4)) {
                    serverPlayer.sendSystemMessage(Component.translatable("message.blacksouls.difficulty.no_permission").withStyle(ChatFormatting.RED));
                    return;
                }
                if (difficulty < MIN_DIFFICULTY || difficulty > MAX_DIFFICULTY) {
                    return;
                }

                BSWorldData data = BSWorldData.get(serverPlayer.serverLevel());
                if (data.difficulty == difficulty) {
                    return;
                }
                data.difficulty = difficulty;
                data.setDirty();

                DifficultyManager.currentDifficulty = difficulty;
                DifficultyManager.updateAllMonstersInstant(serverPlayer.serverLevel());
                Component msg = Component.translatable("message.blacksouls.difficulty.broadcast", serverPlayer.getName().getString(), difficulty).withStyle(ChatFormatting.YELLOW);
                serverPlayer.server.getPlayerList().broadcastSystemMessage(msg, false);

                NetworkHandler.INSTANCE.send(PacketDistributor.ALL.noArg(), new PacketSyncDifficulty(data));
            }
        });
        ctx.setPacketHandled(true);
        return true;
    }
}
