package com.BlackSouls.BlackSoulsMod.story;

import com.BlackSouls.BlackSoulsMod.BlackSouls;
import com.BlackSouls.BlackSoulsMod.capability.OriginalNpcProgress;
import com.BlackSouls.BlackSoulsMod.capability.OriginalAltarGateData;
import com.BlackSouls.BlackSoulsMod.compat.scene.OriginalNpcPlacementData;
import com.BlackSouls.BlackSoulsMod.compat.scene.OriginalNpcPlacements;
import com.BlackSouls.BlackSoulsMod.entity.EntityOriginalNpc;
import com.BlackSouls.BlackSoulsMod.util.BSOriginalNpcData;
import com.BlackSouls.BlackSoulsMod.util.OriginalWorldEventData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Mod.EventBusSubscriber(modid = BlackSouls.MODID)
public final class OriginalAltarEntry {
    public static final String LEVER_EVENT = "map061_ev006";
    public static final BlockPos LEVER = new BlockPos(472, -59, -483);
    private static final List<OriginalNpcPlacements.Site> SITES = createSites();

    private OriginalAltarEntry() {
    }

    public static List<OriginalNpcPlacements.Site> sites() {
        return SITES;
    }

    private static List<OriginalNpcPlacements.Site> createSites() {
        List<OriginalNpcPlacements.Site> sites = new ArrayList<>();
        sites.add(new OriginalNpcPlacements.Site(LEVER_EVENT, new Vec3(472.5D, -59.9375D, -482.5D), 180.0F,
                "minecraft:green_carpet", List.of(
                landmark(472, -59, -482, "minecraft:stone_bricks", Map.of()),
                landmark(474, -60, -486, "cfm:oak_park_bench", Map.of("facing", "north", "type", "left")),
                landmark(469, -60, -490, "cfm:oak_park_bench", Map.of("facing", "north", "type", "left"))), true));
        for (int event : List.of(96, 110, 111, 112, 113)) {
            int x = event == 96 ? 468 : 577 - event;
            sites.add(new OriginalNpcPlacements.Site("map034_ev%03d".formatted(event), new Vec3(x + 0.5D, -37, -500.5D), 0,
                    "blacksouls2:jiao_tang_di_ban", hallLandmarks()));
        }
        for (int event = 32; event <= 36; event++) {
            int x = switch (event) {case 32 -> 466;case 33 -> 464;case 34 -> 465;case 35 -> 467;default -> 468;};
            sites.add(new OriginalNpcPlacements.Site("map062_ev%03d".formatted(event), new Vec3(x + 0.5D, -37, -498.5D), 180,
                    "blacksouls2:jiao_tang_di_ban", hallLandmarks()));
        }
        return List.copyOf(sites);
    }

    private static List<OriginalNpcPlacements.Landmark> hallLandmarks() {
        return List.of(
                landmark(472, -38, -510, "cfm:oak_park_bench", Map.of("facing", "north", "type", "left")),
                landmark(462, -38, -510, "cfm:oak_park_bench", Map.of("facing", "north", "type", "left")),
                landmark(472, -38, -518, "cfm:oak_park_bench", Map.of("facing", "north", "type", "left")),
                landmark(466, -38, -498, "blacksouls2:jiao_tang_di_ban", Map.of()));
    }

    private static OriginalNpcPlacements.Landmark landmark(int x, int y, int z, String block, Map<String, String> properties) {
        return new OriginalNpcPlacements.Landmark(new BlockPos(x, y, z), block, properties);
    }

    public static boolean matches(ServerLevel level) {
        if (!OriginalNpcPlacements.hasDeployedMap(level)) return false;
        for (var site : SITES) {
            if (!OriginalNpcPlacements.chunksReady(level, site) || site.landmarks().stream().anyMatch(landmark -> !landmark.matches(level))) return false;
        }
        var lever = level.getBlockState(LEVER);
        if (!lever.is(Blocks.LEVER) || lever.getValue(LeverBlock.FACE) != AttachFace.WALL
                || lever.getValue(LeverBlock.FACING) != Direction.NORTH) return false;
        for (int x = 464; x <= 468; x++) {
            if (!net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(level.getBlockState(new BlockPos(x, -38, -501)).getBlock())
                    .toString().equals("blacksouls2:jiao_tang_di_ban")) return false;
        }
        return true;
    }

