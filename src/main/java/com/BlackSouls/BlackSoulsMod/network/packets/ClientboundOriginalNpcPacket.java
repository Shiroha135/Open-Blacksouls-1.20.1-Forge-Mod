package com.BlackSouls.BlackSoulsMod.network.packets;

import com.BlackSouls.BlackSoulsMod.story.OriginalNpcInterpreter;
import com.BlackSouls.BlackSoulsMod.util.OriginalNpcItems;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

public record ClientboundOriginalNpcPacket(int entityId, UUID sessionId, long revision,
                                           String name, String text, String portrait, int portraitIndex,
                                           OriginalNpcInterpreter.Type type, List<String> choices, List<Offer> offers) {
    public ClientboundOriginalNpcPacket {
        choices = List.copyOf(choices);
        offers = List.copyOf(offers);
    }

    public ClientboundOriginalNpcPacket(int entityId, UUID sessionId, String name, String text,
                                        OriginalNpcInterpreter.Prompt prompt, List<String> choices) {
        this(entityId, sessionId, prompt.revision(), name, text, prompt.portrait(), prompt.portraitIndex(),
                prompt.type(), choices, prompt.goods().stream().map(goods -> {
                    OriginalNpcItems.Entry item = OriginalNpcItems.get(goods.kind(), goods.id());
                    return new Offer(item.name(), goods.price() >= 0L ? goods.price() : item.price(),
                            item.registry().toString());
                }).toList());
    }

    public ClientboundOriginalNpcPacket(FriendlyByteBuf buffer) {
        this(buffer.readVarInt(), buffer.readUUID(), buffer.readVarLong(), buffer.readUtf(512),
                buffer.readUtf(16384), buffer.readUtf(512), buffer.readVarInt(),
                buffer.readEnum(OriginalNpcInterpreter.Type.class), readChoices(buffer), readOffers(buffer));
    }

    public void toBytes(FriendlyByteBuf buffer) {
        buffer.writeVarInt(entityId);
        buffer.writeUUID(sessionId);
        buffer.writeVarLong(revision);
        buffer.writeUtf(name, 512);
        buffer.writeUtf(text, 16384);
        buffer.writeUtf(portrait, 512);
        buffer.writeVarInt(portraitIndex);
        buffer.writeEnum(type);
        buffer.writeVarInt(choices.size());
        choices.forEach(value -> buffer.writeUtf(value, 4096));
        buffer.writeVarInt(offers.size());
        for (Offer offer : offers) {
            buffer.writeUtf(offer.name(), 512);
            buffer.writeVarLong(offer.price());
            buffer.writeUtf(offer.itemRegistry(), 512);
        }
    }

    public void handle(Supplier<NetworkEvent.Context> context) {
        PacketHandlers.handleClient(context, () -> ClientHandler.open(this));
    }

    private static List<String> readChoices(FriendlyByteBuf buffer) {
        int count = count(buffer, 64);
        List<String> result = new ArrayList<>();
        for (int index = 0; index < count; index++) {
            result.add(buffer.readUtf(4096));
        }
        return result;
    }

    private static List<Offer> readOffers(FriendlyByteBuf buffer) {
        int count = count(buffer, 256);
        List<Offer> result = new ArrayList<>();
        for (int index = 0; index < count; index++) {
            result.add(new Offer(buffer.readUtf(512), buffer.readVarLong(), buffer.readUtf(512)));
        }
        return result;
    }

    private static int count(FriendlyByteBuf buffer, int maximum) {
        int count = buffer.readVarInt();
        if (count < 0 || count > maximum) {
            throw new IllegalArgumentException("Invalid original NPC packet count");
        }
        return count;
    }

    public record Offer(String name, long price, String itemRegistry) {
    }

    private static final class ClientHandler {
        private static void open(ClientboundOriginalNpcPacket packet) {
            net.minecraft.client.Minecraft minecraft = net.minecraft.client.Minecraft.getInstance();
            if (packet.type() == OriginalNpcInterpreter.Type.BATTLE) {
                com.BlackSouls.BlackSoulsMod.client.OriginalNpcBattleContinuation.begin(packet);
                if (minecraft.screen instanceof com.BlackSouls.BlackSoulsMod.client.gui.GuiOriginalNpcDialogue) {
                    minecraft.setScreen(null);
                }
                return;
            }
            if (packet.type() == OriginalNpcInterpreter.Type.END || packet.type() == OriginalNpcInterpreter.Type.BLOCKED) {
                com.BlackSouls.BlackSoulsMod.client.OriginalNpcBattleContinuation.clear(packet);
                if (minecraft.screen instanceof com.BlackSouls.BlackSoulsMod.client.gui.GuiOriginalNpcDialogue dialogue
                        && dialogue.isConversation(packet.entityId(), packet.sessionId())) {
                    minecraft.setScreen(null);
                }
                return;
            }
            com.BlackSouls.BlackSoulsMod.client.OriginalNpcBattleContinuation.clear();
            if (minecraft.screen instanceof com.BlackSouls.BlackSoulsMod.client.gui.GuiOriginalNpcDialogue dialogue
                    && dialogue.isConversation(packet.entityId(), packet.sessionId())) {
                dialogue.update(packet);
            } else {
                minecraft.setScreen(new com.BlackSouls.BlackSoulsMod.client.gui.GuiOriginalNpcDialogue(packet));
            }
        }
    }
}
