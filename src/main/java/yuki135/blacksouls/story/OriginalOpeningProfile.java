package yuki135.blacksouls.story;

import java.util.Objects;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.Set;
import java.util.regex.Pattern;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;

public final class OriginalOpeningProfile {
    public static final String TAG_KEY = "OriginalOpeningProfile";
    public static final int SCHEMA_VERSION = 2;
    private static final Pattern DIMENSION = Pattern.compile("[a-z0-9_.-]+:[a-z0-9/._-]+");

    public enum ReadState { ABSENT_LEGACY, LEGACY, REAL, REJECTED }
    public enum Stage {
        NAME_PENDING, ORIGIN_SELECTION, ORIGIN_CONFIRMED, ALICE_SELECTION,
        MEMORY_DIALOGUE, FIRST_STEP, COLLAPSE1, COLLAPSE2, COLLAPSE3,
        FALLING, ARRIVAL_PENDING, COMPLETE
    }

    public record Cursor(int sourceMapId, int sourceEventId, int pageIndex,
                         int commandIndex, int messageOffset, int waitTicksRemaining) {
        public Cursor {
            range(sourceMapId, 0, 9999, "cursor map");
            range(sourceEventId, 0, 9999, "cursor event");
            range(pageIndex, 0, 1024, "cursor page");
            range(commandIndex, 0, 1_000_000, "cursor command");
            range(messageOffset, 0, 1_000_000, "cursor message");
            range(waitTicksRemaining, 0, 1_000_000, "cursor wait");
            if (sourceMapId == 0 && (sourceEventId != 0 || pageIndex != 0 || commandIndex != 0
                    || messageOffset != 0 || waitTicksRemaining != 0)) fail("unset cursor");
        }
        public static Cursor empty() { return new Cursor(0, 0, 0, 0, 0, 0); }
    }

    public record RegionStamp(int sourceMapId, int regionId, boolean primed) {
        public RegionStamp {
            if (primed) {
                range(sourceMapId, 1, 9999, "region map");
                range(regionId, 0, 255, "region id");
            } else if (sourceMapId != 0 || regionId != -1) fail("unset region");
        }
        public static RegionStamp empty() { return new RegionStamp(0, -1, false); }
        public boolean matches(int mapId, int region) {
            return primed && sourceMapId == mapId && regionId == region;
        }
    }

