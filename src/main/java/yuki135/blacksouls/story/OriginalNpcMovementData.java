package yuki135.blacksouls.story;

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
            ticket.sourceMap = value.getInt("SourceMap");
            if (ticket.sourceMap < 0 || ticket.sourceMap > 999) throw new IllegalArgumentException("Invalid original movement source map");
            ticket.gravityCaptured = value.getBoolean("GravityCaptured");
            ticket.originalNoGravity = value.getBoolean("OriginalNoGravity");
            if (value.contains("ChickenRoute")) {
                if (!value.contains("ChickenRoute",Tag.TAG_COMPOUND)) throw new IllegalArgumentException("Invalid chicken route authority tag");
                ticket.chicken=OriginalChickenRoutes.Authority.load(value.getCompound("ChickenRoute"));
                OriginalChickenRoutes.validateTicket(ticket);
            }
            if (value.contains("Motion")) {
                if (!value.contains("Motion", Tag.TAG_BYTE_ARRAY)) throw new IllegalArgumentException("Invalid original movement motion");
                ticket.motion = OriginalNpcMotion.restore(JsonParser.parseString(new String(value.getByteArray("Motion"), StandardCharsets.UTF_8)).getAsJsonObject());
                if (!ticket.gravityCaptured || ticket.complete || ticket.motion.complete()
                        || ticket.chicken==null && ticket.sourceMap != 62 && !(ticket.sourceMap == 37 && (ticket.npcId.equals("map037_ev053") || OriginalPoolCrowdSurface.isCrowd(ticket.npcId))
                        && ticket.dimension.equals(yuki135.blacksouls.util.HokoniwaDestination.DIMENSION.location().toString())
                        && OriginalNpcMovementService.isOriginalMap37Position(ticket.npcId, ticket.motion.x(), ticket.motion.z())
                        && OriginalNpcMovementService.isOriginalMap37Position(ticket.npcId, ticket.motion.snapshot().get("startX").getAsDouble(),
                        ticket.motion.snapshot().get("startZ").getAsDouble())
                        && OriginalNpcMovementService.isOriginalMap37Position(ticket.npcId, ticket.motion.snapshot().get("targetX").getAsDouble(),
                        ticket.motion.snapshot().get("targetZ").getAsDouble()))) throw new IllegalArgumentException("Invalid active original motion phase");
            }
            if (ticket.chicken!=null) OriginalChickenRoutes.validateTicket(ticket);
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
            if (previous.motion != null) {
                ticket.motion = previous.motion;
                ticket.gravityCaptured = previous.gravityCaptured;
                ticket.originalNoGravity = previous.originalNoGravity;
                previous.motion = null;
            }
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
            value.putInt("SourceMap", ticket.sourceMap);
            value.putBoolean("GravityCaptured", ticket.gravityCaptured);
            value.putBoolean("OriginalNoGravity", ticket.originalNoGravity);
            if (ticket.chicken!=null) value.put("ChickenRoute",ticket.chicken.save());
            if (ticket.motion != null) value.putByteArray("Motion", ticket.motion.snapshot().toString().getBytes(StandardCharsets.UTF_8));
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
        int sourceMap;
        boolean gravityCaptured;
        boolean originalNoGravity;
        OriginalNpcMotion motion;
        OriginalChickenRoutes.Authority chicken;
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
