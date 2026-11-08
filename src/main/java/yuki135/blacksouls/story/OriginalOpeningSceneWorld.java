package yuki135.blacksouls.story;

import yuki135.blacksouls.BlackSouls;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.entity.living.MobSpawnEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import static yuki135.blacksouls.story.OriginalOpeningScenes.*;
import static yuki135.blacksouls.story.OriginalOpeningSceneData.*;

@Mod.EventBusSubscriber(modid = BlackSouls.MODID)
public final class OriginalOpeningSceneWorld {
    public static final int MAX_BATCH = 256;
    private OriginalOpeningSceneWorld() {
    }

    public enum Status { PROGRESS, COMPLETE, ALREADY_READY, BUSY, WAIT_CHUNKS, FOREIGN_OWNER, NO_RECORD, INVALID_DIMENSION, OUTSIDE_WORLD, CAPACITY, CONFLICT, OCCUPIED, WRITE_FAILED }
    public record Result(Status status, UUID journal, int cursor, int total, BlockPos conflict) { }

    public static Result beginApply(ServerLevel level, UUID owner, Lease lease, SceneKind kind, Set<Cell> temporary) {
        if (level == null || !DIMENSION.equals(level.dimension())) return result(Status.INVALID_DIMENSION,null,null);
        return beginApply(new ServerAccess(level),get(level),owner,lease,kind,temporary);
    }
    public static Result advance(ServerLevel level, UUID owner, Lease lease, UUID journal, int budget) {
        if (level == null || !DIMENSION.equals(level.dimension())) return result(Status.INVALID_DIMENSION,null,null);
        return advance(new ServerAccess(level),get(level),owner,lease,journal,budget);
    }
    public static Result beginRestore(ServerLevel level, UUID owner, Lease lease) {
        if (level == null || !DIMENSION.equals(level.dimension())) return result(Status.INVALID_DIMENSION,null,null);
        return beginRestore(new ServerAccess(level),get(level),owner,lease);
    }

    public static Result beginRetireOp(ServerLevel level, UUID owner, Lease lease) {
        if (level == null || !DIMENSION.equals(level.dimension())) return result(Status.INVALID_DIMENSION,null,null);
        return beginRetireOp(new ServerAccess(level),get(level),owner,lease);
    }

    public static Result beginFall(ServerLevel level, UUID owner, Lease lease, Cell cell, ServerPlayer visitor) {
        if (!fallVisitor(level,owner,lease,visitor)) return result(Status.FOREIGN_OWNER,null,null);
        var data=get(level); var entry=data.entry(owner,lease);
        if (entry==null || entry.layout.island()!=SceneKind.ISLAND_99) return result(Status.NO_RECORD,entry,null);
        if (entry.journal!=null) return result(Status.BUSY,entry,null);
        if (entry.layout.fallCell()!=null) return result(entry.layout.fallCell().equals(cell)?Status.ALREADY_READY:Status.CONFLICT,entry,null);
        var access=new ServerAccess(level);
        if (!permitted(access,lease,SceneKind.ISLAND_99)) return result(Status.OUTSIDE_WORLD,entry,null);
        if (!loaded(access,lease,SceneKind.ISLAND_99)) return result(Status.WAIT_CHUNKS,entry,null);
        var conflict=scan(access,lease,SceneKind.ISLAND_99,owned(lease,entry.layout));
        if (conflict!=null) return result(Status.CONFLICT,entry,conflict);
        var target=entry.layout.falling(cell);
        if (data.activeJournalCount()>=MAX_ACTIVE_JOURNALS) return result(Status.CAPACITY,entry,null);
        entry.journal=new Journal(data.nextJournalId(),false,target,null,0,SceneKind.ISLAND_99);
        validateJournal(entry); data.setDirty(); return result(Status.PROGRESS,entry,null);
    }

