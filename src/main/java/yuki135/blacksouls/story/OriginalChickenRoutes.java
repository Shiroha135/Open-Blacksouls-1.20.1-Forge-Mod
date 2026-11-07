package yuki135.blacksouls.story;

import com.google.gson.JsonObject;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import yuki135.blacksouls.entity.EntityOriginalNpc;
import yuki135.blacksouls.util.BSOriginalNpcData;

public final class OriginalChickenRoutes {
    public static final String SOURCE="map031_ev093";
    private static final int KILL_FADE_COMMAND=-40;
    private static final Set<String> FIELDS=Set.of("Version","Source","Owner","Actor","Target","Dimension","Command","X","Y","Z","Speed");

    private OriginalChickenRoutes() {
    }

    public record Authority(UUID owner,UUID actor,UUID target,String dimension,int command,double x,double y,double z,int speed) {
        public Authority {
            if(owner==null||actor==null||target==null||owner.equals(actor)||!target.equals(owner)&&!target.equals(actor)
                    ||dimension==null||ResourceLocation.tryParse(dimension)==null||!finite(x)||!finite(y)||!finite(z)||speed<1||speed>6)throw new IllegalArgumentException("Invalid original chicken route authority");
            var source=sourceCommand(command);
            boolean player=source.parameters().get(0).getAsInt()==-1;
            if(player!=target.equals(owner))throw new IllegalArgumentException("Chicken source target disagrees with its authorized owner");
        }

        public CompoundTag save() {
            var tag=new CompoundTag();tag.putInt("Version",1);tag.putString("Source",SOURCE);tag.putUUID("Owner",owner);tag.putUUID("Actor",actor);tag.putUUID("Target",target);
            tag.putString("Dimension",dimension);tag.putInt("Command",command);tag.putDouble("X",x);tag.putDouble("Y",y);tag.putDouble("Z",z);tag.putInt("Speed",speed);return tag;
        }

        public static Authority load(CompoundTag tag) {
            if(!tag.getAllKeys().equals(FIELDS)||!tag.contains("Version",Tag.TAG_INT)||tag.getInt("Version")!=1||!tag.contains("Source",Tag.TAG_STRING)||!SOURCE.equals(tag.getString("Source"))
                    ||!tag.hasUUID("Owner")||!tag.hasUUID("Actor")||!tag.hasUUID("Target")||!tag.contains("Dimension",Tag.TAG_STRING)||!tag.contains("Command",Tag.TAG_INT)
                    ||!tag.contains("X",Tag.TAG_DOUBLE)||!tag.contains("Y",Tag.TAG_DOUBLE)||!tag.contains("Z",Tag.TAG_DOUBLE)||!tag.contains("Speed",Tag.TAG_INT))throw new IllegalArgumentException("Invalid saved chicken authority schema");
            return new Authority(tag.getUUID("Owner"),tag.getUUID("Actor"),tag.getUUID("Target"),tag.getString("Dimension"),tag.getInt("Command"),tag.getDouble("X"),tag.getDouble("Y"),tag.getDouble("Z"),tag.getInt("Speed"));
        }

        boolean contains(double px,double py,double pz) {
            int moves=0;
            for(var value:route(command).getAsJsonArray("list"))if(value.getAsJsonObject().get("code").getAsInt()>=1&&value.getAsJsonObject().get("code").getAsInt()<=14)moves++;
            double radius=moves+1D;
            return finite(px)&&finite(py)&&finite(pz)&&Math.abs(px-x)<=radius&&Math.abs(pz-z)<=radius&&Math.abs(py-y)<=3D;
        }
    }

