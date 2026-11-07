package yuki135.blacksouls.compat.scene;

import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.CommandBlockEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import yuki135.blacksouls.BlackSouls;
import yuki135.blacksouls.combat.TurnBattleManager;
import yuki135.blacksouls.entity.EntityOriginalNpc;
import yuki135.blacksouls.story.OriginalNpcAutorunService;
import yuki135.blacksouls.story.OriginalOpeningService;
import yuki135.blacksouls.story.OriginalWorldTransferService;
import yuki135.blacksouls.util.HokoniwaDestination;

@Mod.EventBusSubscriber(modid=BlackSouls.MODID,bus=Mod.EventBusSubscriber.Bus.FORGE)
public final class OriginalChickenPlacement {
    public static final String SOURCE="map031_ev093";
    public static final Vec3 POSITION=new Vec3(1170.5D,-22D,9.5D);
    private static final BlockPos EXIT=new BlockPos(1171,-28,5);
    private static final OriginalNpcPlacements.Site SITE=new OriginalNpcPlacements.Site(SOURCE,POSITION,0F,"minecraft:stone_brick_stairs",List.of(
            landmark(1170,-23,9,"minecraft:stone_brick_stairs",Map.of("facing","south","half","bottom","shape","straight","waterlogged","false")),
            landmark(1167,-22,3,"minecraft:oak_fence",Map.of("east","true","south","true","north","false","west","false","waterlogged","false")),
            landmark(1171,-22,3,"minecraft:oak_fence",Map.of("east","false","south","true","north","false","west","true","waterlogged","false")),
            landmark(1166,-22,12,"minecraft:oak_fence",Map.of("east","true","south","false","north","false","west","false","waterlogged","false")),
            landmark(1164,-22,10,"mcwbridges:dry_bamboo_bridge",Map.of("facing","north","connection","base_toggled")),
            landmark(1171,-28,5,"minecraft:command_block",Map.of("facing","up","conditional","false"))),true);

    private OriginalChickenPlacement() {
    }

    public static OriginalNpcPlacements.Site site() { return SITE; }

    public static boolean corridor(double x,double y,double z) {
        return Double.isFinite(x)&&Double.isFinite(y)&&Double.isFinite(z)&&x>=1161D&&x<=1171D&&z>=9D&&z<=12D&&y>=-23.0625D&&y<=-20D;
    }

    public static boolean terrain(ServerLevel level) {
        return level!=null&&HokoniwaDestination.DIMENSION.equals(level.dimension())&&OriginalNpcPlacements.hasDeployedMap(level)
                &&OriginalNpcPlacements.chunksReady(level,SITE)&&SITE.landmarks().stream().allMatch(mark->mark.matches(level))
                &&level.getBlockEntity(EXIT)instanceof CommandBlockEntity command&&"tp @p 450.5 -58 125.5".equals(command.getCommandBlock().getCommand());
    }

    public static boolean owns(ServerPlayer owner,EntityOriginalNpc actor) {
        if(owner==null||actor==null||owner.level()!=actor.level()||!SOURCE.equals(actor.getNpcId())||!actor.isAlive()||actor.isRemoved()
                ||!(actor.level()instanceof ServerLevel level)||!terrain(level)||!corridor(actor.getX(),actor.getY(),actor.getZ()))return false;
        return actor.getUUID().equals(OriginalNpcPlacementData.get(level).entityId(SOURCE));
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if(event.phase!=TickEvent.Phase.END||!(event.player instanceof ServerPlayer player)||player.tickCount%20!=0||!player.isAlive()||player.isSpectator()
                ||OriginalOpeningService.managed(player)||TurnBattleManager.isInBattle(player)||OriginalWorldTransferService.pending(player)
                ||player.getX()<1160D||player.getX()>1174D||player.getZ()<3D||player.getZ()>13D||player.getY()<-25D||player.getY()>-19D
                ||!terrain(player.serverLevel()))return;
        OriginalNpcPlacements.install(player,SOURCE);
        var id=OriginalNpcPlacementData.get(player.serverLevel()).entityId(SOURCE);
        if(id==null||!(player.serverLevel().getEntity(id)instanceof EntityOriginalNpc actor)||!owns(player,actor))return;
        if(!(player.level().dimension().location()+"#map_031").equals(player.getPersistentData().getString("Blacksouls2CurrentScene"))) {
            try {
                Class.forName("cn.zhenhongliya.blacksouls2compat.scene.SceneSpawnManager").getMethod("enterScene",ServerPlayer.class,String.class).invoke(null,player,"map_031");
            } catch(ReflectiveOperationException failure) { throw new IllegalStateException("Original chicken island scene dispatcher unavailable",failure); }
        }
        OriginalNpcAutorunService.tick(player.server);
    }

    private static OriginalNpcPlacements.Landmark landmark(int x,int y,int z,String name,Map<String,String> properties) {
        return new OriginalNpcPlacements.Landmark(new BlockPos(x,y,z),name,properties);
    }
}