    public static Result advanceFall(ServerLevel level, UUID owner, Lease lease, UUID journal, int budget, ServerPlayer visitor) {
        if (!fallVisitor(level,owner,lease,visitor)) return result(Status.FOREIGN_OWNER,null,null);
        var data=get(level); var entry=data.entry(owner,lease);
        if (entry==null || entry.journal==null || !fallApply(entry,entry.journal.target)) return result(Status.NO_RECORD,entry,null);
        return advance(new ServerAccess(level,visitor),data,owner,lease,journal,budget);
    }

    public static boolean fallReady(ServerLevel level, UUID owner, Lease lease, Cell cell, ServerPlayer visitor) {
        if (!fallVisitor(level,owner,lease,visitor)) return false;
        var entry=get(level).entry(owner,lease);
        if (entry==null || entry.journal!=null || !cell.equals(entry.layout.fallCell())) return false;
        var position=BlockPos.containing(feet(lease,SceneKind.ISLAND_99,cell));
        for (int y=MIN_Y;y<FEET_Y;y++) {
            var block=new BlockPos(position.getX(),y,position.getZ());
            if (!level.getBlockState(block).isAir() || level.getBlockEntity(block)!=null) return false;
        }
        return true;
    }

    private static boolean fallVisitor(ServerLevel level, UUID owner, Lease lease, ServerPlayer visitor) {
        if (level==null || visitor==null || lease==null || owner==null || !DIMENSION.equals(level.dimension())
                || visitor.getServer()!=level.getServer() || level.getServer().getPlayerList().getPlayer(visitor.getUUID())!=visitor
                || !OriginalOpeningService.managed(visitor)) return false;
        var stats=visitor.getCapability(yuki135.blacksouls.capability.BSPlayerStats.CAPABILITY).orElse(null);
        if (stats==null) return false;
        var profile=stats.openingProfile(); boolean preview=profile.previewActive();
        var stage=preview?profile.previewStage():profile.realStage().orElse(null);
        var slot=preview?profile.previewSlot():profile.realSlot();
        var identity=preview?UUID.nameUUIDFromBytes(("blacksouls:opening-preview:"+visitor.getUUID()).getBytes(java.nio.charset.StandardCharsets.UTF_8)):visitor.getUUID();
        return identity.equals(owner) && owner.equals(lease.owner()) && slot.isPresent() && slot.getAsLong()==lease.slot()
                && stage==OriginalOpeningProfile.Stage.FALLING
                && profile.activeCinematic().filter(cinematic->cinematic.program().equals("finale99")).isPresent();
    }

    private static boolean fallApply(Entry entry, Layout target) {
        return entry.layout.fallCell()==null && entry.layout.island()==SceneKind.ISLAND_99 && target.fallCell()!=null
                && target.equals(entry.layout.falling(target.fallCell()));
    }

    public static Set<ChunkPos> requiredChunks(Lease lease) {
        var result = new LinkedHashSet<>(OriginalOpeningScenes.requiredChunks(lease,SceneKind.OP));
        result.addAll(OriginalOpeningScenes.requiredChunks(lease,SceneKind.ISLAND_110)); return Set.copyOf(result);
    }

    public static boolean ready(ServerLevel level, UUID owner, Lease lease, SceneKind kind) {
        if (level == null || kind == null || !DIMENSION.equals(level.dimension())) return false;
        var data = get(level); var entry = data.entry(owner,lease);
        if (entry == null || entry.journal != null || kind == SceneKind.OP && !entry.layout.op()
                || kind != SceneKind.OP && entry.layout.island() != kind) return false;
        var access = new ServerAccess(level);
        return permitted(access,lease,kind) && loaded(access,lease,kind) && scan(access,lease,kind,owned(lease,entry.layout)) == null;
    }

    public static boolean verifyCheckpoint(ServerLevel level, UUID owner, Checkpoint point) {
        if (level==null || owner==null) return false;
        return verifyCheckpoint(level,owner,point,level.getServer().getPlayerList().getPlayer(owner));
    }

