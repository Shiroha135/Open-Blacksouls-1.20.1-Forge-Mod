package yuki135.blacksouls.story;

import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;
import net.minecraft.world.level.storage.LevelResource;
import static yuki135.blacksouls.story.OriginalOpeningScenes.*;

public final class OriginalOpeningSceneData extends SavedData {
    public static final String DATA_NAME = "blacksouls_original_opening_scenes";
    public static final int STORAGE_VERSION = 3;
    public static final int MAX_ACTIVE_JOURNALS = 64;
    public static final int MAX_CHANGES = 4096;
    private final Map<UUID,Entry> entries = new LinkedHashMap<>();
    private int nextSlot;

    public record Lease(UUID owner, int slot) {
        public Lease { if (owner == null || slot < 0 || slot >= MAX_SLOTS) throw new IllegalArgumentException("Invalid opening lease"); }
    }

    public static OriginalOpeningSceneData get(ServerLevel level) {
        if (!DIMENSION.equals(level.dimension())) throw new IllegalArgumentException("Opening lease storage requires its dedicated dimension");
        if (!level.getServer().isSameThread()) throw new IllegalStateException("Opening lease mutation requires the server thread");
        var file=DimensionType.getStorageFolder(level.dimension(),level.getServer().getWorldPath(LevelResource.ROOT)).resolve("data").resolve(DATA_NAME+".dat");
        return get(level.getDataStorage(),file);
    }

    static OriginalOpeningSceneData get(DimensionDataStorage storage, Path file) {
        var data=storage.get(OriginalOpeningSceneData::load,DATA_NAME);
        if (data!=null) return data;
        if (!Files.notExists(file,LinkOption.NOFOLLOW_LINKS)) throw new IllegalStateException("Existing or inaccessible opening scene storage could not be loaded: "+file);
        data=new OriginalOpeningSceneData(); storage.set(DATA_NAME,data); return data;
    }

    public Lease lease(UUID owner) {
        if (owner == null) throw new IllegalArgumentException("Opening owner is required");
        var existing = entries.get(owner); if (existing != null) return existing.lease;
        if (journalIdUsed(owner)) throw new IllegalArgumentException("Opening owner collides with an active journal identity");
        if (nextSlot >= MAX_SLOTS) throw new IllegalStateException("Opening scene slot capacity reached");
        var lease = new Lease(owner,nextSlot++); entries.put(owner,new Entry(lease)); setDirty(); return lease;
    }

    public Optional<Lease> find(UUID owner) { var entry = entries.get(owner); return entry == null ? Optional.empty() : Optional.of(entry.lease); }
    public boolean owns(UUID owner, Lease lease) { return owner != null && lease != null && owner.equals(lease.owner) && find(owner).filter(lease::equals).isPresent(); }
    public record JournalView(UUID id, boolean restore, SceneKind zone, boolean targetOp, SceneKind targetIsland,
                              Set<Cell> temporarySupports, int cursor, int total, boolean pending) {
        public JournalView { temporarySupports=Set.copyOf(temporarySupports); }
    }
    public record View(Lease lease, boolean opBuilt, SceneKind island, Set<Cell> temporarySupports, JournalView journal) {
        public View { temporarySupports=Set.copyOf(temporarySupports); }
    }
    public Optional<View> view(UUID owner, Lease lease) {
        var entry=entry(owner,lease); if (entry==null) return Optional.empty(); var journal=entry.journal;
        return Optional.of(new View(entry.lease,entry.layout.op(),entry.layout.island(),entry.layout.temporary(),journal==null?null:
                new JournalView(journal.id,journal.restore,journal.zone,journal.target.op(),journal.target.island(),journal.target.temporary(),
                        journal.cursor,OriginalOpeningSceneWorld.journalSize(entry),journal.pending)));
    }

    public int leaseCount() { return entries.size(); }
    public int activeJournalCount() { return (int) entries.values().stream().filter(entry -> entry.journal != null).count(); }

    UUID nextJournalId() {
        UUID result;
        do { result=UUID.randomUUID(); } while (journalIdUsed(result));
        return result;
    }
    private boolean journalIdUsed(UUID id) { return entries.containsKey(id) || entries.values().stream().anyMatch(entry -> entry.journal != null && entry.journal.id.equals(id)); }

    Entry entry(UUID owner, Lease lease) { return owns(owner,lease) ? entries.get(owner) : null; }

    static final class Entry {
        final Lease lease;
        Layout layout = Layout.EMPTY;
        Journal journal;
        Entry(Lease lease) { this.lease = lease; }
    }