    public static Authority authorize(ServerPlayer owner,EntityOriginalNpc actor,Entity subject,int target,JsonObject requested) {
        if(!OriginalNpcAutorunService.context(owner,actor)||!SOURCE.equals(actor.getNpcId())||!OriginalNpcAutorunService.unique(actor)
                ||subject!=(target==-1?owner:actor)||(target!=-1&&target!=0&&target!=93))throw new UnsupportedOperationException("chicken_route_owner_or_source");
        var source=BSOriginalNpcData.getEvent(SOURCE);
        int active=OriginalNpcService.chickenRouteCommand(owner,actor,target,requested);
        for(int index=0;index<source.pages().get(0).commands().size();index++) {
            if(active>=0&&index!=active)continue;
            var command=source.pages().get(0).commands().get(index);
            if(command.code()!=205||!command.parameters().get(1).getAsJsonObject().equals(requested))continue;
            int expected=command.parameters().get(0).getAsInt();
            if(expected==-1?target!=-1:target==-1)continue;
            int speed=subject instanceof EntityOriginalNpc npc?npc.originalMovementState().getInt("MoveSpeed"):4;
            var authority=new Authority(owner.getUUID(),actor.getUUID(),subject.getUUID(),owner.level().dimension().location().toString(),index,subject.getX(),subject.getY(),subject.getZ(),speed);
            if(!bodyClear(owner,subject,subject.getBoundingBox())||!supported((ServerLevel)subject.level(),subject,subject.getBoundingBox()))throw new UnsupportedOperationException("chicken_route_unsupported_origin");
            return authority;
        }
        if(target==0&&route(KILL_FADE_COMMAND).equals(requested)&&OriginalNpcService.authorizesChickenKillFade(owner,actor)) {
            int speed=actor.originalMovementState().getInt("MoveSpeed");
            var authority=new Authority(owner.getUUID(),actor.getUUID(),subject.getUUID(),owner.level().dimension().location().toString(),KILL_FADE_COMMAND,subject.getX(),subject.getY(),subject.getZ(),speed);
            if(!bodyClear(owner,subject,subject.getBoundingBox())||!supported((ServerLevel)subject.level(),subject,subject.getBoundingBox()))throw new UnsupportedOperationException("chicken_route_unsupported_origin");
            return authority;
        }
        throw new UnsupportedOperationException("chicken_route_not_an_exact_source_command");
    }

    static boolean live(Authority authority,ServerPlayer owner,Entity subject) {
        if(authority==null||owner==null||subject==null||!owner.server.isSameThread()||owner.server.getPlayerList().getPlayer(authority.owner())!=owner
                ||!owner.getUUID().equals(authority.owner())||!subject.getUUID().equals(authority.target())||subject.isRemoved()||!subject.isAlive()
                ||subject.level()!=owner.level()||!authority.dimension().equals(owner.level().dimension().location().toString())
                ||!authority.contains(subject.getX(),subject.getY(),subject.getZ()))return false;
        var entity=owner.serverLevel().getEntity(authority.actor());
        return entity instanceof EntityOriginalNpc actor&&OriginalNpcAutorunService.context(owner,actor)&&SOURCE.equals(actor.getNpcId());
    }

    static void validateTicket(OriginalNpcMovementData.Ticket ticket) {
        var authority=ticket.chicken;
        if(authority==null||ticket.sourceMap!=31||!authority.owner().equals(ticket.owner)||!authority.target().equals(ticket.target)||!authority.dimension().equals(ticket.dimension)
                ||!ticket.npcId.equals(ticket.target.equals(authority.actor())?SOURCE:"")||!ticket.runner.snapshot().getAsJsonObject("route").equals(route(authority.command())))throw new IllegalArgumentException("Chicken motion ticket does not match its source authority");
        if(ticket.motion!=null) {
            var motion=ticket.motion.snapshot();
            if(!authority.contains(motion.get("startX").getAsDouble(),motion.get("startY").getAsDouble(),motion.get("startZ").getAsDouble())
                    ||!authority.contains(motion.get("targetX").getAsDouble(),motion.get("targetY").getAsDouble(),motion.get("targetZ").getAsDouble())
                    ||!authority.contains(ticket.motion.x(),ticket.motion.y(),ticket.motion.z()))throw new IllegalArgumentException("Saved chicken motion escaped its bounded source route");
            int position=ticket.runner.snapshot().get("position").getAsInt()-1;
            var commands=route(authority.command()).getAsJsonArray("list");
            if(position<0||position>=commands.size())throw new IllegalArgumentException("Chicken motion has no preceding source move");
            var command=commands.get(position).getAsJsonObject();int code=command.get("code").getAsInt();int speed=authority.speed();
            for(int i=0;i<position;i++)if(commands.get(i).getAsJsonObject().get("code").getAsInt()==29)speed=commands.get(i).getAsJsonObject().getAsJsonArray("parameters").get(0).getAsInt();
            double dx=motion.get("targetX").getAsDouble()-motion.get("startX").getAsDouble(),dz=motion.get("targetZ").getAsDouble()-motion.get("startZ").getAsDouble();
            double expectedX=code==2?1:code==3?-1:0,expectedZ=code==1?-1:code==4?1:0;
            boolean jump=code==14;
            if(!(code>=1&&code<=4||jump)||motion.get("jump").getAsBoolean()!=jump||motion.get("moveSpeed").getAsInt()!=speed
                    ||dx!=expectedX||dz!=expectedZ||!validHeight(authority,motion,jump)
                    ||jump&&(command.getAsJsonArray("parameters").get(0).getAsInt()!=0||command.getAsJsonArray("parameters").get(1).getAsInt()!=0))throw new IllegalArgumentException("Saved chicken motion is not the exact original command");
        }
    }

