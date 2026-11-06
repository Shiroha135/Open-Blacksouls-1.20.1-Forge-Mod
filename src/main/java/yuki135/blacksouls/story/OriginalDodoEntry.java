package yuki135.blacksouls.story;

import yuki135.blacksouls.BlackSouls;
import yuki135.blacksouls.combat.TurnBattleManager;
import yuki135.blacksouls.compat.scene.OriginalNpcPlacementData;
import yuki135.blacksouls.compat.scene.OriginalNpcPlacements;
import yuki135.blacksouls.entity.EntityOriginalNpc;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.CommandBlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = BlackSouls.MODID)
public final class OriginalDodoEntry {
    public static final Vec3 CEMETERY_ARRIVAL = new Vec3(818.5D, 11.0D, -604.5D);
    public static final Vec3 POOL_CEMETERY_ARRIVAL = new Vec3(822.5D, 11.0625D, -604.5D);
    public static final Vec3 CAVE_ARRIVAL = new Vec3(853.5D, -59.0D, -622.5D);
    public static final Vec3 POOL_CAVE_ARRIVAL = new Vec3(873.5D, 11.1875D, -586.5D);
    private static final List<OriginalNpcPlacements.Site> SITES = List.of(
            site("map037_ev012", new Vec3(821.5D, 11.0625D, -604.5D), "minecraft:moss_carpet", true, cemeteryLandmarks()),
            site("map037_ev019", new Vec3(821.5D, 11.0D, -605.5D), "blacksouls2:g_gu_tou", false, cemeteryLandmarks()),
            site("map033_ev019", new Vec3(819.5D, 11.0D, -604.5D), "minecraft:grass_block", false, cemeteryLandmarks()),
            site("map037_ev020", new Vec3(873.5D, 11.0D, -585.5D), "minecraft:coarse_dirt", false, caveLandmarks()),
            site("map063_ev017", new Vec3(853.5D, -59.0D, -623.5D), "minecraft:deepslate", false, caveLandmarks()));

    private OriginalDodoEntry() {
    }

    public static List<OriginalNpcPlacements.Site> sites() {
        return SITES;
    }

    public static boolean handles(String identity) {
        return SITES.stream().anyMatch(site -> site.npcId().equals(identity));
    }

    private static OriginalNpcPlacements.Site site(String identity, Vec3 position, String support, boolean shaped,
                                                   List<OriginalNpcPlacements.Landmark> landmarks) {
        return new OriginalNpcPlacements.Site(identity, position, 180.0F, support, landmarks, shaped);
    }

    private static List<OriginalNpcPlacements.Landmark> cemeteryLandmarks() {
        return List.of(landmark(818, 10, -605, "minecraft:grass_block", Map.of()),
                landmark(821, 10, -605, "blacksouls2:g_gu_tou", Map.of("facing", "north")),
                landmark(822, 11, -605, "minecraft:moss_carpet", Map.of()));
    }

    private static List<OriginalNpcPlacements.Landmark> caveLandmarks() {
        return List.of(landmark(873, 10, -586, "minecraft:coarse_dirt", Map.of()),
                landmark(873, 11, -587, "mcwtrpdoors:jungle_cottage_trapdoor", Map.of("half", "bottom", "open", "false", "facing", "east")),
                landmark(873, 9, -584, "minecraft:command_block", Map.of()),
                landmark(853, -61, -624, "minecraft:command_block", Map.of()),
                landmark(853, -60, -623, "minecraft:deepslate", Map.of()));
    }

    private static OriginalNpcPlacements.Landmark landmark(int x, int y, int z, String block, Map<String, String> properties) {
        return new OriginalNpcPlacements.Landmark(new BlockPos(x, y, z), block, properties);
    }

    private static boolean cave(String identity) {
        return identity.equals("map037_ev020") || identity.equals("map063_ev017");
    }

    private static boolean originalCommand(ServerLevel level, BlockPos position, String command) {
        return level.getBlockEntity(position) instanceof CommandBlockEntity block
                && command.equals(block.getCommandBlock().getCommand());
    }

    private static boolean matches(ServerLevel level, OriginalNpcPlacements.Site site) {
        return OriginalNpcPlacements.hasDeployedMap(level) && OriginalNpcPlacements.chunksReady(level, site)
                && site.landmarks().stream().allMatch(landmark -> landmark.matches(level))
                && (!cave(site.npcId()) || originalCommand(level, new BlockPos(873, 9, -584), "tp @p 853.5 -59 -622.5")
                && originalCommand(level, new BlockPos(853, -61, -624), "tp @p 873.5 11 -584.5"));
    }

