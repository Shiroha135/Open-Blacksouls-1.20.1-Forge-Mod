package yuki135.blacksouls.story;

import yuki135.blacksouls.util.HokoniwaDestination;
import yuki135.blacksouls.util.OriginalWorldEventData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.Vec3;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class OriginalWorldTransferData extends SavedData {
    public static final String DATA_NAME = "blacksouls_original_world_transfers";
    private static final Set<String> FIELDS = Set.of("Owner", "Source", "SourceEvent", "Dimension", "FromX", "FromY", "FromZ",
            "TargetX", "TargetY", "TargetZ", "Yaw", "Fade", "Elapsed", "Transferred", "FadeInSent");
    final Map<UUID, Ticket> tickets = new LinkedHashMap<>();
    int lastServerTick = Integer.MIN_VALUE;

    public static OriginalWorldTransferData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(OriginalWorldTransferData::load,
                OriginalWorldTransferData::new, DATA_NAME);
    }

    public static OriginalWorldTransferData load(CompoundTag saved) {
        OriginalWorldTransferData data = new OriginalWorldTransferData();
        if (saved.isEmpty()) return data;
        if (!saved.getAllKeys().equals(Set.of("Version", "Tickets")) || !saved.contains("Version", Tag.TAG_INT)
                || saved.getInt("Version") != 1 || !saved.contains("Tickets", Tag.TAG_LIST)) throw new IllegalArgumentException("Invalid original transfer data");
        ListTag list = (ListTag) saved.get("Tickets");
        if (!list.isEmpty() && list.getElementType() != Tag.TAG_COMPOUND) throw new IllegalArgumentException("Invalid original transfer list");
        for (Tag element : list) {
            CompoundTag value = (CompoundTag) element;
            if (!value.getAllKeys().equals(FIELDS) || !value.hasUUID("Owner") || !value.hasUUID("Source")
                    || !value.contains("SourceEvent", Tag.TAG_STRING) || !value.contains("Dimension", Tag.TAG_STRING)
                    || !value.contains("Yaw", Tag.TAG_FLOAT) || !value.contains("Fade", Tag.TAG_INT)
                    || !value.contains("Elapsed", Tag.TAG_INT) || !value.contains("Transferred", Tag.TAG_BYTE)
                    || !value.contains("FadeInSent", Tag.TAG_BYTE)) throw new IllegalArgumentException("Invalid original transfer ticket");
            for (String key : Set.of("FromX", "FromY", "FromZ", "TargetX", "TargetY", "TargetZ")) {
                if (!value.contains(key, Tag.TAG_DOUBLE) || !coordinate(value.getDouble(key))) throw new IllegalArgumentException("Invalid original transfer position");
            }
            UUID owner = value.getUUID("Owner"), source = value.getUUID("Source");
            String sourceEvent = value.getString("SourceEvent"), dimension = value.getString("Dimension");
            int elapsed = value.getInt("Elapsed");
            if (owner.equals(source) || !OriginalWorldEventData.isNativeBlock(sourceEvent)
                    || !HokoniwaDestination.DIMENSION.location().toString().equals(dimension)
                    || !Float.isFinite(value.getFloat("Yaw")) || value.getInt("Fade") != 0
                    || value.getByte("Transferred") < 0 || value.getByte("Transferred") > 1
                    || value.getByte("FadeInSent") < 0 || value.getByte("FadeInSent") > 1
                    || elapsed < 0 || elapsed >= OriginalWorldTransferService.TOTAL_FRAMES
                    || value.getBoolean("Transferred") != (elapsed >= OriginalWorldTransferService.OUT_FRAMES)
                    || value.getBoolean("FadeInSent") != (elapsed >= OriginalWorldTransferService.IN_START_FRAME)) {
                throw new IllegalArgumentException("Invalid original transfer phase");
            }
            Ticket ticket = new Ticket(owner, source, sourceEvent, dimension,
                    new Vec3(value.getDouble("FromX"), value.getDouble("FromY"), value.getDouble("FromZ")),
                    new Vec3(value.getDouble("TargetX"), value.getDouble("TargetY"), value.getDouble("TargetZ")), value.getFloat("Yaw"));
            ticket.elapsed = elapsed;
            ticket.transferred = value.getBoolean("Transferred");
            ticket.fadeInSent = value.getBoolean("FadeInSent");
            if (data.tickets.put(owner, ticket) != null) throw new IllegalArgumentException("Duplicate original transfer owner");
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag saved) {
        saved.putInt("Version", 1);
        ListTag list = new ListTag();
        for (Ticket ticket : tickets.values()) {
            CompoundTag value = new CompoundTag();
            value.putUUID("Owner", ticket.owner);
            value.putUUID("Source", ticket.source);
            value.putString("SourceEvent", ticket.sourceEvent);
            value.putString("Dimension", ticket.dimension);
            value.putDouble("FromX", ticket.from.x);
            value.putDouble("FromY", ticket.from.y);
            value.putDouble("FromZ", ticket.from.z);
            value.putDouble("TargetX", ticket.destination.x);
            value.putDouble("TargetY", ticket.destination.y);
            value.putDouble("TargetZ", ticket.destination.z);
            value.putFloat("Yaw", ticket.yaw);
            value.putInt("Fade", 0);
            value.putInt("Elapsed", ticket.elapsed);
            value.putBoolean("Transferred", ticket.transferred);
            value.putBoolean("FadeInSent", ticket.fadeInSent);
            list.add(value);
        }
        saved.put("Tickets", list);
        return saved;
    }

    static boolean coordinate(double value) {
        return Double.isFinite(value) && Math.abs(value) <= 30000000.0D;
    }

    static final class Ticket {
        final UUID owner;
        final UUID source;
        final String sourceEvent;
        final String dimension;
        final Vec3 from;
        final Vec3 destination;
        final float yaw;
        int elapsed;
        boolean transferred;
        boolean fadeInSent;

        Ticket(UUID owner, UUID source, String sourceEvent, String dimension, Vec3 from, Vec3 destination, float yaw) {
            this.owner = owner;
            this.source = source;
            this.sourceEvent = sourceEvent;
            this.dimension = dimension;
            this.from = from;
            this.destination = destination;
            this.yaw = yaw;
        }
    }
}
