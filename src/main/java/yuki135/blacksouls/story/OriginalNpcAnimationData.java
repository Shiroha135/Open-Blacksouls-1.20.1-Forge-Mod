package yuki135.blacksouls.story;

import com.google.gson.JsonParser;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.level.saveddata.SavedData;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class OriginalNpcAnimationData extends SavedData {
    final Map<String, Channel> channels = new LinkedHashMap<>();
    final Map<String, Ticket> tickets = new LinkedHashMap<>();
    final Set<String> activeChannels = new LinkedHashSet<>();
    int lastServerTick = Integer.MIN_VALUE;

    public static OriginalNpcAnimationData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(OriginalNpcAnimationData::load,
                OriginalNpcAnimationData::new, "blacksouls_original_npc_animations");
    }

    public static OriginalNpcAnimationData load(CompoundTag saved) {
        OriginalNpcAnimationData data = new OriginalNpcAnimationData();
        for (Tag element : saved.getList("Channels", Tag.TAG_COMPOUND)) {
            CompoundTag value = (CompoundTag) element;
            Channel channel = new Channel(value.getUUID("Target"), value.getString("Dimension"), value.getString("NpcId"),
                    value.getUUID("Requester"), OriginalNpcAnimationState.restore(JsonParser.parseString(
                    new String(value.getByteArray("State"), StandardCharsets.UTF_8)).getAsJsonObject()));
            channel.activeOwner = value.hasUUID("ActiveOwner") ? value.getUUID("ActiveOwner") : null;
            channel.playbackId = value.hasUUID("PlaybackId") ? value.getUUID("PlaybackId") : null;
            if (channel.state.activeId() > 0 && channel.playbackId == null) {
                channel.playbackId = UUID.randomUUID();
                data.setDirty();
            }
            channel.paused = value.getBoolean("Paused");
            channel.entityId = value.contains("EntityId") ? value.getInt("EntityId") : -1;
            channel.x = value.getDouble("X");
            channel.y = value.getDouble("Y");
            channel.z = value.getDouble("Z");
            channel.failure = value.getString("Failure");
            if (data.channels.put(channel.key(), channel) != null) throw new IllegalArgumentException("Duplicate NPC animation target");
            if (channel.state.hasWork()) data.activeChannels.add(channel.key());
        }
        for (Tag element : saved.getList("Tickets", Tag.TAG_COMPOUND)) {
            CompoundTag value = (CompoundTag) element;
            String id = value.getString("Ticket");
            UUID.fromString(id);
            Ticket ticket = new Ticket(id, value.getUUID("Owner"), value.getString("Channel"));
            ticket.released = value.getBoolean("Released");
            if (data.tickets.put(id, ticket) != null) throw new IllegalArgumentException("Duplicate NPC animation ticket");
        }
        data.collect();
        return data;
    }

    void collect() {
        boolean changed = tickets.values().removeIf(ticket -> ticket.released);
        Set<String> roots = new HashSet<>(activeChannels);
        for (Ticket ticket : tickets.values()) roots.add(ticket.channel);
        changed |= channels.entrySet().removeIf(entry -> !roots.contains(entry.getKey()));
        activeChannels.removeIf(key -> !channels.containsKey(key));
        if (changed) setDirty();
    }

    @Override
    public CompoundTag save(CompoundTag saved) {
        ListTag channelValues = new ListTag();
        for (Channel channel : channels.values()) {
            CompoundTag value = new CompoundTag();
            value.putUUID("Target", channel.target);
            value.putString("Dimension", channel.dimension);
            value.putString("NpcId", channel.npcId);
            value.putUUID("Requester", channel.requester);
            if (channel.activeOwner != null) value.putUUID("ActiveOwner", channel.activeOwner);
            if (channel.playbackId != null) value.putUUID("PlaybackId", channel.playbackId);
            value.putBoolean("Paused", channel.paused);
            value.putInt("EntityId", channel.entityId);
            value.putDouble("X", channel.x);
            value.putDouble("Y", channel.y);
            value.putDouble("Z", channel.z);
            value.putByteArray("State", channel.state.snapshot().toString().getBytes(StandardCharsets.UTF_8));
            value.putString("Failure", channel.failure);
            channelValues.add(value);
        }
        saved.put("Channels", channelValues);
        ListTag ticketValues = new ListTag();
        for (Ticket ticket : tickets.values()) {
            CompoundTag value = new CompoundTag();
            value.putString("Ticket", ticket.id);
            value.putUUID("Owner", ticket.owner);
            value.putString("Channel", ticket.channel);
            value.putBoolean("Released", ticket.released);
            ticketValues.add(value);
        }
        saved.put("Tickets", ticketValues);
        return saved;
    }

    static final class Channel {
        final UUID target;
        final String dimension;
        final String npcId;
        final OriginalNpcAnimationState state;
        final Map<UUID, ServerGamePacketListenerImpl> observers = new HashMap<>();
        final Map<UUID, ServerGamePacketListenerImpl> listeners = new HashMap<>();
        final Set<UUID> stoppedTracking = new HashSet<>();
        UUID requester;
        UUID activeOwner;
        UUID playbackId;
        boolean paused;
        int entityId = -1;
        double x;
        double y;
        double z;
        String failure = "";

        Channel(UUID target, String dimension, String npcId, UUID requester, OriginalNpcAnimationState state) {
            this.target = target;
            this.dimension = dimension;
            this.npcId = npcId;
            this.requester = requester;
            this.state = state;
        }

        String key() {
            return dimension + ":" + target;
        }
    }

    static final class Ticket {
        final String id;
        final UUID owner;
        final String channel;
        boolean released;

        Ticket(String id, UUID owner, String channel) {
            this.id = id;
            this.owner = owner;
            this.channel = channel;
        }
    }
}