    record Layout(boolean op, SceneKind island, Set<Cell> temporary, Cell fallCell) {
        Layout(boolean op, SceneKind island, Set<Cell> temporary) { this(op,island,temporary,null); }
        static final Layout EMPTY = new Layout(false,null,Set.of());
        Layout {
            if (island != null && !island.island() || temporary == null || island == null && !temporary.isEmpty()) throw new IllegalArgumentException("Invalid opening layout");
            temporary = Set.copyOf(temporary); if (island != null) requireTemporary(island,temporary);
            if (fallCell!=null && (island!=SceneKind.ISLAND_99 || !validCell(island,fallCell)
                    || !sourceSupport(island,fallCell)&&!temporary.contains(fallCell))) throw new IllegalArgumentException("Invalid opening fall shaft");
        }
        Layout with(SceneKind kind, Set<Cell> supports) { return kind == SceneKind.OP ? new Layout(true,island,temporary,fallCell) : new Layout(op,kind,supports); }
        Layout falling(Cell cell) { return new Layout(op,island,temporary,cell); }
        CompoundTag save() {
            var tag = new CompoundTag(); tag.putBoolean("Op",op); tag.putInt("Island",island == null ? 0 : island.sourceMap());
            tag.putIntArray("Temporary",temporary.stream().mapToInt(cell -> cell.y()*30+cell.x()).sorted().toArray());
            if (fallCell!=null) tag.putInt("FallCell",fallCell.y()*30+fallCell.x());
            return tag;
        }
        static Layout load(CompoundTag tag) {
            var fields=new HashSet<>(Set.of("Op","Island","Temporary"));
            if (tag.contains("FallCell")) fields.add("FallCell");
            if (!tag.getAllKeys().equals(fields) || !bool(tag,"Op") || tag.contains("FallCell")&&!tag.contains("FallCell",Tag.TAG_INT)
                    || !tag.contains("Island",Tag.TAG_INT) || !tag.contains("Temporary",Tag.TAG_INT_ARRAY)) throw new IllegalArgumentException("Invalid opening layout schema");
            int map = tag.getInt("Island"); SceneKind kind = map == 0 ? null : SceneKind.fromMap(map);
            int[] cells = tag.getIntArray("Temporary"); if (cells.length > MAX_TEMPORARY_SUPPORTS) throw new IllegalArgumentException("Too many temporary supports");
            var temporary = new LinkedHashSet<Cell>();
            for (int value : cells) {
                if (value < 0 || value >= 720 || !temporary.add(new Cell(value%30,value/30))) throw new IllegalArgumentException("Invalid or duplicate temporary support");
            }
            int fall=tag.contains("FallCell")?tag.getInt("FallCell"):-1;
            if (tag.contains("FallCell")&&(fall<0||fall>=720)) throw new IllegalArgumentException("Invalid opening fall cell");
            return new Layout(tag.getBoolean("Op"),kind,temporary,fall<0?null:new Cell(fall%30,fall/30));
        }
    }

    static final class Journal {
        final UUID id;
        final boolean restore;
        final Layout target;
        final Layout partialTarget;
        final int partialCursor;
        final SceneKind zone;
        int cursor;
        boolean pending;
        Journal(UUID id, boolean restore, Layout target, Layout partialTarget, int partialCursor, SceneKind zone) {
            this.id=id; this.restore=restore; this.target=target; this.partialTarget=partialTarget; this.partialCursor=partialCursor; this.zone=zone;
        }
        CompoundTag save() {
            var tag = new CompoundTag(); tag.putUUID("Id",id); tag.putBoolean("Restore",restore); tag.put("Target",target.save());
            tag.putInt("Cursor",cursor); tag.putBoolean("Pending",pending); tag.putInt("Zone",zone == null ? 0 : zone.sourceMap());
            if (partialTarget != null) { tag.put("PartialTarget",partialTarget.save()); tag.putInt("PartialCursor",partialCursor); }
            return tag;
        }
        static Journal load(CompoundTag tag) {
            var fields = new HashSet<>(Set.of("Id","Restore","Target","Cursor","Pending","Zone"));
            if (tag.contains("PartialTarget")) fields.addAll(Set.of("PartialTarget","PartialCursor"));
            if (!tag.getAllKeys().equals(fields) || !tag.hasUUID("Id") || !bool(tag,"Restore") || !bool(tag,"Pending")
                    || !tag.contains("Target",Tag.TAG_COMPOUND) || !tag.contains("Cursor",Tag.TAG_INT) || !tag.contains("Zone",Tag.TAG_INT)) throw new IllegalArgumentException("Invalid opening journal schema");
            Layout partial = null; int partialCursor = 0;
            if (fields.contains("PartialTarget")) {
                if (!tag.contains("PartialTarget",Tag.TAG_COMPOUND) || !tag.contains("PartialCursor",Tag.TAG_INT)) throw new IllegalArgumentException("Invalid partial opening journal");
                partial = Layout.load(tag.getCompound("PartialTarget")); partialCursor = tag.getInt("PartialCursor");
                if (partialCursor < 0 || partialCursor > MAX_CHANGES) throw new IllegalArgumentException("Invalid partial cursor");
            }
            int zone = tag.getInt("Zone");
            var result = new Journal(tag.getUUID("Id"),tag.getBoolean("Restore"),Layout.load(tag.getCompound("Target")),partial,partialCursor,zone == 0 ? null : SceneKind.fromMap(zone));
            result.cursor = tag.getInt("Cursor"); result.pending = tag.getBoolean("Pending");
            if (result.cursor < 0 || result.cursor > MAX_CHANGES || result.restore && (result.zone != null || !result.target.equals(Layout.EMPTY))
                    || !result.restore && (result.zone == null || partial != null)) throw new IllegalArgumentException("Invalid opening journal phase");
            return result;
        }
    }