    public static boolean verifyCheckpoint(ServerLevel level, UUID owner, Checkpoint point, ServerPlayer visitor) {
        if (level == null || point == null || !DIMENSION.equals(level.dimension())) return false;
        if (visitor!=null && (visitor.getServer()!=level.getServer() || level.getServer().getPlayerList().getPlayer(visitor.getUUID())!=visitor)) return false;
        var entry=get(level).entry(owner,point.lease());
        if (entry==null || !checkpointOwned(new ServerAccess(level),entry,point)) return false;
        var bounds = new AABB(point.feet().x-.3D,point.feet().y,point.feet().z-.3D,
                point.feet().x+.3D,point.feet().y+1.8D,point.feet().z+.3D);
        var floor = BlockPos.containing(point.feet()).below();
        boolean occupied = !level.getEntities((Entity)null,bounds,entity -> !entity.isSpectator() && !entity.isRemoved() && entity!=visitor).isEmpty();
        boolean anotherPlayer = level.getServer().getPlayerList().getPlayers().stream().anyMatch(candidate -> candidate!=visitor
                && candidate.level() == level && candidate.isAlive() && !candidate.isSpectator() && candidate.getBoundingBox().intersects(bounds));
        return !occupied && !anotherPlayer && level.noCollision(visitor,bounds) && !level.containsAnyLiquid(bounds)
                && level.getBlockState(floor).isFaceSturdy(level,floor,net.minecraft.core.Direction.UP);
    }

    static boolean checkpointOwned(Access access, Entry entry, Checkpoint point) {
        if (!access.opening() || point==null || point.scene()==null || !entry.lease.equals(point.lease())) return false;
        var kind=point.scene(); var layout=entry.layout;
        boolean committed=kind==SceneKind.OP?layout.op():layout.island()==kind;
        boolean target=entry.journal!=null && (kind==SceneKind.OP?entry.journal.target.op():entry.journal.target.island()==kind);
        if (!committed && !target) return false;
        var temporary=kind==SceneKind.OP?Set.<Cell>of():committed?layout.temporary():entry.journal.target.temporary();
        Checkpoint expected;
        try { expected=checkpoint(plan(entry.lease,kind,temporary),point.sourceCell()); } catch (IllegalArgumentException failure) { return false; }
        if (!expected.equals(point)) return false;
        var floor=BlockPos.containing(point.feet()).below();
        if (!access.loaded(new ChunkPos(floor)) || !access.permitted(new AABB(floor.getX(),floor.getY(),floor.getZ(),floor.getX()+1,floor.getY()+5,floor.getZ()+1))) return false;
        var owned=current(entry); var support=owned.get(floor);
        if (support==null || !support.solid() || access.blockEntity(floor) || !access.state(floor).equals(support.state())) return false;
        for (int h=1;h<=4;h++) {
            var cell=floor.above(h);var material=owned.get(cell);
            if (material!=null && material.solid() || access.blockEntity(cell) || !access.state(cell).equals(state(material))) return false;
        }
        return true;
    }

    @SubscribeEvent
    public static void onNaturalSpawn(MobSpawnEvent.FinalizeSpawn event) {
        var reason = event.getSpawnType();
        if (DIMENSION.equals(event.getLevel().getLevel().dimension())
                && (reason == MobSpawnType.NATURAL || reason == MobSpawnType.CHUNK_GENERATION
                || reason == MobSpawnType.PATROL || reason == MobSpawnType.REINFORCEMENT)) event.setSpawnCancelled(true);
    }

    interface Access {
        boolean opening();
        boolean loaded(ChunkPos chunk);
        boolean permitted(AABB bounds);
        BlockState state(BlockPos position);
        boolean blockEntity(BlockPos position);
        boolean occupied(AABB bounds);
        boolean set(BlockPos position, BlockState state);
    }