    private static boolean nativeReceipt(Authority authority) {
        return authority!=null&&"blacksouls:hokoniwa".equals(authority.dimension())&&authority.target().equals(authority.actor())
                &&yuki135.blacksouls.compat.scene.OriginalChickenPlacement.corridor(authority.x(),authority.y(),authority.z());
    }

    private static boolean nativeSurface(Authority authority,ServerPlayer owner,Entity subject) {
        return nativeReceipt(authority)&&subject instanceof EntityOriginalNpc actor&&live(authority,owner,subject)
                &&yuki135.blacksouls.compat.scene.OriginalChickenPlacement.owns(owner,actor);
    }

    private static boolean validHeight(Authority authority,JsonObject motion,boolean jump) {
        double from=motion.get("startY").getAsDouble(),to=motion.get("targetY").getAsDouble();
        return from==to||!jump&&nativeReceipt(authority)&&Math.abs(to-from)<=OriginalChickenSurface.MAX_SOURCE_HEIGHT+1E-7D
                &&yuki135.blacksouls.compat.scene.OriginalChickenPlacement.corridor(motion.get("startX").getAsDouble(),from,motion.get("startZ").getAsDouble())
                &&yuki135.blacksouls.compat.scene.OriginalChickenPlacement.corridor(motion.get("targetX").getAsDouble(),to,motion.get("targetZ").getAsDouble());
    }

    static double targetY(Authority authority,ServerPlayer owner,Entity subject,double x,double z,boolean jump) {
        if(nativeReceipt(authority)&&!nativeSurface(authority,owner,subject))return Double.NaN;
        if(!nativeSurface(authority,owner,subject)||jump)return subject.getY();
        var level=(ServerLevel)subject.level();var before=subject.getBoundingBox();
        double startX=subject.getX(),startZ=subject.getZ();double y=subject.getY();
        for(int frame=1;frame<=32;frame++) {
            double px=startX+(x-startX)*frame/32D,pz=startZ+(z-startZ)*frame/32D;
            double next=OriginalChickenSurface.stepY(level,subject,before,px,pz);
            if(!Double.isFinite(next)||Math.abs(next-subject.getY())>OriginalChickenSurface.MAX_SOURCE_HEIGHT+1E-7D
                    ||!yuki135.blacksouls.compat.scene.OriginalChickenPlacement.corridor(px,next,pz))return Double.NaN;
            before=before.move(px-(before.minX+before.maxX)*.5D,next-before.minY,pz-(before.minZ+before.maxZ)*.5D);y=next;
        }
        return y;
    }

    static double nextFeetY(Authority authority,ServerPlayer owner,Entity subject,OriginalNpcMotion motion) {
        var next=OriginalNpcMotion.restore(motion.snapshot());next.advanceFrame();
        if(nativeReceipt(authority)&&!nativeSurface(authority,owner,subject))return Double.NaN;
        if(!next.snapshot().get("jump").getAsBoolean()&&nativeSurface(authority,owner,subject)) {
            double y=OriginalChickenSurface.stepY((ServerLevel)subject.level(),subject,subject.getBoundingBox(),next.x(),next.z());
            return Double.isFinite(y)&&authority.contains(next.x(),y,next.z())&&yuki135.blacksouls.compat.scene.OriginalChickenPlacement.corridor(next.x(),y,next.z())?y:Double.NaN;
        }
        return frameClear(authority,owner,subject,motion)?next.y():Double.NaN;
    }