    public record WorldPoint(String dimension, double x, double y, double z, float yaw, float pitch) {
        public WorldPoint {
            if (dimension == null || !DIMENSION.matcher(dimension).matches()) fail("dimension");
            if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z)
                    || Math.abs(x) > 30_000_000 || Math.abs(y) > 30_000_000 || Math.abs(z) > 30_000_000
                    || !Float.isFinite(yaw) || !Float.isFinite(pitch) || pitch < -90 || pitch > 90) fail("world point");
        }
    }

    public record PendingTransfer(int targetSourceMapId, Stage targetStage, WorldPoint destination) {
        public PendingTransfer {
            range(targetSourceMapId, 1, 9999, "transfer map");
            Objects.requireNonNull(targetStage, "target stage");
            Objects.requireNonNull(destination, "destination");
        }
    }

    public record Checkpoint(Stage stage, Cursor cursor, RegionStamp region, Optional<WorldPoint> position) {
        public Checkpoint {
            Objects.requireNonNull(stage, "checkpoint stage");
            Objects.requireNonNull(cursor, "checkpoint cursor");
            Objects.requireNonNull(region, "checkpoint region");
            Objects.requireNonNull(position, "checkpoint position");
            if (stage == Stage.FALLING) fail("unstable falling checkpoint");
        }
    }

    private static final class Flow {
        int originId;
        int relationshipId;
        Stage stage = Stage.NAME_PENDING;
        Cursor cursor = Cursor.empty();
        RegionStamp region = RegionStamp.empty();
        Checkpoint checkpoint = new Checkpoint(Stage.NAME_PENDING, cursor, region, Optional.empty());
        boolean skipRequested;
        long slot = -1;
        PendingTransfer pendingTransfer;
        OriginalOpeningCinematic cinematic;
    }

    private final ReadState readState;
    private final Tag rejectedTag;
    private final String rejectionReason;
    private final Flow real;
    private Flow preview;
    private WorldPoint previewReturnPoint;
    private boolean kitGrantCommitted;
    private boolean opCodaGrantCommitted;
    private boolean arrivalRingCleanupCommitted;
    private boolean initialRoomSilent;

    private OriginalOpeningProfile(ReadState state, Flow real, Tag rejectedTag, String reason) {
        this.readState = state;
        this.real = real;
        this.rejectedTag = rejectedTag;
        this.rejectionReason = reason;
    }

    public static OriginalOpeningProfile legacy() {
        return new OriginalOpeningProfile(ReadState.ABSENT_LEGACY, null, null, "");
    }

    static OriginalOpeningProfile createForVerifiedNewPlayer() {
        return new OriginalOpeningProfile(ReadState.REAL, new Flow(), null, "");
    }

    public static OriginalOpeningProfile readFrom(CompoundTag owner) {
        Objects.requireNonNull(owner, "owner NBT");
        if (!owner.contains(TAG_KEY)) return legacy();
        Tag raw = owner.get(TAG_KEY);
        try {
            if (!(raw instanceof CompoundTag)) fail("profile compound");
            CompoundTag tag = (CompoundTag) raw;
            int version=integer(tag,"Version");
            if(version!=1&&version!=SCHEMA_VERSION)fail("schema version");
            keys(tag,version==1?Set.of("Version","Mode","Real","Preview"):Set.of("Version","Mode","Real","Preview","InitialRoomSilent"));
            String mode = string(tag, "Mode");
            OriginalOpeningProfile result;
            if (mode.equals("LEGACY")) {
                if (tag.contains("Real")) fail("legacy real state");
                result = new OriginalOpeningProfile(ReadState.LEGACY, null, null, "");
            } else if (mode.equals("REAL")) {
                CompoundTag realTag = compound(tag, "Real");
                result = new OriginalOpeningProfile(ReadState.REAL, readFlow(realTag, true,version), null, "");
                result.kitGrantCommitted = flag(realTag, "KitGrantCommitted");
                result.opCodaGrantCommitted = flag(realTag, "OpCodaGrantCommitted");
                result.arrivalRingCleanupCommitted = flag(realTag, "ArrivalRingCleanupCommitted");
                result.validateReal();
            } else throw new IllegalArgumentException("profile mode");
            if (tag.contains("Preview")) {
                CompoundTag previewTag = compound(tag, "Preview");
                result.preview = readFlow(previewTag, false,version);
                result.previewReturnPoint = readPoint(compound(previewTag, "ReturnPoint"));
                if (result.real != null && result.real.slot >= 0 && result.real.slot == result.preview.slot) fail("shared replay slot");
            }
            result.initialRoomSilent=version>=2&&flag(tag,"InitialRoomSilent");
            if(result.initialRoomSilent&&(result.real==null||result.real.stage.ordinal()<Stage.ARRIVAL_PENDING.ordinal()))fail("initial room audio policy");
            return result;
        } catch (IllegalArgumentException | NullPointerException exception) {
            return new OriginalOpeningProfile(ReadState.REJECTED, null, raw.copy(), exception.getMessage() == null ? "invalid profile" : exception.getMessage());
        }
    }

    public void writeTo(CompoundTag owner) {
        Objects.requireNonNull(owner, "owner NBT");
        if (!hasStoredData()) { owner.remove(TAG_KEY); return; }
        if (readState == ReadState.REJECTED) {
            owner.put(TAG_KEY, rejectedTag.copy());
            return;
        }
        CompoundTag tag = new CompoundTag();
        tag.putInt("Version", SCHEMA_VERSION);
        tag.putString("Mode", real == null ? "LEGACY" : "REAL");
        tag.putBoolean("InitialRoomSilent",initialRoomSilent);
        if (real != null) {
            CompoundTag data = writeFlow(real);
            data.putBoolean("KitGrantCommitted", kitGrantCommitted);
            data.putBoolean("OpCodaGrantCommitted", opCodaGrantCommitted);
            data.putBoolean("ArrivalRingCleanupCommitted", arrivalRingCleanupCommitted);
            tag.put("Real", data);
        }
        if (preview != null) {
            CompoundTag data = writeFlow(preview);
            data.put("ReturnPoint", writePoint(previewReturnPoint));
            tag.put("Preview", data);
        }
        owner.put(TAG_KEY, tag);
    }

    public ReadState readState() { return readState; }
    public boolean isLegacy() { return real == null && readState != ReadState.REJECTED; }
    public boolean hasRejectedData() { return readState == ReadState.REJECTED; }
    public boolean hasStoredData() { return readState != ReadState.ABSENT_LEGACY || preview != null; }
    public Optional<String> rejectionReason() { return hasRejectedData() ? Optional.of(rejectionReason) : Optional.empty(); }
    public boolean previewActive() { return preview != null; }
    public boolean canCommitRealEffects() { return real != null && preview == null && real.stage != Stage.COMPLETE; }
    public int realOriginId() { return real == null ? 0 : real.originId; }
    public int realRelationshipId() { return real == null ? 0 : real.relationshipId; }
    public Optional<Stage> realStage() { return real == null ? Optional.empty() : Optional.of(real.stage); }
    public Cursor realCursor() { return requireReal().cursor; }
    public RegionStamp realRegion() { return requireReal().region; }
    public Checkpoint realCheckpoint() { return requireReal().checkpoint; }
    public OptionalLong realSlot() { return slot(requireReal()); }
    public Optional<PendingTransfer> realPendingTransfer() { return Optional.ofNullable(requireReal().pendingTransfer); }
    public boolean realSkipRequested() { return requireReal().skipRequested; }
    public boolean realKitGrantCommitted() { return kitGrantCommitted; }
    public boolean realOpCodaGrantCommitted() { return opCodaGrantCommitted; }
    public boolean realArrivalRingCleanupCommitted() { return arrivalRingCleanupCommitted; }
    public boolean realInitialRoomSilent() { return initialRoomSilent; }
    public void setRealInitialRoomSilent(boolean silent) {
        var flow=requireRealMutable();
        if(silent&&flow.stage.ordinal()<Stage.ARRIVAL_PENDING.ordinal())throw new IllegalStateException("initial room not reached");
        initialRoomSilent=silent;
    }
    public Optional<OriginalOpeningCinematic> activeCinematic() { return Optional.ofNullable(preview!=null?preview.cinematic:real==null?null:real.cinematic); }
    public void setActiveCinematic(OriginalOpeningCinematic cinematic) {
        var flow=preview!=null?requirePreview():requireRealMutable();
        flow.cinematic=Objects.requireNonNull(cinematic);validateFlow(flow);
    }
    public void clearActiveCinematic() { var flow=preview!=null?requirePreview():requireRealMutable();flow.cinematic=null; }


    public void advanceReal(Stage next) {
        Flow flow = requireRealMutable();
        if (flow.stage == Stage.ORIGIN_SELECTION || flow.stage == Stage.ALICE_SELECTION) throw new IllegalStateException("choice confirmation required");
        if (next == Stage.ALICE_SELECTION && (!kitGrantCommitted || !opCodaGrantCommitted)) throw new IllegalStateException("origin rewards not committed");
        if (next == Stage.COMPLETE && !arrivalRingCleanupCommitted) throw new IllegalStateException("arrival cleanup not committed");
        advance(flow, next);
        validateReal();
    }

    public void confirmRealOrigin(int id) {
        Flow flow = requireRealMutable();
        range(id, 1, 9, "origin");
        requireStage(flow, Stage.ORIGIN_SELECTION);
        flow.originId = id;
        advance(flow, Stage.ORIGIN_CONFIRMED);
    }

    public void confirmRealRelationship(int id) {
        Flow flow = requireRealMutable();
        range(id, 1, 3, "relationship");
        requireStage(flow, Stage.ALICE_SELECTION);
        flow.relationshipId = id;
        advance(flow, Stage.MEMORY_DIALOGUE);
    }

    public boolean markRealKitGrantCommitted() {
        Flow flow = requireRealMutable();
        requireStage(flow, Stage.ORIGIN_CONFIRMED);
        if (kitGrantCommitted) return false;
        kitGrantCommitted = true;
        return true;
    }

    public boolean markRealOpCodaGrantCommitted() {
        Flow flow = requireRealMutable();
        requireStage(flow, Stage.ORIGIN_CONFIRMED);
        if (!kitGrantCommitted) throw new IllegalStateException("origin kit not committed");
        if (opCodaGrantCommitted) return false;
        opCodaGrantCommitted = true;
        return true;
    }

    public boolean markRealArrivalRingCleanupCommitted() {
        Flow flow = requireRealMutable();
        requireStage(flow, Stage.ARRIVAL_PENDING);
        if (arrivalRingCleanupCommitted) return false;
        arrivalRingCleanupCommitted = true;
        return true;
    }

    public void requestRealSkip() { Flow flow = requireRealMutable(); if (flow.stage == Stage.COMPLETE) throw new IllegalStateException("opening complete"); flow.skipRequested = true; }
    public void clearRealSkip() { Flow flow=requireRealMutable();if(flow.stage==Stage.COMPLETE)throw new IllegalStateException("opening complete");flow.skipRequested=false; }
    public void advanceRealSkipToArrival() {
        Flow flow = requireRealMutable();
        if (!flow.skipRequested || flow.originId == 0 || flow.relationshipId == 0 || !kitGrantCommitted || !opCodaGrantCommitted
                || flow.stage.ordinal() < Stage.MEMORY_DIALOGUE.ordinal() || flow.stage.ordinal() > Stage.FALLING.ordinal()) throw new IllegalStateException("skip requires confirmed choices and committed origin rewards");
        flow.stage = Stage.ARRIVAL_PENDING;
        flow.checkpoint = snapshot(flow, Optional.empty());
    }

    public void setRealCursor(Cursor cursor) { requireRealMutable().cursor = Objects.requireNonNull(cursor); }
    public boolean recordRealRegion(int mapId, int regionId) { return recordRegion(requireRealMutable(), mapId, regionId); }
    public void resetRealRegion() { requireRealMutable().region = RegionStamp.empty(); }
    public void checkpointReal(WorldPoint position) { checkpoint(requireRealMutable(), position); }
    public Checkpoint recoverRealCheckpoint() { return recover(requireRealMutable()); }
    public void assignRealSlot(long slot) { assignSlot(requireRealMutable(), slot); }
    public void clearRealSlot(long expected) { clearSlot(requireRealMutable(), expected); }
    public void setRealPendingTransfer(PendingTransfer transfer) { pending(requireRealMutable(), transfer); }
    public void clearRealPendingTransfer(PendingTransfer expected) { clearPending(requireRealMutable(), expected); }

    public void beginPreview(WorldPoint returnPoint) {
        if (hasRejectedData() || preview != null) throw new IllegalStateException("preview unavailable");
        previewReturnPoint = Objects.requireNonNull(returnPoint);
        preview = new Flow();
    }
    public Stage previewStage() { return requirePreview().stage; }
    public int previewOriginId() { return requirePreview().originId; }
    public int previewRelationshipId() { return requirePreview().relationshipId; }
    public Cursor previewCursor() { return requirePreview().cursor; }
    public RegionStamp previewRegion() { return requirePreview().region; }
    public Checkpoint previewCheckpoint() { return requirePreview().checkpoint; }
    public OptionalLong previewSlot() { return slot(requirePreview()); }
    public boolean previewSkipRequested() { return requirePreview().skipRequested; }
    public Optional<PendingTransfer> previewPendingTransfer() { return Optional.ofNullable(requirePreview().pendingTransfer); }
    public WorldPoint previewReturnPoint() { requirePreview(); return previewReturnPoint; }
    public void advancePreview(Stage next) { Flow flow = requirePreview(); if (flow.stage == Stage.ORIGIN_SELECTION || flow.stage == Stage.ALICE_SELECTION) throw new IllegalStateException("preview choice confirmation required"); advance(flow, next); }
    public void confirmPreviewOrigin(int id) { Flow flow = requirePreview(); range(id, 1, 9, "preview origin"); requireStage(flow, Stage.ORIGIN_SELECTION); flow.originId = id; advance(flow, Stage.ORIGIN_CONFIRMED); }
    public void confirmPreviewRelationship(int id) { Flow flow = requirePreview(); range(id, 1, 3, "preview relationship"); requireStage(flow, Stage.ALICE_SELECTION); flow.relationshipId = id; advance(flow, Stage.MEMORY_DIALOGUE); }
    public void requestPreviewSkip() { Flow flow = requirePreview(); if (flow.stage == Stage.COMPLETE) throw new IllegalStateException("preview complete"); flow.skipRequested = true; }
    public void clearPreviewSkip() { Flow flow=requirePreview();if(flow.stage==Stage.COMPLETE)throw new IllegalStateException("preview complete");flow.skipRequested=false; }
    public void advancePreviewSkipToArrival() {
        Flow flow = requirePreview();
        if (!flow.skipRequested || flow.originId == 0 || flow.relationshipId == 0 || flow.stage.ordinal() < Stage.MEMORY_DIALOGUE.ordinal()
                || flow.stage.ordinal() > Stage.FALLING.ordinal()) throw new IllegalStateException("preview skip choices");
        flow.stage = Stage.ARRIVAL_PENDING;
        flow.checkpoint = snapshot(flow, Optional.empty());
    }
    public void setPreviewCursor(Cursor cursor) { requirePreview().cursor = Objects.requireNonNull(cursor); }
    public boolean recordPreviewRegion(int mapId, int regionId) { return recordRegion(requirePreview(), mapId, regionId); }
    public void resetPreviewRegion() { requirePreview().region = RegionStamp.empty(); }
    public void checkpointPreview(WorldPoint position) { checkpoint(requirePreview(), position); }
    public Checkpoint recoverPreviewCheckpoint() { return recover(requirePreview()); }
    public void assignPreviewSlot(long slot) { if (real != null && real.slot == slot) throw new IllegalStateException("replay needs separate slot"); assignSlot(requirePreview(), slot); }
    public void clearPreviewSlot(long expected) { clearSlot(requirePreview(), expected); }
    public void setPreviewPendingTransfer(PendingTransfer transfer) { pending(requirePreview(), transfer); }
    public void clearPreviewPendingTransfer(PendingTransfer expected) { clearPending(requirePreview(), expected); }
    public WorldPoint endPreview() { requirePreview(); WorldPoint result = previewReturnPoint; preview = null; previewReturnPoint = null; return result; }

    private Flow requireReal() { if (real == null) throw new IllegalStateException("no enrolled real opening"); return real; }
    private Flow requireRealMutable() { Flow result = requireReal(); if (preview != null) throw new IllegalStateException("real state frozen during preview"); return result; }
    private Flow requirePreview() { if (preview == null) throw new IllegalStateException("no preview"); return preview; }
    private static OptionalLong slot(Flow flow) { return flow.slot < 0 ? OptionalLong.empty() : OptionalLong.of(flow.slot); }
    private static void requireStage(Flow flow, Stage expected) { if (flow.stage != expected) throw new IllegalStateException("stage " + flow.stage + " requires " + expected); }
    private static void advance(Flow flow, Stage next) {
        Objects.requireNonNull(next);
        if (flow.stage == Stage.COMPLETE || next.ordinal() != flow.stage.ordinal() + 1) throw new IllegalStateException("illegal opening transition");
        if (next == Stage.COMPLETE && flow.pendingTransfer != null) throw new IllegalStateException("transfer not confirmed");
        flow.stage = next;
        if (next != Stage.FALLING) flow.checkpoint = snapshot(flow, Optional.empty());
    }
    private static boolean recordRegion(Flow flow, int mapId, int regionId) {
        RegionStamp next = new RegionStamp(mapId, regionId, true);
        boolean changed = !flow.region.matches(mapId, regionId);
        flow.region = next;
        return changed;
    }
    private static Checkpoint snapshot(Flow flow, Optional<WorldPoint> point) { return new Checkpoint(flow.stage, flow.cursor, flow.region, point); }
    private static void checkpoint(Flow flow, WorldPoint point) { if (flow.stage == Stage.FALLING) throw new IllegalStateException("falling is not stable"); flow.checkpoint = snapshot(flow, Optional.of(Objects.requireNonNull(point))); }
    private static Checkpoint recover(Flow flow) { Checkpoint saved = flow.checkpoint; flow.stage = saved.stage(); flow.cursor = saved.cursor(); flow.region = saved.region(); return saved; }
    private static void assignSlot(Flow flow, long value) { if (value < 0 || flow.slot >= 0 && flow.slot != value) throw new IllegalStateException("slot assignment"); flow.slot = value; }
    private static void clearSlot(Flow flow, long expected) { if (expected < 0 || flow.slot != expected) throw new IllegalStateException("stale slot cleanup"); flow.slot = -1; }
    private static void pending(Flow flow, PendingTransfer value) { Objects.requireNonNull(value); if (flow.stage == Stage.COMPLETE) throw new IllegalStateException("opening complete"); if (flow.pendingTransfer != null && !flow.pendingTransfer.equals(value)) throw new IllegalStateException("transfer already pending"); flow.pendingTransfer = value; }
    private static void clearPending(Flow flow, PendingTransfer expected) { if (expected == null || !expected.equals(flow.pendingTransfer)) throw new IllegalStateException("stale transfer cleanup"); flow.pendingTransfer = null; }

    private void validateReal() {
        validateFlow(real);
        if (kitGrantCommitted && real.originId == 0 || opCodaGrantCommitted && !kitGrantCommitted
                || real.stage.ordinal() >= Stage.ALICE_SELECTION.ordinal() && (!kitGrantCommitted || !opCodaGrantCommitted)
                || arrivalRingCleanupCommitted && real.stage.ordinal() < Stage.ARRIVAL_PENDING.ordinal()
                || real.stage == Stage.COMPLETE && (!arrivalRingCleanupCommitted || real.pendingTransfer != null)) fail("real reward state");
    }
    private static void validateFlow(Flow flow) {
        range(flow.originId, 0, 9, "origin"); range(flow.relationshipId, 0, 3, "relationship");
        boolean hasOrigin = flow.stage.ordinal() >= Stage.ORIGIN_CONFIRMED.ordinal();
        boolean hasRelationship = flow.stage.ordinal() >= Stage.MEMORY_DIALOGUE.ordinal();
        if (hasOrigin != (flow.originId != 0) || hasRelationship != (flow.relationshipId != 0)) fail("choice stage mismatch");
        if (flow.stage == Stage.COMPLETE && flow.pendingTransfer != null) fail("completed pending transfer");
        if(flow.cinematic!=null) {
            String program=flow.cinematic.program();
            boolean matches=switch(flow.stage) {
                case COLLAPSE1 -> program.equals("collapse110");
                case COLLAPSE2 -> program.equals("collapse97");
                case COLLAPSE3 -> program.equals("collapse98");
                case FALLING -> program.equals("finale99");
                case ARRIVAL_PENDING -> program.equals("finale99")||program.equals("arrival");
                case COMPLETE -> flow.cinematic.finished();
                default -> false;
            };
            if(!matches)fail("cinematic source stage mismatch");
        }
        if (flow.checkpoint.stage().ordinal() > flow.stage.ordinal()
                || flow.originId != 0 && flow.checkpoint.stage().ordinal() < Stage.ORIGIN_CONFIRMED.ordinal()
                || flow.relationshipId != 0 && flow.checkpoint.stage().ordinal() < Stage.MEMORY_DIALOGUE.ordinal()) fail("checkpoint choice mismatch");
    }
    private static CompoundTag writeFlow(Flow flow) {
        CompoundTag tag = new CompoundTag();
        tag.putInt("OriginId", flow.originId); tag.putInt("RelationshipId", flow.relationshipId); tag.putString("Stage", flow.stage.name());
        tag.put("Cursor", writeCursor(flow.cursor)); tag.put("Region", writeRegion(flow.region)); tag.put("Stable", writeCheckpoint(flow.checkpoint));
        tag.putBoolean("SkipRequested", flow.skipRequested);
        if (flow.slot >= 0) tag.putLong("Slot", flow.slot);
        if (flow.pendingTransfer != null) tag.put("PendingTransfer", writeTransfer(flow.pendingTransfer));
        if(flow.cinematic!=null)tag.put("Cinematic",flow.cinematic.save());
        return tag;
    }
    private static Flow readFlow(CompoundTag tag, boolean real,int version) {
        var allowed=new java.util.HashSet<>(real ? Set.of("OriginId", "RelationshipId", "Stage", "Cursor", "Region", "Stable", "SkipRequested", "Slot", "PendingTransfer", "KitGrantCommitted", "OpCodaGrantCommitted", "ArrivalRingCleanupCommitted")
                : Set.of("OriginId", "RelationshipId", "Stage", "Cursor", "Region", "Stable", "SkipRequested", "Slot", "PendingTransfer", "ReturnPoint"));
        if(version>=2)allowed.add("Cinematic");
        keys(tag,allowed);
        Flow flow = new Flow(); flow.originId = integer(tag, "OriginId"); flow.relationshipId = integer(tag, "RelationshipId"); flow.stage = stage(tag, "Stage");
        flow.cursor = readCursor(compound(tag, "Cursor")); flow.region = readRegion(compound(tag, "Region")); flow.checkpoint = readCheckpoint(compound(tag, "Stable"));
        flow.skipRequested = flag(tag, "SkipRequested");
        if (tag.contains("Slot")) { typed(tag, "Slot", Tag.TAG_LONG); flow.slot = tag.getLong("Slot"); if (flow.slot < 0) fail("negative slot"); }
        if (tag.contains("PendingTransfer")) flow.pendingTransfer = readTransfer(compound(tag, "PendingTransfer"));
        if(tag.contains("Cinematic"))flow.cinematic=OriginalOpeningCinematic.load(compound(tag,"Cinematic"));
        validateFlow(flow); return flow;
    }
    private static CompoundTag writeCursor(Cursor cursor) { CompoundTag tag = new CompoundTag(); tag.putInt("Map", cursor.sourceMapId()); tag.putInt("Event", cursor.sourceEventId()); tag.putInt("Page", cursor.pageIndex()); tag.putInt("Command", cursor.commandIndex()); tag.putInt("MessageOffset", cursor.messageOffset()); tag.putInt("WaitTicks", cursor.waitTicksRemaining()); return tag; }
    private static Cursor readCursor(CompoundTag tag) { keys(tag, Set.of("Map", "Event", "Page", "Command", "MessageOffset", "WaitTicks")); return new Cursor(integer(tag, "Map"), integer(tag, "Event"), integer(tag, "Page"), integer(tag, "Command"), integer(tag, "MessageOffset"), integer(tag, "WaitTicks")); }
    private static CompoundTag writeRegion(RegionStamp region) { CompoundTag tag = new CompoundTag(); tag.putInt("Map", region.sourceMapId()); tag.putInt("Region", region.regionId()); tag.putBoolean("Primed", region.primed()); return tag; }
    private static RegionStamp readRegion(CompoundTag tag) { keys(tag, Set.of("Map", "Region", "Primed")); return new RegionStamp(integer(tag, "Map"), integer(tag, "Region"), flag(tag, "Primed")); }
    private static CompoundTag writePoint(WorldPoint point) { CompoundTag tag = new CompoundTag(); tag.putString("Dimension", point.dimension()); tag.putDouble("X", point.x()); tag.putDouble("Y", point.y()); tag.putDouble("Z", point.z()); tag.putFloat("Yaw", point.yaw()); tag.putFloat("Pitch", point.pitch()); return tag; }
    private static WorldPoint readPoint(CompoundTag tag) { keys(tag, Set.of("Dimension", "X", "Y", "Z", "Yaw", "Pitch")); typed(tag, "X", Tag.TAG_DOUBLE); typed(tag, "Y", Tag.TAG_DOUBLE); typed(tag, "Z", Tag.TAG_DOUBLE); typed(tag, "Yaw", Tag.TAG_FLOAT); typed(tag, "Pitch", Tag.TAG_FLOAT); return new WorldPoint(string(tag, "Dimension"), tag.getDouble("X"), tag.getDouble("Y"), tag.getDouble("Z"), tag.getFloat("Yaw"), tag.getFloat("Pitch")); }
    private static CompoundTag writeCheckpoint(Checkpoint value) { CompoundTag tag = new CompoundTag(); tag.putString("Stage", value.stage().name()); tag.put("Cursor", writeCursor(value.cursor())); tag.put("Region", writeRegion(value.region())); value.position().ifPresent(point -> tag.put("Position", writePoint(point))); return tag; }
    private static Checkpoint readCheckpoint(CompoundTag tag) { keys(tag, Set.of("Stage", "Cursor", "Region", "Position")); return new Checkpoint(stage(tag, "Stage"), readCursor(compound(tag, "Cursor")), readRegion(compound(tag, "Region")), tag.contains("Position") ? Optional.of(readPoint(compound(tag, "Position"))) : Optional.empty()); }
    private static CompoundTag writeTransfer(PendingTransfer transfer) { CompoundTag tag = new CompoundTag(); tag.putInt("TargetMap", transfer.targetSourceMapId()); tag.putString("TargetStage", transfer.targetStage().name()); tag.put("Destination", writePoint(transfer.destination())); return tag; }
    private static PendingTransfer readTransfer(CompoundTag tag) { keys(tag, Set.of("TargetMap", "TargetStage", "Destination")); return new PendingTransfer(integer(tag, "TargetMap"), stage(tag, "TargetStage"), readPoint(compound(tag, "Destination"))); }
    private static Stage stage(CompoundTag tag, String key) { return Stage.valueOf(string(tag, key)); }
    private static CompoundTag compound(CompoundTag tag, String key) { typed(tag, key, Tag.TAG_COMPOUND); return tag.getCompound(key); }
    private static String string(CompoundTag tag, String key) { typed(tag, key, Tag.TAG_STRING); return tag.getString(key); }
    private static int integer(CompoundTag tag, String key) { typed(tag, key, Tag.TAG_INT); return tag.getInt(key); }
    private static boolean flag(CompoundTag tag, String key) { typed(tag, key, Tag.TAG_BYTE); byte value = tag.getByte(key); if (value != 0 && value != 1) fail("boolean " + key); return value == 1; }
    private static void typed(CompoundTag tag, String key, int type) { if (!tag.contains(key, type)) fail("NBT type " + key); }
    private static void keys(CompoundTag tag, Set<String> allowed) { if (!allowed.containsAll(tag.getAllKeys())) fail("unknown schema field"); }
    private static void range(int value, int min, int max, String label) { if (value < min || value > max) fail(label); }
    private static void fail(String reason) { throw new IllegalArgumentException(reason); }
}