    static Result beginApply(Access access, OriginalOpeningSceneData data, UUID owner, Lease lease, SceneKind kind, Set<Cell> temporary) {
        if (!access.opening()) return result(Status.INVALID_DIMENSION,null,null);
        var entry = data.entry(owner,lease); if (entry == null) return result(Status.FOREIGN_OWNER,null,null);
        requireTemporary(kind,temporary);
        if (entry.journal != null) return result(Status.BUSY,entry,null);
        if (!permitted(access,lease,kind)) return result(Status.OUTSIDE_WORLD,entry,null);
        if (!loaded(access,lease,kind)) return result(Status.WAIT_CHUNKS,entry,null);
        BlockPos conflict = scan(access,lease,kind,owned(lease,entry.layout));
        if (conflict != null) return result(Status.CONFLICT,entry,conflict);
        var target = entry.layout.with(kind,temporary);
        if (target.equals(entry.layout)) return result(Status.ALREADY_READY,entry,null);
        if (data.activeJournalCount() >= MAX_ACTIVE_JOURNALS) return result(Status.CAPACITY,entry,null);
        entry.journal = new Journal(data.nextJournalId(),false,target,null,0,kind);
        if (changes(entry).isEmpty()) { entry.layout=target; entry.journal=null; data.setDirty(); return result(Status.COMPLETE,entry,null); }
        validateJournal(entry); data.setDirty(); return result(Status.PROGRESS,entry,null);
    }

    static Result beginRestore(Access access, OriginalOpeningSceneData data, UUID owner, Lease lease) {
        if (!access.opening()) return result(Status.INVALID_DIMENSION,null,null);
        var entry = data.entry(owner,lease); if (entry == null) return result(Status.FOREIGN_OWNER,null,null);
        if (!permitted(access,lease,null)) return result(Status.OUTSIDE_WORLD,entry,null);
        if (!loaded(access,lease,null)) return result(Status.WAIT_CHUNKS,entry,null);
        if (entry.journal != null && entry.journal.restore) return result(Status.PROGRESS,entry,null);
        if (entry.journal != null) {
            var reconciled = reconcile(access,data,entry); if (reconciled != null) return reconciled;
        }
        var expected = current(entry); var conflict = scan(access,lease,null,expected);
        if (conflict != null) return result(Status.CONFLICT,entry,conflict);
        if (expected.isEmpty()) { entry.layout=Layout.EMPTY; entry.journal=null; data.setDirty(); return result(Status.ALREADY_READY,entry,null); }
        if (entry.journal == null && data.activeJournalCount() >= MAX_ACTIVE_JOURNALS) return result(Status.CAPACITY,entry,null);
        var partial = entry.journal != null && entry.journal.cursor > 0 ? entry.journal.target : null;
        int cursor = partial == null ? 0 : entry.journal.cursor;
        entry.journal = new Journal(data.nextJournalId(),true,Layout.EMPTY,partial,cursor,null);
        validateJournal(entry); data.setDirty(); return result(Status.PROGRESS,entry,null);
    }

    static Result beginRetireOp(Access access, OriginalOpeningSceneData data, UUID owner, Lease lease) {
        if (!access.opening()) return result(Status.INVALID_DIMENSION,null,null);
        var entry=data.entry(owner,lease);if(entry==null)return result(Status.FOREIGN_OWNER,null,null);
        if(entry.journal!=null)return result(opRetirement(entry.layout,entry.journal.target)&&entry.journal.zone==SceneKind.OP&&!entry.journal.restore?Status.PROGRESS:Status.BUSY,entry,null);
        if(!entry.layout.op())return result(Status.ALREADY_READY,entry,null);
        if(entry.layout.island()==null)return result(Status.NO_RECORD,entry,null);
        if(!permitted(access,lease,SceneKind.OP))return result(Status.OUTSIDE_WORLD,entry,null);
        if(!loaded(access,lease,SceneKind.OP))return result(Status.WAIT_CHUNKS,entry,null);
        var conflict=scan(access,lease,SceneKind.OP,owned(lease,entry.layout));
        if(conflict!=null)return result(Status.CONFLICT,entry,conflict);
        if(data.activeJournalCount()>=MAX_ACTIVE_JOURNALS)return result(Status.CAPACITY,entry,null);
        entry.journal=new Journal(data.nextJournalId(),false,entry.layout.withoutOp(),null,0,SceneKind.OP);
        validateJournal(entry);data.setDirty();return result(Status.PROGRESS,entry,null);
    }

