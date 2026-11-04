package com.BlackSouls.BlackSoulsMod.story;

import com.google.gson.JsonParser;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.HashSet;
import java.util.ArrayDeque;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class OriginalNpcMovementData extends SavedData {
    final Map<String, Ticket> tickets = new LinkedHashMap<>();
    final Set<String> activeTickets = new LinkedHashSet<>();
    final Map<UUID, String> latestByTarget = new LinkedHashMap<>();
    int lastServerTick = Integer.MIN_VALUE;

    public static OriginalNpcMovementData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(OriginalNpcMovementData::load,
                OriginalNpcMovementData::new, "blacksouls_original_npc_movement");
    }

    public static OriginalNpcMovementData load(CompoundTag saved) {
        OriginalNpcMovementData data = new OriginalNpcMovementData();
        for (Tag element : saved.getList("Tickets", Tag.TAG_COMPOUND)) {
            CompoundTag value = (CompoundTag) element;
            String id = value.getString("Ticket");
            UUID.fromString(id);
            Ticket ticket = new Ticket(id, value.getUUID("Owner"), value.getUUID("Target"),
                    value.getString("Dimension"), value.getString("NpcId"),
                    OriginalNpcMoveRouteRunner.restore(JsonParser.parseString(new String(
                            value.getByteArray("Runner"), StandardCharsets.UTF_8)).getAsJsonObject()));
            ticket.complete = value.getBoolean("Complete");
            ticket.awaited = value.contains("Awaited") ? value.getBoolean("Awaited") : ticket.runner.waitsForCompletion();
            ticket.released = value.contains("Released") ? value.getBoolean("Released") : !ticket.awaited;
            ticket.replacement = value.getString("Replacement");
            ticket.failure = value.getString("Failure");
            if (!ticket.replacement.isEmpty()) UUID.fromString(ticket.replacement);
            if (data.tickets.put(id, ticket) != null) throw new IllegalArgumentException("Duplicate movement ticket");
            data.latestByTarget.put(ticket.target, id);
            if (!ticket.complete && ticket.replacement.isEmpty()) data.activeTickets.add(id);
        }
        data.collect();
        return data;
    }

    void add(Ticket ticket) {
        String previousId = latestByTarget.get(ticket.target);
        Ticket previous = tickets.get(previousId);
        if (previous != null) {
            previous.replacement = ticket.id;
            activeTickets.remove(previous.id);
        }
        tickets.put(ticket.id, ticket);
        latestByTarget.put(ticket.target, ticket.id);
        activeTickets.add(ticket.id);
        collect();
        setDirty();
    }

    void finish(Ticket ticket) {
        ticket.complete = true;
        activeTickets.remove(ticket.id);
        setDirty();
    }

    void collect() {
        Set<String> reachable = new HashSet<>();
        ArrayDeque<String> pending = new ArrayDeque<>(activeTickets);
        for (Ticket ticket : tickets.values()) {
            if (ticket.awaited && !ticket.released) pending.add(ticket.id);
        }
        while (!pending.isEmpty()) {
            String id = pending.removeFirst();
            if (!reachable.add(id)) continue;
            Ticket ticket = tickets.get(id);
            if (ticket != null && !ticket.replacement.isEmpty()) pending.add(ticket.replacement);
        }
        boolean changed = tickets.entrySet().removeIf(entry -> {
            if (reachable.contains(entry.getKey())) return false;
            latestByTarget.remove(entry.getValue().target, entry.getKey());
            return true;
        });
        activeTickets.removeIf(id -> !tickets.containsKey(id));
        if (changed) setDirty();
    }

    @Override
    public CompoundTag save(CompoundTag saved) {
        ListTag entries = new ListTag();
        for (Ticket ticket : tickets.values()) {
            CompoundTag value = new CompoundTag();
            value.putString("Ticket", ticket.id);
            value.putUUID("Owner", ticket.owner);
            value.putUUID("Target", ticket.target);
            value.putString("Dimension", ticket.dimension);
            value.putString("NpcId", ticket.npcId);
            value.putByteArray("Runner", ticket.runner.snapshot().toString().getBytes(StandardCharsets.UTF_8));
            value.putBoolean("Complete", ticket.complete);
            value.putBoolean("Awaited", ticket.awaited);
            value.putBoolean("Released", ticket.released);
            value.putString("Replacement", ticket.replacement);
            value.putString("Failure", ticket.failure);
            entries.add(value);
        }
        saved.put("Tickets", entries);
        return saved;
    }

    static final class Ticket {
        final String id;
        final UUID owner;
        final UUID target;
        final String dimension;
        final String npcId;
        final OriginalNpcMoveRouteRunner runner;
        boolean complete;
        boolean awaited;
        boolean released;
        boolean validated;
        String replacement = "";
        String failure = "";

        Ticket(String id, UUID owner, UUID target, String dimension, String npcId, OriginalNpcMoveRouteRunner runner) {
            this.id = id;
            this.owner = owner;
            this.target = target;
            this.dimension = dimension;
            this.npcId = npcId;
            this.runner = runner;
            this.awaited = runner.waitsForCompletion();
            this.released = !awaited;
        }
    }
}