    public static boolean synchronize(ServerLevel level) {
        if (!matches(level)) return false;
        boolean open = OriginalNpcProgress.get(level.getServer()).switchValue(41);
        if (!OriginalAltarGateData.get(level).synchronize(level, open)) return false;
        var state = level.getBlockState(LEVER);
        if (state.getValue(LeverBlock.POWERED) != open) level.setBlock(LEVER, state.setValue(LeverBlock.POWERED, open), 3);
        return true;
    }

    public static boolean tryInteract(ServerPlayer player) {
        if (!player.isAlive() || player.isSpectator() || player.distanceToSqr(LEVER.getCenter()) > 64
                || !matches(player.serverLevel())) return false;
        synchronize(player.serverLevel());
        OriginalNpcPlacements.install(player, LEVER_EVENT);
        var actor = resolve(player.serverLevel(), LEVER_EVENT);
        if (actor == null) return false;
        OriginalNpcService.interact(player, actor);
        return true;
    }

    private static EntityOriginalNpc resolve(ServerLevel level, String source) {
        var id = OriginalNpcPlacementData.get(level).entityId(source);
        if (id == null || !(level.getEntity(id) instanceof EntityOriginalNpc npc) || !npc.getNpcId().equals(source)
                || !npc.isAlive() || npc.isRemoved()) return null;
        var site = SITES.stream().filter(candidate -> candidate.npcId().equals(source)).findFirst().orElse(null);
        return site != null && npc.position().distanceToSqr(site.position()) < 0.015625D ? npc : null;
    }

    public static boolean isBound(ServerPlayer player, EntityOriginalNpc actor) {
        return player.level() == actor.level() && matches(player.serverLevel())
                && resolve(player.serverLevel(), actor.getNpcId()) == actor;
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getHand() == InteractionHand.MAIN_HAND && event.getPos().equals(LEVER)
                && event.getEntity() instanceof ServerPlayer player && tryInteract(player)) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)
                || player.isSpectator() || !player.isAlive() || player.position().distanceToSqr(LEVER.getCenter()) > 4096
                || !synchronize(player.serverLevel())) return;
        if (player.tickCount % 20 == 0) for (var site : SITES) OriginalNpcPlacements.install(player, site.npcId());
        if (OriginalNpcService.isConversing(player) || com.BlackSouls.BlackSoulsMod.combat.TurnBattleManager.isInBattle(player)) return;
        BlockPos position = player.blockPosition();
        if (position.getY() != -37 || position.getX() < 464 || position.getX() > 468) return;
        boolean entering = position.getZ() == -501 && player.getDeltaMovement().z >= 0;
        boolean leaving = position.getZ() == -499 && player.getDeltaMovement().z <= 0;
        if (!entering && !leaving || entering && !OriginalNpcProgress.get(player.server).switchValue(41)) return;
        var site = SITES.stream().filter(candidate -> !candidate.npcId().equals(LEVER_EVENT)
                && BlockPos.containing(candidate.position()).equals(position)).findFirst().orElse(null);
        if (site == null) return;
        OriginalNpcPlacements.install(player, site.npcId());
        var actor = resolve(player.serverLevel(), site.npcId());
        if (actor != null) OriginalNpcService.interact(player, actor);
    }

    public static int transfer(ServerPlayer player, EntityOriginalNpc source, int map, int x, int y, int direction, int fade) {
        if (!matches(player.serverLevel()) || !OriginalWorldEventData.isNativeBlock(source.getNpcId())
                || resolve(player.serverLevel(), source.getNpcId()) != source) throw new UnsupportedOperationException("altar_transfer_identity");
        BSOriginalNpcData.Entry definition = BSOriginalNpcData.getEvent(source.getNpcId());
        if (definition.mapId() == 34 && map == 62 && x == 24 && y == 30 && direction == 8 && fade == 0
                && OriginalNpcProgress.get(player.server).switchValue(41)) {
            return OriginalWorldTransferService.begin(player, source, new Vec3(466.5D, -37, -497.5D), 0, fade);
        }
        if (definition.mapId() == 62 && map == 34 && x == 27 && y == 28 && direction == 0 && fade == 0) {
            return OriginalWorldTransferService.begin(player, source, new Vec3(466.5D, -37, -501.5D), player.getYRot(), fade);
        }
        throw new UnsupportedOperationException("altar_transfer_destination:" + map + ":" + x + ":" + y);
    }
}