    static Result advance(Access access, OriginalOpeningSceneData data, UUID owner, Lease lease, UUID journal, int budget) {
        if (budget < 1 || budget > MAX_BATCH) throw new IllegalArgumentException("Opening batch budget outside bound");
        if (!access.opening()) return result(Status.INVALID_DIMENSION,null,null);
        var entry = data.entry(owner,lease); if (entry == null) return result(Status.FOREIGN_OWNER,null,null);
        if (entry.journal == null || journal == null || !journal.equals(entry.journal.id)) return result(Status.NO_RECORD,entry,null);
        if (!permitted(access,lease,entry.journal.zone)) return result(Status.OUTSIDE_WORLD,entry,null);
        if (!loaded(access,lease,entry.journal.zone)) return result(Status.WAIT_CHUNKS,entry,null);
        var reconciled = reconcile(access,data,entry); if (reconciled != null) return reconciled;
        var changes = changes(entry); int writes = 0;
        while (entry.journal.cursor < changes.size() && writes < budget) {
            Change change = changes.get(entry.journal.cursor);
            if (access.blockEntity(change.position) || !access.state(change.position).equals(state(change.before))) return result(Status.CONFLICT,entry,change.position);
            if (change.after != null && change.after.solid() && access.occupied(new AABB(change.position))) return result(Status.OCCUPIED,entry,change.position);
            if (change.before != null && change.before.solid() && (change.after == null || !change.after.solid())
                    && access.occupied(new AABB(change.position).expandTowards(0,.05D,0))) return result(Status.OCCUPIED,entry,change.position);
            entry.journal.pending=true; data.setDirty();
            if (!access.set(change.position,state(change.after)) || !access.state(change.position).equals(state(change.after))) return result(Status.WRITE_FAILED,entry,change.position);
            entry.journal.cursor++; entry.journal.pending=false; data.setDirty(); writes++;
        }
        if (entry.journal.cursor == changes.size()) {
            var conflict = scan(access,lease,entry.journal.zone,owned(lease,entry.journal.target));
            if (conflict != null) return result(Status.CONFLICT,entry,conflict);
            UUID id=entry.journal.id; int count=changes.size(); entry.layout=entry.journal.target; entry.journal=null; data.setDirty();
            return new Result(Status.COMPLETE,id,count,count,null);
        }
        return result(Status.PROGRESS,entry,null);
    }

    private static Result reconcile(Access access, OriginalOpeningSceneData data, Entry entry) {
        if (entry.journal == null || !entry.journal.pending) return null;
        var row = changes(entry).get(entry.journal.cursor);
        if (access.blockEntity(row.position)) return result(Status.CONFLICT,entry,row.position);
        var current = access.state(row.position);
        if (current.equals(state(row.after))) entry.journal.cursor++;
        else if (!current.equals(state(row.before))) return result(Status.CONFLICT,entry,row.position);
        entry.journal.pending=false; data.setDirty(); return null;
    }