    static boolean startClear(Authority authority,ServerPlayer owner,Entity subject,double x,double y,double z,boolean jump,int speed) {
        if(!live(authority,owner,subject)||!authority.contains(x,y,z))return false;
        if(!jump&&nativeSurface(authority,owner,subject)) {
            double actual=targetY(authority,owner,subject,x,z,false);
            return Double.isFinite(actual)&&Math.abs(actual-y)<1E-7D;
        }
        var target=subject.getBoundingBox().move(x-subject.getX(),y-subject.getY(),z-subject.getZ());
        var swept=subject.getBoundingBox().minmax(target);
        if(jump) {
            int peak=10-speed;
            swept=swept.expandTowards(0,(peak*peak/2)/32D,0);
        }
        return bodyClear(owner,subject,swept)&&supported((ServerLevel)subject.level(),subject,target);
    }

    static boolean frameClear(Authority authority,ServerPlayer owner,Entity subject,OriginalNpcMotion motion) {
        if(!live(authority,owner,subject))return false;
        var next=OriginalNpcMotion.restore(motion.snapshot());next.advanceFrame();
        if(!authority.contains(next.x(),next.y(),next.z()))return false;
        var body=subject.getBoundingBox().move(next.x()-subject.getX(),next.y()-subject.getY(),next.z()-subject.getZ());
        return bodyClear(owner,subject,subject.getBoundingBox().minmax(body))
                &&(next.snapshot().get("jump").getAsBoolean()&&!next.complete()||supported((ServerLevel)subject.level(),subject,body));
    }

    private static boolean bodyClear(ServerPlayer owner,Entity subject,AABB body) {
        var level=(ServerLevel)subject.level();
        var min=BlockPos.containing(body.minX,body.minY,body.minZ);var max=BlockPos.containing(Math.nextDown(body.maxX),Math.nextDown(body.maxY),Math.nextDown(body.maxZ));
        if(body.minY<level.getMinBuildHeight()||body.maxY>level.getMaxBuildHeight()||!level.getWorldBorder().isWithinBounds(min)||!level.getWorldBorder().isWithinBounds(max))return false;
        for(int cx=min.getX()>>4;cx<=max.getX()>>4;cx++)for(int cz=min.getZ()>>4;cz<=max.getZ()>>4;cz++)
            if(level.getChunkSource().getChunkNow(cx,cz)==null||!level.areEntitiesLoaded(net.minecraft.world.level.ChunkPos.asLong(cx,cz)))return false;
        return level.noCollision(subject,body)&&!level.containsAnyLiquid(body)&&level.getEntities(subject,body,entity->!entity.isRemoved()&&!entity.isSpectator()).isEmpty()
                &&level.getServer().getPlayerList().getPlayers().stream().noneMatch(player->player!=subject&&player.level()==level&&!player.isRemoved()&&player.isAlive()&&!player.isSpectator()&&player.getBoundingBox().intersects(body));
    }

    private static boolean supported(ServerLevel level,Entity subject,AABB body) { return !level.noCollision(subject,body.move(0,-.05D,0)); }
    private static boolean finite(double value) { return Double.isFinite(value)&&Math.abs(value)<=30000000; }
    private static BSOriginalNpcData.Command sourceCommand(int index) {
        if(index==KILL_FADE_COMMAND) {
            var event=BSOriginalNpcData.getEvent(SOURCE);
            var common=BSOriginalNpcData.commonEvent(40);
            if(event==null||event.pages().size()!=4||event.pages().get(0).commands().size()!=122||event.pages().get(0).commands().get(37).code()!=117
                    ||event.pages().get(0).commands().get(37).parameters().get(0).getAsInt()!=40||common==null||common.size()!=13
                    ||common.get(0).code()!=205||common.get(0).parameters().get(0).getAsInt()!=0)throw new IllegalArgumentException("Unknown chicken source common-route command");
            return common.get(0);
        }
        var source=BSOriginalNpcData.getEvent(SOURCE);
        if(source==null||source.mapId()!=31||source.eventId()!=93||source.pages().size()!=4||index<0||index>=source.pages().get(0).commands().size())throw new IllegalArgumentException("Unknown chicken source route command");
        var command=source.pages().get(0).commands().get(index);
        if(command.code()!=205)throw new IllegalArgumentException("Chicken authority is not a source move-route command");
        return command;
    }
    private static JsonObject route(int index) { return sourceCommand(index).parameters().get(1).getAsJsonObject(); }
}