    public static OriginalOpeningSceneData load(CompoundTag tag) {
        if (!tag.getAllKeys().equals(Set.of("Version","Catalog","NextSlot","Leases")) || !tag.contains("Version",Tag.TAG_INT)
                || tag.getInt("Version") != STORAGE_VERSION || !tag.contains("Catalog",Tag.TAG_STRING) || !CATALOG_HASH.equals(tag.getString("Catalog"))
                || !tag.contains("NextSlot",Tag.TAG_INT) || !tag.contains("Leases",Tag.TAG_LIST)) throw new IllegalArgumentException("Invalid opening scene storage schema or plan version");
        int next = tag.getInt("NextSlot"); var rows = (ListTag)tag.get("Leases");
        if (next < 0 || next > MAX_SLOTS || rows.size() != next || !rows.isEmpty() && rows.getElementType() != Tag.TAG_COMPOUND) throw new IllegalArgumentException("Invalid opening lease count");
        var result = new OriginalOpeningSceneData(); var slots = new HashSet<Integer>(); var journals = new HashSet<UUID>();
        for (var value : rows) {
            var row = (CompoundTag)value; var keys = new HashSet<>(Set.of("Owner","Slot","Layout")); if (row.contains("Journal")) keys.add("Journal");
            if (!row.getAllKeys().equals(keys) || !row.hasUUID("Owner") || !row.contains("Slot",Tag.TAG_INT) || !row.contains("Layout",Tag.TAG_COMPOUND)
                    || row.contains("Journal") && !row.contains("Journal",Tag.TAG_COMPOUND)) throw new IllegalArgumentException("Invalid opening lease row");
            var lease = new Lease(row.getUUID("Owner"),row.getInt("Slot")); if (lease.slot >= next || !slots.add(lease.slot)) throw new IllegalArgumentException("Duplicate or skipped opening slot");
            var entry = new Entry(lease); entry.layout = Layout.load(row.getCompound("Layout"));
            if (row.contains("Journal")) entry.journal = Journal.load(row.getCompound("Journal"));
            if (result.entries.put(lease.owner,entry) != null) throw new IllegalArgumentException("Duplicate opening owner");
            if (entry.journal != null) {
                if (!journals.add(entry.journal.id)) throw new IllegalArgumentException("Duplicate opening journal identity");
                OriginalOpeningSceneWorld.validateJournal(entry);
            }
        }
        if (!java.util.Collections.disjoint(journals,result.entries.keySet())) throw new IllegalArgumentException("Opening journal collides with an owner identity");
        if (result.activeJournalCount() > MAX_ACTIVE_JOURNALS) throw new IllegalArgumentException("Opening journal capacity exceeded");
        result.nextSlot=next; return result;
    }

    @Override public CompoundTag save(CompoundTag tag) {
        tag.putInt("Version",STORAGE_VERSION); tag.putString("Catalog",CATALOG_HASH); tag.putInt("NextSlot",nextSlot); var rows = new ListTag();
        entries.values().stream().sorted(java.util.Comparator.comparingInt(entry -> entry.lease.slot)).forEach(entry -> {
            var row = new CompoundTag(); row.putUUID("Owner",entry.lease.owner); row.putInt("Slot",entry.lease.slot); row.put("Layout",entry.layout.save());
            if (entry.journal != null) row.put("Journal",entry.journal.save()); rows.add(row);
        }); tag.put("Leases",rows); return tag;
    }

    private static boolean bool(CompoundTag tag, String key) { return tag.contains(key,Tag.TAG_BYTE) && (tag.getByte(key)==0 || tag.getByte(key)==1); }
}