    static void validateJournal(Entry entry) {
        var journal = entry.journal; if (journal == null) return;
        if (journal.id.equals(entry.lease.owner())) throw new IllegalArgumentException("Opening owner cannot be its journal identity");
        if (!journal.restore) {
            if (!entry.layout.with(journal.zone,journal.target.temporary()).equals(journal.target) && !(journal.zone==SceneKind.ISLAND_99&&fallApply(entry,journal.target))
                    && !(journal.zone==SceneKind.OP&&opRetirement(entry.layout,journal.target))) throw new IllegalArgumentException("Opening apply changes another scene zone");
        } else if (journal.partialTarget != null) {
            if (!validApplyTarget(entry.layout,journal.partialTarget)) throw new IllegalArgumentException("Invalid prior opening partial target");
            int count = delta(owned(entry.lease,entry.layout),owned(entry.lease,journal.partialTarget)).size();
            if (journal.partialCursor < 1 || journal.partialCursor > count) throw new IllegalArgumentException("Invalid opening partial recovery cursor");
        }
        var rows = changes(entry);
        if (rows.isEmpty() || rows.size() > MAX_CHANGES || journal.cursor > rows.size() || journal.pending && journal.cursor == rows.size()) throw new IllegalArgumentException("Invalid opening journal cursor or size");
        for (var row : rows) {
            if (!inside(entry.lease,journal.zone,row.position) || row.before == row.after) throw new IllegalArgumentException("Opening journal escaped its leased zone");
        }
    }

    private static boolean validApplyTarget(Layout base, Layout target) {
        return target.op() && target.island() == base.island() && target.temporary().equals(base.temporary())
                || target.op() == base.op() && target.island() != null || opRetirement(base,target);
    }

    private static boolean opRetirement(Layout base,Layout target) {
        return base.op()&&base.island()!=null&&base.withoutOp().equals(target);
    }

    private record Change(BlockPos position, Material before, Material after) { }

