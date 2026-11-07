package yuki135.blacksouls.story;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;

public final class OriginalChickenSurface {
    public static final double MAX_FRAME_STEP=0.625D;
    public static final double MAX_SOURCE_HEIGHT=1.0625D;

    private OriginalChickenSurface() {
    }

    public static double supportY(ServerLevel level,Entity subject,AABB body,double previousY) {
        if(level==null||subject==null||subject.level()!=level||body==null||!Double.isFinite(previousY)||Math.abs(body.minY-previousY)>MAX_SOURCE_HEIGHT+1E-7D)return Double.NaN;
        var min=BlockPos.containing(body.minX,previousY-MAX_FRAME_STEP-.02D,body.minZ);
        var max=BlockPos.containing(Math.nextDown(body.maxX),previousY+MAX_FRAME_STEP+body.getYsize(),Math.nextDown(body.maxZ));
        if(min.getY()<level.getMinBuildHeight()||max.getY()>=level.getMaxBuildHeight()||!level.getWorldBorder().isWithinBounds(min)||!level.getWorldBorder().isWithinBounds(max))return Double.NaN;
        for(int cx=min.getX()>>4;cx<=max.getX()>>4;cx++)for(int cz=min.getZ()>>4;cz<=max.getZ()>>4;cz++)
            if(level.getChunkSource().getChunkNow(cx,cz)==null||!level.areEntitiesLoaded(net.minecraft.world.level.ChunkPos.asLong(cx,cz)))return Double.NaN;
        var scan=new AABB(body.minX,previousY-MAX_FRAME_STEP-.02D,body.minZ,body.maxX,previousY+MAX_FRAME_STEP+.02D,body.maxZ);
        double result=Double.NaN,distance=Double.POSITIVE_INFINITY;
        for(var shape:level.getBlockCollisions(subject,scan))for(var box:shape.toAabbs()) {
            double y=box.maxY;
            if(y<previousY-MAX_FRAME_STEP-1E-7D||y>previousY+MAX_FRAME_STEP+1E-7D)continue;
            var at=body.move(0,y-body.minY,0);
            if(!level.noCollision(subject,at)||level.containsAnyLiquid(at)||level.noCollision(subject,at.move(0,-.02D,0)))continue;
            double delta=Math.abs(y-previousY);
            if(delta<distance) { result=y;distance=delta; }
        }
        return result;
    }

    public static boolean clearStep(ServerLevel level,Entity subject,AABB before,AABB after) {
        if(level==null||subject==null||before==null||after==null||Math.abs(after.minY-before.minY)>MAX_FRAME_STEP+1E-7D)return false;
        double lift=Math.max(0D,after.minY-before.minY);
        var raised=before.move(0,lift,0);
        var across=after.move(0,Math.max(0D,before.minY-after.minY),0);
        return clear(level,subject,before.minmax(raised))&&clear(level,subject,raised.minmax(across))&&clear(level,subject,across.minmax(after))
                &&!level.noCollision(subject,after.move(0,-.02D,0));
    }

    public static double stepY(ServerLevel level,Entity subject,AABB before,double targetX,double targetZ) {
        double centerX=(before.minX+before.maxX)*.5D,centerZ=(before.minZ+before.maxZ)*.5D;
        var next=before.move(targetX-centerX,0,targetZ-centerZ);
        double y=supportY(level,subject,next,before.minY);
        return Double.isFinite(y)&&clearStep(level,subject,before,next.move(0,y-next.minY,0))?y:Double.NaN;
    }

    private static boolean clear(ServerLevel level,Entity subject,AABB box) {
        return level.noCollision(subject,box)&&!level.containsAnyLiquid(box)&&level.getEntities(subject,box,entity->!entity.isRemoved()&&!entity.isSpectator()).isEmpty()
                &&level.getServer().getPlayerList().getPlayers().stream().noneMatch(player->player!=subject&&player.level()==level&&!player.isRemoved()&&player.isAlive()&&!player.isSpectator()&&player.getBoundingBox().intersects(box));
    }
}
