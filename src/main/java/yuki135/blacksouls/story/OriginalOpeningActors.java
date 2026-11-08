package yuki135.blacksouls.story;

import yuki135.blacksouls.entity.EntityOpeningActor;
import yuki135.blacksouls.handler.BSEntityRegistry;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import static yuki135.blacksouls.story.OriginalOpeningScenes.*;
import yuki135.blacksouls.story.OriginalOpeningSceneData.Lease;

public final class OriginalOpeningActors {
    private OriginalOpeningActors() { }

    public static boolean ensure(ServerPlayer owner,Lease lease,SceneKind scene,int relationship,boolean showAlice) {
        return ensure(owner,lease,scene,relationship,showAlice,true);
    }

    public static boolean ensure(ServerPlayer owner,Lease lease,SceneKind scene,int relationship,boolean showAlice,boolean showProfessions) {
        if (owner==null || lease==null || scene==null || relationship<0 || relationship>3 || owner.getServer()==null
                || owner.getServer().getPlayerList().getPlayer(owner.getUUID())!=owner) return false;
        var level=owner.getServer().getLevel(DIMENSION);
        if (level==null || !level.getServer().isSameThread() || !canonical(level,lease) || !loaded(level,lease)
                || !OriginalOpeningSceneWorld.ready(level,lease.owner(),lease,scene)) return false;
        var targets=targets(lease,scene,showAlice,showProfessions);var existing=owned(level,owner.getUUID(),lease);if (existing.size()>64) return false;
        for (var target:targets) if (!safe(level,owner,lease,target)) return false;
        var retained=new HashSet<EntityOpeningActor>();var missing=new ArrayList<Target>();
        for (var target:targets) {
            var found=existing.stream().filter(actor->actor.originId()==target.origin && actor.position().distanceToSqr(target.feet)<1.0E-8D && !retained.contains(actor)).findFirst();
            if (found.isPresent()) retained.add(found.get());else missing.add(target);
        }
        var created=new ArrayList<EntityOpeningActor>();
        for (var target:missing) {
            var actor=BSEntityRegistry.OPENING_ACTOR.get().create(level);
            if (actor==null) { created.forEach(Entity::discard);return false; }
            actor.configure(owner.getUUID(),target.origin,relationship,lease.slot());actor.moveTo(target.feet.x,target.feet.y,target.feet.z,target.origin==0?90:0,0);
            if(target.origin==0) { actor.setYHeadRot(90);actor.setYBodyRot(90); }
            if (!level.addFreshEntity(actor)) { actor.discard();created.forEach(Entity::discard);return false; }created.add(actor);
        }
        retained.forEach(actor->actor.configure(owner.getUUID(),actor.originId(),relationship,lease.slot()));
        existing.stream().filter(actor->!retained.contains(actor)).forEach(Entity::discard);return true;
    }

    public static void clear(ServerLevel level,UUID owner,Lease lease) {
        if (level==null || owner==null || lease==null || !DIMENSION.equals(level.dimension()) || !level.getServer().isSameThread()
                || !canonical(level,lease) || !loaded(level,lease)) throw new IllegalStateException("Opening actors can only clear a loaded canonical owner lease in its dimension");
        owned(level,owner,lease).forEach(Entity::discard);
    }

    private static boolean canonical(ServerLevel level,Lease lease) { return OriginalOpeningSceneData.get(level).owns(lease.owner(),lease); }
    private static boolean loaded(ServerLevel level,Lease lease) {
        return OriginalOpeningSceneWorld.requiredChunks(lease).stream().allMatch(chunk->level.getChunkSource().getChunkNow(chunk.x,chunk.z)!=null && level.areEntitiesLoaded(chunk.toLong()));
    }
    private static List<EntityOpeningActor> owned(ServerLevel level,UUID owner,Lease lease) {
        var result=new ArrayList<EntityOpeningActor>();
        for (var entity:level.getAllEntities()) if (entity instanceof EntityOpeningActor actor && !actor.isRemoved() && owner.equals(actor.ownerId()) && actor.leaseSlot()==lease.slot()) result.add(actor);
        return List.copyOf(result);
    }
    private static List<Target> targets(Lease lease,SceneKind scene,boolean showAlice,boolean showProfessions) {
        var result=new ArrayList<Target>();
        if (scene==SceneKind.OP&&showProfessions) { int origin=0;for (var anchor:professionAnchors(lease)) { origin++;if(OriginalOpeningGreeting.selectable(origin))result.add(new Target(origin,anchor.sourceCell(),anchor.feet(),scene)); } }
        else if (scene.island()&&showAlice) { var anchor=aliceAnchor(lease,scene);result.add(new Target(0,anchor.sourceCell(),anchor.feet(),scene)); }
        return List.copyOf(result);
    }
    private static boolean safe(ServerLevel level,ServerPlayer owner,Lease lease,Target target) {
        Checkpoint point;try { point=checkpoint(plan(lease,target.scene),target.source); } catch (IllegalArgumentException failure) { return false; }
        var feet=point.feet();var bounds=new AABB(feet.x-.3D,feet.y,feet.z-.3D,feet.x+.3D,feet.y+1.8D,feet.z+.3D);var floor=BlockPos.containing(feet).below();
        if (!level.getWorldBorder().isWithinBounds(bounds) || !level.getBlockState(floor).isFaceSturdy(level,floor,Direction.UP) || level.getBlockEntity(floor)!=null
                || !level.noCollision(owner,bounds) || level.containsAnyLiquid(bounds)) return false;
        return level.getEntities((Entity)null,bounds,entity->entity!=owner && !entity.isRemoved() && !entity.isSpectator()
                && !(entity instanceof EntityOpeningActor actor && owner.getUUID().equals(actor.ownerId()) && actor.leaseSlot()==lease.slot())).isEmpty();
    }
    private record Target(int origin,Cell source,Vec3 feet,SceneKind scene) { }
}