    private static Map<BlockPos,Material> owned(Lease lease, Layout layout) {
        var blocks = new LinkedHashMap<BlockPos,Material>();
        if (layout.op()) blocks.putAll(plan(lease,SceneKind.OP).blocks());
        if (layout.island() != null) blocks.putAll(plan(lease,layout.island(),layout.temporary()).blocks());
        if (layout.fallCell()!=null) {
            var position=BlockPos.containing(feet(lease,SceneKind.ISLAND_99,layout.fallCell()));
            blocks.keySet().removeIf(block->block.getX()==position.getX()&&block.getZ()==position.getZ()&&block.getY()>=MIN_Y&&block.getY()<FEET_Y);
        }
        if (blocks.size() > MAX_PLAN_BLOCKS) throw new IllegalArgumentException("Opening owned layout exceeds bound");
        return blocks;
    }
    private static Map<BlockPos,Material> start(Entry entry) {
        var result = owned(entry.lease,entry.layout); var journal = entry.journal;
        if (journal != null && journal.partialTarget != null) {
            var partial = delta(result,owned(entry.lease,journal.partialTarget));
            for (int i=0; i<journal.partialCursor; i++) put(result,partial.get(i));
        }
        return result;
    }
    private static Map<BlockPos,Material> current(Entry entry) {
        var result = start(entry);
        if (entry.journal != null) {
            var changes = delta(result,owned(entry.lease,entry.journal.target));
            for (int i=0; i<entry.journal.cursor; i++) put(result,changes.get(i));
        }
        return result;
    }
    static int journalSize(Entry entry) { return changes(entry).size(); }
    private static List<Change> changes(Entry entry) { return entry.journal == null ? List.of() : delta(start(entry),owned(entry.lease,entry.journal.target)); }
    private static void put(Map<BlockPos,Material> blocks, Change change) { if (change.after == null) blocks.remove(change.position); else blocks.put(change.position,change.after); }
    private static List<Change> delta(Map<BlockPos,Material> before, Map<BlockPos,Material> after) {
        var positions = new LinkedHashSet<>(before.keySet()); positions.addAll(after.keySet()); var result = new ArrayList<Change>();
        for (var position : positions) if (before.get(position) != after.get(position)) result.add(new Change(position.immutable(),before.get(position),after.get(position)));
        result.sort(Comparator.comparingInt((Change change) -> change.after == null ? change.before.solid() ? 1 : 0 : change.after.solid() ? 2 : 3)
                .thenComparingInt(change -> change.after != null && change.after.solid() ? -change.position.getY() : change.position.getY())
                .thenComparingInt(change -> change.position.getX()).thenComparingInt(change -> change.position.getZ()));
        return result;
    }
    private static boolean inside(Lease lease, SceneKind zone, BlockPos position) { return zone == null ? contains(lease,SceneKind.OP,position) || contains(lease,SceneKind.ISLAND_110,position) : contains(lease,zone,position); }
    private static boolean permitted(Access access, Lease lease, SceneKind zone) {
        return zone == null ? access.permitted(bounds(lease,SceneKind.OP)) && access.permitted(bounds(lease,SceneKind.ISLAND_110)) : access.permitted(bounds(lease,zone));
    }
    private static boolean loaded(Access access, Lease lease, SceneKind zone) {
        var chunks = zone == null ? requiredChunks(lease) : OriginalOpeningScenes.requiredChunks(lease,zone);
        return chunks.stream().allMatch(access::loaded);
    }
    private static BlockPos scan(Access access, Lease lease, SceneKind zone, Map<BlockPos,Material> expected) {
        for (var kind : zone == null ? List.of(SceneKind.OP,SceneKind.ISLAND_110) : List.of(zone)) {
            var start = origin(lease,kind);
            for (BlockPos position : BlockPos.betweenClosed(start.getX(),MIN_Y,start.getZ(),start.getX()+kind.width()-1,MAX_Y-1,start.getZ()+kind.height()-1)) {
                if (access.blockEntity(position) || !access.state(position).equals(state(expected.get(position)))) return position.immutable();
            }
        }
        return null;
    }
    private static BlockState state(Material material) { return material == null ? Blocks.AIR.defaultBlockState() : material.state(); }
    private static Result result(Status status, Entry entry, BlockPos conflict) {
        return new Result(status,entry == null || entry.journal == null ? null : entry.journal.id,
                entry == null || entry.journal == null ? 0 : entry.journal.cursor,
                entry == null || entry.journal == null ? 0 : changes(entry).size(),conflict);
    }

    private record ServerAccess(ServerLevel level, ServerPlayer fallingVisitor) implements Access {
        ServerAccess(ServerLevel level) { this(level,null); }
        public boolean opening() { return DIMENSION.equals(level.dimension()); }
        public boolean loaded(ChunkPos chunk) { return level.getChunkSource().getChunkNow(chunk.x,chunk.z) != null && level.areEntitiesLoaded(chunk.toLong()); }
        public boolean permitted(AABB bounds) {
            return bounds.minY >= level.getMinBuildHeight() && bounds.maxY <= level.getMaxBuildHeight()
                    && level.getWorldBorder().isWithinBounds(BlockPos.containing(bounds.minX,bounds.minY,bounds.minZ))
                    && level.getWorldBorder().isWithinBounds(BlockPos.containing(bounds.maxX-1,bounds.maxY-1,bounds.maxZ-1));
        }
        public BlockState state(BlockPos position) { return level.getBlockState(position); }
        public boolean blockEntity(BlockPos position) { return level.getBlockEntity(position) != null; }
        public boolean occupied(AABB bounds) {
            if (!level.getEntities((Entity)null,bounds,entity -> entity!=fallingVisitor && !entity.isSpectator() && !entity.isRemoved()).isEmpty()) return true;
            return level.getServer().getPlayerList().getPlayers().stream().anyMatch(player -> player!=fallingVisitor && player.level() == level && player.isAlive() && !player.isSpectator() && player.getBoundingBox().intersects(bounds));
        }
        public boolean set(BlockPos position, BlockState state) { return level.setBlock(position,state,Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE); }
    }
}
