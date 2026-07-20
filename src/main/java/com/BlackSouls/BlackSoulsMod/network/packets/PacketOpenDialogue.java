package com.BlackSouls.BlackSoulsMod.network.packets;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class PacketOpenDialogue {
    private static final int MAX_DIALOGUE_LINES = 64;
    private static final int MAX_DIALOGUE_TEXT_LENGTH = 2048;
    private static final int MAX_ID_LENGTH = 128;

    private final String nameKey;
    private final String avatarId;
    private final String[] dialogues;
    private final boolean isLaterDialogue;
    private final int entityId;
    private final int covLevel;
    private final boolean valid;

    
    public PacketOpenDialogue(String nameKey, String avatarId, String[] dialogues, boolean isLaterDialogue, int entityId, int covLevel) {
        this.nameKey = nameKey;
        this.avatarId = avatarId;
        this.dialogues = dialogues;
        this.isLaterDialogue = isLaterDialogue;
        this.entityId = entityId;
        this.covLevel = covLevel;
        this.valid = true;
    }

    
    public PacketOpenDialogue(FriendlyByteBuf buf) {
        this.nameKey = buf.readUtf(MAX_ID_LENGTH);
        this.avatarId = buf.readUtf(MAX_ID_LENGTH);
        int len = buf.readVarInt();
        if (len < 0 || len > MAX_DIALOGUE_LINES) {
            this.dialogues = new String[0];
            this.isLaterDialogue = false;
            this.entityId = -1;
            this.covLevel = -1;
            this.valid = false;
            return;
        }
        this.dialogues = new String[len];
        for (int i = 0; i < len; i++) {
            this.dialogues[i] = buf.readUtf(MAX_DIALOGUE_TEXT_LENGTH);
        }
        this.isLaterDialogue = buf.readBoolean();
        this.entityId = buf.readVarInt();
        this.covLevel = buf.readVarInt();
        this.valid = true;
    }

    
    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUtf(this.nameKey, MAX_ID_LENGTH);
        buf.writeUtf(this.avatarId, MAX_ID_LENGTH);
        int len = Math.min(this.dialogues.length, MAX_DIALOGUE_LINES);
        buf.writeVarInt(len);
        for (int i = 0; i < len; i++) {
            buf.writeUtf(this.dialogues[i], MAX_DIALOGUE_TEXT_LENGTH);
        }
        buf.writeBoolean(this.isLaterDialogue);
        buf.writeVarInt(this.entityId);
        buf.writeVarInt(this.covLevel);
    }

    
    public void handle(Supplier<NetworkEvent.Context> ctx) {
        PacketHandlers.handleClient(ctx, () -> {
            if (this.valid) {
                ClientHandler.doOpen(this);
            }
        });
    }

    
    private static class ClientHandler {
        public static void doOpen(PacketOpenDialogue msg) {
            net.minecraft.client.Minecraft.getInstance().setScreen(new com.BlackSouls.BlackSoulsMod.client.gui.GuiDialogueEnhanced(
                    msg.nameKey, msg.avatarId, msg.dialogues,
                    msg.isLaterDialogue, msg.entityId, msg.covLevel
            ));
        }
    }
}