    private static EntityOriginalNpc resolve(ServerLevel level, OriginalNpcPlacements.Site site) {
        var uuid = OriginalNpcPlacementData.get(level).entityId(site.npcId());
        return uuid != null && level.getEntity(uuid) instanceof EntityOriginalNpc actor && actor.isAlive() && !actor.isRemoved()
                && actor.getNpcId().equals(site.npcId()) && actor.position().distanceToSqr(site.position()) < 0.015625D ? actor : null;
    }

    public static boolean isBound(ServerPlayer player, EntityOriginalNpc actor) {
        var site = SITES.stream().filter(candidate -> candidate.npcId().equals(actor.getNpcId())).findFirst().orElse(null);
        return site != null && player.level() == actor.level() && matches(player.serverLevel(), site)
                && resolve(player.serverLevel(), site) == actor;
    }

    public static boolean claimNative(ServerPlayer player, String identity) {
        if (!cave(identity)) return false;
        return tryEnter(player, identity);
    }

    public static boolean tryEnter(ServerPlayer player, String identity) {
        var site = SITES.stream().filter(candidate -> candidate.npcId().equals(identity)).findFirst().orElse(null);
        if (site == null || !player.isAlive() || player.isSpectator() || player.position().distanceToSqr(site.position()) > 64.0D
                || !matches(player.serverLevel(), site)) return false;
        OriginalNpcPlacements.install(player, identity);
        EntityOriginalNpc actor = resolve(player.serverLevel(), site);
        if (actor == null) return false;
        if (!OriginalNpcService.isConversing(player) && !TurnBattleManager.isInBattle(player)) OriginalNpcService.interact(player, actor);
        return true;
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)
                || !player.isAlive() || player.isSpectator() || !OriginalNpcPlacements.hasDeployedMap(player.serverLevel())) return;
        if (player.tickCount % 20 == 0) {
            for (var site : SITES) if (player.position().distanceToSqr(site.position()) <= 4096.0D && matches(player.serverLevel(), site)) {
                OriginalNpcPlacements.install(player, site.npcId());
            }
        }
        if (OriginalNpcService.isConversing(player) || TurnBattleManager.isInBattle(player)) return;
        BlockPos position = player.blockPosition();
        for (var site : SITES) {
            BlockPos trigger = BlockPos.containing(site.position());
            boolean contact = position.getX() == trigger.getX() && position.getZ() == trigger.getZ();
            if (site.npcId().equals("map037_ev020")) contact |= position.getX() == 873 && position.getZ() >= -585 && position.getZ() <= -584;
            if (contact && Math.abs(player.getY() - site.position().y) < 0.5D && tryEnter(player, site.npcId())) return;
        }
    }

    public static int transfer(ServerPlayer player, EntityOriginalNpc source, int map, int x, int y, int direction, int fade) {
        if (!isBound(player, source) || fade != 0) throw new UnsupportedOperationException("dodo_transfer_identity");
        String identity = source.getNpcId();
        Vec3 destination;
        float yaw;
        if ((identity.equals("map037_ev012") || identity.equals("map037_ev019")) && map == 33 && x == 1 && y == 89 && direction == 6) {
            destination = CEMETERY_ARRIVAL; yaw = 90.0F;
        } else if (identity.equals("map033_ev019") && map == 37 && x == 63 && y == 56 && direction == 0) {
            destination = POOL_CEMETERY_ARRIVAL; yaw = player.getYRot();
        } else if (identity.equals("map037_ev020") && map == 63 && x == 18 && y == 53 && direction == 8) {
            destination = CAVE_ARRIVAL; yaw = 0.0F;
        } else if (identity.equals("map063_ev017") && map == 37 && x == 12 && y == 38 && direction == 2) {
            destination = POOL_CAVE_ARRIVAL; yaw = 180.0F;
        } else {
            throw new UnsupportedOperationException("dodo_transfer_destination:" + map + ":" + x + ":" + y);
        }
        return OriginalWorldTransferService.begin(player, source, destination, yaw, fade);
    }

    public static boolean supportsShapedArrival(ServerPlayer player, Vec3 destination, AABB bounds) {
        ServerLevel level = player.serverLevel();
        BlockPos floor = BlockPos.containing(destination.add(0, -0.02D, 0));
        var state = level.getBlockState(floor);
        boolean known = destination.equals(POOL_CEMETERY_ARRIVAL) && state.is(Blocks.MOSS_CARPET)
                || destination.equals(POOL_CAVE_ARRIVAL) && BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString()
                .equals("mcwtrpdoors:jungle_cottage_trapdoor") && caveLandmarks().get(1).matches(level);
        return known && !level.noCollision(player, bounds.move(0, -0.02D, 0));
    }
}
