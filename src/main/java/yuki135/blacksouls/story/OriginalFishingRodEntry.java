package yuki135.blacksouls.story;

import yuki135.blacksouls.BlackSouls;
import yuki135.blacksouls.combat.TurnBattleManager;
import yuki135.blacksouls.compat.scene.OriginalNpcPlacementData;
import yuki135.blacksouls.compat.scene.OriginalNpcPlacements;
import yuki135.blacksouls.entity.EntityOriginalNpc;
import yuki135.blacksouls.util.HokoniwaDestination;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = BlackSouls.MODID)
public final class OriginalFishingRodEntry {
    public static final String EVENT_ID = "map031_ev023";
    public static final BlockPos NATIVE_BLOCK = new BlockPos(1113, -21, -3);
    public static final Vec3 DISPLAY_ORIGIN = new Vec3(1113.5D, -21.9375D, -2.5D);
    public static final Vec3 CONTROLLER_POSITION = new Vec3(1113.5D, -20.0D, -2.5D);
    private static final OriginalNpcPlacements.Site SITE = new OriginalNpcPlacements.Site(EVENT_ID,
            CONTROLLER_POSITION, 90.0F, "minecraft:barrier", List.of(
            landmark(1113, -21, -3, "minecraft:barrier", Map.of()),
            landmark(1114, -22, -4, "dustydecorations:cod_barrel", Map.of("facing", "north")),
            landmark(1114, -21, -4, "minecraft:barrier", Map.of()),
            landmark(1146, -22, -3, "blacksouls2:l_lu_deng_1", Map.of("facing", "north")),
            landmark(1146, -19, -3, "blacksouls2:l_lu_deng_3", Map.of("facing", "north")),
            landmark(1146, -28, -17, "minecraft:command_block", Map.of("facing", "north", "conditional", "false"))));

    private OriginalFishingRodEntry() {
    }

    public static List<OriginalNpcPlacements.Site> sites() {
        return List.of(SITE);
    }

    public static boolean handles(String identity) {
        return EVENT_ID.equals(identity);
    }

    private static OriginalNpcPlacements.Landmark landmark(int x, int y, int z, String block, Map<String, String> properties) {
        return new OriginalNpcPlacements.Landmark(new BlockPos(x, y, z), block, properties);
    }

    public static boolean matches(ServerLevel level) {
        return OriginalNpcPlacements.hasDeployedMap(level) && OriginalNpcPlacements.chunksReady(level, SITE)
                && SITE.landmarks().stream().allMatch(landmark -> landmark.matches(level));
    }

    private static List<EntityOriginalNpc> sourceActors(ServerLevel level) {
        List<EntityOriginalNpc> result = new ArrayList<>();
        for (Entity entity : level.getAllEntities()) {
            if (entity instanceof EntityOriginalNpc actor && !actor.isRemoved() && handles(actor.getNpcId())) result.add(actor);
        }
        return result;
    }

    private static EntityOriginalNpc recordedActor(ServerLevel level) {
        var uuid = OriginalNpcPlacementData.get(level).entityId(EVENT_ID);
        if (uuid == null || !(level.getEntity(uuid) instanceof EntityOriginalNpc actor) || !actor.isAlive()
                || actor.isRemoved() || !handles(actor.getNpcId())
                || actor.position().distanceToSqr(CONTROLLER_POSITION) > 1.0E-8D) return null;
        var actors = sourceActors(level);
        return actors.size() == 1 && actors.get(0) == actor ? actor : null;
    }

    public static EntityOriginalNpc resolve(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        if (!player.isAlive() || player.isSpectator() || TurnBattleManager.isInBattle(player)
                || player.position().distanceToSqr(DISPLAY_ORIGIN) > 4096.0D || !matches(level)) return null;
        var data = OriginalNpcPlacementData.get(level);
        if (data.isInstalled(EVENT_ID)) return recordedActor(level);
        if (!sourceActors(level).isEmpty()) return null;
        OriginalNpcPlacements.install(player, EVENT_ID);
        return recordedActor(level);
    }

    public static boolean isBound(ServerPlayer player, EntityOriginalNpc actor) {
        return handles(actor.getNpcId()) && player.level() == actor.level() && player.isAlive() && !player.isSpectator()
                && player.position().distanceToSqr(DISPLAY_ORIGIN) <= 64.0D && player.distanceToSqr(actor) <= 64.0D
                && matches(player.serverLevel())
                && recordedActor(player.serverLevel()) == actor;
    }

    public static boolean tryInteract(ServerPlayer player) {
        if (player.position().distanceToSqr(DISPLAY_ORIGIN) > 64.0D) return false;
        EntityOriginalNpc actor = resolve(player);
        if (actor == null || !isBound(player, actor)) return false;
        OriginalNpcService.interact(player, actor);
        return true;
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.isCanceled() || event.getHand() != InteractionHand.MAIN_HAND
                || !HokoniwaDestination.DIMENSION.equals(event.getLevel().dimension())
                || !NATIVE_BLOCK.equals(event.getPos()) || !event.getLevel().getBlockState(event.getPos()).is(Blocks.BARRIER)) return;
        if (event.getEntity() instanceof ServerPlayer player && tryInteract(player)) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.END && event.player instanceof ServerPlayer player
                && player.tickCount % 20 == 0) resolve(player);
    }
}
