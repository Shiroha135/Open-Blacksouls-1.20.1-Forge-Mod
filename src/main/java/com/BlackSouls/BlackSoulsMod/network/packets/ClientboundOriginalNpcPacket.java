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
                                           OriginalNpcInterpreter.Type type, List<String> choices, List<Offer> offers,
                                           ShopView shopView) {
    public ClientboundOriginalNpcPacket {
        choices = List.copyOf(choices);
        offers = List.copyOf(offers);
        if (shopView != null && type != OriginalNpcInterpreter.Type.SHOP) {
            throw new IllegalArgumentException("Shop state outside an original shop prompt");
        }
    }

    public ClientboundOriginalNpcPacket(int entityId, UUID sessionId, long revision,
                                       String name, String text, String portrait, int portraitIndex,
                                       OriginalNpcInterpreter.Type type, List<String> choices, List<Offer> offers) {
        this(entityId, sessionId, revision, name, text, portrait, portraitIndex, type, choices, offers, null);
    }

    public ClientboundOriginalNpcPacket(int entityId, UUID sessionId, String name, String text,
                                        OriginalNpcInterpreter.Prompt prompt, List<String> choices) {
        this(entityId, sessionId, name, text, prompt, choices, null);
    }

    public ClientboundOriginalNpcPacket(int entityId, UUID sessionId, String name, String text,
                                       OriginalNpcInterpreter.Prompt prompt, List<String> choices, ShopView shopView) {
        this(entityId, sessionId, prompt.revision(), name, text, prompt.portrait(), prompt.portraitIndex(),
                prompt.type(), choices, prompt.goods().stream().map(goods -> {
                    OriginalNpcItems.Entry item = OriginalNpcItems.get(goods.kind(), goods.id());
                    return new Offer(item.name(), goods.price() >= 0L ? goods.price() : item.price(),
                            item.registry().toString());
                }).toList(), shopView);
    }

    public ClientboundOriginalNpcPacket(FriendlyByteBuf buffer) {
        this(buffer.readVarInt(), buffer.readUUID(), buffer.readVarLong(), buffer.readUtf(512),
                buffer.readUtf(16384), buffer.readUtf(512), buffer.readVarInt(),
                buffer.readEnum(OriginalNpcInterpreter.Type.class), readChoices(buffer), readOffers(buffer), readShopView(buffer));
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
        buffer.writeBoolean(shopView != null);
        if (shopView != null) {
            buffer.writeBoolean(shopView.storage());
            buffer.writeBoolean(shopView.purchaseOnly());
            buffer.writeBoolean(shopView.canToggleStorage());
            buffer.writeVarLong(shopView.money());
            writeTrades(buffer, shopView.buys());
            writeTrades(buffer, shopView.sells());
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

    private static ShopView readShopView(FriendlyByteBuf buffer) {
        return buffer.readBoolean() ? new ShopView(buffer.readBoolean(), buffer.readBoolean(), buffer.readBoolean(),
                buffer.readVarLong(), readTrades(buffer), readTrades(buffer)) : null;
    }

    private static List<TradeOffer> readTrades(FriendlyByteBuf buffer) {
        int count = count(buffer, 1150);
        List<TradeOffer> result = new ArrayList<>();
        for (int index = 0; index < count; index++) {
            result.add(new TradeOffer(buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt(),
                    buffer.readUtf(512), buffer.readUtf(512), buffer.readVarLong(), buffer.readVarInt(),
                    buffer.readVarInt(), buffer.readBoolean()));
        }
        return result;
    }

    private static void writeTrades(FriendlyByteBuf buffer, List<TradeOffer> offers) {
        buffer.writeVarInt(offers.size());
        for (TradeOffer offer : offers) {
            buffer.writeVarInt(offer.kind());
            buffer.writeVarInt(offer.id());
            buffer.writeVarInt(offer.category());
            buffer.writeUtf(offer.name(), 512);
            buffer.writeUtf(offer.registry(), 512);
            buffer.writeVarLong(offer.price());
            buffer.writeVarInt(offer.owned());
            buffer.writeVarInt(offer.maximum());
            buffer.writeBoolean(offer.enabled());
        }
    }

    public record ShopView(boolean storage, boolean purchaseOnly, boolean canToggleStorage, long money,
                           List<TradeOffer> buys, List<TradeOffer> sells) {
        public ShopView {
            buys = List.copyOf(buys);
            sells = List.copyOf(sells);
            if (money < 0L || buys.size() > 1150 || sells.size() > 1150) {
                throw new IllegalArgumentException("Invalid original shop view");
            }
        }
    }

    public record TradeOffer(int kind, int id, int category, String name, String registry,
                             long price, int owned, int maximum, boolean enabled) {
        public TradeOffer {
            if (kind < 0 || kind > 2 || id < 1 || category < 0 || category > 3
                    || name == null || registry == null || price < 0L || owned < 0 || maximum < 0 || maximum > 999) {
                throw new IllegalArgumentException("Invalid original shop offer");
            }
        }
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
