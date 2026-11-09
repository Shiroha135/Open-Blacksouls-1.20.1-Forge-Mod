package yuki135.blacksouls.story;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import yuki135.blacksouls.capability.OriginalNpcProgress;
import yuki135.blacksouls.combat.TurnBattleManager;
import yuki135.blacksouls.compat.scene.SceneSpawnerBossData;
import yuki135.blacksouls.compat.scene.SceneSpawnerBossState;
import yuki135.blacksouls.entity.EntityOriginalDatabaseEnemy;
import yuki135.blacksouls.entity.EntityOriginalNpc;
import yuki135.blacksouls.handler.BSEntityRegistry;

@Mod.EventBusSubscriber(modid="blacksouls")
public final class OriginalSkullBeastService {
    public static final String SOURCE="map052_ev023";
    public static final String KNIGHT="map052_ev010";
    public static final String BEAST="map052_ev017";
    public static final BlockPos SPAWNER=new BlockPos(132,-19,213);
    private static final BlockPos ENTRANCE=new BlockPos(132,-19,208);
    private static final AABB ARENA=new AABB(126,-20,207,139,-12,221);

    private OriginalSkullBeastService() { }

    public static boolean handles(String id) {
        return SOURCE.equals(id)||KNIGHT.equals(id)||BEAST.equals(id);
    }

    public static boolean matches(ServerLevel level) {
        String dimension=level.dimension().location().toString();
        if (!dimension.equals("minecraft:overworld")&&!dimension.equals("blacksouls:hokoniwa")) return false;
        if (!level.hasChunkAt(SPAWNER)||!level.hasChunkAt(ENTRANCE)) return false;
        var spawner=level.getBlockEntity(SPAWNER);
        var entrance=level.getBlockEntity(ENTRANCE);
        if (!(spawner instanceof Container inventory)||!(spawner instanceof SceneSpawnerBossState boss)
                ||!boss.blacksouls$isBossMode()||entrance==null) return false;
        var item=ForgeRegistries.ITEMS.getKey(inventory.getItem(0).getItem());
        String scene=boss.blacksouls$getSceneId();
        return item!=null&&item.toString().equals("blacksouls:original_enemy_185_spawn_egg")
                &&(scene.equals("map_052")||scene.equals("map_051"))
                &&entrance.saveWithFullMetadata().getString("SceneId").equals("map_052");
    }

    public static boolean managesSpawner(ServerLevel level,BlockPos pos) {
        return SPAWNER.equals(pos)&&matches(level);
    }

    public static EntityOriginalNpc actor(ServerLevel level,String id) {
        EntityOriginalNpc result=null;
        for (var entity:level.getEntitiesOfClass(EntityOriginalNpc.class,ARENA,actor->actor.isAlive()&&!actor.isRemoved()&&id.equals(actor.getNpcId()))) {
            if (result!=null) return null;
            result=entity;
        }
        return result;
    }

    public static boolean routeAllowed(ServerPlayer owner,Entity subject,double x,double z) {
        return owner!=null&&owner.level()==subject.level()&&subject.level() instanceof ServerLevel level&&matches(level)
                &&(subject==owner||subject instanceof EntityOriginalNpc npc&&handles(npc.getNpcId()))
                &&x>=131.5D&&x<=133.5D&&z>=207.5D&&z<=215.5D&&Math.abs(subject.getY()+18D)<0.1D;
    }

    public static boolean prepareBattle(ServerPlayer player,EntityOriginalDatabaseEnemy enemy) {
        var beast=actor(player.serverLevel(),BEAST);
        if (beast==null) return false;
        enemy.moveTo(beast.getX(),beast.getY(),beast.getZ(),beast.getYRot(),0);
        beast.applyOriginalMoveCommand(42,List.of(new com.google.gson.JsonPrimitive(0)));
        return true;
    }

    public static void finishBattle(ServerPlayer player) {
        var beast=actor(player.serverLevel(),BEAST);
        if (beast!=null) beast.applyOriginalMoveCommand(42,List.of(new com.google.gson.JsonPrimitive(255)));
    }

    public static void retry(ServerPlayer player) {
        var beast=actor(player.serverLevel(),BEAST);
        if (beast!=null) beast.moveTo(132.5D,-18D,214.5D,0,0);
    }

    public static void defeated(ServerPlayer player) {
        SceneSpawnerBossData.get(player.server).markDefeated(SceneSpawnerBossData.spawnerKey(player.level().dimension(),SPAWNER));
    }

    public static void install(ServerPlayer player) {
        var level=player.serverLevel();
        if (!matches(level)) return;
        for (String id:List.of(SOURCE,KNIGHT,BEAST)) {
            if (!level.getEntitiesOfClass(EntityOriginalNpc.class,ARENA,npc->!npc.isRemoved()&&id.equals(npc.getNpcId())).isEmpty()) continue;
            double z=SOURCE.equals(id)?208.5D:KNIGHT.equals(id)?213.5D:214.5D;
            var npc=BSEntityRegistry.ORIGINAL_NPC.get().create(level);
            if (npc==null) throw new IllegalStateException("Missing original NPC entity");
            npc.moveTo(132.5D,-18D,z,0,0);
            npc.setPersistenceRequired();
            npc.setNpcId(id);
            if (!level.addFreshEntity(npc)) throw new IllegalStateException("Cannot install skull beast event "+id);
        }
    }

    @SubscribeEvent
    public static void tick(TickEvent.PlayerTickEvent event) {
        if (event.phase!=TickEvent.Phase.END||!(event.player instanceof ServerPlayer player)||!player.isAlive()
                ||player.isSpectator()||player.distanceToSqr(new Vec3(132.5D,-18D,211.5D))>1024D||!matches(player.serverLevel())) return;
        if (player.tickCount%20==0) {
            install(player);
            String key=SceneSpawnerBossData.spawnerKey(player.level().dimension(),SPAWNER);
            for (var legacy:player.serverLevel().getEntitiesOfClass(EntityOriginalDatabaseEnemy.class,ARENA,
                    enemy->enemy.getProfileId()==185&&key.equals(enemy.getPersistentData().getString(SceneSpawnerBossState.ENTITY_SPAWNER_KEY_TAG)))) {
                if (!TurnBattleManager.isInBattle(legacy)) legacy.discard();
            }
        }
        if (OriginalOpeningService.managed(player)||OriginalNpcService.isConversing(player)||TurnBattleManager.isInBattle(player)
                ||OriginalWorldTransferService.pending(player)||OriginalNpcProgress.get(player.server).switchValue(56)) return;
        var trigger=actor(player.serverLevel(),SOURCE);
        if (trigger==null) return;
        boolean saved=OriginalNpcProgress.get(player.server).conversation(player.getUUID(),SOURCE)!=null;
        if (saved) {
            var checkpoint=OriginalNpcProgress.get(player.server).conversation(player.getUUID(),SOURCE);
            var interpreter=checkpoint.getAsJsonObject("interpreter");
            if (interpreter==null) return;
            var pending=interpreter.get("pending");
            if (pending!=null&&pending.isJsonObject()&&"BLOCKED".equals(pending.getAsJsonObject().get("type").getAsString())) return;
        }
        if (saved&&player.distanceToSqr(trigger)<=64D||!saved&&Math.abs(player.getX()-132.5D)<0.65D
                &&Math.abs(player.getY()+18D)<0.1D&&player.getZ()>=208D&&player.getZ()<209D) {
            OriginalNpcService.interact(player,trigger);
        }
    }
}
