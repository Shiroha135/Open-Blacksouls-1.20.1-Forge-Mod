package yuki135.blacksouls.compat.scene;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class OriginalNpcPlacementData extends SavedData {
    public static final String DATA_NAME = "blacksouls_original_npc_placements";
    private final Map<String, UUID> installed = new HashMap<>();
    private final Map<String, VisualMigration> migrations = new HashMap<>();

    public static OriginalNpcPlacementData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(OriginalNpcPlacementData::load,
                OriginalNpcPlacementData::new, DATA_NAME);
    }

    public boolean isInstalled(String npcId) {
        return installed.containsKey(npcId);
    }

    public UUID entityId(String npcId) {
        return installed.get(npcId);
    }

    public boolean record(String npcId, UUID entityId) {
        if (npcId == null || npcId.isBlank() || entityId == null) {
            throw new IllegalArgumentException("Invalid original NPC placement record");
        }
        if (installed.putIfAbsent(npcId, entityId) != null) {
            return false;
        }
        setDirty();
        return true;
    }

    public VisualMigration visualMigration(String npcId) {
        return migrations.get(npcId);
    }

    public boolean prepareVisualMigration(String npcId, BlockPos position, CompoundTag state, UUID entityId) {
        if (npcId == null || npcId.isBlank() || position == null || state == null || entityId == null) {
            throw new IllegalArgumentException("Invalid original visual migration");
        }
        if (installed.containsKey(npcId) || migrations.containsKey(npcId)) return false;
        migrations.put(npcId, new VisualMigration(npcId, position.immutable(), state, entityId, false));
        setDirty();
        return true;
    }

    public void completeVisualMigration(String npcId) {
        VisualMigration migration = migrations.get(npcId);
        if (migration == null || installed.containsKey(npcId) && !installed.get(npcId).equals(migration.entityId())) {
            throw new IllegalStateException("Missing or conflicting original visual migration");
        }
        migrations.put(npcId, new VisualMigration(npcId, migration.position(), migration.state(), migration.entityId(), true));
        record(npcId, migration.entityId());
        setDirty();
    }

    public void cancelVisualMigration(String npcId) {
        VisualMigration migration = migrations.get(npcId);
        if (migration != null && !migration.complete()) {
            migrations.remove(npcId);
            setDirty();
        }
    }

    public record VisualMigration(String npcId, BlockPos position, CompoundTag state, UUID entityId, boolean complete) {
        public VisualMigration {
            position = position.immutable();
            state = state.copy();
        }

        @Override public CompoundTag state() {
            return state.copy();
        }
    }

    public static OriginalNpcPlacementData load(CompoundTag tag) {
        OriginalNpcPlacementData data = new OriginalNpcPlacementData();
        for (Tag value : tag.getList("Installed", Tag.TAG_COMPOUND)) {
            CompoundTag entry = (CompoundTag) value;
            String npcId = entry.getString("NpcId");
            if (!npcId.isBlank() && entry.hasUUID("EntityId")) {
                data.installed.putIfAbsent(npcId, entry.getUUID("EntityId"));
            }
        }
        for (Tag value : tag.getList("VisualMigrations", Tag.TAG_COMPOUND)) {
            CompoundTag entry = (CompoundTag) value;
            String id = entry.getString("NpcId");
            if (!id.isBlank() && entry.hasUUID("EntityId") && entry.contains("OriginalBlock", Tag.TAG_COMPOUND)) {
                data.migrations.putIfAbsent(id, new VisualMigration(id, BlockPos.of(entry.getLong("Pos")),
                        entry.getCompound("OriginalBlock"), entry.getUUID("EntityId"), entry.getBoolean("Complete")));
            }
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag entries = new ListTag();
        installed.keySet().stream().sorted().forEach(npcId -> {
            CompoundTag entry = new CompoundTag();
            entry.putString("NpcId", npcId);
            entry.putUUID("EntityId", installed.get(npcId));
            entries.add(entry);
        });
        tag.put("Installed", entries);
        ListTag visuals = new ListTag();
        migrations.values().stream().sorted(java.util.Comparator.comparing(VisualMigration::npcId)).forEach(value -> {
            CompoundTag entry = new CompoundTag();
            entry.putString("NpcId", value.npcId());
            entry.putUUID("EntityId", value.entityId());
            entry.putLong("Pos", value.position().asLong());
            entry.put("OriginalBlock", value.state());
            entry.putBoolean("Complete", value.complete());
            visuals.add(entry);
        });
        tag.put("VisualMigrations", visuals);
        return tag;
    }
}
