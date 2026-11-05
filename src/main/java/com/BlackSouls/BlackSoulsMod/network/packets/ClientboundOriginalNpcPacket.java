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
                                           ShopView shopView, KeepView keepView, SynthesisView synthesisView) {
    public ClientboundOriginalNpcPacket {
        choices = List.copyOf(choices);
        offers = List.copyOf(offers);
        if (shopView != null && type != OriginalNpcInterpreter.Type.SHOP) {
            throw new IllegalArgumentException("Shop state outside an original shop prompt");
        }
        if (keepView != null && type != OriginalNpcInterpreter.Type.ITEM_KEEP) {
            throw new IllegalArgumentException("Storage state outside an original item keep prompt");
        }
        if (synthesisView != null && type != OriginalNpcInterpreter.Type.SYNTHESIS) {
            throw new IllegalArgumentException("Synthesis state outside an original synthesis prompt");
        }
    }

    public ClientboundOriginalNpcPacket(int entityId, UUID sessionId, long revision,
                                       String name, String text, String portrait, int portraitIndex,
                                       OriginalNpcInterpreter.Type type, List<String> choices, List<Offer> offers,
                                       ShopView shopView, KeepView keepView) {
        this(entityId, sessionId, revision, name, text, portrait, portraitIndex, type, choices, offers,
                shopView, keepView, null);
    }

    public ClientboundOriginalNpcPacket(int entityId, UUID sessionId, long revision,
                                       String name, String text, String portrait, int portraitIndex,
                                       OriginalNpcInterpreter.Type type, List<String> choices, List<Offer> offers,
                                       ShopView shopView) {
        this(entityId, sessionId, revision, name, text, portrait, portraitIndex, type, choices, offers, shopView, null);
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
        this(entityId, sessionId, name, text, prompt, choices, shopView, null);
    }

    public ClientboundOriginalNpcPacket(int entityId, UUID sessionId, String name, String text,
                                       OriginalNpcInterpreter.Prompt prompt, List<String> choices,
                                       ShopView shopView, KeepView keepView) {
        this(entityId, sessionId, name, text, prompt, choices, shopView, keepView, null);
    }

    public ClientboundOriginalNpcPacket(int entityId, UUID sessionId, String name, String text,
                                       OriginalNpcInterpreter.Prompt prompt, List<String> choices,
                                       ShopView shopView, KeepView keepView, SynthesisView synthesisView) {
        this(entityId, sessionId, prompt.revision(), name, text, prompt.portrait(), prompt.portraitIndex(),
                prompt.type(), choices, prompt.goods().stream().map(goods -> {
                    OriginalNpcItems.Entry item = OriginalNpcItems.get(goods.kind(), goods.id());
                    return new Offer(item.name(), goods.price() >= 0L ? goods.price() : item.price(),
                            item.registry().toString());
                }).toList(), shopView, keepView, synthesisView);
    }

    public ClientboundOriginalNpcPacket(FriendlyByteBuf buffer) {
        this(buffer.readVarInt(), buffer.readUUID(), buffer.readVarLong(), buffer.readUtf(512),
                buffer.readUtf(16384), buffer.readUtf(512), buffer.readVarInt(),
                buffer.readEnum(OriginalNpcInterpreter.Type.class), readChoices(buffer), readOffers(buffer), readShopView(buffer), readKeepView(buffer), readSynthesisView(buffer));
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
            buffer.writeBoolean(shopView.exchange());
            buffer.writeVarLong(shopView.money());
            writeTrades(buffer, shopView.buys());
            writeTrades(buffer, shopView.sells());
        }
        buffer.writeBoolean(keepView != null);
        if (keepView != null) {
            buffer.writeVarInt(keepView.offers().size());
            for (KeepOffer offer : keepView.offers()) {
                buffer.writeVarInt(offer.kind());
                buffer.writeVarInt(offer.id());
                buffer.writeVarInt(offer.category());
                buffer.writeUtf(offer.name(), 512);
                buffer.writeUtf(offer.registry(), 512);
                buffer.writeVarInt(offer.carried());
                buffer.writeVarInt(offer.stored());
                buffer.writeVarInt(offer.maxDeposit());
                buffer.writeVarInt(offer.maxWithdraw());
                buffer.writeVarInt(offer.storageLimit());
                buffer.writeBoolean(offer.storageForbidden());
            }
        }
        buffer.writeBoolean(synthesisView != null);
        if (synthesisView != null) {
            buffer.writeVarLong(synthesisView.money());
            buffer.writeBoolean(synthesisView.costVisible());
            buffer.writeVarInt(synthesisView.offers().size());
            for (SynthesisOffer offer : synthesisView.offers()) {
                buffer.writeVarInt(offer.kind());
                buffer.writeVarInt(offer.id());
                buffer.writeVarInt(offer.category());
                buffer.writeUtf(offer.name(), 512);
                buffer.writeUtf(offer.registry(), 512);
                buffer.writeUtf(offer.description(), 8192);
                buffer.writeVarInt(offer.owned());
                buffer.writeVarInt(offer.maximum());
                buffer.writeBoolean(offer.enabled());
                buffer.writeVarLong(offer.cost());
                offer.parameters().forEach(buffer::writeVarInt);
                buffer.writeVarInt(offer.materials().size());
                for (SynthesisMaterial material : offer.materials()) {
                    buffer.writeVarInt(material.kind());
                    buffer.writeVarInt(material.id());
                    buffer.writeUtf(material.name(), 512);
                    buffer.writeUtf(material.registry(), 512);
                    buffer.writeVarInt(material.required());
                    buffer.writeVarInt(material.owned());
                }
                buffer.writeVarInt(offer.comparisons().size());
                for (SynthesisComparison comparison : offer.comparisons()) {
                    buffer.writeVarInt(comparison.actorId());
                    buffer.writeUtf(comparison.actorName(), 512);
                    buffer.writeBoolean(comparison.equippable());
                    buffer.writeUtf(comparison.equippedName(), 512);
                    buffer.writeVarInt(comparison.attackDifference());
                }
            }
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
                buffer.readBoolean(), buffer.readVarLong(), readTrades(buffer), readTrades(buffer)) : null;
    }

    private static KeepView readKeepView(FriendlyByteBuf buffer) {
        if (!buffer.readBoolean()) return null;
        int count = count(buffer, 1150);
        List<KeepOffer> result = new ArrayList<>();
        for (int index = 0; index < count; index++) {
            result.add(new KeepOffer(buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt(),
                    buffer.readUtf(512), buffer.readUtf(512), buffer.readVarInt(), buffer.readVarInt(),
                    buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt(), buffer.readBoolean()));
        }
        return new KeepView(result);
    }

    private static SynthesisView readSynthesisView(FriendlyByteBuf buffer) {
        if (!buffer.readBoolean()) return null;
        long money = buffer.readVarLong();
        boolean costVisible = buffer.readBoolean();
        int count = count(buffer, 1150);
        List<SynthesisOffer> offers = new ArrayList<>();
        for (int index = 0; index < count; index++) {
            int kind = buffer.readVarInt();
            int id = buffer.readVarInt();
            int category = buffer.readVarInt();
            String name = buffer.readUtf(512);
            String registry = buffer.readUtf(512);
            String description = buffer.readUtf(8192);
            int owned = buffer.readVarInt();
            int maximum = buffer.readVarInt();
            boolean enabled = buffer.readBoolean();
            long cost = buffer.readVarLong();
            List<Integer> parameters = new ArrayList<>();
            for (int parameter = 0; parameter < 8; parameter++) parameters.add(buffer.readVarInt());
            int materialCount = count(buffer, 64);
            List<SynthesisMaterial> materials = new ArrayList<>();
            for (int material = 0; material < materialCount; material++) {
                materials.add(new SynthesisMaterial(buffer.readVarInt(), buffer.readVarInt(), buffer.readUtf(512),
                        buffer.readUtf(512), buffer.readVarInt(), buffer.readVarInt()));
            }
            int comparisonCount = count(buffer, 16);
            List<SynthesisComparison> comparisons = new ArrayList<>();
            for (int comparison = 0; comparison < comparisonCount; comparison++) {
                comparisons.add(new SynthesisComparison(buffer.readVarInt(), buffer.readUtf(512), buffer.readBoolean(),
                        buffer.readUtf(512), buffer.readVarInt()));
            }
            offers.add(new SynthesisOffer(kind, id, category, name, registry, description, owned, maximum,
                    enabled, cost, parameters, materials, comparisons));
        }
        return new SynthesisView(money, costVisible, offers);
    }

    private static List<TradeOffer> readTrades(FriendlyByteBuf buffer) {
        int count = count(buffer, 1150);
        List<TradeOffer> result = new ArrayList<>();
        for (int index = 0; index < count; index++) {
            result.add(new TradeOffer(buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt(),
                    buffer.readUtf(512), buffer.readUtf(512), buffer.readVarLong(), buffer.readVarInt(),
                    buffer.readVarInt(), buffer.readBoolean(), readTradeMaterials(buffer)));
        }
        return result;
    }

    private static List<SynthesisMaterial> readTradeMaterials(FriendlyByteBuf buffer) {
        int count = count(buffer, 64);
        List<SynthesisMaterial> materials = new ArrayList<>();
        for (int index = 0; index < count; index++) {
            materials.add(new SynthesisMaterial(buffer.readVarInt(), buffer.readVarInt(), buffer.readUtf(512),
                    buffer.readUtf(512), buffer.readVarInt(), buffer.readVarInt()));
        }
        return materials;
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
            buffer.writeVarInt(offer.materials().size());
            for (SynthesisMaterial material : offer.materials()) {
                buffer.writeVarInt(material.kind());
                buffer.writeVarInt(material.id());
                buffer.writeUtf(material.name(), 512);
                buffer.writeUtf(material.registry(), 512);
                buffer.writeVarInt(material.required());
                buffer.writeVarInt(material.owned());
            }
        }
    }

    public record ShopView(boolean storage, boolean purchaseOnly, boolean canToggleStorage, boolean exchange, long money,
                           List<TradeOffer> buys, List<TradeOffer> sells) {
        public ShopView(boolean storage, boolean purchaseOnly, boolean canToggleStorage, long money,
                        List<TradeOffer> buys, List<TradeOffer> sells) {
            this(storage, purchaseOnly, canToggleStorage, false, money, buys, sells);
        }

        public ShopView {
            buys = List.copyOf(buys);
            sells = List.copyOf(sells);
            if (money < 0L || buys.size() > 1150 || sells.size() > 1150) {
                throw new IllegalArgumentException("Invalid original shop view");
            }
        }
    }

    public record TradeOffer(int kind, int id, int category, String name, String registry,
                             long price, int owned, int maximum, boolean enabled, List<SynthesisMaterial> materials) {
        public TradeOffer(int kind, int id, int category, String name, String registry,
                          long price, int owned, int maximum, boolean enabled) {
            this(kind, id, category, name, registry, price, owned, maximum, enabled, List.of());
        }

        public TradeOffer {
            materials = List.copyOf(materials);
            if (kind < 0 || kind > 2 || id < 1 || category < 0 || category > 3
                    || name == null || registry == null || price < 0L || owned < 0 || maximum < 0 || maximum > 999
                    || materials.size() > 64) {
                throw new IllegalArgumentException("Invalid original shop offer");
            }
        }
    }

    public record KeepView(List<KeepOffer> offers) {
        public KeepView {
            offers = List.copyOf(offers);
            if (offers.size() > 1150) throw new IllegalArgumentException("Invalid original storage view");
        }
    }

    public record KeepOffer(int kind, int id, int category, String name, String registry,
                            int carried, int stored, int maxDeposit, int maxWithdraw, int storageLimit, boolean storageForbidden) {
        public KeepOffer {
            if (kind < 0 || kind > 2 || id < 1 || category < 0 || category > 3
                    || name == null || registry == null || carried < 0 || stored < 0 || stored > 999
                    || storageLimit < 0 || storageLimit > 999 || maxDeposit < 0 || maxDeposit > 999
                    || maxDeposit > carried || maxWithdraw < 0 || maxWithdraw > 999 || maxWithdraw > stored
                    || storageForbidden && (maxDeposit != 0 || maxWithdraw != 0)) {
                throw new IllegalArgumentException("Invalid original storage offer");
            }
        }
    }

    public record SynthesisView(long money, boolean costVisible, List<SynthesisOffer> offers) {
        public SynthesisView {
            offers = List.copyOf(offers);
            if (money < 0L || offers.size() > 1150) throw new IllegalArgumentException("Invalid original synthesis view");
        }
    }

    public record SynthesisOffer(int kind, int id, int category, String name, String registry, String description,
                                 int owned, int maximum, boolean enabled, long cost, List<Integer> parameters,
                                 List<SynthesisMaterial> materials, List<SynthesisComparison> comparisons) {
        public SynthesisOffer {
            parameters = List.copyOf(parameters);
            materials = List.copyOf(materials);
            comparisons = List.copyOf(comparisons);
            if (kind < 0 || kind > 2 || id < 1 || category < 0 || category > 3 || name == null || registry == null
                    || description == null || owned < 0 || maximum < 0 || maximum > 99 || cost < 0L
                    || parameters.size() != 8 || materials.size() > 64 || comparisons.size() > 16) {
                throw new IllegalArgumentException("Invalid original synthesis offer");
            }
        }
    }

    public record SynthesisMaterial(int kind, int id, String name, String registry, int required, int owned) {
        public SynthesisMaterial {
            if (kind < 0 || kind > 2 || id < 1 || name == null || registry == null || required < 0 || owned < 0) {
                throw new IllegalArgumentException("Invalid original synthesis material");
            }
        }
    }

    public record SynthesisComparison(int actorId, String actorName, boolean equippable,
                                      String equippedName, int attackDifference) {
        public SynthesisComparison {
            if (actorId < 1 || actorName == null || equippedName == null) {
                throw new IllegalArgumentException("Invalid original synthesis comparison");
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
